package com.plsql.tools.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class CaseConverter {

    // Pre-compiled patterns for better performance
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*[a-zA-Z0-9]$|^[a-zA-Z]$");
    private static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("_{2,}");
    private static final Pattern LEADING_TRAILING_UNDERSCORE = Pattern.compile("^_+|_+$");

    // Constants for optimization
    private static final char UNDERSCORE = '_';

    /**
     * Conversion modes for different camelCase styles
     */
    public enum CamelCaseMode {
        LOWER_CAMEL_CASE,  // someVariableName
        UPPER_CAMEL_CASE   // SomeVariableName (PascalCase)
    }

    /**
     * Configuration options for the converter
     */
    public static final class Config {
        private final boolean strictValidation;
        private final boolean preserveConsecutiveUnderscores;
        private final CamelCaseMode camelCaseMode;

        private Config(Builder builder) {
            this.strictValidation = builder.strictValidation;
            this.preserveConsecutiveUnderscores = builder.preserveConsecutiveUnderscores;
            this.camelCaseMode = builder.camelCaseMode;
        }

        public static class Builder {
            private boolean strictValidation = false;
            private boolean preserveConsecutiveUnderscores = false;
            private CamelCaseMode camelCaseMode = CamelCaseMode.LOWER_CAMEL_CASE;

            public Builder strictValidation(boolean strict) {
                this.strictValidation = strict;
                return this;
            }

            public Builder preserveConsecutiveUnderscores(boolean preserve) {
                this.preserveConsecutiveUnderscores = preserve;
                return this;
            }

            public Builder camelCaseMode(CamelCaseMode mode) {
                this.camelCaseMode = Objects.requireNonNull(mode, "Mode cannot be null");
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Config defaultConfig() {
            return new Builder().build();
        }
    }

    // Prevent instantiation
    private CaseConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== SNAKE_CASE TO CAMEL_CASE ====================

    /**
     * Converts snake_case to camelCase using default configuration.
     *
     * @param snakeCase the snake_case string to convert
     * @return camelCase string, or null if input is null
     * @throws IllegalArgumentException if input is invalid and strict validation is enabled
     */
    public static String toCamelCase(String snakeCase) {
        return toCamelCase(snakeCase, Config.defaultConfig());
    }

    /**
     * Converts snake_case to camelCase with custom configuration.
     *
     * @param snakeCase the snake_case string to convert
     * @param config    conversion configuration
     * @return camelCase string, or null if input is null
     * @throws IllegalArgumentException if input is invalid and strict validation is enabled
     */
    public static String toCamelCase(String snakeCase, Config config) {
        Objects.requireNonNull(config, "Config cannot be null");

        // Handle null and empty cases
        if (snakeCase == null) {
            return null;
        }

        if (snakeCase.isEmpty()) {
            return snakeCase;
        }

        // Validate input if strict validation is enabled
        if (config.strictValidation) {
            validateSnakeCase(snakeCase);
        }

        // Perform conversion
        return convertSnakeToCamel(snakeCase, config);
    }

    /**
     * Core conversion logic for snake_case to camelCase.
     */
    private static String convertSnakeToCamel(String snakeCase, Config config) {
        // Handle single character
        if (snakeCase.length() == 1) {
            return handleSingleCharacter(snakeCase, config.camelCaseMode);
        }

        // Clean input if needed
        String cleaned = cleanSnakeInput(snakeCase, config);

        // Early return for strings without underscores
        if (cleaned.indexOf(UNDERSCORE) == -1) {
            return handleNoUnderscores(cleaned, config.camelCaseMode);
        }

        StringBuilder result = new StringBuilder(cleaned.length());
        boolean capitalizeNext = (config.camelCaseMode == CamelCaseMode.UPPER_CAMEL_CASE);
        boolean isFirstChar = true;

        for (int i = 0; i < cleaned.length(); i++) {
            char currentChar = cleaned.charAt(i);

            if (currentChar == UNDERSCORE) {
                capitalizeNext = true;
            } else {
                if (capitalizeNext && Character.isLetter(currentChar)) {
                    result.append(Character.toUpperCase(currentChar));
                    capitalizeNext = false;
                } else if (isFirstChar && config.camelCaseMode == CamelCaseMode.LOWER_CAMEL_CASE) {
                    result.append(Character.toLowerCase(currentChar));
                } else {
                    result.append(Character.toLowerCase(currentChar));
                }
                isFirstChar = false;
            }
        }

        return result.toString();
    }

    // ==================== CAMEL_CASE TO SNAKE_CASE ====================

    /**
     * Converts camelCase to snake_case.
     * Handles edge cases like consecutive uppercase letters (XMLHttpRequest -> xml_http_request)
     * and numbers (parseHTML5 -> parse_html5).
     *
     * @param camelCase the camelCase string to convert
     * @return snake_case string, or null if input is null
     */
    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        // If already snake_case, return as is
        if (camelCase.contains("_") && camelCase.equals(camelCase.toLowerCase())) {
            return camelCase;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = camelCase.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            char next = (i + 1 < chars.length) ? chars[i + 1] : '\0';
            char prev = (i > 0) ? chars[i - 1] : '\0';

            // Add underscore before uppercase letter if:
            // 1. Not the first character
            // 2. Previous character is lowercase or digit
            // 3. OR current is uppercase, next is lowercase (for consecutive caps like "XMLHttp")
            if (Character.isUpperCase(current) && i > 0 &&
                    (Character.isLowerCase(prev) || Character.isDigit(prev) ||
                            (Character.isUpperCase(prev) && Character.isLowerCase(next)))) {
                result.append('_');
            }

            result.append(Character.toLowerCase(current));
        }

        return result.toString();
    }

    // ==================== BATCH CONVERSION METHODS ====================

    /**
     * Converts multiple snake_case strings to camelCase using default configuration.
     */
    public static List<String> toCamelCaseBulk(List<String> snakeCaseStrings) {
        return toCamelCaseBulk(snakeCaseStrings, Config.defaultConfig());
    }

    /**
     * Converts multiple snake_case strings to camelCase with custom configuration.
     */
    public static List<String> toCamelCaseBulk(List<String> snakeCaseStrings, Config config) {
        Objects.requireNonNull(snakeCaseStrings, "PlsqlParam list cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        List<String> result = new ArrayList<>(snakeCaseStrings.size());
        for (String snakeCase : snakeCaseStrings) {
            result.add(toCamelCase(snakeCase, config));
        }
        return result;
    }

    /**
     * Converts multiple camelCase strings to snake_case.
     */
    public static List<String> toSnakeCaseBulk(List<String> camelCaseStrings) {
        Objects.requireNonNull(camelCaseStrings, "PlsqlParam list cannot be null");

        List<String> result = new ArrayList<>(camelCaseStrings.size());
        for (String camelCase : camelCaseStrings) {
            result.add(toSnakeCase(camelCase));
        }
        return result;
    }

    /**
     * Converts array of camelCase strings to snake_case.
     */
    public static String[] toSnakeCaseArray(String[] camelCaseStrings) {
        if (camelCaseStrings == null) return null;

        String[] results = new String[camelCaseStrings.length];
        for (int i = 0; i < camelCaseStrings.length; i++) {
            results[i] = toSnakeCase(camelCaseStrings[i]);
        }
        return results;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Handles single character conversion based on mode.
     */
    private static String handleSingleCharacter(String input, CamelCaseMode mode) {
        char ch = input.charAt(0);
        if (ch == UNDERSCORE) {
            return ""; // Single underscore becomes empty string
        }

        switch (mode) {
            case UPPER_CAMEL_CASE:
                return String.valueOf(Character.toUpperCase(ch));
            case LOWER_CAMEL_CASE:
            default:
                return String.valueOf(Character.toLowerCase(ch));
        }
    }

    /**
     * Handles strings without underscores.
     */
    private static String handleNoUnderscores(String input, CamelCaseMode mode) {
        if (input.isEmpty()) {
            return input;
        }

        switch (mode) {
            case UPPER_CAMEL_CASE:
                return Character.toUpperCase(input.charAt(0)) +
                        (input.length() > 1 ? input.substring(1).toLowerCase() : "");
            case LOWER_CAMEL_CASE:
            default:
                return input.toLowerCase();
        }
    }

    /**
     * Cleans snake_case input string based on configuration.
     */
    private static String cleanSnakeInput(String input, Config config) {
        String cleaned = input;

        // Remove leading and trailing underscores
        cleaned = LEADING_TRAILING_UNDERSCORE.matcher(cleaned).replaceAll("");

        // Handle consecutive underscores
        if (!config.preserveConsecutiveUnderscores) {
            cleaned = MULTIPLE_UNDERSCORES.matcher(cleaned).replaceAll("_");
        }

        return cleaned;
    }

    /**
     * Validates snake_case format strictly.
     */
    private static void validateSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        // Check for invalid characters or patterns
        if (!SNAKE_CASE_PATTERN.matcher(input).matches() && !input.equals("_")) {
            throw new IllegalArgumentException(
                    String.format("Invalid snake_case format: '%s'. " +
                            "Expected format: letters, numbers, and underscores only, " +
                            "starting and ending with alphanumeric characters.", input));
        }

        // Additional validations
        if (input.startsWith("_") || input.endsWith("_")) {
            throw new IllegalArgumentException(
                    String.format("Invalid snake_case format: '%s'. " +
                            "Cannot start or end with underscore in strict mode.", input));
        }

        if (input.contains("__")) {
            throw new IllegalArgumentException(
                    String.format("Invalid snake_case format: '%s'. " +
                            "Consecutive underscores not allowed in strict mode.", input));
        }
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Validates if a string is in valid snake_case format.
     */
    public static boolean isValidSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return true; // null/empty are considered valid for conversion
        }

        try {
            validateSnakeCase(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates if a string is in valid camelCase format.
     */
    public static boolean isValidCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        // Basic camelCase validation: no underscores, starts with letter
        return !input.contains("_") &&
                Character.isLetter(input.charAt(0)) &&
                input.chars().allMatch(Character::isLetterOrDigit);
    }

    public static String upperCaseFirstLetter(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}