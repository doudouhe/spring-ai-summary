package com.glmapper.ai.rag.controller;

import com.glmapper.ai.rag.RagApplication;
import com.glmapper.ai.rag.service.OptimizedRagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @Classname OptimizedRagControllerTest
 * @Description Test for OptimizedRagController
 * @Date 2025/6/12
 * @Created by glmapper
 */
@WebMvcTest(OptimizedRagController.class)
@ActiveProfiles("test")
public class OptimizedRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OptimizedRagService ragService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSearch() throws Exception {
        // Given
        String query = "test query";
        when(ragService.searchSimilar(eq(query), eq(5), eq(0.7)))
            .thenReturn(Arrays.asList(
                new org.springframework.ai.document.Document("Result 1"),
                new org.springframework.ai.document.Document("Result 2")
            ));

        // When & Then
        mockMvc.perform(get("/api/rag/search")
                .param("query", query)
                .param("topK", "5")
                .param("threshold", "0.7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value(query))
                .andExpect(jsonPath("$.totalResults").value(2))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].content").value("Result 1"));
    }

    @Test
    public void testAddDocuments() throws Exception {
        // Given
        List<OptimizedRagController.DocumentRequest> documents = Arrays.asList(
            new OptimizedRagController.DocumentRequest("Content 1", Map.of("key", "value")),
            new OptimizedRagController.DocumentRequest("Content 2", Map.of())
        );
        
        when(ragService.addDocumentsAsync(any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // When & Then
        mockMvc.perform(post("/api/rag/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(documents)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Documents added successfully"))
                .andExpect(jsonPath("$.count").value("2"));
    }

    @Test
    public void testDeleteDocuments() throws Exception {
        // Given
        List<String> documentIds = Arrays.asList("id1", "id2");
        doNothing().when(ragService).deleteDocuments(documentIds);

        // When & Then
        mockMvc.perform(delete("/api/rag/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(documentIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Documents deleted successfully"))
                .andExpect(jsonPath("$.count").value("2"));
    }

    @Test
    public void testGetStats() throws Exception {
        // Given
        OptimizedRagService.VectorStoreStats stats = OptimizedRagService.VectorStoreStats.builder()
                .connected(true)
                .collectionName("test_collection")
                .documentCount(100)
                .build();
        when(ragService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/rag/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.collectionName").value("test_collection"))
                .andExpect(jsonPath("$.documentCount").value(100));
    }

    @Test
    public void testHealth_Up() throws Exception {
        // Given
        when(ragService.isHealthy()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/rag/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.vectorStore").value("connected"));
    }

    @Test
    public void testHealth_Down() throws Exception {
        // Given
        when(ragService.isHealthy()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/rag/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.vectorStore").value("disconnected"));
    }

    @Test
    public void testRebuildIndex() throws Exception {
        // Given
        when(ragService.rebuildIndexAsync())
            .thenReturn(CompletableFuture.completedFuture(null));

        // When & Then
        mockMvc.perform(post("/api/rag/rebuild-index"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Index rebuild started successfully"));
    }
}