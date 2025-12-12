package com.glmapper.ai.rag.controller;

import com.glmapper.ai.rag.service.OptimizedRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @Classname OptimizedRagController
 * @Description Optimized REST controller for RAG operations
 * @Date 2025/6/12
 * @Created by glmapper
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
public class OptimizedRagController {

    @Autowired
    private OptimizedRagService ragService;

    /**
     * Search for similar documents
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.7") double threshold) {
        
        log.info("Received search request: query='{}', topK={}, threshold={}", 
            query.substring(0, Math.min(50, query.length())), topK, threshold);
        
        List<Document> results = ragService.searchSimilar(query, topK, threshold);
        
        SearchResponse response = SearchResponse.builder()
                .query(query)
                .results(results.stream()
                    .map(doc -> DocumentResult.builder()
                        .content(doc.getText())
                        .metadata(doc.getMetadata())
                        .score(doc.getMetadata().getOrDefault("distance", 0.0).toString())
                        .build())
                    .collect(Collectors.toList()))
                .totalResults(results.size())
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Add documents to vector store
     */
    @PostMapping("/documents")
    public CompletableFuture<ResponseEntity<Map<String, String>>> addDocuments(
            @RequestBody List<DocumentRequest> documentRequests) {
        
        log.info("Received request to add {} documents", documentRequests.size());
        
        List<Document> documents = documentRequests.stream()
            .map(req -> new Document(req.getContent(), req.getMetadata()))
            .collect(Collectors.toList());
        
        return ragService.addDocumentsAsync(documents)
            .thenApply(v -> ResponseEntity.ok(Map.of(
                "message", "Documents added successfully",
                "count", String.valueOf(documents.size())
            )))
            .exceptionally(throwable -> {
                log.error("Failed to add documents", throwable);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to add documents: " + throwable.getMessage()
                ));
            });
    }

    /**
     * Upload and process file
     */
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "auto") String processor) {
        
        log.info("Received file upload: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // File processing logic would go here
                // For now, just return success
                return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "filename", file.getOriginalFilename(),
                    "size", String.valueOf(file.getSize())
                ));
            } catch (Exception e) {
                log.error("Failed to process file", e);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process file: " + e.getMessage()
                ));
            }
        });
    }

    /**
     * Delete documents by IDs
     */
    @DeleteMapping("/documents")
    public ResponseEntity<Map<String, String>> deleteDocuments(
            @RequestBody List<String> documentIds) {
        
        log.info("Received request to delete {} documents", documentIds.size());
        
        try {
            ragService.deleteDocuments(documentIds);
            return ResponseEntity.ok(Map.of(
                "message", "Documents deleted successfully",
                "count", String.valueOf(documentIds.size())
            ));
        } catch (Exception e) {
            log.error("Failed to delete documents", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to delete documents: " + e.getMessage()
            ));
        }
    }

    /**
     * Get vector store statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<OptimizedRagService.VectorStoreStats> getStats() {
        OptimizedRagService.VectorStoreStats stats = ragService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean healthy = ragService.isHealthy();
        Map<String, Object> health = Map.of(
            "status", healthy ? "UP" : "DOWN",
            "timestamp", System.currentTimeMillis(),
            "vectorStore", healthy ? "connected" : "disconnected"
        );
        
        return ResponseEntity.status(healthy ? 200 : 503).body(health);
    }

    /**
     * Rebuild index
     */
    @PostMapping("/rebuild-index")
    public CompletableFuture<ResponseEntity<Map<String, String>>> rebuildIndex() {
        log.info("Received request to rebuild index");
        
        return ragService.rebuildIndexAsync()
            .thenApply(v -> ResponseEntity.ok(Map.of(
                "message", "Index rebuild started successfully"
            )))
            .exceptionally(throwable -> {
                log.error("Failed to rebuild index", throwable);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to rebuild index: " + throwable.getMessage()
                ));
            });
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class SearchResponse {
        private String query;
        private List<DocumentResult> results;
        private int totalResults;
    }

    @lombok.Data
    @lombok.Builder
    public static class DocumentResult {
        private String content;
        private Map<String, Object> metadata;
        private String score;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DocumentRequest {
        private String content;
        private Map<String, Object> metadata;
    }
}