package com.agenthub.server.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class PgVectorVectorStoreConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Test
    void pgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("AgentHub relationship assistant knowledge", Map.of("meta1", "meta1")),
                new Document("AgentHub research assistant knowledge"),
                new Document("AgentHub task execution knowledge", Map.of("meta2", "meta2")));

        pgVectorVectorStore.add(documents);

        List<Document> results = this.pgVectorVectorStore.similaritySearch(
                SearchRequest.builder().query("relationship assistant").topK(3).build());
        Assertions.assertNotNull(results);
    }
}
