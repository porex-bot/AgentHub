import { createSSE } from './sseClient'
import { API_BASE_URL } from './httpClient'
import { buildOutgoingMessage } from './workflowSteps'

export function sendChatMessage(config, params, callbacks) {
  const endpointPath = config.endpoint.replace(/^\/api\/?/, '')
  const url = new URL(endpointPath, `${API_BASE_URL}/`)
  url.searchParams.set('message', buildOutgoingMessage(params.message, config))
  if (config.requiresChatId && params.chatId) {
    url.searchParams.set('chatId', params.chatId)
  }
  return createSSE(url.toString(), callbacks)
}
