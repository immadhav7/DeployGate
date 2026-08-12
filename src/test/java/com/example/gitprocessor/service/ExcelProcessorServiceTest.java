package com.example.gitprocessor.service;

import com.example.gitprocessor.util.GitTypeDetector;
import com.example.gitprocessor.util.TypeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests covering the two core utility classes:
 *   - GitTypeDetector  (GIT type classification + path extraction)
 *   - TypeValidator    (type column validation logic)
 *
 * These tests do not start a Spring context – they run pure Java.
 */
class ExcelProcessorServiceTest {

    // ══════════════════════════════════════════════════════════════════════════
    // GitTypeDetector – detection
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Files containing Customer_LAYER → Customer_GIT")
    void detectCustomerGit() {
        String files = "C:\\PROD_P1_T2\\Cust_RP1_BaNCS01_SBIG\\Customer_LAYER\\Cust_Layer_SBI"
                + "\\Database\\IIMS_PRD\\DML Scripts\\GST_ARG001.sql";
        assertEquals(GitTypeDetector.CUSTOMER_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("Files containing 'Base Source Code' → Product_GIT")
    void detectProductGit() {
        String files = "Base Source Code/GRADLEWORKSPACE/GradleBuild/TransitionHandler/src/"
                + "main/java/com/tcs/bancs/insurance/transition/controller/PolicyNumberController.java";
        assertEquals(GitTypeDetector.PRODUCT_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("Files containing 'Rule context name=' (lowercase) → Rule_GIT")
    void detectRuleGitLowercase() {
        String files = "Rule context name=ARG001-VSCR-MEMVAL,Sub Event code=T1-MEMDTL";
        assertEquals(GitTypeDetector.RULE_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("Files containing 'Rule Context Name =' (mixed case + spaces) → Rule_GIT")
    void detectRuleGitMixedCaseWithSpaces() {
        String files = "Rule Context Name = COND_T843_ARG001 , Sub Event Code = COND-T843";
        assertEquals(GitTypeDetector.RULE_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("Files containing 'RULE CONTEXT NAME =' (all caps) → Rule_GIT")
    void detectRuleGitAllCaps() {
        String files = "RULE CONTEXT NAME = TEST_001, SUB EVENT CODE = EVT01";
        assertEquals(GitTypeDetector.RULE_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("Files containing 'RULE CONTEXT NAME:' (colon separator) → Rule_GIT")
    void detectRuleGitColonSeparator() {
        String files = "RULE CONTEXT NAME: UWR_REFFERRAL_ARG001,SUB EVENT CODE: ABHI-WEAVER";
        assertEquals(GitTypeDetector.RULE_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("RULE CONTEXT NAME: extraction gives UWR_REFFERRAL_ARG001 (trimmed)")
    void extractRuleNameColonNoSpaceAroundSeparator() {
        String files = "RULE CONTEXT NAME: UWR_REFFERRAL_ARG001,SUB EVENT CODE: ABHI-WEAVER";
        assertEquals("UWR_REFFERRAL_ARG001", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("Files containing 'Rule Context Name :' (colon with spaces) → Rule_GIT")
    void detectRuleGitColonWithSpaces() {
        String files = "Rule Context Name : TEST_RULE_001";
        assertEquals(GitTypeDetector.RULE_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("MDT Name = VALUE → MDT")
    void detectMdt() {
        assertEquals(GitTypeDetector.MDT, GitTypeDetector.detect("MDT Name = CUSTOMER_DETAILS"));
    }

    @Test
    @DisplayName("mdt name : VALUE (lowercase + colon) → MDT")
    void detectMdtLowercaseColon() {
        assertEquals(GitTypeDetector.MDT, GitTypeDetector.detect("mdt name : POLICY_MASTER"));
    }

    @Test
    @DisplayName("MDT NAME: VALUE (all caps + colon, no space) → MDT")
    void detectMdtAllCapsColon() {
        assertEquals(GitTypeDetector.MDT, GitTypeDetector.detect("MDT NAME: CLAIM_DETAILS, Team = Insurance"));
    }

    @Test
    @DisplayName("Rule takes priority over MDT when both keywords present")
    void detectRulePriorityOverMdt() {
        String files = "Rule Context Name = SOME_RULE, MDT Name = SOME_MDT";
        assertEquals(GitTypeDetector.RULE_GIT, GitTypeDetector.detect(files));
    }

    @Test
    @DisplayName("Unrecognised path → Unknown")
    void detectUnknown() {
        assertEquals(GitTypeDetector.UNKNOWN, GitTypeDetector.detect("some/random/path/file.txt"));
    }

    @Test
    @DisplayName("Null or blank → Unknown")
    void detectNullOrBlank() {
        assertEquals(GitTypeDetector.UNKNOWN, GitTypeDetector.detect(null));
        assertEquals(GitTypeDetector.UNKNOWN, GitTypeDetector.detect("   "));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GitTypeDetector – path extraction
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("extractCustomerPath strips prefix before Customer_LAYER")
    void extractCustomerPath() {
        String files = "C:\\PROD_P1_T2\\Cust_RP1_BaNCS01_SBIG\\Customer_LAYER\\Cust_Layer_SBI"
                + "\\Database\\IIMS_PRD\\DML Scripts\\GST_ARG001.sql";
        String expected = "Customer_LAYER\\Cust_Layer_SBI\\Database\\IIMS_PRD\\DML Scripts\\GST_ARG001.sql";
        assertEquals(expected, GitTypeDetector.extractCustomerPath(files));
    }

    @Test
    @DisplayName("extractProductPath keeps path from 'Base Source Code'")
    void extractProductPath() {
        String files = "Base Source Code/GRADLEWORKSPACE/PolicyNumberController.java";
        assertEquals(files, GitTypeDetector.extractProductPath(files));
    }

    @Test
    @DisplayName("extractRuleName – lowercase, no spaces around '='")
    void extractRuleNameLowercase() {
        String files = "Rule context name=ARG001-VSCR-MEMVAL,Sub Event code=T1-MEMDTL";
        assertEquals("ARG001-VSCR-MEMVAL", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("extractRuleName – mixed case with spaces, trims whitespace")
    void extractRuleNameMixedCaseSpaces() {
        String files = "Rule Context Name = COND_T843_ARG001 , Sub Event Code = COND-T843";
        assertEquals("COND_T843_ARG001", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("extractRuleName – all caps variant")
    void extractRuleNameAllCaps() {
        String files = "RULE CONTEXT NAME = TEST_001, SUB EVENT CODE = EVT01";
        assertEquals("TEST_001", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("extractRuleName when no comma – extracts to end of string")
    void extractRuleNameNoComma() {
        String files = "Rule context name=RULE-XYZ";
        assertEquals("RULE-XYZ", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("extractRuleName – colon separator (RULE CONTEXT NAME: VALUE)")
    void extractRuleNameColonSeparator() {
        String files = "RULE CONTEXT NAME: UWR_REFFERRAL_ARG001,SUB EVENT CODE: ABHI-WEAVER";
        assertEquals("UWR_REFFERRAL_ARG001", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("extractRuleName – colon with spaces (Rule Context Name : VALUE)")
    void extractRuleNameColonWithSpaces() {
        String files = "Rule Context Name : TEST_RULE_001";
        assertEquals("TEST_RULE_001", GitTypeDetector.extractRuleName(files));
    }

    @Test
    @DisplayName("extractMdtName – equals separator")
    void extractMdtNameEquals() {
        assertEquals("CUSTOMER_DETAILS", GitTypeDetector.extractMdtName("MDT Name = CUSTOMER_DETAILS"));
    }

    @Test
    @DisplayName("extractMdtName – colon separator (lowercase)")
    void extractMdtNameColon() {
        assertEquals("POLICY_MASTER", GitTypeDetector.extractMdtName("mdt name : POLICY_MASTER"));
    }

    @Test
    @DisplayName("extractMdtName – stops at comma and trims whitespace")
    void extractMdtNameStopsAtComma() {
        assertEquals("CLAIM_DETAILS", GitTypeDetector.extractMdtName("MDT Name : CLAIM_DETAILS, Team = Insurance"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TypeValidator
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Existing type matches detected → Correct")
    void validateCorrect() {
        String files = "Base Source Code/.../PolicyNumberController.java";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("java", files, GitTypeDetector.PRODUCT_GIT);
        assertEquals("Correct", r.getStatus());
        assertEquals("java", r.getFinalType());
    }

    @Test
    @DisplayName("Existing type wrong → Corrected to detected")
    void validateCorrected() {
        String files = "Base Source Code/.../PolicyNumberController.java";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("txt", files, GitTypeDetector.PRODUCT_GIT);
        assertEquals("Corrected", r.getStatus());
        assertEquals("java", r.getFinalType());
    }

    @Test
    @DisplayName("Blank existing type for Java file → Corrected to 'java'")
    void validateBlankTypeCorrectedToJava() {
        String files = "Base Source Code/.../MyService.java";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("", files, GitTypeDetector.PRODUCT_GIT);
        assertEquals("Corrected", r.getStatus());
        assertEquals("java", r.getFinalType());
    }

    @Test
    @DisplayName("Rule_GIT row (mixed case + spaces) → type corrected to 'Rule'")
    void validateRuleGitType() {
        String files = "Rule Context Name = COND_T843_ARG001 , Sub Event Code = COND-T843";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("txt", files, GitTypeDetector.RULE_GIT);
        assertEquals("Corrected", r.getStatus());
        assertEquals("Rule", r.getFinalType());
    }

    @Test
    @DisplayName("Rule_GIT row with correct type → Correct")
    void validateRuleGitCorrectType() {
        String files = "RULE CONTEXT NAME = TEST_001, SUB EVENT CODE = EVT01";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("Rule", files, GitTypeDetector.RULE_GIT);
        assertEquals("Correct", r.getStatus());
        assertEquals("Rule", r.getFinalType());
    }

    @Test
    @DisplayName("MDT row → type corrected to 'MDT'")
    void validateMdtType() {
        TypeValidator.ValidationResult r =
                TypeValidator.validate("txt", "MDT Name = CUSTOMER_DETAILS", GitTypeDetector.MDT);
        assertEquals("Corrected", r.getStatus());
        assertEquals("MDT", r.getFinalType());
    }

    @Test
    @DisplayName("MDT row with correct type → Correct")
    void validateMdtCorrectType() {
        TypeValidator.ValidationResult r =
                TypeValidator.validate("MDT", "mdt name : POLICY_MASTER", GitTypeDetector.MDT);
        assertEquals("Correct", r.getStatus());
        assertEquals("MDT", r.getFinalType());
    }

    @Test
    @DisplayName("SQL file validated correctly")
    void validateSqlFile() {
        String files = "Customer_LAYER/Cust_Layer_SBI/Database/GST_ARG001.sql";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("sql", files, GitTypeDetector.CUSTOMER_GIT);
        assertEquals("Correct", r.getStatus());
    }

    @Test
    @DisplayName("XML file validated correctly")
    void validateXmlFile() {
        String files = "abc/test/config.xml";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("xml", files, GitTypeDetector.UNKNOWN);
        assertEquals("Correct", r.getStatus());
    }

    @Test
    @DisplayName("Properties file validated correctly")
    void validatePropertiesFile() {
        String files = "test/application.properties";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("properties", files, GitTypeDetector.UNKNOWN);
        assertEquals("Correct", r.getStatus());
    }

    @Test
    @DisplayName("No file extension and not a rule → Unable To Identify")
    void validateUnableToIdentify() {
        String files = "some/path/without/extension";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("", files, GitTypeDetector.UNKNOWN);
        assertEquals("Unable To Identify", r.getStatus());
    }

    @Test
    @DisplayName("Type comparison is case-insensitive (JAVA == java)")
    void validateCaseInsensitive() {
        String files = "Base Source Code/.../MyController.java";
        TypeValidator.ValidationResult r =
                TypeValidator.validate("JAVA", files, GitTypeDetector.PRODUCT_GIT);
        assertEquals("Correct", r.getStatus());
    }
}
