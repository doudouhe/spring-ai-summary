package com.glmapper.ai.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for thread pools used by Orchestrator Workers
 */
@Configuration
public class ThreadPoolConfig {

    private final OrchestratorWorkersProperties properties;

    public ThreadPoolConfig(OrchestratorWorkersProperties properties) {
        this.properties = properties;
    }

    @Bean("orchestratorExecutorService")
    public ExecutorService orchestratorExecutorService() {
        return Executors.newFixedThreadPool(properties.getThreadPoolSize());
    }
}