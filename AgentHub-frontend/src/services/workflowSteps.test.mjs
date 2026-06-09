import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildOutgoingMessage,
  deriveWorkflowSteps,
  shouldShowThinking,
} from './workflowSteps.js'

test('buildOutgoingMessage returns raw message when no prefix exists', () => {
  assert.equal(buildOutgoingMessage('Summarize this article', {}), 'Summarize this article')
})

test('buildOutgoingMessage prepends configured system prefix', () => {
  const result = buildOutgoingMessage('AI Agent trends', {
    systemPrefix: 'Follow the research report workflow.',
  })

  assert.equal(result, 'Follow the research report workflow.\n\nUser task: AI Agent trends')
})

test('deriveWorkflowSteps marks configured steps from streamed content', () => {
  const steps = deriveWorkflowSteps(
    'I will analyze the task, search sources, and generate a PDF report.',
    [
      { key: 'analyse', label: 'Analyze task', keywords: ['analyze the task'] },
      { key: 'search', label: 'Search sources', keywords: ['search sources'] },
      { key: 'pdf', label: 'Generate PDF', keywords: ['generate a PDF'] },
    ]
  )

  assert.deepEqual(steps, [
    { key: 'analyse', label: 'Analyze task', status: 'done' },
    { key: 'search', label: 'Search sources', status: 'done' },
    { key: 'pdf', label: 'Generate PDF', status: 'done' },
  ])
})

test('shouldShowThinking is true only before assistant content arrives', () => {
  assert.equal(shouldShowThinking({ role: 'assistant', status: 'streaming', content: '' }), true)
  assert.equal(shouldShowThinking({ role: 'assistant', status: 'streaming', content: 'Hello' }), false)
  assert.equal(shouldShowThinking({ role: 'user', status: 'streaming', content: '' }), false)
  assert.equal(shouldShowThinking({ role: 'assistant', status: 'done', content: '' }), false)
})
