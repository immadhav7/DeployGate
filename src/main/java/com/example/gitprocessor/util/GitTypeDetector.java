package com.example.gitprocessor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the GIT classification for each Excel row based on the content
 * of the "Files" column and extracts the relevant path segments for
 * text file generation.
 *
 * Priority order (evaluated top to bottom):
 *   1. RULE CONTEXT NAME  (case-insensitive, = or :) → Rule_GIT
 *   2. MDT NAME           (case-insensitive, = or :) → MDT
 *   3. Contains "Customer_LAYER"                      → Customer_GIT
 *   4. Contains "Base Source Code"                    → Product_GIT
 *   5. Otherwise                                      → Unknown
 */
public final class GitTypeDetector {

    // ── GIT type constants ────────────────────────────────────────────────────
    public static final String CUSTOMER_GIT = "Customer_GIT";
    public static final String PRODUCT_GIT  = "Product_GIT";
    public static final String RULE_GIT     = "Rule_GIT";
    public static final String MDT          = "MDT";
    public static final String UNKNOWN      = "Unknown";

    // ── Marker strings ────────────────────────────────────────────────────────
    public static final String CUSTOMER_LAYER_MARKER = "Customer_LAYER";
    public static final String CUSTOMER_GIT_MARKER   = "Customer_GIT";
    public static final String BASE_SOURCE_MARKER    = "Base Source Code";

    /** Case-insensitive pattern to find Customer_GIT in a path */
    private static final Pattern CUSTOMER_GIT_PATH_PATTERN =
            Pattern.compile("(?i)Customer_GIT");

    /**
     * Matches "Rule Context Name" followed by = or : (with optional spaces),
     * then captures the rule name up to the first comma or end of string.
     *
     * Flags:
     *   (?i) – case-insensitive
     *   (?U) – Unicode-aware \s, so non-breaking spaces (\u00A0) from Excel are matched
     *
     * Handles all variants:
     *   Rule Context Name = COND_T843_ARG001 , Sub Event Code = COND-T843
     *   RULE CONTEXT NAME: UWR_REFFERRAL_ARG001,SUB EVENT CODE: ABHI-WEAVER
     *   rule context name=ARG001-VSCR-MEMVAL,sub event code=T1-MEMDTL
     *   Rule Context Name : TEST_RULE_001
     */
    private static final Pattern RULE_PATTERN =
            Pattern.compile("(?Ui)rule\\s+context\\s+name\\s*[=:]\\s*([^,]+)");

    /**
     * Matches "MDT Name" followed by = or : (with optional spaces),
     * then captures the MDT name up to the first comma or end of string.
     *
     * Flags:
     *   (?i) – case-insensitive
     *   (?U) – Unicode-aware \s
     *
     * Handles all variants:
     *   MDT Name = CUSTOMER_DETAILS
     *   mdt name : POLICY_MASTER
     *   MDT NAME: CLAIM_DETAILS, Team = Insurance
     */
    private static final Pattern MDT_PATTERN =
            Pattern.compile("(?Ui)mdt\\s+name\\s*[=:]\\s*([^,]+)");

    private GitTypeDetector() {}

    // ── Detection ─────────────────────────────────────────────────────────────

    /**
     * Returns the GIT type for the given "Files" cell value.
     *
     * @param files Raw content of the Files column
     * @return One of {@code Customer_GIT}, {@code Product_GIT},
     *         {@code Rule_GIT}, or {@code Unknown}
     */
    public static String detect(String files) {
        if (files == null || files.isBlank()) {
            return UNKNOWN;
        }
        // Priority 1 – Rule_GIT (must be checked before everything else)
        if (RULE_PATTERN.matcher(files).find()) {
            return RULE_GIT;
        }
        // Priority 2 – MDT
        if (MDT_PATTERN.matcher(files).find()) {
            return MDT;
        }
        // Priority 3 – Customer_GIT (Customer_LAYER or Customer_GIT, case-insensitive)
        if (files.contains(CUSTOMER_LAYER_MARKER) || CUSTOMER_GIT_PATH_PATTERN.matcher(files).find()) {
            return CUSTOMER_GIT;
        }
        // Priority 4 – Product_GIT
        if (files.contains(BASE_SOURCE_MARKER)) {
            return PRODUCT_GIT;
        }
        return UNKNOWN;
    }

