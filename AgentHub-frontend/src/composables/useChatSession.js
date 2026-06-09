import { ref, onUnmounted } from 'vue'
import { applyAgentEventToMessage } from '../services/agentEvents'
import { sendChatMessage } from '../services/chatService'
import { API_BASE_URL } from '../services/httpClient'
import { deriveWorkflowSteps } from '../services/workflowSteps'

let msgCounter = 0

function genId() {
  return `msg_${Date.now()}_${++msgCounter}`
}

function genChatId() {
  if (crypto.randomUUID) {
    return crypto.randomUUID()
  }

  return `chat_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export function useChatSession(config) {
  const messages = ref([])
  const inputText = ref('')
  const status = ref('idle')
  const chatId = config.requiresChatId ? genChatId() : null

  let currentSSE = null

  function addMessage(role, content, msgStatus = 'done', extra = {}) {
    const len = messages.value.push({
      id: genId(),
      role,
      content,
      status: msgStatus,
      createdAt: new Date(),
      errorMessage: extra.errorMessage || null,
      sourceMessageId: extra.sourceMessageId || null,
      steps: extra.steps || [],
      attachments: extra.attachments || [],
    })

    return messages.value[len - 1]
  }

  function closeSSE() {
    if (currentSSE) {
      currentSSE.close()
      currentSSE = null
    }
  }

  function updateAssistantSteps(assistantMsg) {
    assistantMsg.steps = deriveWorkflowSteps(assistantMsg.content, config.workflowSteps)
  }

  function finishStreamingAssistant(contentReceived) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant' && last.status === 'streaming') {
      if (contentReceived) {
        last.status = 'done'
      } else {
        last.status = 'error'
        last.errorMessage = '生成中断，可以重试。'
      }
    }
  }

  function openStream(message, assistantMsg) {
    status.value = 'connecting'

    currentSSE = sendChatMessage(
      config,
      { message, chatId },
      {
        onMessage: (data) => {
          status.value = 'streaming'
          applyAgentEventToMessage(data, assistantMsg, API_BASE_URL)
          updateAssistantSteps(assistantMsg)
        },
        onComplete: () => {
          assistantMsg.status = 'done'
          updateAssistantSteps(assistantMsg)
          status.value = 'idle'
          currentSSE = null
        },
        onError: (error) => {
          assistantMsg.status = 'error'
          assistantMsg.errorMessage = error.message || '连接失败，请稍后重试。'
          updateAssistantSteps(assistantMsg)
          status.value = 'error'
          currentSSE = null
        },
      }
    )
  }

  function send() {
    const text = inputText.value.trim()
    if (!text) return
    if (status.value === 'streaming' || status.value === 'connecting') return

    closeSSE()

    inputText.value = ''
    addMessage('user', text, 'done')
    const assistantMsg = addMessage('assistant', '', 'streaming')

    openStream(text, assistantMsg)
  }

  function stopGeneration() {
    closeSSE()
    finishStreamingAssistant(messages.value.length > 0)
    status.value = 'idle'
  }

  function retry() {
    closeSSE()

    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant' && last.status === 'error') {
      messages.value.pop()
    }

    const userMsg = [...messages.value].reverse().find((m) => m.role === 'user')
    if (!userMsg) return

    const assistantMsg = addMessage('assistant', '', 'streaming')
    openStream(userMsg.content, assistantMsg)
  }

  onUnmounted(() => {
    closeSSE()
  })

  return {
    messages,
    inputText,
    status,
    chatId,
    send,
    stopGeneration,
    retry,
  }
}
