const DONE_MARKERS = ['[DONE]', 'complete', 'end']

export function createSSE(url, { onMessage, onError, onComplete }) {
  let hasReceivedData = false
  let isClosed = false

  const eventSource = new EventSource(url)

  eventSource.onopen = () => {
    console.log('[SSE] Connection opened:', url)
  }

  eventSource.onmessage = (event) => {
    hasReceivedData = true
    const data = event.data
    if (DONE_MARKERS.includes(data)) {
      eventSource.close()
      isClosed = true
      onComplete?.()
      return
    }
    onMessage?.(data)
  }

  eventSource.onerror = () => {
    if (isClosed) return
    eventSource.close()
    isClosed = true
    if (hasReceivedData) {
      onComplete?.()
    } else {
      console.error('[SSE] Connection failed:', url)
      onError?.(new Error('连接失败，请确认后端服务已启动。'))
    }
  }

  return {
    close: () => {
      if (isClosed) return
      eventSource.close()
      isClosed = true
      if (hasReceivedData) {
        onComplete?.()
      }
    },
  }
}
