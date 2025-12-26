package com.glmapper.ai.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Orchestrator Workers module
 */
@Component
@ConfigurationProperties(prefix = "orchestrator-workers")
public class OrchestratorWorkersProperties {

    /**
     * System prompt for task decomposition
     */
    private String taskDecompositionPrompt = "你是一个任务拆解专家。请将用户输入的复杂任务描述拆解为若干可以独立执行的子任务，输出格式为 JSON 数组，每个元素为一个子任务字符串。";

    /**
     * System prompt for worker processing
     */
    private String workerProcessingPrompt = "你是一个高效的AI助手，请认真完成以下子任务：";

    /**
     * Enable/disable parallel execution
     */
    private boolean parallelExecution = true;

    /**
     * Thread pool size for parallel execution
     */
    private int threadPoolSize = 10;

    public String getTaskDecompositionPrompt() {
        return taskDecompositionPrompt;
    }

    public void setTaskDecompositionPrompt(String taskDecompositionPrompt) {
        this.taskDecompositionPrompt = taskDecompositionPrompt;
    }

    public String getWorkerProcessingPrompt() {
        return workerProcessingPrompt;
    }

    public void setWorkerProcessingPrompt(String workerProcessingPrompt) {
        this.workerProcessingPrompt = workerProcessingPrompt;
    }

    public boolean isParallelExecution() {
        return parallelExecution;
    }

    public void setParallelExecution(boolean parallelExecution) {
        this.parallelExecution = parallelExecution;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}