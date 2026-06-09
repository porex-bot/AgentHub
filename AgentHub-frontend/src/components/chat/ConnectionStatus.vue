<template>
  <div v-if="text" class="cs" :class="'cs--' + type">
    <span v-if="type === 'connecting' || type === 'streaming'" class="cs-dot"></span>
    <span>{{ text }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'idle' },
})

const type = computed(() => {
  switch (props.status) {
    case 'connecting':
      return 'connecting'
    case 'streaming':
      return 'streaming'
    case 'error':
      return 'error'
    default:
      return ''
  }
})

const text = computed(() => {
  switch (props.status) {
    case 'connecting':
      return '正在连接...'
    case 'streaming':
      return '正在生成回复'
    case 'error':
      return '连接失败'
    default:
      return ''
  }
})
</script>

<style scoped>
.cs {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  font-size: var(--text-xs);
}

.cs--connecting,
.cs--streaming {
  color: var(--app-primary, var(--color-primary));
}

.cs--error {
  color: var(--color-error);
}

.cs-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-sm);
  background: currentColor;
  animation: pulse 1.4s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.3;
    transform: scale(0.75);
  }
}
</style>
