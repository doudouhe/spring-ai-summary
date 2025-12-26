package com.glmapper.ai.rag.exception;

/**
 * @Classname VectorStoreException
 * @Description Exception for vector store operations
 * @Date 2025/6/12
 * @Created by glmapper
 */
public class VectorStoreException extends RuntimeException {
    
    public VectorStoreException(String message) {
        super(message);
    }
    
    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}