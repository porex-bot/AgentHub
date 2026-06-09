package com.agenthub.server.chatMemory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileBasedChatMemoryTest {

    @TempDir
    Path tempDir;

    @Test
    void addRecreatesBaseDirectoryWhenItIsMissing() throws IOException {
        Path memoryDir = tempDir.resolve("chat-memory");
        FileBasedChatMemory memory = new FileBasedChatMemory(memoryDir.toString());
        deleteRecursively(memoryDir);

        memory.add("chat-1", new UserMessage("hello"));

        List<Message> messages = memory.get("chat-1", 10);
        assertEquals(1, messages.size());
        assertInstanceOf(UserMessage.class, messages.get(0));
        assertEquals("hello", ((UserMessage) messages.get(0)).getText());
    }

    @Test
    void rejectsUnsafeConversationId() {
        Path memoryDir = tempDir.resolve("chat-memory");
        FileBasedChatMemory memory = new FileBasedChatMemory(memoryDir.toString());

        assertThrows(IllegalArgumentException.class,
                () -> memory.add("../escape", new UserMessage("hello")));
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path current : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(current);
            }
        }
    }
}
