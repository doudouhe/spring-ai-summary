package com.glmapper.ai.workflow.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glmapper.ai.workflow.config.OrchestratorWorkersProperties;
import com.glmapper.ai.workflow.workflow.model.WorkflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @Classname OrchestratorWorkersWorkflow
 * @Description Orchestrator 用大模型拆解任务，Worker 并行处理，合并结果
 * @Date 2025/6/12 20:19
 * @Created by glmapper
 */

@Component
@Slf4j
public class OrchestratorWorkersWorkflow {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private OrchestratorWorkersProperties properties;

    @Autowired
    private ExecutorService orchestratorExecutorService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public WorkflowResponse process(String taskDescription) {
        log.info("Starting orchestrator workflow for task: {}", taskDescription);
        try {
            // 1. 用大模型拆解任务
            LlmSubtaskResult subtaskResult = callLlmForSubtasks(taskDescription);
            List<String> subtasks = subtaskResult.subtasks;
            String analysis = subtaskResult.analysis;
            log.debug("Task decomposition completed. Found {} subtasks", subtasks != null ? subtasks.size() : 0);

            if (subtasks == null || subtasks.isEmpty()) {
                log.warn("AI model failed to decompose the task into subtasks");
                return WorkflowResponse.builder()
                        .success(false)
                        .errorMessage("大模型未能拆解出子任务")
                        .analysis(analysis)
                        .subtasks(subtasks)
                        .build();
            }

            // 2. Workers process subtasks in parallel using configurable thread pool
            List<String> workerResponses;
            if (properties.isParallelExecution()) {
                log.debug("Starting parallel execution with thread pool for {} subtasks", subtasks.size());
                workerResponses = subtasks.stream()
                        .map(subtask -> CompletableFuture.supplyAsync(() -> workerProcess(subtask), orchestratorExecutorService))
                        .collect(Collectors.toList())
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());
                log.debug("Parallel execution completed");
            } else {
                log.debug("Starting sequential execution for {} subtasks", subtasks.size());
                // Sequential execution if parallel execution is disabled
                workerResponses = subtasks.stream()
                        .map(this::workerProcess)
                        .collect(Collectors.toList());
                log.debug("Sequential execution completed");
            }

            // 3. Results are combined into final response
            String combined = String.join("\n", workerResponses);
            log.info("Orchestrator workflow completed successfully for task: {}", taskDescription);
            return WorkflowResponse.builder()
                    .content(combined)
                    .success(true)
                    .analysis(analysis)
                    .subtasks(subtasks)
                    .workerResponses(workerResponses)
                    .build();
        } catch (Exception e) {
            log.error("Orchestrator/Worker execution failed for task: {}", taskDescription, e);
            return WorkflowResponse.builder()
                    .success(false)
                    .errorMessage("Orchestrator/Worker 执行失败: " + e.getMessage())
                    .build();
        }
    }

    // 用大模型拆解任务，返回原始分析和子任务列表
    private LlmSubtaskResult callLlmForSubtasks(String taskDescription) throws Exception {
        List messages = List.of(
            new SystemMessage(properties.getTaskDecompositionPrompt()),
            new UserMessage(taskDescription)
        );
        Prompt prompt = new Prompt(messages);
        String modelResult;
        try {
            modelResult = chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            // Return error result if AI model call fails
            return new LlmSubtaskResult("Error: Failed to call AI model - " + e.getMessage(), null);
        }

        // 解析为 List<String>
        List<String> subtasks;
        try {
            subtasks = OBJECT_MAPPER.readValue(modelResult, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            // 容错：如果模型返回不是严格 JSON 数组，尝试简单分割
            subtasks = Arrays.stream(modelResult.split("[。,.，\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // If still no subtasks after splitting, return error
            if (subtasks.isEmpty()) {
                subtasks = null;
            }
        }
        return new LlmSubtaskResult(modelResult, subtasks);
    }

    // 内部类：封装大模型分析结果
    private record LlmSubtaskResult(String analysis, List<String> subtasks) {
    }

    private String workerProcess(String subtask) {
        log.debug("Starting worker process for subtask: {}", subtask.substring(0, Math.min(subtask.length(), 100)));
        try {
            List messages = List.of(
                new SystemMessage(properties.getWorkerProcessingPrompt()),
                new UserMessage(subtask)
            );
            Prompt prompt = new Prompt(messages);
            // 直接用 chatClient 让大模型"执行"子任务
            String result = chatClient.prompt(prompt).call().content();
            log.debug("Completed worker process for subtask, result length: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("Worker process failed for subtask: {}", subtask, e);
            // 失败时返回错误信息，便于排查
            return "[Worker Error] " + e.getMessage();
        }
    }
}
