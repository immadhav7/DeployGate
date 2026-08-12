package com.example.gitprocessor.model;

/**
 * Supported deployment environments.
 * Each environment carries its Rule Engine base URL used when generating
 * Rule_GIT_Files.txt.
 */
public enum Environment {

    UAT1   ("UAT1",    "https://uat1-udaan.sbigen.in/RuleEngine/RuleEngineController/downloadRuleWithParam/"),
    UAT2   ("UAT2",    "https://uat2-udaan.sbigen.in/RuleEngine/RuleEngineController/downloadRuleWithParam/"),
    ST1    ("ST1",     "http://172.18.15.186:7052/RuleEngine/RuleEngineController/downloadRuleWithParam/"),
    ST2    ("ST2",     "http://172.18.15.246:7022/RuleEngine/RuleEngineController/downloadRuleWithParam/"),
    PREPROD("PreProd", "https://preprod-udaan.sbigen.in/RuleEngine/RuleEngineController/downloadRuleWithParam/"),
    PROD   ("PROD",    "https://udaan.sbigen.in/RuleEngine/RuleEngineController/downloadRuleWithParam/");

    private final String displayName;
    private final String baseUrl;

    Environment(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl     = baseUrl;
    }

    /** The name shown in the UI dropdown (e.g. "UAT1", "PreProd"). */
    public String getDisplayName() { return displayName; }

    /** Base URL to prepend to every rule name in Rule_GIT_Files.txt. */
    public String getBaseUrl()     { return baseUrl; }

    /**
     * Resolves an {@link Environment} from a display name string
     * (case-insensitive, trimmed).
     *
     * @return the matching Environment, or {@code null} if not found
     */
    public static Environment fromName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        for (Environment env : values()) {
            if (env.displayName.equalsIgnoreCase(trimmed)) {
                return env;
            }
        }
        return null;
    }
}
