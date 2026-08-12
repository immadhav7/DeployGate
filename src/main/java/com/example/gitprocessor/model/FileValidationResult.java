package com.example.gitprocessor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation result for a single uploaded file.
 * Returned by /api/validate so the UI can show a per-file grid
 * (File Name | Rows | Status | Error) before the user starts processing.
 */
public class FileValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Original file name from the multipart upload. */
    private String fileName;

    /** Number of non-blank data rows (excluding header). -1 if file could not be opened. */
    private int rowCount;

    /** true = all mandatory columns present; false = one or more missing. */
    private boolean valid;

    /** Display names of any missing mandatory columns (empty when valid = true). */
    private List<String> missingColumns = new ArrayList<>();

    /** General error message (e.g. unreadable file). null when absent. */
    private String error;

    public FileValidationResult() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getMissingColumns() { return missingColumns; }
    public void setMissingColumns(List<String> missingColumns) {
        this.missingColumns = missingColumns != null ? missingColumns : new ArrayList<>();
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
