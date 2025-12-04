package com.glmapper.ai.tc.tools.methods;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @Classname CalculatorTools
 * @Description Advanced calculator tools for evaluating mathematical expressions
 * @Date 2025/12/4 14:35
 * @Created by glmapper
 */
@Component
public class CalculatorTools {

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * Evaluates a mathematical expression and returns the result
     *
     * @param expression The mathematical expression to evaluate (e.g., "2 + 3 * 4", "Math.sin(Math.PI/2)")
     * @return The result of the evaluation
     */
    @Tool(description = "Evaluates a mathematical expression and returns the result. Supports basic operations (+, -, *, /, %) and functions (Math.sin, Math.cos, Math.sqrt, etc.)")
    public String evaluateExpression(String expression) {
        try {
            // Validate expression to prevent dangerous operations
            if (containsDangerousOperations(expression)) {
                return "Error: Expression contains potentially dangerous operations";
            }
            
            Object result = engine.eval(expression);
            return String.valueOf(result);
        } catch (ScriptException e) {
            return "Error evaluating expression: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Calculates the result of a basic arithmetic operation between two numbers
     *
     * @param operation The operation to perform: add, subtract, multiply, divide, power, modulo
     * @param operand1  The first operand
     * @param operand2  The second operand
     * @return The result of the operation
     */
    @Tool(description = "Performs basic arithmetic operations on two numbers. Operation can be: add, subtract, multiply, divide, power, modulo")
    public double calculate(String operation, double operand1, double operand2) {
        switch (operation.toLowerCase()) {
            case "add":
            case "addition":
            case "plus":
                return operand1 + operand2;
            case "subtract":
            case "subtraction":
            case "minus":
                return operand1 - operand2;
            case "multiply":
            case "multiplication":
                return operand1 * operand2;
            case "divide":
            case "division":
                if (operand2 == 0) {
                    throw new IllegalArgumentException("Cannot divide by zero");
                }
                return operand1 / operand2;
            case "power":
            case "exponent":
                return Math.pow(operand1, operand2);
            case "modulo":
            case "remainder":
                return operand1 % operand2;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation + ". Supported operations: add, subtract, multiply, divide, power, modulo");
        }
    }

    /**
     * Calculates the square root of a number
     *
     * @param number The number to calculate the square root of
     * @return The square root of the number
     */
    @Tool(description = "Calculates the square root of a number")
    public double squareRoot(double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of negative number");
        }
        return Math.sqrt(number);
    }

    /**
     * Calculates the logarithm of a number
     *
     * @param number The number to calculate the logarithm of
     * @return The natural logarithm of the number
     */
    @Tool(description = "Calculates the natural logarithm (base e) of a number")
    public double logarithm(double number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Cannot calculate logarithm of zero or negative number");
        }
        return Math.log(number);
    }

    /**
     * Calculates the factorial of a number
     *
     * @param number The number to calculate the factorial of (must be non-negative integer)
     * @return The factorial of the number
     */
    @Tool(description = "Calculates the factorial of a number. The input must be a non-negative integer.")
    public long factorial(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        }
        if (number > 20) {
            throw new IllegalArgumentException("Factorial input is too large (max 20 supported)");
        }
        
        long result = 1;
        for (int i = 2; i <= number; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Validates if the expression contains potentially dangerous operations
     */
    private boolean containsDangerousOperations(String expression) {
        String lowerExpression = expression.toLowerCase();
        // Block potentially dangerous operations
        return lowerExpression.contains("import") ||
               lowerExpression.contains("java.lang") ||
               lowerExpression.contains("function") ||
               lowerExpression.contains("eval") ||
               lowerExpression.contains("constructor") ||
               lowerExpression.contains("prototype") ||
               lowerExpression.contains("__");
    }
}