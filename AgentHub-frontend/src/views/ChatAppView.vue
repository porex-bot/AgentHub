<template>
  <div v-if="config" class="cv">
    <ChatWindow
      :key="config.key"
      :config="config"
      :messages="messages"
      :input-text="inputText"
      :status="status"
      :chat-id="chatId"
      :send="send"
      :stop-generation="stopGeneration"
      :retry="retry"
      @update:input-text="onInput"
      @use-suggestion="onInput"
    />
  </div>
  <div v-else class="cv-empty">
    <p class="cv-empty-text">未知智能体模式</p>
    <button class="cv-empty-btn" @click="goHome">返回工作台</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAppConfig } from '../config/chatApps'
import { useChatSession } from '../composables/useChatSession'
import ChatWindow from '../components/chat/ChatWindow.vue'

const route = useRoute()
const router = useRouter()
const config = computed(() => getAppConfig(route.path))

const { messages, inputText, status, chatId, send, stopGeneration, retry } =
  useChatSession(config.value || {})

function onInput(val) {
  inputText.value = val
}

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.cv {
  height: 100vh;
  height: 100dvh;
}

.cv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  height: 100dvh;
  gap: var(--space-4);
  background: var(--bg-page);
}

.cv-empty-text {
  font-size: var(--text-lg);
  color: var(--text-secondary);
}

.cv-empty-btn {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  transition: background 0.16s;
}

.cv-empty-btn:hover {
  background: var(--color-primary-light);
}
</style>
