<template>
  <div class="cw" :class="`cw--${config.key}`">
    <TopBar :title="config.name" :show-back="true" @back="goHome">
      <template #center>
        <div class="cw-heading">
          <span class="cw-title">{{ config.name }}</span>
          <span class="cw-category">{{ config.category }}</span>
        </div>
      </template>
      <template #right>
        <span v-if="chatId" class="cw-tag">{{ shortChatId }}</span>
      </template>
    </TopBar>

    <section class="cw-context">
      <div class="cw-copy">
        <span class="cw-desc">{{ config.description }}</span>
      </div>
      <div class="cw-caps" aria-label="能力标签">
        <span v-for="capability in config.capabilities" :key="capability" class="cw-cap">
          {{ capability }}
        </span>
      </div>
    </section>

    <MessageList
      :messages="messages"
      :assistant-label="config.assistantLabel"
      :empty-icon="config.icon"
      :empty-title="emptyTitle"
      :empty-sub="emptySub"
      :suggestions="config.suggestions"
      @retry="retry"
      @use-suggestion="$emit('useSuggestion', $event)"
    />

    <ChatInput
      :model-value="inputText"
      :status="status"
      :placeholder="config.placeholder"
      @update:model-value="$emit('update:inputText', $event)"
      @send="send"
      @stop="stopGeneration"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '../layout/TopBar.vue'
import MessageList from './MessageList.vue'
import ChatInput from './ChatInput.vue'

const props = defineProps({
  config: { type: Object, required: true },
  messages: { type: Array, default: () => [] },
  inputText: { type: String, default: '' },
  status: { type: String, default: 'idle' },
  chatId: { type: String, default: null },
  send: { type: Function, required: true },
  stopGeneration: { type: Function, required: true },
  retry: { type: Function, required: true },
})

defineEmits(['update:inputText', 'useSuggestion'])

const router = useRouter()

const shortChatId = computed(() => {
  if (!props.chatId) return ''
  const s = props.chatId
  return s.length > 10 ? `${s.slice(0, 10)}...` : s
})

const emptyTitle = computed(() => `开始使用 ${props.config.name}`)
const emptySub = computed(() => '输入任务或选择下方建议问题，AI 会通过流式响应实时返回结果。')

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.cw {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  background: var(--bg-page);
}

.cw--relationship {
  --app-primary: #e11d48;
  --app-primary-hover: #be123c;
  --app-primary-light: #fff1f2;
  --app-assistant-bg: #fff1f2;
  --app-assistant-color: #be123c;
  --app-assistant-icon: #e11d48;
  --app-chat-bg: #f8f9fa;
}

.cw--task {
  --app-primary: var(--color-primary);
  --app-primary-hover: var(--color-primary-hover);
  --app-primary-light: var(--color-primary-light);
  --app-assistant-bg: var(--color-success-light);
  --app-assistant-color: #047857;
  --app-assistant-icon: var(--color-teal);
  --app-chat-bg: #f8f9fa;
}

.cw--report {
  --app-primary: var(--color-teal);
  --app-primary-hover: #115e59;
  --app-primary-light: var(--color-success-light);
  --app-assistant-bg: #f0fdfa;
  --app-assistant-color: var(--color-teal);
  --app-assistant-icon: var(--color-teal);
  --app-chat-bg: #f8f9fa;
}

.cw-heading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.cw-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.cw-category {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.cw-tag {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  padding: var(--space-1) var(--space-2);
  background: var(--bg-subtle);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
}

.cw-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  background: var(--bg-surface);
  border-bottom: 1px solid var(--color-neutral-200);
}

.cw-copy {
  min-width: 0;
}

.cw-desc {
  display: block;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cw-caps {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.cw-cap {
  font-size: var(--text-xs);
  color: var(--app-primary, var(--color-primary));
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--app-primary, var(--color-primary));
  border-radius: var(--radius-sm);
  background: var(--app-primary-light, var(--color-primary-light));
}

@media (max-width: 640px) {
  .cw-context {
    align-items: flex-start;
    flex-direction: column;
  }

  .cw-desc {
    white-space: normal;
  }

  .cw-caps {
    flex-wrap: wrap;
  }
}
</style>
