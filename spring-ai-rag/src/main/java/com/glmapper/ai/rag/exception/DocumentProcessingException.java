package com.glmapper.ai.rag.exception;

/**
 * @Classname DocumentProcessingException
 * @Description Exception for document processing errors
 * @Date 2025/6/12
 * @Created by glmapper
 */
public class DocumentProcessingException extends RuntimeException {
    
    public DocumentProcessingException(String message) {
        super(message);
    }
    
    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}