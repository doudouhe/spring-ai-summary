package com.glmapper.ai.tc.tools.methods;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @Classname CurrencyConversionTools
 * @Description Currency conversion tools for converting between different currencies
 * @Date 2025/12/4 14:40
 * @Created by glmapper
 */
@Component
public class CurrencyConversionTools {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Converts amount from one currency to another using exchange rates
     *
     * @param fromCurrency The source currency code (e.g., USD, EUR, GBP)
     * @param toCurrency   The target currency code (e.g., USD, EUR, GBP)
     * @param amount       The amount to convert
     * @return The converted amount
     */
    @Tool(description = "Converts amount from one currency to another. Example: convertCurrency('USD', 'EUR', 100)")
    public String convertCurrency(String fromCurrency, String toCurrency, double amount) {
        try {
            // Using a free currency API for demonstration
            String apiUrl = String.format("https://api.exchangerate-api.com/v4/latest/%s", fromCurrency.toUpperCase());
            
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            
            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                
                if (rates.containsKey(toCurrency.toUpperCase())) {
                    Double rate = Double.parseDouble(rates.get(toCurrency.toUpperCase()).toString());
                    double convertedAmount = amount * rate;
                    
                    return String.format("%.2f %s = %.2f %s (exchange rate: 1 %s = %.4f %s)",
                            amount, fromCurrency.toUpperCase(),
                            convertedAmount, toCurrency.toUpperCase(),
                            fromCurrency.toUpperCase(), rate, toCurrency.toUpperCase());
                } else {
                    return "Currency " + toCurrency.toUpperCase() + " not found in exchange rates";
                }
            } else {
                return "Error: Could not retrieve exchange rates for " + fromCurrency.toUpperCase();
            }
        } catch (Exception e) {
            // Fallback to mock conversion for demo purposes
            return mockCurrencyConversion(fromCurrency, toCurrency, amount, e.getMessage());
        }
    }

    /**
     * Gets the current exchange rate between two currencies
     *
     * @param fromCurrency The source currency code
     * @param toCurrency   The target currency code
     * @return The exchange rate
     */
    @Tool(description = "Gets the current exchange rate between two currencies. Example: getExchangeRate('USD', 'EUR')")
    public String getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            // Using a free currency API for demonstration
            String apiUrl = String.format("https://api.exchangerate-api.com/v4/latest/%s", fromCurrency.toUpperCase());
            
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            
            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                
                if (rates.containsKey(toCurrency.toUpperCase())) {
                    Double rate = Double.parseDouble(rates.get(toCurrency.toUpperCase()).toString());
                    
                    return String.format("1 %s = %.4f %s", 
                            fromCurrency.toUpperCase(), rate, toCurrency.toUpperCase());
                } else {
                    return "Currency " + toCurrency.toUpperCase() + " not found in exchange rates";
                }
            } else {
                return "Error: Could not retrieve exchange rates for " + fromCurrency.toUpperCase();
            }
        } catch (Exception e) {
            return "Error retrieving exchange rate: " + e.getMessage();
        }
    }

    /**
     * Mock conversion for demo purposes when API is not available
     */
    private String mockCurrencyConversion(String fromCurrency, String toCurrency, double amount, String error) {
        // Using approximate exchange rates for demo purposes
        Map<String, Double> usdRates = new HashMap<>();
        usdRates.put("USD", 1.0);
        usdRates.put("EUR", 0.85);
        usdRates.put("GBP", 0.73);
        usdRates.put("JPY", 110.0);
        usdRates.put("CAD", 1.25);
        usdRates.put("AUD", 1.35);
        usdRates.put("CHF", 0.92);
        usdRates.put("CNY", 6.45);
        usdRates.put("INR", 74.0);

        Double fromRate = usdRates.get(fromCurrency.toUpperCase());
        Double toRate = usdRates.get(toCurrency.toUpperCase());

        if (fromRate != null && toRate != null) {
            // Convert through USD as base
            double usdValue = amount / fromRate;
            double convertedValue = usdValue * toRate;
            
            return String.format("[DEMO MODE] %.2f %s ≈ %.2f %s (using mock rates - API error: %s)", 
                    amount, fromCurrency.toUpperCase(), convertedValue, toCurrency.toUpperCase(), error);
        } else {
            return "Currency conversion not available for " + fromCurrency + " to " + toCurrency + 
                   " (API error: " + error + ")";
        }
    }
}