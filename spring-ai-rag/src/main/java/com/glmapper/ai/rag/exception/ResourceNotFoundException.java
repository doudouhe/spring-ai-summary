package com.glmapper.ai.rag.exception;

/**
 * @Classname ResourceNotFoundException
 * @Description Exception for missing resources
 * @Date 2025/6/12
 * @Created by glmapper
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}