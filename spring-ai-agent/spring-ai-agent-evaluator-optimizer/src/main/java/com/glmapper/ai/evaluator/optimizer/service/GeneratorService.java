package com.glmapper.ai.evaluator.optimizer.service;

import com.glmapper.ai.evaluator.optimizer.model.GenerationRequest;
import com.glmapper.ai.evaluator.optimizer.model.GenerationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating solutions
 *
 * @author glmapper
 */
@Service
@Slf4j
public class GeneratorService {

    private final ChatClient chatClient;

    private static final String GENERATION_PROMPT =
            "Task: {task}\n" +
            "\n" +
            "{context}\n" +
            "\n" +
            "{refinement_instructions}\n" +
            "\n" +
            "Please provide a solution that is:\n" +
            "1. Complete and functional\n" +
            "2. Well-structured and clear\n" +
            "3. Addresses all requirements\n" +
            "4. Includes explanatory comments where needed\n" +
            "\n" +
            "Provide your reasoning for the approach you chose.";

    private static final String REFINEMENT_PROMPT =
            "Task: {task}\n" +
            "\n" +
            "Previous attempt:\n" +
            "{previous_attempt}\n" +
            "\n" +
            "Feedback received:\n" +
            "{feedback}\n" +
            "\n" +
            "Please improve the solution based on the feedback. Focus on:\n" +
            "1. Addressing the specific issues mentioned in the feedback\n" +
            "2. Maintaining what worked well in the previous attempt\n" +
            "3. Ensuring the solution is better than before\n" +
            "\n" +
            "Provide your reasoning for the improvements made.";

    public GeneratorService(@Qualifier("generatorChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Cacheable(value = "generationCache", key = "#request.task + '_' + (#request.context != null ? #request.context : '') + '_' + (#request.previousAttempt != null ? #request.previousAttempt.hashCode() : '') + '_' + (#request.feedback != null ? #request.feedback.hashCode() : '')")
    public GenerationResponse generate(GenerationRequest request) {
        log.debug("Starting generation for task: {}", request.getTask());
        String prompt = buildPrompt(request);

        log.debug("Generated prompt for AI model: {}", prompt.substring(0, Math.min(prompt.length(), 500)) + "...");

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Received response from AI model: {} characters", response.length());

            // Extract solution and reasoning from the response
            String solution;
            String reasoning;

            // Look for various possible reasoning separators
            if (response.contains("Reasoning:")) {
                String[] parts = response.split("Reasoning:");
                solution = parts[0].trim();
                reasoning = parts.length > 1 ? parts[1].trim() : "No reasoning provided";
            } else if (response.contains("Reason:")) {
                String[] parts = response.split("Reason:");
                solution = parts[0].trim();
                reasoning = parts.length > 1 ? parts[1].trim() : "No reasoning provided";
            } else if (response.contains("Thinking:")) {
                String[] parts = response.split("Thinking:");
                solution = parts[0].trim();
                reasoning = parts.length > 1 ? parts[1].trim() : "No reasoning provided";
            } else {
                // If no explicit reasoning separator, return the full response as solution
                // and provide a default reasoning
                solution = response;
                reasoning = "No explicit reasoning provided by the model";
            }

            log.debug("Successfully generated solution and reasoning");
            return new GenerationResponse(solution, reasoning, request.getPreviousAttempt() == null ? 1 : 2);
        } catch (Exception e) {
            log.error("Error generating solution for task: {}", request.getTask(), e);
            // Return an error response in case of AI model failure
            return new GenerationResponse(
                "Error generating solution: " + e.getMessage(),
                "An error occurred while communicating with the AI model",
                request.getPreviousAttempt() == null ? 1 : 2
            );
        }
    }

    private String buildPrompt(GenerationRequest request) {
        if (request.getPreviousAttempt() == null) {
            // Initial generation
            return GENERATION_PROMPT
                    .replace("{task}", request.getTask())
                    .replace("{context}", request.getContext() != null ? "Context: " + request.getContext() : "")
                    .replace("{refinement_instructions}", "");
        } else {
            // Refinement generation
            return REFINEMENT_PROMPT
                    .replace("{task}", request.getTask())
                    .replace("{previous_attempt}", request.getPreviousAttempt())
                    .replace("{feedback}", request.getFeedback() != null ? request.getFeedback() : "No specific feedback provided");
        }
    }
}