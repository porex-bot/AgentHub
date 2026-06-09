<template>
  <div class="msg" :class="`msg--${message.role}`">
    <div class="msg-avatar" :class="`msg-avatar--${message.role}`">
      <User v-if="message.role === 'user'" :size="16" />
      <Sparkles v-else :size="16" />
    </div>

    <div class="msg-main">
      <span class="msg-label">{{ label }}</span>
      <div class="msg-bubble">
        <span v-if="showThinking" class="msg-thinking">正在思考...</span>
        <span v-else class="msg-text">{{ message.content }}</span>
        <span v-if="message.status === 'streaming' && !showThinking" class="msg-cursor">|</span>
      </div>

      <div v-if="showAttachments" class="msg-attachments" aria-label="attachments">
        <a
          v-for="attachment in message.attachments"
          :key="attachment.url"
          class="msg-attachment"
          :href="attachment.url"
          :download="attachment.fileName || undefined"
          target="_blank"
          rel="noopener"
        >
          <Download :size="16" />
          <span>{{ attachment.label || attachment.fileName || '下载文件' }}</span>
        </a>
      </div>

      <div v-if="showSteps" class="msg-steps" aria-label="workflow steps">
        <div
          v-for="step in message.steps"
          :key="step.key"
          class="msg-step"
          :class="`msg-step--${step.status}`"
        >
          <CheckCircle2 v-if="step.status === 'done'" :size="16" />
          <Circle v-else :size="16" />
          <span>{{ step.label }}</span>
        </div>
      </div>

      <div class="msg-meta">
        <span class="msg-time">{{ formattedTime }}</span>
        <button v-if="message.status === 'error'" class="msg-retry" @click="$emit('retry')">
          <RefreshCw :size="12" />
          重试
        </button>
      </div>

      <p v-if="message.status === 'error' && message.errorMessage" class="msg-err">
        {{ message.errorMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CheckCircle2, Circle, Download, RefreshCw, Sparkles, User } from 'lucide-vue-next'
import { shouldShowThinking } from '../../services/workflowSteps'

const props = defineProps({
  message: { type: Object, required: true },
  label: { type: String, default: '' },
})

defineEmits(['retry'])

const showSteps = computed(
  () => props.message.role === 'assistant' && props.message.steps && props.message.steps.length > 0
)

const showThinking = computed(() => shouldShowThinking(props.message))

const showAttachments = computed(
  () => props.message.role === 'assistant' && props.message.attachments && props.message.attachments.length > 0
)

const formattedTime = computed(() => {
  if (!props.message.createdAt) return ''
  const d = new Date(props.message.createdAt)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
})
</script>

<style scoped>
.msg {
  display: flex;
  gap: var(--space-3);
  max-width: 76%;
  margin-bottom: var(--space-6);
}

.msg--user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg--assistant {
  align-self: flex-start;
}

.msg-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  margin-top: var(--space-6);
}

.msg-avatar--user {
  background: var(--app-primary-light, var(--color-primary-light));
  color: var(--app-primary, var(--color-primary));
}

.msg-avatar--assistant {
  background: var(--app-assistant-bg, var(--color-success-light));
  color: var(--app-assistant-icon, var(--color-teal));
}

.msg-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.msg--user .msg-main {
  align-items: flex-end;
}

.msg-label {
  font-size: var(--text-xs);
  color: var(--text-weak);
  margin-bottom: var(--space-1);
  padding: 0 var(--space-1);
}

.msg-bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg--user .msg-bubble {
  background: var(--app-primary, var(--color-primary));
  color: #ffffff;
}

.msg--assistant .msg-bubble {
  background: var(--bg-surface);
  color: var(--text-primary);
  border: 1px solid var(--color-neutral-200);
}

.msg-thinking {
  font-size: var(--text-xs);
  color: var(--text-weak);
}

.msg-cursor {
  color: var(--app-primary, var(--color-primary));
  animation: blink 1s step-end infinite;
}

.msg--user .msg-cursor {
  color: #ffffff;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.msg-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.msg-attachment {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  max-width: 100%;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  color: var(--app-primary, var(--color-primary));
  background: var(--bg-surface);
  border: 1px solid var(--app-primary, var(--color-primary));
  border-radius: var(--radius-sm);
  transition: background 0.16s;
}

.msg-attachment:hover {
  background: var(--app-primary-light, var(--color-primary-light));
}

.msg-attachment span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding: var(--space-3);
  background: var(--bg-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
}

.msg-step {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.msg-step--done {
  color: var(--color-success);
}

.msg-step span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-1);
  padding: 0 var(--space-1);
}

.msg-time {
  font-size: var(--text-xs);
  color: var(--text-weak);
}

.msg-retry {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-error);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  transition: background 0.16s;
}

.msg-retry:hover {
  background: var(--color-error-light);
}

.msg-err {
  font-size: var(--text-xs);
  color: var(--color-error);
  margin-top: var(--space-1);
  padding: var(--space-2) var(--space-3);
  background: var(--color-error-light);
  border-radius: var(--radius-sm);
  border: 1px solid #fecaca;
}

@media (max-width: 640px) {
  .msg {
    max-width: 92%;
    gap: var(--space-2);
  }

  .msg-avatar {
    width: 32px;
    height: 32px;
    margin-top: var(--space-6);
  }

  .msg-steps {
    grid-template-columns: 1fr;
  }
}
</style>
