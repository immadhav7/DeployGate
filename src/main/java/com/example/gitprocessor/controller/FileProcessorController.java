package com.example.gitprocessor.controller;

import com.example.gitprocessor.exception.InvalidExcelFormatException;
import com.example.gitprocessor.exception.ProcessingException;
import com.example.gitprocessor.model.Environment;
import com.example.gitprocessor.model.FileValidationResult;
import com.example.gitprocessor.model.ProcessingResult;
import com.example.gitprocessor.service.ExcelProcessorService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MVC controller that:
 *   GET  /                          → serves the main UI page
 *   POST /api/process               → uploads + processes the Excel file
 *   GET  /api/download/template     → downloads empty Template.xlsx
 *   GET  /api/download/excel        → streams Processed_File.xlsx
 *   GET  /api/download/customer-git → streams Customer_GIT_Files.txt
 *   GET  /api/download/product-git  → streams Product_GIT_Files.txt
 *   GET  /api/download/rule-git     → streams Rule_GIT_Files.txt
 *   GET  /api/download/mdt          → streams MDT_Name.txt
 *   POST /api/reset                 → clears the session
 *
 * All processed artifacts are stored in the HTTP session (keyed by
 * SESSION_RESULT_KEY) so that download requests can serve the exact
 * byte arrays produced during the last /api/process call.
 */
@Controller
public class FileProcessorController {

    private static final Logger log = LoggerFactory.getLogger(FileProcessorController.class);

    /** Session key under which the ProcessingResult is stored. */
    private static final String SESSION_RESULT_KEY = "processingResult";

    private final ExcelProcessorService processorService;

    public FileProcessorController(ExcelProcessorService processorService) {
        this.processorService = processorService;
    }

    // ── UI page ───────────────────────────────────────────────────────────────

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // ── Process (upload + analyse) ────────────────────────────────────────────

    /**
     * Validates each uploaded file (column check + row count) without processing.
     * Returns a per-file grid so the UI can show status before the user clicks Process.
     */
    @PostMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateFiles(
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        Map<String, Object> response = new HashMap<>();

        if (files == null || files.isEmpty()) {
            response.put("success", false);
            response.put("message", "No files received.");
            return ResponseEntity.badRequest().body(response);
        }

        List<FileValidationResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(processorService.validateFile(file));
        }

        boolean allValid = results.stream().allMatch(FileValidationResult::isValid);
        response.put("success", true);
        response.put("allValid", allValid);
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    /**
     * Accepts multiple multipart Excel uploads, merges them, runs the full
     * processing pipeline, stores the result in the session, and returns JSON.
     */
    @PostMapping("/api/process")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processFile(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam("environment") String environmentName,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        // ── Environment validation ────────────────────────────────────────────
        Environment environment = Environment.fromName(environmentName);
        if (environment == null) {
            response.put("success", false);
            response.put("message", "Please select a valid environment.");
            return ResponseEntity.badRequest().body(response);
        }

        // ── File list validation ──────────────────────────────────────────────
        if (files == null || files.isEmpty()) {
            response.put("success", false);
            response.put("message", "No files selected. Please choose at least one Excel file.");
            return ResponseEntity.badRequest().body(response);
        }

        for (MultipartFile f : files) {
            String fn = f.getOriginalFilename();
            if (fn == null || (!fn.toLowerCase().endsWith(".xlsx") && !fn.toLowerCase().endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "Invalid file type for '" + fn + "'. Only .xlsx and .xls are supported.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        // ── Processing ────────────────────────────────────────────────────────
        try {
            ProcessingResult result = processorService.process(files, environment);

            session.setAttribute(SESSION_RESULT_KEY, result);

            response.put("success", true);
            response.put("message", "Processing completed successfully. " +
                    result.getFilesUploaded() + " file(s), " +
                    result.getSummary().getTotalRows() + " rows merged.");
            response.put("summary", result.getSummary());
            response.put("logMessages", result.getLogMessages());

            return ResponseEntity.ok(response);

        } catch (InvalidExcelFormatException e) {
            log.warn("Invalid Excel format detected during processing: {}", e.getMessage());
            response.put("success", false);
            response.put("invalidFormat", true);
            response.put("missingColumns", e.getMissingColumns());
            response.put("message",
                    "Excel format is wrong. Expected columns: Sl No, Files, Type, Pushed by, Team");
            return ResponseEntity.badRequest().body(response);

        } catch (ProcessingException e) {
            log.error("Processing failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);

        } catch (Exception e) {
            log.error("Unexpected error during processing: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An unexpected error occurred. Please check the server logs.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── Template download ─────────────────────────────────────────────────────

    /** Download empty Template.xlsx with mandatory column headers only. */
    @GetMapping("/api/download/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = processorService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ── Downloads ─────────────────────────────────────────────────────────────

    /** Download the processed Excel file (Processed_File.xlsx). */
    @GetMapping("/api/download/excel")
    public ResponseEntity<byte[]> downloadExcel(HttpSession session) {
        ProcessingResult result = getResult(session);
        if (result == null) return notFound();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Processed_File.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(result.getProcessedExcelBytes());
    }

    /** Download Customer_GIT_Files.txt */
    @GetMapping("/api/download/customer-git")
    public ResponseEntity<byte[]> downloadCustomerGit(HttpSession session) {
        ProcessingResult result = getResult(session);
        if (result == null) return notFound();

        return textFileResponse("Customer_GIT_Files.txt",
                result.getCustomerGitContent());
    }

    /** Download Product_GIT_Files.txt */
    @GetMapping("/api/download/product-git")
    public ResponseEntity<byte[]> downloadProductGit(HttpSession session) {
        ProcessingResult result = getResult(session);
        if (result == null) return notFound();

        return textFileResponse("Product_GIT_Files.txt",
                result.getProductGitContent());
    }

    /** Download Rule_GIT_Files.txt */
    @GetMapping("/api/download/rule-git")
    public ResponseEntity<byte[]> downloadRuleGit(HttpSession session) {
        ProcessingResult result = getResult(session);
        if (result == null) return notFound();

        return textFileResponse("Rule_GIT_Files.txt",
                result.getRuleGitContent());
    }

    /** Download MDT_Name.txt */
    @GetMapping("/api/download/mdt")
    public ResponseEntity<byte[]> downloadMdt(HttpSession session) {
        ProcessingResult result = getResult(session);
        if (result == null) return notFound();

        return textFileResponse("MDT_Name.txt",
                result.getMdtContent());
    }

    /** Download jarsname.txt */
    @GetMapping("/api/download/jarsname")
    public ResponseEntity<byte[]> downloadJarsName(HttpSession session) {
        ProcessingResult result = getResult(session);
        if (result == null) return notFound();

        return textFileResponse("jarsname.txt",
                result.getJarsNameContent());
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    /** Clears the processing result from the current session. */
    @PostMapping("/api/reset")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reset(HttpSession session) {
        session.removeAttribute(SESSION_RESULT_KEY);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session cleared. Ready for a new file.");
        return ResponseEntity.ok(response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ProcessingResult getResult(HttpSession session) {
        return (ProcessingResult) session.getAttribute(SESSION_RESULT_KEY);
    }

    private ResponseEntity<byte[]> textFileResponse(String filename, String content) {
        byte[] bytes = (content != null ? content : "").getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> notFound() {
        return (ResponseEntity<T>) ResponseEntity.notFound().build();
    }
}
