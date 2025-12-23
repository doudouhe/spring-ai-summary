package com.glmapper.ai.evaluator.optimizer.service;

import com.glmapper.ai.evaluator.optimizer.config.EvaluatorOptimizerProperties;
import com.glmapper.ai.evaluator.optimizer.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Main service implementing the Evaluator-Optimizer pattern workflow
 *
 * @author glmapper
 */
@Service
@Slf4j
public class EvaluatorOptimizerService {

    private final GeneratorService generatorService;
    private final EvaluatorService evaluatorService;
    private final EvaluatorOptimizerProperties properties;

    public EvaluatorOptimizerService(GeneratorService generatorService, EvaluatorService evaluatorService,
                                   EvaluatorOptimizerProperties properties) {
        this.generatorService = generatorService;
        this.evaluatorService = evaluatorService;
        this.properties = properties;
    }

    /**
     * Execute the evaluator-optimizer loop
     *
     * @param task The task to solve
     * @return RefinedResponse containing the final solution and evolution trace
     */
    public RefinedResponse loop(String task) {
        log.info("Starting evaluator-optimizer loop for task: {}", task);
        return loop(task, null, null);
    }

    /**
     * Execute the evaluator-optimizer loop with context and custom criteria
     *
     * @param task The task to solve
     * @param context Additional context for the task
     * @param criteria Custom evaluation criteria
     * @return RefinedResponse containing the final solution and evolution trace
     */
    public RefinedResponse loop(String task, String context, String criteria) {
        log.info("Evaluator-Optimizer loop started with task: {}, context: {}, criteria: {}", task, context, criteria);

        List<String> chainOfThought = new ArrayList<>();
        List<EvaluationResponse> evaluationHistory = new ArrayList<>();

        String currentSolution = null;
        EvaluationResponse lastEvaluation = null;
        int iteration = 0;
        boolean converged = false;

        chainOfThought.add("Starting Evaluator-Optimizer loop for task: " + task);

        while (iteration < properties.getMaxIterations() && !converged) {
            iteration++;
            log.debug("Starting iteration {} of evaluator-optimizer loop", iteration);

            // Generation Phase
            GenerationRequest genRequest = createGenerationRequest(task, context, currentSolution, lastEvaluation, iteration);
            GenerationResponse generation = generatorService.generate(genRequest);
            currentSolution = generation.getSolution();

            log.debug("Generated solution for iteration {}: {}", iteration, truncateForDisplay(generation.getSolution()));
            chainOfThought.add(String.format("Iteration %d - Generated solution: %s", iteration,
                                            truncateForDisplay(generation.getSolution())));
            chainOfThought.add(String.format("Iteration %d - Reasoning: %s", iteration, generation.getReasoning()));

            // Evaluation Phase
            EvaluationRequest evalRequest = new EvaluationRequest(task, currentSolution, criteria, iteration);
            EvaluationResponse evaluation = evaluatorService.evaluate(evalRequest);
            evaluationHistory.add(evaluation);
            lastEvaluation = evaluation;

            log.debug("Evaluation for iteration {}: Score: {}, Acceptable: {}",
                     iteration, evaluation.getScore(), evaluation.isAcceptable());
            chainOfThought.add(String.format("Iteration %d - Score: %.1f, Acceptable: %s",
                                            iteration, evaluation.getScore(), evaluation.isAcceptable()));
            chainOfThought.add(String.format("Iteration %d - Feedback: %s", iteration, evaluation.getFeedback()));

            // Check convergence
            if (evaluation.isAcceptable() && evaluation.getScore() >= properties.getConvergenceThreshold()) {
                converged = true;
                log.info("Evaluator-optimizer loop converged at iteration {} with score {}", iteration, evaluation.getScore());
                chainOfThought.add(String.format("Converged at iteration %d with score %.1f", iteration, evaluation.getScore()));
            } else if (iteration == properties.getMaxIterations()) {
                log.warn("Reached maximum iterations ({}) without convergence", properties.getMaxIterations());
                chainOfThought.add(String.format("Reached maximum iterations (%d) without convergence", properties.getMaxIterations()));
            } else {
                log.debug("Iteration {} - Continuing with improvements: {}",
                         iteration, evaluation.getImprovementSuggestions());
                chainOfThought.add(String.format("Iteration %d - Continuing with improvements: %s",
                                                iteration, evaluation.getImprovementSuggestions()));
            }
        }

        double finalScore = lastEvaluation != null ? lastEvaluation.getScore() : 0.0;
        log.info("Evaluator-optimizer loop completed. Converged: {}, Total iterations: {}, Final score: {}",
                converged, iteration, finalScore);

        return new RefinedResponse(currentSolution, chainOfThought, evaluationHistory, iteration, finalScore, converged);
    }

    private GenerationRequest createGenerationRequest(String task, String context, String currentSolution, 
                                                    EvaluationResponse lastEvaluation, int iteration) {
        if (iteration == 1) {
            // First iteration - no previous attempt
            return new GenerationRequest(task, context);
        } else {
            // Subsequent iterations - include previous attempt and feedback
            String feedback = lastEvaluation != null ? 
                String.format("Score: %.1f. %s. Improvements needed: %s", 
                            lastEvaluation.getScore(), 
                            lastEvaluation.getFeedback(),
                            lastEvaluation.getImprovementSuggestions()) : 
                "No specific feedback available";
            
            return new GenerationRequest(task, context, currentSolution, feedback);
        }
    }

    private String truncateForDisplay(String text) {
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}