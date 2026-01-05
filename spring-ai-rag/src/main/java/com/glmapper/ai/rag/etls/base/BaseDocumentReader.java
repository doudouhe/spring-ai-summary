package com.glmapper.ai.rag.etls.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @Classname BaseDocumentReader
 * @Description Base document reader with caching and async processing
 * @Date 2025/6/12
 * @Created by glmapper
 */
@Slf4j
public abstract class BaseDocumentReader {
    
    private static final Executor asyncExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        r -> {
            Thread t = new Thread(r, "document-reader-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        }
    );
    
    /**
     * Template method for reading documents
     */
    protected abstract List<Document> doReadDocuments(Resource resource) throws IOException;
    
    /**
     * Read documents with caching support
     */
    @Cacheable(value = "documents", key = "#resource.getFilename() + '_' + #resource.hashCode()")
    public List<Document> readDocuments(Resource resource) {
        try {
            log.debug("Reading documents from resource: {}", resource.getFilename());
            List<Document> documents = doReadDocuments(resource);
            log.debug("Successfully read {} documents from {}", documents.size(), resource.getFilename());
            return documents;
        } catch (IOException e) {
            log.error("Failed to read documents from resource: {}", resource.getFilename(), e);
            throw new RuntimeException("Failed to read documents", e);
        }
    }
    
    /**
     * Read documents asynchronously
     */
    public CompletableFuture<List<Document>> readDocumentsAsync(Resource resource) {
        return CompletableFuture.supplyAsync(() -> readDocuments(resource), asyncExecutor);
    }
    
    /**
     * Validate resource before processing
     */
    protected void validateResource(Resource resource) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        if (!resource.exists()) {
            throw new IllegalArgumentException("Resource does not exist: " + resource.getFilename());
        }
        if (!resource.isReadable()) {
            throw new IllegalArgumentException("Resource is not readable: " + resource.getFilename());
        }
    }
    
    /**
     * Get resource metadata
     */
    protected void addResourceMetadata(List<Document> documents, Resource resource) {
        try {
            String filename = resource.getFilename();
            long contentLength = resource.contentLength();
            
            documents.forEach(doc -> {
                doc.getMetadata().put("source_filename", filename);
                doc.getMetadata().put("source_size", contentLength);
                doc.getMetadata().put("processed_at", System.currentTimeMillis());
            });
        } catch (IOException e) {
            log.warn("Failed to add resource metadata", e);
        }
    }
}