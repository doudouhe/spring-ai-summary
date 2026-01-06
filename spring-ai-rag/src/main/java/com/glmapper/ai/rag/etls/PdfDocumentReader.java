package com.glmapper.ai.rag.etls;

import com.glmapper.ai.rag.etls.base.BaseDocumentReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @Classname PdfDocumentReader
 * @Description Optimized PdfDocumentReader with caching and async processing
 * @Date 2025/6/4 09:49
 * @Created by glmapper
 */
@Slf4j
@Component
public class PdfDocumentReader extends BaseDocumentReader {
    
    @Value("${rag.pdf.page-top-margin:0}")
    private int pageTopMargin;

    @Value("${rag.pdf.top-lines-to-delete:0}")
    private int topLinesToDelete;

    @Value("${rag.pdf.pages-per-document:1}")
    private int pagesPerDocument;

    private PdfDocumentReaderConfig defaultPageConfig;
    private PdfDocumentReaderConfig defaultParagraphConfig;

    @PostConstruct
    public void init() {
        this.defaultPageConfig = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(pageTopMargin)
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfTopTextLinesToDelete(topLinesToDelete)
                        .build())
                .withPagesPerDocument(pagesPerDocument)
                .build();

        this.defaultParagraphConfig = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(pageTopMargin)
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfTopTextLinesToDelete(topLinesToDelete)
                        .build())
                .build();
    }

    /**
     * PagePdfDocumentReader 是依赖 Apache PdfBox 来解析 pdf
     */
    public List<Document> getDocsFromPdf() {
        return getDocsFromPdf("classpath:files/test_page.pdf");
    }
    
    public List<Document> getDocsFromPdf(String resourcePath) {
        Resource resource = getResourceFromPath(resourcePath);
        return readDocumentsWithPageConfig(resource);
    }
    
    public CompletableFuture<List<Document>> getDocsFromPdfAsync(String resourcePath) {
        Resource resource = getResourceFromPath(resourcePath);
        return CompletableFuture.supplyAsync(() -> readDocumentsWithPageConfig(resource));
    }
    
    public List<Document> getDocsFromPdfWithCatalog() {
        return getDocsFromPdfWithCatalog("classpath:files/test_paragraph.pdf");
    }
    
    public List<Document> getDocsFromPdfWithCatalog(String resourcePath) {
        Resource resource = getResourceFromPath(resourcePath);
        return readDocumentsWithParagraphConfig(resource);
    }
    
    public List<Document> getDocsFromPdfWithCustomConfig(String resourcePath, PdfDocumentReaderConfig config) {
        Resource resource = getResourceFromPath(resourcePath);
        try {
            validateResource(resource);
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
            List<Document> documents = pdfReader.read();
            addResourceMetadata(documents, resource);
            return documents;
        } catch (IOException e) {
            log.error("Failed to load PDF document: {}", resourcePath, e);
            throw new RuntimeException("Failed to load PDF document", e);
        }
    }
    
    private List<Document> readDocumentsWithPageConfig(Resource resource) {
        try {
            validateResource(resource);
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, defaultPageConfig);
            List<Document> documents = pdfReader.read();
            addResourceMetadata(documents, resource);
            return documents;
        } catch (IOException e) {
            log.error("Failed to read PDF with page config", e);
            throw new RuntimeException("Failed to read PDF", e);
        }
    }
    
    private List<Document> readDocumentsWithParagraphConfig(Resource resource) {
        try {
            validateResource(resource);
            ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource, defaultParagraphConfig);
            List<Document> documents = pdfReader.read();
            addResourceMetadata(documents, resource);
            return documents;
        } catch (IOException e) {
            log.error("Failed to read PDF with paragraph config", e);
            throw new RuntimeException("Failed to read PDF", e);
        }
    }
    
    private Resource getResourceFromPath(String resourcePath) {
        if (resourcePath.startsWith("classpath:")) {
            return new org.springframework.core.io.ClassPathResource(resourcePath.substring(10));
        } else {
            return new org.springframework.core.io.FileSystemResource(resourcePath);
        }
    }

    @Override
    protected List<Document> doReadDocuments(Resource resource) throws IOException {
        return readDocumentsWithPageConfig(resource);
    }
}