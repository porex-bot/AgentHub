package com.agenthub.server.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 本地向量库配置，把关系知识 Markdown 加载进 SimpleVectorStore，作为本地知识库检索来源。
 */
@Configuration
public class RelationshipVectorStoreConfig {

    @Resource
    private RelationshipDocumentLoader documentLoader;

    @Bean
    VectorStore relationshipVectorStore(EmbeddingModel dashscopeEmbeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        List<Document> documents = documentLoader.loadMarkDowns();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}


