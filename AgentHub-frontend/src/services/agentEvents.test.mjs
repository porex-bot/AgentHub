import test from 'node:test'
import assert from 'node:assert/strict'

import {
  AGENT_EVENT_PREFIX,
  applyAgentEventToMessage,
  parseAgentEvent,
} from './agentEvents.js'

test('parseAgentEvent returns raw content for normal SSE text', () => {
  assert.deepEqual(parseAgentEvent('hello'), {
    type: 'raw',
    content: 'hello',
  })
})

test('parseAgentEvent parses structured content events', () => {
  const event = parseAgentEvent(`${AGENT_EVENT_PREFIX}{"type":"content","content":"报告正文"}`)

  assert.deepEqual(event, {
    type: 'content',
    content: '报告正文',
  })
})

test('applyAgentEventToMessage appends content and preserves raw fallback', () => {
  const message = { content: '', attachments: [] }

  applyAgentEventToMessage('hello', message)
  applyAgentEventToMessage(`${AGENT_EVENT_PREFIX}{"type":"content","content":" world"}`, message)

  assert.equal(message.content, 'hello world')
  assert.deepEqual(message.attachments, [])
})

test('applyAgentEventToMessage adds normalized attachment links', () => {
  const message = { content: '', attachments: [] }

  const result = applyAgentEventToMessage(
    `${AGENT_EVENT_PREFIX}{"type":"attachment","label":"下载 PDF","url":"/api/ai/files/pdf/report.pdf","fileName":"report.pdf","mimeType":"application/pdf"}`,
    message,
    'http://backend.test/api'
  )

  assert.equal(result.type, 'attachment')
  assert.equal(message.content, '')
  assert.deepEqual(message.attachments, [
    {
      label: '下载 PDF',
      url: 'http://backend.test/api/ai/files/pdf/report.pdf',
      fileName: 'report.pdf',
      mimeType: 'application/pdf',
    },
  ])
})

test('applyAgentEventToMessage handles concatenated structured events', () => {
  const message = { content: '', attachments: [] }

  const result = applyAgentEventToMessage(
    `${AGENT_EVENT_PREFIX}{"type":"content","content":"报告正文"}` +
      `${AGENT_EVENT_PREFIX}{"type":"content","content":"\\n\\nPDF 已生成，可点击链接下载。"}`,
    message
  )

  assert.equal(message.content, '报告正文\n\nPDF 已生成，可点击链接下载。')
  assert.deepEqual(message.attachments, [])
  assert.equal(result.type, 'batch')
  assert.equal(result.events.length, 2)
})
