package com.agenthub.server.chatMemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileBasedChatMemory implements ChatMemory {

    private static final Pattern SAFE_CONVERSATION_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private final Path baseDir;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public FileBasedChatMemory(String dir) {
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
        if (!Files.exists(baseDir)) {
            try {
                Files.createDirectories(baseDir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create chat memory directory: " + baseDir, e);
            }
        }
    }

    @Override
    public void add(String conversationId, Message message) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.add(message);
        saveConversation(conversationId, messageList);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        saveConversation(conversationId, messageList);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList.stream()
                .skip(Math.max(0, messageList.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        File parent = file.getParentFile();
        if (parent != null) {
            try {
                Files.createDirectories(parent.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getConversationFile(String conversationId) {
        if (conversationId == null || !SAFE_CONVERSATION_ID.matcher(conversationId).matches()) {
            throw new IllegalArgumentException("Invalid conversationId");
        }
        Path file = baseDir.resolve(conversationId + ".kryo").normalize();
        if (!file.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid conversationId");
        }
        return file.toFile();
    }
}
