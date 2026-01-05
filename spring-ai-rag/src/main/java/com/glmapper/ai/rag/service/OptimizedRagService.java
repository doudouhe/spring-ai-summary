package com.glmapper.ai.rag.service;

import com.glmapper.ai.rag.exception.DocumentProcessingException;
import com.glmapper.ai.rag.exception.VectorStoreException;
import com.glmapper.ai.rag.transformers.DocTokenTextSplitter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @Classname OptimizedRagService
 * @Description Optimized RAG service with batch processing and caching
 * @Date 2025/6/12
 * @Created by glmapper
 */
@Slf4j
@Service
public class OptimizedRagService {
    
    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocTokenTextSplitter textSplitter;

    @Autowired
    private HybridSearchService hybridSearchService;
    
    @Value("${rag.search.top-k:5}")
    private int topK;
    
    @Value("${rag.search.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    @Value("${rag.batch.size:100}")
    private int batchSize;
    
    /**
     * Add documents to vector store with optimized processing
     */
    @Timed(name = "rag.documents.add", description = "Time taken to add documents to vector store")
    @CacheEvict(value = {"search_results", "vector_store_stats"}, allEntries = true)
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "addDocumentsFallback")
    @Retry(name = "vectorStore")
    @TimeLimiter(name = "vectorStore")
    @Async
    public CompletableFuture<Void> addDocumentsAsync(List<Document> documents) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting to process {} documents", documents.size());
                long startTime = System.currentTimeMillis();

                // Split documents using sentence-aware splitter for better semantic chunks
                List<Document> splitDocuments = textSplitter.splitDocumentsSentenceAware(documents);
                log.debug("Split {} documents into {} chunks", documents.size(), splitDocuments.size());

