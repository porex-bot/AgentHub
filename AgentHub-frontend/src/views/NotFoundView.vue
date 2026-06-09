<template>
  <div class="nf">
    <p class="nf-code">404</p>
    <p class="nf-text">页面未找到</p>
    <button class="nf-btn" @click="goHome">返回工作台</button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.nf {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  height: 100dvh;
  gap: var(--space-2);
  background: var(--bg-page);
}

.nf-code {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--color-neutral-300);
  line-height: 1.25;
}

.nf-text {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
}

.nf-btn {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: 600;
  color: #ffffff;
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  transition: background 0.16s;
}

.nf-btn:hover {
  background: var(--color-primary-hover);
}
</style>
