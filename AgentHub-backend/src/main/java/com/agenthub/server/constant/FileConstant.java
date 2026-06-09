package com.agenthub.server.constant;

import java.nio.file.Path;

public interface FileConstant {

    String FILE_SAVE_DIR_PROPERTY = "agenthub.file-save-dir";
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";

    static String fileSaveDir() {
        String configuredDir = System.getProperty(FILE_SAVE_DIR_PROPERTY);
        if (configuredDir != null && !configuredDir.isEmpty()) {
            return Path.of(configuredDir).toString();
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "agenthub-files").toString();
    }
}


