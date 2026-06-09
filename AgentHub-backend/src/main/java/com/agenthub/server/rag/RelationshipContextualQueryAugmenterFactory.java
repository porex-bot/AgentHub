package com.agenthub.server.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 上下文增强器工厂，当知识库检索不到内容时，生成更合适的兜底提示，避免模型胡编。
 */
public class RelationshipContextualQueryAugmenterFactory {

    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，这个问题超出了我当前的关系咨询知识范围。如果你愿意，可以把问题改写为恋爱、亲密关系或沟通冲突相关的场景，我会尽量给出具体建议。
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
