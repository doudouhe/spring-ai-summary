package com.glmapper.ai.tc.tools.methods;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @Classname FileWriterTools
 * @Description Tools for writing content to files
 * @Date 2025/12/4 14:50
 * @Created by glmapper
 */
@Component
public class FileWriterTools {

    /**
     * Writes content to a file at the specified path
     *
     * @param filePath The path where the file should be created/overwritten
     * @param content  The content to write to the file
     * @return Success or error message
     */
    @Tool(description = "Writes content to a file at the specified path. Example: writeFile('/path/to/file.txt', 'Hello World')")
    public String writeFile(String filePath, String content) {
        try {
            // Validate file path to prevent directory traversal
            if (filePath.contains("../") || filePath.contains("..\\") || filePath.startsWith("../") || filePath.startsWith("..\\") || filePath.contains("/..") || filePath.contains("\\..")) {
                return "Error: Invalid file path - directory traversal not allowed";
            }

            // Ensure the directory exists
            Path path = Paths.get(filePath);
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // Write content to file
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
            }

            return "Successfully wrote content to file: " + filePath;
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Appends content to an existing file at the specified path
     *
     * @param filePath The path of the file to append to
     * @param content  The content to append to the file
     * @return Success or error message
     */
    @Tool(description = "Appends content to an existing file at the specified path. Example: appendToFile('/path/to/file.txt', 'Additional content')")
    public String appendToFile(String filePath, String content) {
        try {
            // Validate file path to prevent directory traversal
            if (filePath.contains("../") || filePath.contains("..\\") || filePath.startsWith("../") || filePath.startsWith("..\\") || filePath.contains("/..") || filePath.contains("\\..")) {
                return "Error: Invalid file path - directory traversal not allowed";
            }

            Path path = Paths.get(filePath);
            
            // Create file if it doesn't exist
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            // Append content to file
            try (FileWriter writer = new FileWriter(filePath, true)) {
                writer.write(content);
            }

            return "Successfully appended content to file: " + filePath;
        } catch (IOException e) {
            return "Error appending to file: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Creates a directory at the specified path
     *
     * @param dirPath The path where the directory should be created
     * @return Success or error message
     */
    @Tool(description = "Creates a directory at the specified path. Example: createDirectory('/path/to/new/directory')")
    public String createDirectory(String dirPath) {
        try {
            // Validate directory path to prevent directory traversal
            if (dirPath.contains("../") || dirPath.contains("..\\") || dirPath.startsWith("../") || dirPath.startsWith("..\\") || dirPath.contains("/..") || dirPath.contains("\\..")) {
                return "Error: Invalid directory path - directory traversal not allowed";
            }

            Path path = Paths.get(dirPath);
            Files.createDirectories(path);

            return "Successfully created directory: " + dirPath;
        } catch (IOException e) {
            return "Error creating directory: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}