package com.glmapper.ai.tc.controller;

import com.glmapper.ai.tc.tools.methods.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Classname ToolCallingController
 * @Description Controller for exposing various AI tools via REST API endpoints
 * @Date 2025/12/4 15:00
 * @Created by glmapper
 */
@RestController
@RequestMapping("/api/tools")
public class ToolCallingController {

    @Autowired
    private ChatClient chatClient;

    private final DateTimeTools dateTimeTools;
    private final FileReaderTools fileReaderTools;
    private final FileWriterTools fileWriterTools;
    private final CalculatorTools calculatorTools;
    private final CurrencyConversionTools currencyConversionTools;
    private final WebSearchTools webSearchTools;
    private final UrlContentTools urlContentTools;

    public ToolCallingController(
            DateTimeTools dateTimeTools,
            FileReaderTools fileReaderTools,
            FileWriterTools fileWriterTools,
            CalculatorTools calculatorTools,
            CurrencyConversionTools currencyConversionTools,
            WebSearchTools webSearchTools,
            UrlContentTools urlContentTools
    ) {
        this.dateTimeTools = dateTimeTools;
        this.fileReaderTools = fileReaderTools;
        this.fileWriterTools = fileWriterTools;
        this.calculatorTools = calculatorTools;
        this.currencyConversionTools = currencyConversionTools;
        this.webSearchTools = webSearchTools;
        this.urlContentTools = urlContentTools;
    }

    @GetMapping("/datetime/current")
    public String getCurrentDateTime() {
        return dateTimeTools.getCurrentDateTime();
    }

    @GetMapping("/file/read")
    public String readFile(@RequestParam String filePath) {
        return fileReaderTools.readFileAndPrint(filePath);
    }

    @PostMapping("/file/write")
    public String writeFile(@RequestParam String filePath, @RequestBody String content) {
        return fileWriterTools.writeFile(filePath, content);
    }

    @PostMapping("/file/append")
    public String appendToFile(@RequestParam String filePath, @RequestBody String content) {
        return fileWriterTools.appendToFile(filePath, content);
    }

    @PostMapping("/calculate/evaluate")
    public String evaluateExpression(@RequestParam String expression) {
        return calculatorTools.evaluateExpression(expression);
    }

    @PostMapping("/calculate")
    public String calculate(
            @RequestParam String operation,
            @RequestParam double operand1,
            @RequestParam double operand2
    ) {
        try {
            double result = calculatorTools.calculate(operation, operand1, operand2);
            return String.valueOf(result);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/calculate/sqrt")
    public String squareRoot(@RequestParam double number) {
        try {
            double result = calculatorTools.squareRoot(number);
            return String.valueOf(result);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/calculate/log")
    public String logarithm(@RequestParam double number) {
        try {
            double result = calculatorTools.logarithm(number);
            return String.valueOf(result);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/calculate/factorial")
    public String factorial(@RequestParam int number) {
        try {
            long result = calculatorTools.factorial(number);
            return String.valueOf(result);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/currency/convert")
    public String convertCurrency(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam double amount
    ) {
        return currencyConversionTools.convertCurrency(fromCurrency, toCurrency, amount);
    }

    @GetMapping("/currency/rate")
    public String getExchangeRate(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency
    ) {
        return currencyConversionTools.getExchangeRate(fromCurrency, toCurrency);
    }

    @GetMapping("/web/search")
    public String webSearch(@RequestParam String query) {
        return webSearchTools.webSearch(query);
    }

    @GetMapping("/url/content")
    public String fetchUrlContent(@RequestParam String url) {
        return urlContentTools.fetchUrlContent(url);
    }

    @GetMapping("/url/title")
    public String fetchPageTitle(@RequestParam String url) {
        return urlContentTools.fetchPageTitle(url);
    }

    @PostMapping("/chat")
    public String chatWithTools(@RequestBody String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}