package com.agenthub.server.constant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileConstantTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("agenthub.file-save-dir");
    }

    @Test
    void fileSaveDirDefaultsToJavaTempDirectory() {
        System.clearProperty("agenthub.file-save-dir");

        assertEquals(
                Path.of(System.getProperty("java.io.tmpdir"), "agenthub-files").toString(),
                FileConstant.fileSaveDir()
        );
    }

    @Test
    void fileSaveDirUsesConfiguredDirectory() {
        System.setProperty("agenthub.file-save-dir", "/data/agenthub-files");

        assertEquals(Path.of("/data/agenthub-files").toString(), FileConstant.fileSaveDir());
    }
}
