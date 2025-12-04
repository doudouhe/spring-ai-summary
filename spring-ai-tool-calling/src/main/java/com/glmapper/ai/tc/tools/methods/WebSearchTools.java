package com.glmapper.ai.tc.tools.methods;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @Classname WebSearchTools
 * @Description Web search tools for fetching information from the internet
 * @Date 2025/12/4 14:30
 * @Created by glmapper
 */
@Component
public class WebSearchTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${web.search.api.url:https://api.duckduckgo.com/}")
    private String searchApiUrl;

    /**
     * Performs a web search for the given query and returns the results
     *
     * @param query The search query
     * @return Search results as a string
     */
    @Tool(description = "Performs a web search for the given query and returns the results")
    public String webSearch(String query) {
        try {
            // Using DuckDuckGo API as a free alternative
            String url = searchApiUrl + "?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&format=json&no_html=1&skip_disambig=1";
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("RelatedTopics")) {
                StringBuilder result = new StringBuilder();
                result.append("Search results for: ").append(query).append("\n\n");
                
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> topics = (java.util.List<Map<String, Object>>) response.get("RelatedTopics");
                
                int count = 0;
                for (Map<String, Object> topic : topics) {
                    if (count >= 5) break; // Limit to first 5 results
                    
                    if (topic.containsKey("Text")) {
                        result.append("Title: ").append(topic.get("Text")).append("\n");
                        if (topic.containsKey("FirstURL")) {
                            result.append("URL: ").append(topic.get("FirstURL")).append("\n");
                        }
                        result.append("\n");
                        count++;
                    }
                }
                
                if (count == 0) {
                    return "No results found for: " + query;
                }
                
                return result.toString();
            } else {
                return "No results found for: " + query;
            }
        } catch (Exception e) {
            return "Error performing web search: " + e.getMessage();
        }
    }
}