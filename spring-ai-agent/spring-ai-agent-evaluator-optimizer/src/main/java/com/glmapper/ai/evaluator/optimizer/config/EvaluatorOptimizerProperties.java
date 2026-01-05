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
     * Convergence threshold - target score at which the optimizer stops iterating.
     * When the solution is acceptable AND score >= this threshold, the optimization loop converges.
     */
    private double convergenceThreshold = 8.5;

    /**
     * Acceptable threshold - minimum score to consider a solution acceptable.
     * Solutions meeting this bar are valid but may continue optimizing until convergence.
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