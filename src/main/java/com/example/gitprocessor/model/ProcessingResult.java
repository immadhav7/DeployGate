package com.example.gitprocessor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Holds every artifact produced by processing a single Excel upload.
 * Stored in the HTTP session so download endpoints can serve the files.
 *
 * NOTE: byte[] and String are both Serializable, so this object is safe
 *       for session storage even with distributable deployments.
 */
public class ProcessingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** All processed rows (used for debugging / potential future preview). */
    private List<ExcelRow> rows;

    /** Binary content of the generated Processed_File.xlsx */
    private byte[] processedExcelBytes;

    /** Text content of Customer_GIT_Files.txt */
    private String customerGitContent;

    /** Text content of Product_GIT_Files.txt */
    private String productGitContent;

    /** Text content of Rule_GIT_Files.txt */
    private String ruleGitContent;

    /** Text content of MDT_Name.txt */
    private String mdtContent;

    /** Text content of jarsname.txt */
    private String jarsNameContent;

    /** Summary statistics. */
    private ProcessingSummary summary;

    /** Ordered list of timestamped log messages. */
    private List<String> logMessages;

    /** When the file was processed. */
    private LocalDateTime processedAt;

    /** Original uploaded file name. */
    private String originalFileName;

    /** Selected environment display name (e.g. "UAT1", "PROD"). */
    private String environment;

    /** Number of files uploaded in this processing run. */
    private int filesUploaded;

    public ProcessingResult() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public List<ExcelRow> getRows() { return rows; }
    public void setRows(List<ExcelRow> rows) { this.rows = rows; }

    public byte[] getProcessedExcelBytes() { return processedExcelBytes; }
    public void setProcessedExcelBytes(byte[] processedExcelBytes) { this.processedExcelBytes = processedExcelBytes; }

    public String getCustomerGitContent() { return customerGitContent; }
    public void setCustomerGitContent(String customerGitContent) { this.customerGitContent = customerGitContent; }

    public String getProductGitContent() { return productGitContent; }
    public void setProductGitContent(String productGitContent) { this.productGitContent = productGitContent; }

    public String getRuleGitContent() { return ruleGitContent; }
    public void setRuleGitContent(String ruleGitContent) { this.ruleGitContent = ruleGitContent; }

    public String getMdtContent() { return mdtContent; }
    public void setMdtContent(String mdtContent) { this.mdtContent = mdtContent; }

    public String getJarsNameContent() { return jarsNameContent; }
    public void setJarsNameContent(String jarsNameContent) { this.jarsNameContent = jarsNameContent; }

    public ProcessingSummary getSummary() { return summary; }
    public void setSummary(ProcessingSummary summary) { this.summary = summary; }

    public List<String> getLogMessages() { return logMessages; }
    public void setLogMessages(List<String> logMessages) { this.logMessages = logMessages; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public int getFilesUploaded() { return filesUploaded; }
    public void setFilesUploaded(int filesUploaded) { this.filesUploaded = filesUploaded; }
}
