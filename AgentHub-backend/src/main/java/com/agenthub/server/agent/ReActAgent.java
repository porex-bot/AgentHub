package com.agenthub.server.agent;

import com.agenthub.server.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                setState(AgentState.FINISHED);
                return getNoActionResult();
            }
            return act();
        } catch (Exception e) {
            return "步骤执行失败：" + e.getMessage();
        }
    }

    protected String getNoActionResult() {
        return "思考完成，无需调用工具。";
    }
}
