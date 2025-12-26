package com.glmapper.ai.evaluator.optimizer.service;

import com.glmapper.ai.evaluator.optimizer.model.EvaluationRequest;
import com.glmapper.ai.evaluator.optimizer.model.EvaluationResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for evaluating generated solutions
 * 
 * @author glmapper
 */
@Slf4j
@Service
public class EvaluatorService {

    private final ChatClient chatClient;
    private final EvaluatorOptimizerProperties properties;

    private static final String EVALUATION_PROMPT =
            "You are an expert evaluator. Please evaluate the following solution against the given task and criteria.\n" +
            "\n" +
            "Original Task: {task}\n" +
            "\n" +
            "Solution to Evaluate:\n" +
            "{solution}\n" +
            "\n" +
            "Evaluation Criteria: {criteria}\n" +
            "\n" +
            "Please provide:\n" +
            "1. A score from 0.0 to 10.0 (where 10.0 is perfect)\n" +
            "2. Detailed feedback on strengths and weaknesses\n" +
            "3. Whether this solution is acceptable (YES/NO)\n" +
            "4. Specific suggestions for improvement\n" +
            "\n" +
            "Format your response as:\n" +
            "SCORE: [0.0-10.0]\n" +
            "FEEDBACK: [detailed feedback]\n" +
            "ACCEPTABLE: [YES/NO]\n" +
            "IMPROVEMENTS: [specific suggestions]";

    public EvaluatorService(@Qualifier("evaluatorChatClient") ChatClient chatClient,
                          EvaluatorOptimizerProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    @Cacheable(value = "evaluationCache", key = "#request.originalTask + '_' + #request.solution + '_' + (#request.criteria != null ? #request.criteria : '')")
    public EvaluationResponse evaluate(EvaluationRequest request) {
        log.debug("Starting evaluation for task: {}, iteration: {}", request.getOriginalTask(), request.getIteration());

        String prompt = EVALUATION_PROMPT
                .replace("{task}", request.getOriginalTask())
                .replace("{solution}", request.getSolution())
                .replace("{criteria}", request.getCriteria() != null ? request.getCriteria() : getDefaultCriteria());

        log.debug("Generated evaluation prompt: {}", prompt.substring(0, Math.min(prompt.length(), 500)) + "...");

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("Received evaluation response from AI model: {} characters", response.length());

        EvaluationResponse evaluationResponse = parseEvaluationResponse(response, request.getIteration());
        log.debug("Parsed evaluation result - Score: {}, Acceptable: {}, Iteration: {}",
                 evaluationResponse.getScore(), evaluationResponse.isAcceptable(), request.getIteration());

        return evaluationResponse;
    }

    private EvaluationResponse parseEvaluationResponse(String response, int iteration) {
        try {
            double score = extractScore(response);
            String feedback = extractFeedback(response);
            boolean isAcceptable = extractAcceptable(response) || score >= properties.getAcceptableThreshold();
            String improvements = extractImprovements(response);

            return new EvaluationResponse(score, feedback, isAcceptable, improvements, iteration);
        } catch (Exception e) {
            // Fallback in case of parsing errors
            return new EvaluationResponse(5.0, response, false, "Unable to parse specific improvements", iteration);
        }
    }

    private double extractScore(String response) {
        try {
            String scoreLine = findLine(response, "SCORE:");
            if (scoreLine == null) {
                return 5.0; // Default score if not found
            }
            String scoreStr = scoreLine.substring(scoreLine.indexOf(":") + 1).trim();
            // Extract numeric value, allowing for various formats
            scoreStr = scoreStr.replaceAll("[^0-9.]", "");
            return Double.parseDouble(scoreStr);
        } catch (Exception e) {
            return 5.0; // Default score
        }
    }

    private String extractFeedback(String response) {
        try {
            String feedbackLine = findLine(response, "FEEDBACK:");
            if (feedbackLine == null) {
                return "No detailed feedback available";
            }
            return feedbackLine.substring(feedbackLine.indexOf(":") + 1).trim();
        } catch (Exception e) {
            // Try to extract feedback even if format is not exact
            return extractSection(response, "FEEDBACK:", "ACCEPTABLE:");
        }
    }

    private boolean extractAcceptable(String response) {
        try {
            String acceptableLine = findLine(response, "ACCEPTABLE:");
            if (acceptableLine == null) {
                return false;
            }
            return acceptableLine.toUpperCase().contains("YES") || acceptableLine.contains("1");
        } catch (Exception e) {
            return false;
        }
    }

    private String extractImprovements(String response) {
        try {
            String improvementsLine = findLine(response, "IMPROVEMENTS:");
            if (improvementsLine == null) {
                return "No specific improvements suggested";
            }
            return improvementsLine.substring(improvementsLine.indexOf(":") + 1).trim();
        } catch (Exception e) {
            // Try to extract improvements even if format is not exact
            return extractSection(response, "IMPROVEMENTS:", null);
        }
    }

    private String findLine(String response, String prefix) {
        String[] lines = response.split("\n|\\r\\n");
        for (String line : lines) {
            if (line.trim().toUpperCase().startsWith(prefix.toUpperCase())) {
                return line;
            }
        }
        return null; // Return null instead of throwing exception
    }

    /**
     * Extract content between two markers in the response
     */
    private String extractSection(String response, String startMarker, String endMarker) {
        try {
            int startIndex = response.toUpperCase().indexOf(startMarker.toUpperCase());
            if (startIndex == -1) {
                return "No " + startMarker + " section found";
            }

            startIndex = response.indexOf(":", startIndex) + 1; // Move past the colon

            int endIndex = response.length();
            if (endMarker != null) {
                endIndex = response.toUpperCase().indexOf(endMarker.toUpperCase(), startIndex);
                if (endIndex == -1) endIndex = response.length();
            }

            return response.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return "Could not extract " + startMarker + " section";
        }
    }

    private String getDefaultCriteria() {
        return String.format("- Correctness: Does the solution solve the problem correctly?\n" +
               "- Completeness: Are all requirements addressed?\n" +
               "- Clarity: Is the solution easy to understand?\n" +
               "- Best Practices: Does it follow good coding/design practices?\n" +
               "- Efficiency: Is the solution reasonably efficient?\n" +
               "- Minimum acceptable score: %.1f", properties.getAcceptableThreshold());
    }
}