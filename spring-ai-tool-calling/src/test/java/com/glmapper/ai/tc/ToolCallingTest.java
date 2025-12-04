package com.glmapper.ai.tc;

import com.glmapper.ai.tc.tools.methods.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Classname ToolCallingTest
 * @Description Test class to verify the new tools work properly
 * @Date 2025/12/4 15:10
 * @Created by glmapper
 */
@SpringBootTest
public class ToolCallingTest {

    @Test
    public void testCalculatorTools() {
        CalculatorTools calculatorTools = new CalculatorTools();

        // Test basic operations
        assertEquals(10.0, calculatorTools.calculate("add", 5, 5), 0.001);
        assertEquals(0.0, calculatorTools.calculate("subtract", 5, 5), 0.001);
        assertEquals(25.0, calculatorTools.calculate("multiply", 5, 5), 0.001);
        assertEquals(2.0, calculatorTools.calculate("divide", 10, 5), 0.001);
        assertEquals(32.0, calculatorTools.calculate("power", 2, 5), 0.001);
        assertEquals(1.0, calculatorTools.calculate("modulo", 10, 3), 0.001);

        // Test other functions
        assertEquals(5.0, calculatorTools.squareRoot(25), 0.001);
        assertEquals(2.302585092994046, calculatorTools.logarithm(10), 0.001);
        assertEquals(120, calculatorTools.factorial(5));
    }

    @Test
    public void testDateTimeTools() {
        DateTimeTools dateTimeTools = new DateTimeTools();
        String result = dateTimeTools.getCurrentDateTime();
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testWebSearchTools() {
        WebSearchTools webSearchTools = new WebSearchTools();
        String result = webSearchTools.webSearch("test query");
        // The result might be empty if API is not configured, but shouldn't throw exception
        assertNotNull(result);
    }

    @Test
    public void testCurrencyConversionTools() {
        CurrencyConversionTools currencyConversionTools = new CurrencyConversionTools();
        String result = currencyConversionTools.convertCurrency("USD", "EUR", 100);
        // The result might use mock conversion if API is not available, but shouldn't throw exception
        assertNotNull(result);
        assertTrue(result.contains("USD") || result.contains("DEMO MODE"));
    }

    @Test
    public void testUrlContentTools() {
        UrlContentTools urlContentTools = new UrlContentTools();
        String result = urlContentTools.fetchUrlContent("https://httpbin.org/html");
        // The result might be empty if network is not available, but shouldn't throw exception
        assertNotNull(result);
    }

    @Test
    public void testFileTools() {
        // For file operations, we can only test that the methods don't throw exceptions
        // with valid parameters
        FileReaderTools fileReaderTools = new FileReaderTools();
        String result = fileReaderTools.readFileAndPrint("nonexistent_file.txt");
        assertNotNull(result);
        
        FileWriterTools fileWriterTools = new FileWriterTools();
        String writeResult = fileWriterTools.writeFile("test_output.txt", "test content");
        assertNotNull(writeResult);
    }
}