package com.glmapper.ai.rag.etls;

import com.glmapper.ai.rag.etls.base.BaseDocumentReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @Classname MdDocumentReader
 * @Description Optimized MarkdownDocumentReader with caching and async processing
 * @Date 2025/6/3 20:20
 * @Created by glmapper
 */
@Slf4j
@Component
public class MdDocumentReader extends BaseDocumentReader {

    @Value("${rag.markdown.include-code-block:false}")
    private boolean includeCodeBlock;

    @Value("${rag.markdown.include-blockquote:false}")
    private boolean includeBlockquote;

    @Value("${rag.markdown.horizontal-rule-create-document:true}")
    private boolean horizontalRuleCreateDocument;

    private final MarkdownDocumentReaderConfig defaultConfig;

    public MdDocumentReader() {
        this.defaultConfig = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(horizontalRuleCreateDocument)
                .withIncludeCodeBlock(includeCodeBlock)
                .withIncludeBlockquote(includeBlockquote)
                .build();
    }

    public List<Document> loadMarkdown(String filePath) {
        Resource resource = new ClassPathResource(filePath);
        return readDocuments(resource);
    }

    public CompletableFuture<List<Document>> loadMarkdownAsync(String filePath) {
        Resource resource = new ClassPathResource(filePath);
        return readDocumentsAsync(resource);
    }

    public List<Document> loadMarkdownWithConfig(String filePath, MarkdownDocumentReaderConfig config) {
        Resource resource = new ClassPathResource(filePath);
        try {
            validateResource(resource);
            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            List<Document> documents = reader.get();
            addResourceMetadata(documents, resource);
            return documents;
        } catch (IOException e) {
            log.error("Failed to load markdown document: {}", filePath, e);
            throw new RuntimeException("Failed to load markdown document", e);
        }
    }

    @Override
    protected List<Document> doReadDocuments(Resource resource) throws IOException {
        validateResource(resource);
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, defaultConfig);
        List<Document> documents = reader.get();
        addResourceMetadata(documents, resource);
        return documents;
    }
}