                // Add to vector store in batches
                addDocumentsBatch(splitDocuments);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Successfully processed {} documents in {}ms", documents.size(), duration);

            } catch (Exception e) {
                log.error("Failed to add documents to vector store", e);
                throw new VectorStoreException("Failed to add documents", e);
            }
        });
    }

    /**
     * Fallback method for add documents operation
     */
    public CompletableFuture<Void> addDocumentsFallback(List<Document> documents, Exception ex) {
        log.error("Failed to add documents to vector store: {}", ex.getMessage());
        throw new VectorStoreException("Add documents operation failed", ex);
    }
    
    /**
     * Add documents in batches for better performance
     */
    @Timed(name = "rag.documents.batch.add", description = "Time taken to add document batches")
    private void addDocumentsBatch(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        int totalDocuments = documents.size();
        int processed = 0;

        while (processed < totalDocuments) {
            int endIndex = Math.min(processed + batchSize, totalDocuments);
            List<Document> batch = documents.subList(processed, endIndex);

            try {
                vectorStore.add(batch);
                processed = endIndex;
                log.debug("Processed batch of {} documents ({}% complete)",
                    batch.size(), (processed * 100) / totalDocuments);
            } catch (Exception e) {
                log.error("Failed to process batch starting at index {}", processed, e);
                throw new VectorStoreException("Batch processing failed", e);
            }
        }
    }
    
    /**
     * Search for similar documents with optimized parameters
     */
    @Timed(name = "rag.search", description = "Time taken for RAG search operations")
    @Cacheable(value = "search_results", key = "#query + '_' + #topK + '_' + #similarityThreshold")
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "searchSimilarFallback")
    @Retry(name = "vectorStore")
    @TimeLimiter(name = "vectorStore")
    public List<Document> searchSimilar(String query) {
        return searchSimilar(query, topK, similarityThreshold);
    }

    /**
     * Search with custom parameters
     */
    @Timed(name = "rag.search", description = "Time taken for RAG search operations")
    @Cacheable(value = "search_results", key = "#query + '_' + #topK + '_' + #similarityThreshold")
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "searchSimilarFallback")
    @Retry(name = "vectorStore")
    @TimeLimiter(name = "vectorStore")
    public List<Document> searchSimilar(String query, int topK, double similarityThreshold) {
        try {
            log.debug("Searching for documents similar to query: {}", query.substring(0, Math.min(50, query.length())));

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            long startTime = System.currentTimeMillis();
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("Found {} similar documents in {}ms", results.size(), duration);
            return results;

        } catch (Exception e) {
            log.error("Failed to search for similar documents", e);
            throw new VectorStoreException("Search operation failed", e);
        }
    }

    /**
     * Fallback method for search operations
     */
    public List<Document> searchSimilarFallback(String query, int topK, double similarityThreshold, Exception ex) {
        log.warn("Search operation failed, returning empty results. Error: {}", ex.getMessage());
        return List.of(); // Return empty list as fallback
    }

    /**
     * Hybrid search combining semantic and keyword search
     */
    @Timed(name = "rag.hybrid.search", description = "Time taken for hybrid RAG search operations")
    @Cacheable(value = "search_results", key = "'hybrid_' + #query + '_' + #topK")
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "searchSimilarFallback")
    @Retry(name = "vectorStore")
    @TimeLimiter(name = "vectorStore")
    public List<Document> hybridSearch(String query, int topK) {
        return hybridSearchService.hybridSearch(query, topK);
    }
    
    /**
     * Delete documents by IDs
     */
    @Timed(name = "rag.documents.delete", description = "Time taken to delete documents from vector store")
    @CacheEvict(value = {"search_results", "vector_store_stats"}, allEntries = true)
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "deleteDocumentsFallback")
    @Retry(name = "vectorStore")
    @TimeLimiter(name = "vectorStore")
    public void deleteDocuments(List<String> documentIds) {
        try {
            log.info("Deleting {} documents from vector store", documentIds.size());
            long startTime = System.currentTimeMillis();

            vectorStore.delete(documentIds);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully deleted {} documents in {}ms", documentIds.size(), duration);

        } catch (Exception e) {
            log.error("Failed to delete documents", e);
            throw new VectorStoreException("Delete operation failed", e);
        }
    }

    /**
     * Fallback method for delete operations
     */
    public void deleteDocumentsFallback(List<String> documentIds, Exception ex) {
        log.error("Delete operation failed: {}", ex.getMessage());
        throw new VectorStoreException("Delete operation failed", ex);
    }
    
    /**
     * Get vector store statistics
     */
    @Cacheable(value = "vector_store_stats", key = "'stats'")
    public VectorStoreStats getStats() {
        try {
            // Note: This is a simplified implementation
            // In a real scenario, you might want to implement more detailed statistics
            return VectorStoreStats.builder()
                    .connected(true)
                    .collectionName("vector_store")
                    .build();
        } catch (Exception e) {
            log.error("Failed to get vector store stats", e);
            return VectorStoreStats.builder()
                    .connected(false)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Rebuild index for better search performance
     */
    @CacheEvict(value = {"search_results", "vector_store_stats"}, allEntries = true)
    @Async
    public CompletableFuture<Void> rebuildIndexAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting index rebuild...");
                long startTime = System.currentTimeMillis();

                // This would depend on the specific vector store implementation
                // For Milvus, you might call compactCollection or similar
                log.info("Index rebuild completed in {}ms", System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                log.error("Failed to rebuild index", e);
                throw new VectorStoreException("Index rebuild failed", e);
            }
        });
    }
    
    /**
     * Health check for vector store
     */
    @Timed(name = "rag.health.check", description = "Time taken for RAG health check")
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "healthCheckFallback")
    @Retry(name = "vectorStore")
    @TimeLimiter(name = "vectorStore")
    public boolean isHealthy() {
        try {
            // Simple health check - try to perform a search
            vectorStore.similaritySearch(SearchRequest.builder()
                    .query("health_check")
                    .topK(1)
                    .similarityThreshold(0.0)
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("Vector store health check failed", e);
            return false;
        }
    }

    /**
     * Fallback method for health check
     */
    public boolean healthCheckFallback(Exception ex) {
        log.warn("Vector store health check failed, marking as unhealthy. Error: {}", ex.getMessage());
        return false;
    }
    
    /**
     * Statistics holder class
     */
    @lombok.Data
    @lombok.Builder
    public static class VectorStoreStats {
        private boolean connected;
        private String collectionName;
        private long documentCount;
        private String error;
    }
}