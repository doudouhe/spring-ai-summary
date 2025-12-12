package com.glmapper.ai.rag.service;

import com.glmapper.ai.rag.RagApplication;
import com.glmapper.ai.rag.transformers.DocTokenTextSplitter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @Classname OptimizedRagServiceTest
 * @Description Test for OptimizedRagService
 * @Date 2025/6/12
 * @Created by glmapper
 */
@SpringBootTest(classes = RagApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OptimizedRagServiceTest {

    @Autowired
    private OptimizedRagService ragService;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private DocTokenTextSplitter textSplitter;

    @Test
    public void testSearchSimilar() {
        // Given
        String query = "test query";
        List<Document> mockResults = Arrays.asList(
            new Document("Result 1", Map.of("score", 0.9)),
            new Document("Result 2", Map.of("score", 0.8))
        );
        
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockResults);

        // When
        List<Document> results = ragService.searchSimilar(query);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    public void testAddDocumentsAsync() throws Exception {
        // Given
        List<Document> documents = Arrays.asList(
            new Document("Document 1"),
            new Document("Document 2")
        );
        
        List<Document> splitDocuments = Arrays.asList(
            new Document("Split 1"),
            new Document("Split 2"),
            new Document("Split 3")
        );
        
        when(textSplitter.splitDocumentsOptimized(documents)).thenReturn(splitDocuments);
        doNothing().when(vectorStore).add(any(List.class));

        // When
        CompletableFuture<Void> future = ragService.addDocumentsAsync(documents);
        future.get(); // Wait for completion

        // Then
        verify(textSplitter, times(1)).splitDocumentsOptimized(documents);
        verify(vectorStore, times(1)).add(splitDocuments);
    }

    @Test
    public void testDeleteDocuments() {
        // Given
        List<String> documentIds = Arrays.asList("id1", "id2");
        doNothing().when(vectorStore).delete(documentIds);

        // When
        ragService.deleteDocuments(documentIds);

        // Then
        verify(vectorStore, times(1)).delete(documentIds);
    }

    @Test
    public void testGetStats() {
        // When
        OptimizedRagService.VectorStoreStats stats = ragService.getStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.isConnected());
    }

    @Test
    public void testIsHealthy() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(Arrays.asList(new Document("test")));

        // When
        boolean healthy = ragService.isHealthy();

        // Then
        assertTrue(healthy);
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    public void testIsHealthy_Unhealthy() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // When
        boolean healthy = ragService.isHealthy();

        // Then
        assertFalse(healthy);
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }
}