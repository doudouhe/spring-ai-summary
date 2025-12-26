package com.glmapper.ai.evaluator.optimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Evaluator-Optimizer module
 */
@Component
@ConfigurationProperties(prefix = "evaluator-optimizer")
public class EvaluatorOptimizerProperties {

    /**
     * Maximum number of iterations for the evaluator-optimizer loop
     */
    private int maxIterations = 3;

    /**
     * Convergence threshold - minimum score to consider solution acceptable
     */
    private double convergenceThreshold = 8.5;

    /**
     * Acceptable threshold - minimum score to consider solution acceptable
     */
    private double acceptableThreshold = 7.0;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public double getConvergenceThreshold() {
        return convergenceThreshold;
    }

    public void setConvergenceThreshold(double convergenceThreshold) {
        this.convergenceThreshold = convergenceThreshold;
    }

    public double getAcceptableThreshold() {
        return acceptableThreshold;
    }

    public void setAcceptableThreshold(double acceptableThreshold) {
        this.acceptableThreshold = acceptableThreshold;
    }
}