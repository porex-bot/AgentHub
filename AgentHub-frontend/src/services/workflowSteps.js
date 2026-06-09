export function buildOutgoingMessage(message, config = {}) {
  const text = String(message || '').trim()
  const prefix = String(config.systemPrefix || '').trim()

  if (!prefix) {
    return text
  }

  return `${prefix}\n\nUser task: ${text}`
}

export function deriveWorkflowSteps(content, workflowSteps = []) {
  const text = String(content || '')

  return workflowSteps.map((step) => {
    const matched = (step.keywords || []).some((keyword) => text.includes(keyword))
    return {
      key: step.key,
      label: step.label,
      status: matched ? 'done' : 'pending',
    }
  })
}

export function shouldShowThinking(message) {
  return (
    message?.role === 'assistant' &&
    message?.status === 'streaming' &&
    String(message?.content || '').trim().length === 0
  )
}
