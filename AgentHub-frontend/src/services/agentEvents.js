export const AGENT_EVENT_PREFIX = '__agenthub_event__:'

export function parseAgentEvent(data) {
  const text = String(data ?? '')
  if (!text.startsWith(AGENT_EVENT_PREFIX)) {
    return { type: 'raw', content: text }
  }

  const json = text.slice(AGENT_EVENT_PREFIX.length)
  try {
    const payload = JSON.parse(json)
    if (!payload || typeof payload !== 'object' || typeof payload.type !== 'string') {
      return { type: 'raw', content: text }
    }
    return payload
  } catch {
    return { type: 'raw', content: text }
  }
}

export function applyAgentEventToMessage(data, message, apiBaseUrl = '') {
  const events = parseAgentEvents(data)
  if (events.length > 1) {
    events.forEach((event) => applyParsedAgentEvent(event, message, apiBaseUrl))
    return { type: 'batch', events }
  }

  return applyParsedAgentEvent(events[0], message, apiBaseUrl)
}

function parseAgentEvents(data) {
  const text = String(data ?? '')
  if (!text.startsWith(AGENT_EVENT_PREFIX)) {
    return [{ type: 'raw', content: text }]
  }

  const singleEvent = parseAgentEvent(text)
  if (singleEvent.type !== 'raw') {
    return [singleEvent]
  }

  const chunks = text
    .split(AGENT_EVENT_PREFIX)
    .filter((chunk) => chunk.length > 0)

  return chunks.map((chunk) => parseAgentEvent(`${AGENT_EVENT_PREFIX}${chunk}`))
}

function applyParsedAgentEvent(event, message, apiBaseUrl = '') {
  if (event.type === 'raw' || event.type === 'content') {
    message.content += String(event.content ?? '')
    return event
  }

  if (event.type === 'attachment') {
    if (!Array.isArray(message.attachments)) {
      message.attachments = []
    }
    const attachment = {
      label: event.label || event.fileName || '下载文件',
      url: normalizeAttachmentUrl(event.url, apiBaseUrl),
      fileName: event.fileName || '',
      mimeType: event.mimeType || 'application/octet-stream',
    }
    message.attachments.push(attachment)
    return { ...event, ...attachment }
  }

  return event
}

function normalizeAttachmentUrl(url, apiBaseUrl = '') {
  const value = String(url || '')
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) {
    return value
  }
  const base = apiBaseUrl || window.location.origin
  return new URL(value, base.endsWith('/') ? base : `${base}/`).toString()
}
