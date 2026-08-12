package com.example.gitprocessor.util;

/**
 * Validates and corrects the "Type" column against the file extension
 * detected from the file path (or the expected "Rule" type for rule rows).
 *
 * Validation Status values:
 *   Correct          – existing type matches detected type
 *   Corrected        – existing type was blank / wrong; corrected to detected
 *   Unable To Identify – cannot determine type (no extension, not a rule)
 */
public final class TypeValidator {

    private TypeValidator() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Validates {@code existingType} against the type inferred from the file
     * path and GIT type classification.
     *
     * @param existingType The value in the Type column (may be null/blank)
     * @param files        The file path or rule context string
     * @param gitType      The detected GIT type (use {@link GitTypeDetector} constants)
     * @return A {@link ValidationResult} with the corrected type and status
     */
    public static ValidationResult validate(String existingType, String files, String gitType) {
        String detected = detectExpectedType(files, gitType);

        if (detected == null) {
            // Cannot determine – keep whatever was there (or empty)
            String kept = (existingType != null) ? existingType.trim() : "";
            return new ValidationResult(kept, "Unable To Identify");
        }

        String existing = (existingType != null) ? existingType.trim() : "";

        if (existing.equalsIgnoreCase(detected)) {
            // Preserve original casing if it matches
            return new ValidationResult(existing.isEmpty() ? detected : existing, "Correct");
        }

        // Blank or wrong – correct it
        return new ValidationResult(detected, "Corrected");
    }

    // ── Type detection ────────────────────────────────────────────────────────

    /**
     * Infers the expected file type from the file path and GIT type.
     *
     * @return lowercase extension string, "Rule", or {@code null} if unknown
     */
    public static String detectExpectedType(String files, String gitType) {
        // Rule rows always expect type "Rule"
        if (GitTypeDetector.RULE_GIT.equals(gitType)) {
            return "Rule";
        }

        // MDT rows always expect type "MDT"
        if (GitTypeDetector.MDT.equals(gitType)) {
            return "MDT";
        }

        if (files == null || files.isBlank()) {
            return null;
        }

        // Normalise separators so we can split reliably
        String normalised = files.trim().replace('\\', '/');

        // Extract only the filename (last path segment)
        int lastSlash = normalised.lastIndexOf('/');
        String filename = (lastSlash >= 0) ? normalised.substring(lastSlash + 1) : normalised;

        // Locate the last dot in the filename
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < filename.length() - 1) {
            return filename.substring(dotIdx + 1).toLowerCase();
        }

        return null; // No extension found
    }

    // ── Result DTO ────────────────────────────────────────────────────────────

    /**
     * Immutable result of a type validation operation.
     */
    public static final class ValidationResult {

        private final String finalType;
        private final String status;

        public ValidationResult(String finalType, String status) {
            this.finalType = finalType;
            this.status    = status;
        }

        /** The corrected (or original) type to write to the output Excel. */
        public String getFinalType() { return finalType; }

        /** Correct | Corrected | Unable To Identify */
        public String getStatus() { return status; }
    }
}
