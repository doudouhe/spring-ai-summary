package com.glmapper.ai.tc.tools.methods;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * @Classname UrlContentTools
 * @Description Tools for fetching content from URLs
 * @Date 2025/12/4 14:45
 * @Created by glmapper
 */
@Component
public class UrlContentTools {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches content from a given URL
     *
     * @param url The URL to fetch content from
     * @return Content of the URL as a string
     */
    @Tool(description = "Fetches content from a given URL. Example: fetchUrlContent('https://example.com')")
    public String fetchUrlContent(String url) {
        try {
            // Validate URL format
            URI uri = new URI(url);
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                return "Error: Only HTTP and HTTPS URLs are allowed";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Spring-AI-Tool-Client/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                
                // Limit response size to prevent huge responses
                if (responseBody.length() > 5000) {
                    return responseBody.substring(0, 5000) + "\n... [Content truncated]";
                }
                
                return responseBody;
            } else {
                return "Error: HTTP " + response.statusCode() + " - Failed to fetch content from " + url;
            }
        } catch (URISyntaxException e) {
            return "Error: Invalid URL format - " + e.getMessage();
        } catch (IOException e) {
            return "Error: IO exception occurred while fetching URL - " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Request was interrupted - " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Fetches the title of a webpage from a given URL
     *
     * @param url The URL to fetch the title from
     * @return Title of the webpage
     */
    @Tool(description = "Fetches the title of a webpage from a given URL. Example: fetchPageTitle('https://example.com')")
    public String fetchPageTitle(String url) {
        try {
            // Validate URL format
            URI uri = new URI(url);
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                return "Error: Only HTTP and HTTPS URLs are allowed";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Spring-AI-Tool-Client/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                
                // Extract title from HTML using simple string operations
                int titleStart = responseBody.toLowerCase().indexOf("<title>");
                if (titleStart != -1) {
                    int titleEnd = responseBody.toLowerCase().indexOf("</title>", titleStart);
                    if (titleEnd != -1) {
                        String title = responseBody.substring(titleStart + 7, titleEnd); // +7 to skip "<title>"
                        return title.trim();
                    }
                }
                
                return "Title not found in the page";
            } else {
                return "Error: HTTP " + response.statusCode() + " - Failed to fetch content from " + url;
            }
        } catch (URISyntaxException e) {
            return "Error: Invalid URL format - " + e.getMessage();
        } catch (IOException e) {
            return "Error: IO exception occurred while fetching URL - " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Request was interrupted - " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}