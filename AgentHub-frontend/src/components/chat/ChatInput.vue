<template>
  <div class="ci">
    <ConnectionStatus :status="status" />
    <div class="ci-row">
      <textarea
        ref="taRef"
        v-model="model"
        class="ci-ta"
        :placeholder="placeholder"
        :disabled="streaming"
        rows="1"
        aria-label="输入消息"
        @input="resize"
        @keydown="onKey"
      ></textarea>
      <button
        v-if="streaming"
        class="ci-btn ci-btn--stop"
        aria-label="停止生成"
        @click="$emit('stop')"
      >
        <Square :size="16" />
      </button>
      <button
        v-else
        class="ci-btn ci-btn--send"
        :disabled="!canSend"
        aria-label="发送"
        @click="doSend"
      >
        <ArrowUp :size="16" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { ArrowUp, Square } from 'lucide-vue-next'
import ConnectionStatus from './ConnectionStatus.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  status: { type: String, default: 'idle' },
  placeholder: { type: String, default: '输入消息...' },
})

const emit = defineEmits(['update:modelValue', 'send', 'stop'])
const taRef = ref(null)

const model = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const streaming = computed(() => props.status === 'streaming' || props.status === 'connecting')
const canSend = computed(() => model.value.trim().length > 0)

function resize() {
  const el = taRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 144)}px`
}

function onKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    doSend()
  }
}

function doSend() {
  if (!canSend.value) return
  emit('send')
  nextTick(() => {
    const el = taRef.value
    if (el) el.style.height = 'auto'
  })
}
</script>

<style scoped>
.ci {
  flex-shrink: 0;
  padding: var(--space-3) var(--space-4) var(--space-4);
  background: var(--bg-surface);
  border-top: 1px solid var(--color-neutral-200);
}

.ci-row {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  background: var(--bg-page);
  border: 1px solid var(--color-neutral-300);
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-2) var(--space-2) var(--space-4);
  transition: border-color 0.16s, outline-color 0.16s;
}

.ci-row:focus-within {
  border-color: var(--app-primary, var(--color-primary));
  outline: 2px solid var(--app-primary-light, var(--color-primary-light));
  outline-offset: 0;
}

.ci-ta {
  flex: 1;
  resize: none;
  border: none;
  background: transparent;
  outline: none;
  font-size: var(--text-sm);
  line-height: 1.5;
  color: var(--text-primary);
  padding: var(--space-2) 0;
  min-height: 32px;
  max-height: 144px;
  align-self: center;
}

.ci-ta::placeholder {
  color: var(--text-weak);
}

.ci-ta:disabled {
  opacity: 0.45;
}

.ci-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  transition: background 0.16s, color 0.16s;
}

.ci-btn--send {
  background: var(--app-primary, var(--color-primary));
  color: #ffffff;
}

.ci-btn--send:hover:not(:disabled) {
  background: var(--app-primary-hover, var(--color-primary-hover));
}

.ci-btn--send:disabled {
  background: var(--color-neutral-200);
  color: var(--color-neutral-400);
  cursor: not-allowed;
}

.ci-btn--stop {
  background: var(--color-error);
  color: #ffffff;
}

.ci-btn--stop:hover {
  background: #b91c1c;
}

@media (max-width: 640px) {
  .ci {
    padding: var(--space-2) var(--space-3) var(--space-3);
    padding-bottom: max(var(--space-3), env(safe-area-inset-bottom));
  }

  .ci-row {
    padding-left: var(--space-3);
  }
}
</style>
