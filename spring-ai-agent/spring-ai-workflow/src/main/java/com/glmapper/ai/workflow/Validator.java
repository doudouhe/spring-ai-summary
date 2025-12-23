package com.glmapper.ai.workflow;

import java.util.regex.Pattern;

/**
 * Interface for validation operations
 */
public interface Validator {
    /**
     * The regex pattern for numeric validation
     */
    Pattern NUMBERIC = Pattern.compile("^\\d+$");

    /**
     * Validates the input string
     * @param str the string to validate
     * @return true if valid, false otherwise
     */
    boolean validate(String str);

    /**
     * Gets the error message if validation fails
     * @return the error message
     */
    String getMessage();
}