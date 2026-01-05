package com.glmapper.ai.rag.transformers;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @Classname DocTokenTextSplitter
 * @Description Optimized DocTokenTextSplitter with configurable parameters and async processing
 * @Date 2025/6/4 10:30
 * @Created by glmapper
 */
@Slf4j
@Component
public class DocTokenTextSplitter {

    private static final Executor asyncExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        r -> {
            Thread t = new Thread(r, "text-splitter-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        }
    );

    @Value("${rag.splitter.default.chunk-size:800}")
    private int defaultChunkSize;

    @Value("${rag.splitter.default.chunk-overlap:200}")
    private int defaultChunkOverlap;

    @Value("${rag.splitter.default.min-chunk-size-to-embed:50}")
    private int minChunkSizeToEmbed;

    @Value("${rag.splitter.default.max-chunk-size-to-embed:1500}")
    private int maxChunkSizeToEmbed;

    @Value("${rag.splitter.default.keep-separator:true}")
    private boolean keepSeparator;

    @Value("${rag.splitter.sentence-aware.enabled:true}")
    private boolean sentenceAwareEnabled;

    @Value("${rag.splitter.sentence-aware.max-chunk-size:500}")
    private int sentenceAwareMaxChunkSize;

    private final TokenTextSplitter defaultSplitter;
    private final TokenTextSplitter optimizedSplitter;

    public DocTokenTextSplitter() {
        this.defaultSplitter = new TokenTextSplitter();
        this.optimizedSplitter = new TokenTextSplitter(defaultChunkSize, defaultChunkOverlap, minChunkSizeToEmbed, maxChunkSizeToEmbed, keepSeparator);
    }

    /**
     * 使用默认的 TokenTextSplitter 来分割文档
     */
    public List<Document> splitDocuments(List<Document> documents) {
        log.debug("Splitting {} documents using default splitter", documents.size());
        long startTime = System.currentTimeMillis();

        try {
            List<Document> result = defaultSplitter.apply(documents);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Successfully split {} documents into {} chunks in {}ms",
                documents.size(), result.size(), duration);
            return result;
        } catch (Exception e) {
            log.error("Failed to split documents", e);
            throw new RuntimeException("Document splitting failed", e);
        }
    }

