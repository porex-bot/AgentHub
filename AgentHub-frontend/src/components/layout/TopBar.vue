<template>
  <header class="tb">
    <div class="tb-left">
      <button v-if="showBack" class="tb-back" aria-label="返回" @click="$emit('back')">
        <ArrowLeft :size="20" />
      </button>
      <slot name="left" />
    </div>
    <div class="tb-center">
      <slot name="center">
        <span class="tb-title">{{ title }}</span>
      </slot>
    </div>
    <div class="tb-right">
      <slot name="right" />
    </div>
    <div class="tb-accent"></div>
  </header>
</template>

<script setup>
import { ArrowLeft } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: false },
})

defineEmits(['back'])
</script>

<style scoped>
.tb {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 var(--space-4);
  background: var(--bg-surface);
  border-bottom: 1px solid var(--color-neutral-200);
  flex-shrink: 0;
  gap: var(--space-3);
  position: relative;
}

.tb-accent {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--app-primary, var(--color-primary));
}

.tb-left,
.tb-right {
  display: flex;
  align-items: center;
  min-width: 48px;
}

.tb-right {
  justify-content: flex-end;
}

.tb-center {
  flex: 1;
  text-align: center;
  min-width: 0;
}

.tb-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.tb-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  transition: background 0.16s, color 0.16s;
}

.tb-back:hover {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

@media (max-width: 640px) {
  .tb {
    height: 64px;
    padding: 0 var(--space-3);
  }
}
</style>
