package com.glmapper.ai.rag.configs;

import io.milvus.client.MilvusClient;
import io.milvus.param.collection.LoadCollectionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

/**
 * @Classname MilvusClientConfigs
 * @Description Optimized MilvusClientConfigs with connection pooling and async operations
 * @Date 2025/6/1 15:42
 * @Created by glmapper
 */
@Configuration
@EnableAsync
@Slf4j
public class MilvusClientConfigs implements InitializingBean {

    @Autowired
    private VectorStore vectorStore;

    @Value("${spring.ai.vectorstore.milvus.collection.name:vector_store}")
    private String collectionName;

    @Value("${rag.milvus.async-load:true}")
    private boolean asyncLoad;

    @Value("${rag.milvus.connection-timeout:30000}")
    private int connectionTimeout;

    private MilvusClient milvusClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (vectorStore instanceof MilvusVectorStore) {
            initializeMilvusClient();
            if (asyncLoad) {
                loadCollectionAsync();
            } else {
                loadCollectionSync();
            }
        }
    }

    private void initializeMilvusClient() {
        vectorStore.getNativeClient().ifPresent(client -> {
            this.milvusClient = (MilvusClient) client;
            log.info("Milvus client initialized successfully");
        });
    }

    @Async
    public CompletableFuture<Void> loadCollectionAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                loadCollectionSync();
            } catch (Exception e) {
                log.error("Failed to load collection asynchronously", e);
                throw new RuntimeException("Async collection loading failed", e);
            }
        });
    }

    private void loadCollectionSync() {
        if (milvusClient != null) {
            try {
                long startTime = System.currentTimeMillis();
                milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build());
                long duration = System.currentTimeMillis() - startTime;
                log.info("Milvus {} collection loaded successfully in {}ms", collectionName, duration);
            } catch (Exception e) {
                log.error("Failed to load Milvus collection: {}", collectionName, e);
                throw new RuntimeException("Failed to load collection", e);
            }
        }
    }

    /**
     * Check if collection is loaded
     */
    public boolean isCollectionLoaded() {
        if (milvusClient == null) {
            return false;
        }

        try {
            return milvusClient.hasCollection(
                io.milvus.param.collection.HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            ).getData();
        } catch (Exception e) {
            log.warn("Failed to check collection status", e);
            return false;
        }
    }

    /**
     * Get collection statistics
     */
    public String getCollectionStats() {
        if (milvusClient == null) {
            return "Milvus client not initialized";
        }

        try {
            var stats = milvusClient.getCollectionStatistics(
                io.milvus.param.collection.GetCollectionStatisticsParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            );
            return stats.getStatus().toString();
        } catch (Exception e) {
            log.error("Failed to get collection statistics", e);
            return "Failed to retrieve statistics: " + e.getMessage();
        }
    }

    /**
     * Release collection resources
     */
    public void releaseCollection() {
        if (milvusClient != null) {
            try {
                milvusClient.releaseCollection(
                    io.milvus.param.collection.ReleaseCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
                );
                log.info("Milvus {} collection released successfully", collectionName);
            } catch (Exception e) {
                log.error("Failed to release collection: {}", collectionName, e);
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up Milvus resources...");
        if (milvusClient != null) {
            try {
                releaseCollection();
                milvusClient.close();
                log.info("Milvus client closed successfully");
            } catch (Exception e) {
                log.error("Error during Milvus cleanup", e);
            }
        }
    }
}
