package com.agenthub.server.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerminalOperationToolTest {

    @Test
    void executeTerminalCommand() {
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        String command = "cd C:";
        String result = terminalOperationTool.executeTerminalCommand(command);
        assertNotNull(result);
    }
}

