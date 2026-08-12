package com.example.gitprocessor.service;

import com.example.gitprocessor.exception.InvalidExcelFormatException;
import com.example.gitprocessor.exception.ProcessingException;
import com.example.gitprocessor.model.Environment;
import com.example.gitprocessor.model.ExcelRow;
import com.example.gitprocessor.model.FileValidationResult;
import com.example.gitprocessor.model.ProcessingResult;
import com.example.gitprocessor.model.ProcessingSummary;
import com.example.gitprocessor.util.GitTypeDetector;
import com.example.gitprocessor.util.TypeValidator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelProcessorService {

    private static final Logger log = LoggerFactory.getLogger(ExcelProcessorService.class);
    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── 9 Required columns (lowercase key -> display name) ────────────────────
    private static final LinkedHashMap<String, String> REQUIRED_COLUMNS = new LinkedHashMap<>();
    static {
        REQUIRED_COLUMNS.put("sl no",                     "Sl No");
        REQUIRED_COLUMNS.put("files",                     "Files");
        REQUIRED_COLUMNS.put("type",                      "Type");
        REQUIRED_COLUMNS.put("pushed by",                 "Pushed By");
        REQUIRED_COLUMNS.put("release branch",            "Release Branch");
        REQUIRED_COLUMNS.put("target env",                "Target Env");
        REQUIRED_COLUMNS.put("date",                      "Date");
        REQUIRED_COLUMNS.put("team",                      "Team");
        REQUIRED_COLUMNS.put("cr/inc no/filepush reason", "CR/INC No/FilePush reason");
    }

    // Mandatory DATA fields (must not be blank in each row)
    private static final String[][] MANDATORY_FIELDS = {
        {"files",          "Files"},
        {"type",           "Type"},
        {"pushedBy",       "Pushed By"},
        {"releaseBranch",  "Release Branch"},
        {"targetEnv",      "Target Env"},
        {"team",           "Team"},
        {"crIncReason",    "CR/INC No/FilePush reason"}
    };

    // Output Excel headers
    private static final String[] OUTPUT_HEADERS = {
        "Sl No", "Files", "Type", "Pushed By", "Release Branch", "Target Env",
        "Date", "Team", "CR/INC No/FilePush reason",
        "GIT Type", "Validation Status", "Validation Reason", "Source File"
    };

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════════

    public ProcessingResult process(List<MultipartFile> files, Environment environment) {
        List<String> logMessages = new ArrayList<>();
        LocalDateTime startTime  = LocalDateTime.now();
        int fileCount            = files.size();

        logMessages.add(logEntry("Files Uploaded : " + fileCount));
        logMessages.add(logEntry("Environment    : " + environment.getDisplayName()));

        List<ExcelRow> allRows = new ArrayList<>();
        for (MultipartFile file : files) {
            String fname = Objects.toString(file.getOriginalFilename(), "unknown.xlsx");
            logMessages.add(logEntry("Parsing : " + fname));
            List<ExcelRow> fileRows = parseExcel(file, fname, logMessages);
            allRows.addAll(fileRows);
            logMessages.add(logEntry("  -> " + fileRows.size() + " rows from " + fname));
        }
        logMessages.add(logEntry("Total Merged Rows : " + allRows.size()));

        // Validate mandatory fields + process valid rows
        validateMandatoryFields(allRows);
        processValidRows(allRows);

        int rejected = (int) allRows.stream()
                .filter(r -> "Rejected".equals(r.getValidationStatus())).count();
        logMessages.add(logEntry("Valid Rows: " + (allRows.size() - rejected) +
                " | Rejected Rows: " + rejected));

        byte[] excelBytes      = generateOutputExcel(allRows);
        String customerContent = generateCustomerGitContent(allRows);
        String productContent  = generateProductGitContent(allRows);
        String ruleContent     = generateRuleGitContent(allRows, environment.getBaseUrl());
        String mdtContent      = generateMdtContent(allRows);
        String jarsContent     = generateJarsNameContent(allRows);
        logMessages.add(logEntry("Files Generated: 1 Excel + 5 text files"));

        ProcessingSummary summary = buildSummary(allRows, environment.getDisplayName(), fileCount);
        logMessages.add(logEntry("Download Ready - " + summary.getTotalRows() + " rows"));

        ProcessingResult result = new ProcessingResult();
        result.setRows(allRows);
        result.setProcessedExcelBytes(excelBytes);
        result.setCustomerGitContent(customerContent);
        result.setProductGitContent(productContent);
        result.setRuleGitContent(ruleContent);
        result.setMdtContent(mdtContent);
        result.setJarsNameContent(jarsContent);
        result.setSummary(summary);
        result.setLogMessages(logMessages);
        result.setProcessedAt(startTime);
        result.setFilesUploaded(fileCount);
        result.setEnvironment(environment.getDisplayName());
        return result;
    }

    public ProcessingResult processFromStoredFiles(List<Path> filePaths,
                                                    List<String> fileNames,
                                                    Environment environment) {
        List<String> logMessages = new ArrayList<>();
        LocalDateTime startTime  = LocalDateTime.now();
        int fileCount            = filePaths.size();

        logMessages.add(logEntry("Import from Deployment Requests : " + fileCount + " file(s)"));
        logMessages.add(logEntry("Environment : " + environment.getDisplayName()));

        List<ExcelRow> allRows = new ArrayList<>();
        for (int i = 0; i < filePaths.size(); i++) {
            Path   path  = filePaths.get(i);
            String fname = fileNames.get(i);
            logMessages.add(logEntry("Parsing : " + fname));
            try (InputStream is = Files.newInputStream(path)) {
                List<ExcelRow> fileRows = parseExcelFromStream(is, fname, logMessages);
                allRows.addAll(fileRows);
                logMessages.add(logEntry("  -> " + fileRows.size() + " rows from " + fname));
            } catch (IOException e) {
                throw new ProcessingException("Failed to read '" + fname + "': " + e.getMessage(), e);
            }
        }

        validateMandatoryFields(allRows);
        processValidRows(allRows);

        byte[] excelBytes      = generateOutputExcel(allRows);
        String customerContent = generateCustomerGitContent(allRows);
        String productContent  = generateProductGitContent(allRows);
        String ruleContent     = generateRuleGitContent(allRows, environment.getBaseUrl());
        String mdtContent      = generateMdtContent(allRows);
        String jarsContent     = generateJarsNameContent(allRows);

        ProcessingSummary summary = buildSummary(allRows, environment.getDisplayName(), fileCount);

        ProcessingResult result = new ProcessingResult();
        result.setRows(allRows);
        result.setProcessedExcelBytes(excelBytes);
        result.setCustomerGitContent(customerContent);
        result.setProductGitContent(productContent);
        result.setRuleGitContent(ruleContent);
        result.setMdtContent(mdtContent);
        result.setJarsNameContent(jarsContent);
        result.setSummary(summary);
        result.setLogMessages(logMessages);
        result.setProcessedAt(startTime);
        result.setFilesUploaded(fileCount);
        result.setEnvironment(environment.getDisplayName());
        return result;
    }

    /** Validates a file (column check + row count). */
    public FileValidationResult validateFile(MultipartFile file) {
        FileValidationResult vr = new FileValidationResult();
        String fname = Objects.toString(file.getOriginalFilename(), "unknown.xlsx");
        vr.setFileName(fname);
        vr.setRowCount(-1);

        String fnLower = fname.toLowerCase();
        if (!fnLower.endsWith(".xlsx") && !fnLower.endsWith(".xls")) {
            vr.setValid(false);
            vr.setError("Not a supported file type. Only .xlsx and .xls are accepted.");
            return vr;
        }

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = fnLower.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            try {
                validateColumns(sheet, fmt);
                vr.setValid(true);
            } catch (InvalidExcelFormatException e) {
                vr.setValid(false);
                vr.setMissingColumns(e.getMissingColumns());
                workbook.close();
                return vr;
            }

            int count = 0;
            for (int ri = sheet.getFirstRowNum() + 1; ri <= sheet.getLastRowNum(); ri++) {
                Row row = sheet.getRow(ri);
                if (row != null && !isBlankRow(row, fmt)) count++;
            }
            vr.setRowCount(count);
            workbook.close();
        } catch (IOException e) {
            vr.setValid(false);
            vr.setError("Could not read file: " + e.getMessage());
        }
        return vr;
    }

    /** Generates the new 9-column Template.xlsx */
    public byte[] generateTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Input");
            CellStyle headerStyle = buildHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Sl No", "Files", "Type", "Pushed By", "Release Branch",
                "Target Env", "Date", "Team", "CR/INC No/FilePush reason"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }
            sheet.setColumnWidth(8, 9000); // wider for last column

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ProcessingException("Failed to generate template: " + e.getMessage(), e);
        }
    }

    /**
     * Validates rows for deployment request upload.
     * Returns a list of error messages (empty = all rows valid).
     * Format: "Row N : Field cannot be blank"
     */
    public List<String> validateRowsForDeployment(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        String fname = Objects.toString(file.getOriginalFilename(), "unknown.xlsx");
        String fnLower = fname.toLowerCase();

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = fnLower.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            // Column validation first
            validateColumns(sheet, fmt);

            // Find column indices dynamically
            Map<String, Integer> colMap = buildColumnMap(sheet, fmt);

            for (int ri = sheet.getFirstRowNum() + 1; ri <= sheet.getLastRowNum(); ri++) {
                Row row = sheet.getRow(ri);
                if (row == null || isBlankRow(row, fmt)) continue;

                int excelRowNum = ri + 1; // 1-based for user display
                List<String> rowErrors = new ArrayList<>();

                checkMandatory(row, colMap, fmt, "files",                     "Files",                     rowErrors);
                checkMandatory(row, colMap, fmt, "type",                      "Type",                      rowErrors);
                checkMandatory(row, colMap, fmt, "pushed by",                 "Pushed By",                 rowErrors);
                checkMandatory(row, colMap, fmt, "release branch",            "Release Branch",            rowErrors);
                checkMandatory(row, colMap, fmt, "target env",                "Target Env",                rowErrors);
                checkMandatory(row, colMap, fmt, "team",                      "Team",                      rowErrors);
                checkMandatory(row, colMap, fmt, "cr/inc no/filepush reason", "CR/INC No/FilePush reason", rowErrors);

                for (String err : rowErrors) {
                    errors.add("Row " + excelRowNum + " : " + err);
                }
            }
            workbook.close();
        } catch (InvalidExcelFormatException e) {
            errors.add("Invalid Excel Format. Missing columns: " + String.join(", ", e.getMissingColumns()));
        } catch (IOException e) {
            errors.add("Could not read file: " + e.getMessage());
        }
        return errors;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: PARSING
    // ══════════════════════════════════════════════════════════════════════════

    private List<ExcelRow> parseExcel(MultipartFile file, String sourceFileName, List<String> logMessages) {
        try (InputStream is = file.getInputStream()) {
            return parseExcelFromStream(is, sourceFileName, logMessages);
        } catch (IOException e) {
            throw new ProcessingException("Failed to open '" + sourceFileName + "': " + e.getMessage(), e);
        }
    }

    private List<ExcelRow> parseExcelFromStream(InputStream is, String sourceFileName, List<String> logMessages) {
        List<ExcelRow> rows = new ArrayList<>();
        String fnLower = sourceFileName.toLowerCase();

        try {
            Workbook workbook = fnLower.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            validateColumns(sheet, fmt);
            Map<String, Integer> colMap = buildColumnMap(sheet, fmt);

            for (int ri = sheet.getFirstRowNum() + 1; ri <= sheet.getLastRowNum(); ri++) {
                Row row = sheet.getRow(ri);
                if (row == null || isBlankRow(row, fmt)) continue;

                ExcelRow er = new ExcelRow();
                er.setSlNo(readInt(row, colMap.getOrDefault("sl no", 0), fmt, ri));
                er.setFiles(normalizeFiles(readStr(row, colMap.getOrDefault("files", 1), fmt)));
                er.setOriginalType(readStr(row, colMap.getOrDefault("type", 2), fmt));
                er.setPushedBy(readStr(row, colMap.getOrDefault("pushed by", 3), fmt));
                er.setReleaseBranch(readStr(row, colMap.getOrDefault("release branch", 4), fmt));
                er.setTargetEnv(readStr(row, colMap.getOrDefault("target env", 5), fmt));
                er.setDate(readStr(row, colMap.getOrDefault("date", 6), fmt));
                er.setTeam(readStr(row, colMap.getOrDefault("team", 7), fmt));
                er.setCrIncReason(readStr(row, colMap.getOrDefault("cr/inc no/filepush reason", 8), fmt));
                er.setSourceFile(sourceFileName);
                rows.add(er);
            }
            workbook.close();
        } catch (IOException e) {
            throw new ProcessingException("Failed to parse '" + sourceFileName + "': " + e.getMessage(), e);
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: MANDATORY FIELD VALIDATION (row-level)
    // ══════════════════════════════════════════════════════════════════════════

    private void validateMandatoryFields(List<ExcelRow> rows) {
        for (ExcelRow row : rows) {
            List<String> reasons = new ArrayList<>();
            if (isBlank(row.getFiles()))         reasons.add("Files cannot be blank");
            if (isBlank(row.getOriginalType()))  reasons.add("Type cannot be blank");
            if (isBlank(row.getPushedBy()))      reasons.add("Pushed By cannot be blank");
            if (isBlank(row.getReleaseBranch())) reasons.add("Release Branch cannot be blank");
            if (isBlank(row.getTargetEnv()))     reasons.add("Target Env cannot be blank");
            if (isBlank(row.getTeam()))          reasons.add("Team cannot be blank");
            if (isBlank(row.getCrIncReason()))   reasons.add("CR/INC No/FilePush reason cannot be blank");

            if (!reasons.isEmpty()) {
                row.setValidationStatus("Rejected");
                row.setValidationReason(String.join(", ", reasons));
                row.setGitType("");
                row.setFinalType(row.getOriginalType());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: PROCESS VALID ROWS (GIT type detection + type validation)
    // ══════════════════════════════════════════════════════════════════════════

    private void processValidRows(List<ExcelRow> rows) {
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) continue;

            String gitType = GitTypeDetector.detect(row.getFiles());
            row.setGitType(gitType);

            TypeValidator.ValidationResult vr =
                    TypeValidator.validate(row.getOriginalType(), row.getFiles(), gitType);
            row.setFinalType(vr.getFinalType());
            row.setValidationStatus(vr.getStatus());

            // Build reason for type corrections
            if ("Corrected".equals(vr.getStatus())) {
                row.setValidationReason("Type corrected from " +
                        row.getOriginalType() + " to " + vr.getFinalType());
            } else if ("Unable To Identify".equals(vr.getStatus())) {
                row.setValidationReason("Unable to determine GIT Type");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: OUTPUT EXCEL
    // ══════════════════════════════════════════════════════════════════════════

    private byte[] generateOutputExcel(List<ExcelRow> rows) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        try {
            Sheet sheet = workbook.createSheet("Processed");
            CellStyle headerStyle = buildHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < OUTPUT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(OUTPUT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ExcelRow er : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(er.getSlNo());
                row.createCell(1).setCellValue(safe(er.getFiles()));
                row.createCell(2).setCellValue(safe(er.getFinalType()));
                row.createCell(3).setCellValue(safe(er.getPushedBy()));
                row.createCell(4).setCellValue(safe(er.getReleaseBranch()));
                row.createCell(5).setCellValue(safe(er.getTargetEnv()));
                row.createCell(6).setCellValue(safe(er.getDate()));
                row.createCell(7).setCellValue(safe(er.getTeam()));
                row.createCell(8).setCellValue(safe(er.getCrIncReason()));
                row.createCell(9).setCellValue(safe(er.getGitType()));
                row.createCell(10).setCellValue(safe(er.getValidationStatus()));
                row.createCell(11).setCellValue(safe(er.getValidationReason()));
                row.createCell(12).setCellValue(safe(er.getSourceFile()));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ProcessingException("Failed to generate output Excel: " + e.getMessage(), e);
        } finally {
            workbook.dispose();
            try { workbook.close(); } catch (IOException ignored) {}
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: TEXT FILE GENERATION (only valid rows)
    // ══════════════════════════════════════════════════════════════════════════

    private String generateCustomerGitContent(List<ExcelRow> rows) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) continue;
            if (GitTypeDetector.CUSTOMER_GIT.equals(row.getGitType())) {
                String path = GitTypeDetector.extractCustomerPath(row.getFiles()).replace('\\', '/');
                if (!path.isBlank()) seen.add(path);
            }
        }
        StringBuilder sb = new StringBuilder();
        seen.forEach(p -> sb.append(p).append('\n'));
        return sb.toString();
    }

    private String generateProductGitContent(List<ExcelRow> rows) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) continue;
            if (GitTypeDetector.PRODUCT_GIT.equals(row.getGitType())) {
                String path = GitTypeDetector.extractProductPath(row.getFiles()).replace('\\', '/');
                if (!path.isBlank()) seen.add(path);
            }
        }
        StringBuilder sb = new StringBuilder();
        seen.forEach(p -> sb.append(p).append('\n'));
        return sb.toString();
    }

    private String generateRuleGitContent(List<ExcelRow> rows, String baseUrl) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) continue;
            if (GitTypeDetector.RULE_GIT.equals(row.getGitType())) {
                String ruleName = GitTypeDetector.extractRuleName(row.getFiles());
                if (!ruleName.isBlank()) seen.add(baseUrl + ruleName);
            }
        }
        StringBuilder sb = new StringBuilder();
        seen.forEach(url -> sb.append(url).append('\n'));
        return sb.toString();
    }

    private String generateMdtContent(List<ExcelRow> rows) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) continue;
            if (GitTypeDetector.MDT.equals(row.getGitType())) {
                String name = GitTypeDetector.extractMdtName(row.getFiles());
                if (!name.isBlank()) seen.add(name);
            }
        }
        StringBuilder sb = new StringBuilder();
        seen.forEach(n -> sb.append(n).append('\n'));
        return sb.toString();
    }

    /**
     * Extracts unique JAR names from Product_GIT and Customer_GIT rows.
     * Looks for paths containing /src/ or \src\ and takes the folder immediately before it.
     */
    private String generateJarsNameContent(List<ExcelRow> rows) {
        LinkedHashSet<String> jars = new LinkedHashSet<>();
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) continue;
            String gitType = row.getGitType();
            if ("Product_GIT".equals(gitType) || "Customer_GIT".equals(gitType)) {
                String jarName = extractJarName(row.getFiles());
                if (jarName != null && !jarName.isBlank()) {
                    jars.add(jarName);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        jars.forEach(j -> sb.append(j).append('\n'));
        return sb.toString();
    }

    /**
     * Extracts JAR name from a file path by finding the folder immediately before /src/ or \src\.
     * e.g. ".../BaNCSDomainData/src/main/java/..." → "BaNCSDomainData.jar"
     */
    private String extractJarName(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;
        // Normalize to forward slashes
        String normalized = filePath.replace('\\', '/');
        int srcIdx = normalized.indexOf("/src/");
        if (srcIdx <= 0) return null;
        // Get the part before /src/
        String before = normalized.substring(0, srcIdx);
        // Get the last segment (folder name)
        int lastSlash = before.lastIndexOf('/');
        String folderName = (lastSlash >= 0) ? before.substring(lastSlash + 1) : before;
        if (folderName.isBlank()) return null;
        return folderName + ".jar";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: SUMMARY
    // ══════════════════════════════════════════════════════════════════════════

    private ProcessingSummary buildSummary(List<ExcelRow> rows, String environment, int filesUploaded) {
        ProcessingSummary s = new ProcessingSummary();
        s.setTotalRows(rows.size());
        s.setEnvironment(environment);
        s.setFilesUploaded(filesUploaded);

        int rejected = 0;
        Set<String> uniqueJars = new HashSet<>();
        for (ExcelRow row : rows) {
            if ("Rejected".equals(row.getValidationStatus())) {
                rejected++;
                continue;
            }

            // GIT type counts (only valid rows)
            String git = row.getGitType();
            if (git != null) {
                switch (git) {
                    case "Customer_GIT" -> s.setCustomerGitCount(s.getCustomerGitCount() + 1);
                    case "Product_GIT"  -> s.setProductGitCount(s.getProductGitCount() + 1);
                    case "Rule_GIT"     -> s.setRuleGitCount(s.getRuleGitCount() + 1);
                    case "MDT"          -> s.setMdtCount(s.getMdtCount() + 1);
                    default             -> s.setUnknownCount(s.getUnknownCount() + 1);
                }
                // Count unique JARs from Product_GIT and Customer_GIT
                if ("Product_GIT".equals(git) || "Customer_GIT".equals(git)) {
                    String jarName = extractJarName(row.getFiles());
                    if (jarName != null && !jarName.isBlank()) {
                        uniqueJars.add(jarName);
                    }
                }
            }

            // Validation status counts (only valid rows)
            String vs = row.getValidationStatus();
            if (vs != null) {
                switch (vs) {
                    case "Correct"            -> s.setCorrectTypeCount(s.getCorrectTypeCount() + 1);
                    case "Corrected"          -> s.setCorrectedTypeCount(s.getCorrectedTypeCount() + 1);
                    case "Unable To Identify" -> s.setUnableToIdentifyCount(s.getUnableToIdentifyCount() + 1);
                }
            }
        }
        s.setRejectedRows(rejected);
        s.setValidRows(rows.size() - rejected);
        s.setUniqueJarsCount(uniqueJars.size());
        return s;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE: HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void validateColumns(Sheet sheet, DataFormatter fmt) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new InvalidExcelFormatException(new ArrayList<>(REQUIRED_COLUMNS.values()));
        }

        Set<String> found = new HashSet<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String val = fmt.formatCellValue(cell).trim().toLowerCase();
                if (!val.isEmpty()) found.add(val);
            }
        }

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : REQUIRED_COLUMNS.entrySet()) {
            if (!found.contains(entry.getKey())) {
                missing.add(entry.getValue());
            }
        }
        if (!missing.isEmpty()) {
            throw new InvalidExcelFormatException(missing);
        }
    }

    /** Build a map of lowercase-column-name -> column-index from the header row. */
    private Map<String, Integer> buildColumnMap(Sheet sheet, DataFormatter fmt) {
        Map<String, Integer> map = new HashMap<>();
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String val = fmt.formatCellValue(cell).trim().toLowerCase();
                    if (!val.isEmpty()) map.put(val, i);
                }
            }
        }
        return map;
    }

    private void checkMandatory(Row row, Map<String, Integer> colMap, DataFormatter fmt,
                                 String colKey, String displayName, List<String> errors) {
        Integer idx = colMap.get(colKey);
        if (idx == null) return;
        String val = readStr(row, idx, fmt);
        if (val.isBlank()) {
            errors.add(displayName + " cannot be blank");
        }
    }

    private String readStr(Row row, int col, DataFormatter fmt) {
        Cell cell = row.getCell(col);
        return (cell == null) ? "" : fmt.formatCellValue(cell).trim();
    }

    private int readInt(Row row, int col, DataFormatter fmt, int fallback) {
        Cell cell = row.getCell(col);
        if (cell == null) return fallback;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        String val = fmt.formatCellValue(cell).trim();
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return fallback; }
    }

    private boolean isBlankRow(Row row, DataFormatter fmt) {
        for (int c = 0; c <= 8; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !fmt.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private String normalizeFiles(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\u00A0\\u2002\\u2003\\u2009\\u202F\\t]+", " ")
                .replaceAll(" {2,}", " ").trim();
    }

    private String safe(String s) { return s != null ? s : ""; }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String logEntry(String msg) { return LocalDateTime.now().format(LOG_FMT) + " - " + msg; }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}