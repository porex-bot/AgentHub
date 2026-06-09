package com.agenthub.server.chatMemory;

import java.nio.file.Path;

public final class ChatMemoryPaths {

    private static final String BASE_DIR_PROPERTY = "agenthub.chat-memory.base-dir";
    private static final String DEFAULT_BASE_DIR = "agenthub-chat-memory";

    private ChatMemoryPaths() {
    }

    public static Path directory(String memoryName) {
        String configuredBaseDir = firstConfiguredBaseDir();
        if (configuredBaseDir != null && !configuredBaseDir.isBlank()) {
            return Path.of(configuredBaseDir, memoryName);
        }
        return Path.of(System.getProperty("java.io.tmpdir"), DEFAULT_BASE_DIR, memoryName);
    }

    private static String firstConfiguredBaseDir() {
        String configuredBaseDir = System.getProperty(BASE_DIR_PROPERTY);
        if (configuredBaseDir != null && !configuredBaseDir.isBlank()) {
            return configuredBaseDir;
        }
        configuredBaseDir = System.getProperty("AGENTHUB_CHAT_MEMORY_BASE_DIR");
        if (configuredBaseDir != null && !configuredBaseDir.isBlank()) {
            return configuredBaseDir;
        }
        return System.getenv("AGENTHUB_CHAT_MEMORY_BASE_DIR");
    }
}
