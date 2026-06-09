package com.agenthub.server.agent;

import com.agenthub.server.agent.model.AgentState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReActAgentTest {

    @Test
    void stepFinishesAndReturnsFinalResponseWhenNoActionIsNeeded() {
        FinalAnswerAgent agent = new FinalAnswerAgent("final answer");
        agent.setState(AgentState.RUNNING);

        String result = agent.step();

        assertEquals("final answer", result);
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    private static class FinalAnswerAgent extends ReActAgent {

        private final String finalAnswer;

        private FinalAnswerAgent(String finalAnswer) {
            this.finalAnswer = finalAnswer;
        }

        @Override
        public boolean think() {
            return false;
        }

        @Override
        public String act() {
            throw new IllegalStateException("No action should be executed for final answers");
        }

        @Override
        protected String getNoActionResult() {
            return finalAnswer;
        }
    }
}
