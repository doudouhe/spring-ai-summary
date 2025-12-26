package com.glmapper.ai.evaluator.optimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Evaluator Optimizer Agent Pattern Application
 *
 * This application demonstrates the Evaluator-Optimizer pattern for iterative
 * refinement of AI-generated solutions through generation, evaluation, and optimization cycles.
 *
 * @author mrliu
 */
@SpringBootApplication
@EnableCaching
public class EvaluatorOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluatorOptimizerApplication.class, args);
    }
}