    // ── Path extraction helpers ───────────────────────────────────────────────

    /**
     * Extracts the path starting from "Customer_LAYER" or "Customer_GIT" (inclusive).
     * Priority: Customer_LAYER first, then Customer_GIT (case-insensitive).
     *
     * Input : C:\PROD_P1_T2\Cust_RP1_BaNCS01_SBIG\Customer_LAYER\Cust_Layer_SBI\Database\GST_ARG001.sql
     * Output: Customer_LAYER\Cust_Layer_SBI\Database\GST_ARG001.sql
     *
     * Input : D:/PROJECTS/Customer_GIT/DATABASE/IIMS_CLM/PACKAGE/PKG_IMOSS_I.pck
     * Output: Customer_GIT/DATABASE/IIMS_CLM/PACKAGE/PKG_IMOSS_I.pck
     */
    public static String extractCustomerPath(String files) {
        if (files == null) return "";
        // Priority 1: Customer_LAYER (exact case)
        int idx = files.indexOf(CUSTOMER_LAYER_MARKER);
        if (idx >= 0) return files.substring(idx);
        // Priority 2: Customer_GIT (case-insensitive)
        Matcher m = CUSTOMER_GIT_PATH_PATTERN.matcher(files);
        if (m.find()) return files.substring(m.start());
        return files;
    }

    /**
     * Extracts the path starting from "Base Source Code" (inclusive).
     *
     * Input : Base Source Code/GRADLEWORKSPACE/.../PolicyNumberController.java
     * Output: Base Source Code/GRADLEWORKSPACE/.../PolicyNumberController.java
     */
    public static String extractProductPath(String files) {
        if (files == null) return "";
        int idx = files.indexOf(BASE_SOURCE_MARKER);
        return idx >= 0 ? files.substring(idx) : files;
    }

    /**
     * Extracts only the rule name value using the case-insensitive regex.
     * Supports both {@code =} and {@code :} as separator.
     * The extracted value is trimmed to remove any surrounding whitespace.
     *
     * Input : Rule Context Name = COND_T843_ARG001 , Sub Event Code = COND-T843
     * Output: COND_T843_ARG001
     *
     * Input : RULE CONTEXT NAME: UWR_REFFERRAL_ARG001,SUB EVENT CODE: ABHI-WEAVER
     * Output: UWR_REFFERRAL_ARG001
     *
     * Input : rule context name=ARG001-VSCR-MEMVAL,sub event code=T1-MEMDTL
     * Output: ARG001-VSCR-MEMVAL
     *
     * Input : Rule Context Name : TEST_RULE_001
     * Output: TEST_RULE_001
     */
    public static String extractRuleName(String files) {
        if (files == null) return "";
        Matcher matcher = RULE_PATTERN.matcher(files);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * Extracts only the MDT name value using the case-insensitive regex.
     * Supports both {@code =} and {@code :} as separator.
     * The extracted value is trimmed to remove any surrounding whitespace.
     *
     * Input : MDT Name = CUSTOMER_DETAILS
     * Output: CUSTOMER_DETAILS
     *
     * Input : mdt name : POLICY_MASTER
     * Output: POLICY_MASTER
     *
     * Input : MDT Name : CLAIM_DETAILS, Team = Insurance
     * Output: CLAIM_DETAILS
     */
    public static String extractMdtName(String files) {
        if (files == null) return "";
        Matcher matcher = MDT_PATTERN.matcher(files);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
