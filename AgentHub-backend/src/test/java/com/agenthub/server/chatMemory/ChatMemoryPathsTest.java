package com.agenthub.server.chatMemory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMemoryPathsTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("agenthub.chat-memory.base-dir");
        System.clearProperty("AGENTHUB_CHAT_MEMORY_BASE_DIR");
    }

    @Test
    void directoryDefaultsToJavaTempDirectory() {
        System.clearProperty("agenthub.chat-memory.base-dir");

        Path directory = ChatMemoryPaths.directory("relationship");

        assertEquals(
                Path.of(System.getProperty("java.io.tmpdir"), "agenthub-chat-memory", "relationship").toString(),
                directory.toString()
        );
    }

    @Test
    void directoryUsesConfiguredBaseDirectory() {
        System.setProperty("agenthub.chat-memory.base-dir", "/data/agenthub-memory");

        Path directory = ChatMemoryPaths.directory("relationship");

        assertEquals(Path.of("/data/agenthub-memory", "relationship").toString(), directory.toString());
    }

    @Test
    void directoryUsesEnvironmentStyleSystemProperty() {
        System.setProperty("AGENTHUB_CHAT_MEMORY_BASE_DIR", "/data/agenthub-memory-env");

        Path directory = ChatMemoryPaths.directory("relationship");

        assertEquals(Path.of("/data/agenthub-memory-env", "relationship").toString(), directory.toString());
    }
}