    /**
     * 使用优化的 TokenTextSplitter 来分割文档
     */
    public List<Document> splitDocumentsOptimized(List<Document> documents) {
        log.debug("Splitting {} documents using optimized splitter", documents.size());
        long startTime = System.currentTimeMillis();

        try {
            List<Document> result = optimizedSplitter.apply(documents);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Successfully split {} documents into {} chunks in {}ms",
                documents.size(), result.size(), duration);
            return result;
        } catch (Exception e) {
            log.error("Failed to split documents with optimized splitter", e);
            throw new RuntimeException("Document splitting failed", e);
        }
    }

    /**
     * 使用自定义的 TokenTextSplitter 来分割文档
     */
    public List<Document> splitCustomized(List<Document> documents,
                                          int chunkSize,
                                          int chunkOverlap,
                                          int minChunkSize,
                                          int maxChunkSize) {
        log.debug("Splitting {} documents with custom parameters", documents.size());
        long startTime = System.currentTimeMillis();

        try {
            TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, minChunkSize, maxChunkSize, true);

            List<Document> result = splitter.apply(documents);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Successfully split {} documents into {} chunks in {}ms",
                documents.size(), result.size(), duration);
            return result;
        } catch (Exception e) {
            log.error("Failed to split documents with custom parameters", e);
            throw new RuntimeException("Document splitting failed", e);
        }
    }

    /**
     * 异步分割文档
     */
    public CompletableFuture<List<Document>> splitDocumentsAsync(List<Document> documents) {
        return CompletableFuture.supplyAsync(() -> splitDocumentsOptimized(documents), asyncExecutor);
    }

    /**
     * 批量分割文档 - 处理大量文档时的优化方法
     */
    public List<Document> splitDocumentsBatch(List<Document> documents, int batchSize) {
        log.debug("Batch splitting {} documents with batch size {}", documents.size(), batchSize);

        return documents.stream()
                .collect(java.util.stream.Collectors.groupingBy(doc -> doc.hashCode() % batchSize))
                .values()
                .parallelStream()
                .flatMap(batch -> splitDocumentsOptimized(batch).stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 智能分割 - 根据文档内容自动调整分割参数
     */
    public List<Document> splitDocumentsSmart(List<Document> documents) {
        log.debug("Smart splitting {} documents", documents.size());

        return documents.parallelStream()
                .flatMap(doc -> {
                    int docLength = doc.getText().length();
                    TokenTextSplitter splitter = createSmartSplitter(docLength);
                    return splitter.apply(List.of(doc)).stream();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Sentence-aware splitting that respects sentence boundaries to avoid cutting sentences
     */
    public List<Document> splitDocumentsSentenceAware(List<Document> documents) {
        if (!sentenceAwareEnabled) {
            return splitDocumentsOptimized(documents);
        }

        log.debug("Sentence-aware splitting {} documents", documents.size());
        long startTime = System.currentTimeMillis();

        try {
            // Convert Spring AI documents to LangChain4j documents for sentence-aware splitting
            java.util.List<dev.langchain4j.data.document.Document> langchainDocs = documents.stream()
                    .map(doc -> dev.langchain4j.data.document.Document.from(doc.getText(), doc.getMetadata()))
                    .collect(java.util.stream.Collectors.toList());

            // Create a sentence-aware splitter using LangChain4j
            DocumentSplitter splitter = new DocumentBySentenceSplitter(s -> sentenceAwareMaxChunkSize);
            java.util.List<TextSegment> segments = splitter.splitAll(langchainDocs);

            // Convert back to Spring AI documents
            List<Document> result = segments.stream()
                    .map(segment -> new org.springframework.ai.document.Document(
                            segment.text(),
                            segment.metadata().toMap()))
                    .collect(java.util.stream.Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Successfully sentence-aware split {} documents into {} chunks in {}ms",
                documents.size(), result.size(), duration);
            return result;
        } catch (Exception e) {
            log.error("Failed to split documents with sentence-aware splitting", e);
            // Fallback to optimized splitting
            return splitDocumentsOptimized(documents);
        }
    }

    /**
     * Semantic-aware splitting that considers semantic boundaries when splitting documents
     */
    public List<Document> splitDocumentsSemanticAware(List<Document> documents) {
        log.debug("Semantic-aware splitting {} documents", documents.size());
        long startTime = System.currentTimeMillis();

        try {
            // This is a more sophisticated approach that would consider semantic boundaries
            // For now, we'll implement a hybrid approach combining sentence-aware splitting with
            // configurable chunk size based on content type
            java.util.List<dev.langchain4j.data.document.Document> langchainDocs = documents.stream()
                    .map(doc -> dev.langchain4j.data.document.Document.from(doc.getText(), doc.getMetadata()))
                    .collect(java.util.stream.Collectors.toList());

            // Determine optimal splitting based on content characteristics
            java.util.List<TextSegment> segments = langchainDocs.stream()
                    .flatMap(doc -> {
                        // Detect if document has semantic boundaries (headers, sections, etc.)
                        if (hasSemanticStructure(doc.text())) {
                            // Use smaller chunks for documents with clear structure
                            DocumentSplitter splitter = new DocumentByWordSplitter(s -> defaultChunkSize / 2);
                            return splitter.split(doc).stream();
                        } else {
                            // Use sentence-aware splitting for unstructured text
                            DocumentSplitter splitter = new DocumentBySentenceSplitter(s -> defaultChunkSize);
                            return splitter.split(doc).stream();
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Convert back to Spring AI documents
            List<Document> result = segments.stream()
                    .map(segment -> new org.springframework.ai.document.Document(
                            segment.text(),
                            segment.metadata().toMap()))
                    .collect(java.util.stream.Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Successfully semantic-aware split {} documents into {} chunks in {}ms",
                documents.size(), result.size(), duration);
            return result;
        } catch (Exception e) {
            log.error("Failed to split documents with semantic-aware splitting", e);
            // Fallback to optimized splitting
            return splitDocumentsOptimized(documents);
        }
    }

    /**
     * Check if a document has semantic structure (headers, sections, etc.)
     */
    private boolean hasSemanticStructure(String text) {
        // Look for common semantic patterns like headers
        Pattern headerPattern = Pattern.compile("^(#+\\s+|\\d+\\.\\s+|##+\\s+|\\*\\*[^\\*]+\\*\\*|==[^=]+==)",
            Pattern.MULTILINE);
        return headerPattern.matcher(text).find();
    }

    private TokenTextSplitter createSmartSplitter(int docLength) {
        if (docLength < 1000) {
            return new TokenTextSplitter(300, 50, 20, 500, true);
        } else if (docLength < 5000) {
            return optimizedSplitter;
        } else {
            return new TokenTextSplitter(1200, 300, 100, 2000, true);
        }
    }
}
