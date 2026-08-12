package com.example.gitprocessor.exception;

import java.util.List;

/**
 * Thrown when the uploaded Excel file is missing one or more of the mandatory
 * columns (Sl No, Files, Type, Pushed by, Team).
 *
 * The controller catches this exception and returns a structured JSON response
 * containing {@code invalidFormat=true} and the list of missing column names so
 * the UI can display a meaningful error to the user.
 */
public class InvalidExcelFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> missingColumns;

    public InvalidExcelFormatException(List<String> missingColumns) {
        super("Excel format is invalid. Missing columns: " + missingColumns);
        this.missingColumns = missingColumns;
    }

    /** Returns the display names of the columns that were not found. */
    public List<String> getMissingColumns() {
        return missingColumns;
    }
}
