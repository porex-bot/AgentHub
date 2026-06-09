<template>
  <div ref="listRef" class="ml" @scroll="onScroll">
    <div v-if="messages.length === 0" class="ml-empty">
      <div class="ml-empty-ring">
        <component :is="emptyIcon" :size="28" />
      </div>
      <p class="ml-empty-hd">{{ emptyTitle }}</p>
      <p class="ml-empty-sub">{{ emptySub }}</p>
      <div v-if="suggestions.length" class="ml-suggestions">
        <button
          v-for="suggestion in suggestions"
          :key="suggestion"
          class="ml-suggestion"
          @click="$emit('useSuggestion', suggestion)"
        >
          {{ suggestion }}
        </button>
      </div>
    </div>

    <MessageBubble
      v-for="msg in messages"
      :key="msg.id"
      :message="msg"
      :label="msg.role === 'user' ? '你' : assistantLabel"
      @retry="$emit('retry')"
    />

    <div v-if="showHint" class="ml-hint">
      <button class="ml-hint-btn" @click="scrollToBottom">
        <ArrowDown :size="16" />
        新消息
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { ArrowDown, MessageCircle } from 'lucide-vue-next'
import MessageBubble from './MessageBubble.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  assistantLabel: { type: String, default: 'AI' },
  emptyIcon: { type: Object, default: () => MessageCircle },
  emptyTitle: { type: String, default: '开始对话' },
  emptySub: { type: String, default: '输入内容后发送，AI 会实时返回结果。' },
  suggestions: { type: Array, default: () => [] },
})

defineEmits(['retry', 'useSuggestion'])

const listRef = ref(null)
const showHint = ref(false)
let userScrolledUp = false

function nearBottom() {
  const el = listRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 64
}

function scrollToBottom() {
  const el = listRef.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: 'instant' })
  showHint.value = false
  userScrolledUp = false
}

function onScroll() {
  if (nearBottom()) {
    showHint.value = false
    userScrolledUp = false
  } else {
    userScrolledUp = true
  }
}

watch(
  () => props.messages.length,
  () => nextTick(() => {
    if (!userScrolledUp) scrollToBottom()
  })
)

watch(
  () => {
    const last = props.messages[props.messages.length - 1]
    return last ? last.content : ''
  },
  () => nextTick(() => {
    if (!userScrolledUp) {
      scrollToBottom()
    } else {
      showHint.value = true
    }
  })
)

onMounted(() => scrollToBottom())
</script>

<style scoped>
.ml {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  padding: var(--space-6);
  background: var(--app-chat-bg, var(--bg-page));
}

.ml-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-12) var(--space-4);
}

.ml-empty-ring {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  border: 1px solid var(--color-neutral-200);
  color: var(--app-primary, var(--color-primary));
  margin-bottom: var(--space-2);
}

.ml-empty-hd {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.ml-empty-sub {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  text-align: center;
  max-width: 420px;
  line-height: 1.5;
}

.ml-suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
  max-width: 720px;
  margin-top: var(--space-4);
}

.ml-suggestion {
  max-width: 320px;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  color: var(--text-primary);
  text-align: left;
  background: var(--bg-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-sm);
  transition: border-color 0.16s, background 0.16s;
}

.ml-suggestion:hover {
  border-color: var(--app-primary, var(--color-primary));
  background: var(--app-primary-light, var(--color-primary-light));
}

.ml-hint {
  display: flex;
  justify-content: center;
  padding: var(--space-2) 0;
  position: sticky;
  bottom: 0;
}

.ml-hint-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--app-primary, var(--color-primary));
  background: var(--bg-surface);
  border: 1px solid var(--app-primary, var(--color-primary));
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-sm);
  transition: background 0.16s;
}

.ml-hint-btn:hover {
  background: var(--app-primary-light, var(--color-primary-light));
}

@media (max-width: 640px) {
  .ml {
    padding: var(--space-4) var(--space-3);
  }
}
</style>
