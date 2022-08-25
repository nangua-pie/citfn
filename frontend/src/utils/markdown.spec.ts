import { describe, expect, it } from 'vitest'

import { renderMarkdown, toPlainText } from '@/utils/markdown'

/**
 * AGENTS.md section 5 forbids handing unsanitised HTML to `v-html`, so these
 * guard the sanitisation boundary itself rather than the Markdown styling.
 */
describe('renderMarkdown sanitisation', () => {
  it('strips script tags', () => {
    const html = renderMarkdown('before <script>alert(1)</script> after')
    expect(html).not.toContain('<script')
    expect(html).not.toContain('alert(1)')
  })

  it('strips inline event handlers', () => {
    const html = renderMarkdown('<img src="x" onerror="alert(1)">')
    expect(html).not.toContain('onerror')
  })

  it('drops javascript: URLs', () => {
    const html = renderMarkdown('[click me](javascript:alert(1))')
    expect(html.toLowerCase()).not.toContain('javascript:')
  })

  it('strips iframes and object embeds', () => {
    expect(renderMarkdown('<iframe src="https://example.com"></iframe>')).not.toContain('<iframe')
    expect(renderMarkdown('<object data="x"></object>')).not.toContain('<object')
  })

  it('keeps the formatting the forum depends on', () => {
    const html = renderMarkdown('# Title\n\n**bold** and `code`\n\n- one\n- two')
    expect(html).toContain('<h1')
    expect(html).toContain('<strong>bold</strong>')
    expect(html).toContain('<code>code</code>')
    expect(html).toContain('<li>one</li>')
  })

  it('keeps table markup, which the list and detail styles both target', () => {
    const html = renderMarkdown('| a | b |\n| --- | --- |\n| 1 | 2 |')
    expect(html).toContain('<table')
    expect(html).toContain('<th')
  })

  it('returns an empty string for empty input rather than throwing', () => {
    expect(renderMarkdown(null)).toBe('')
    expect(renderMarkdown(undefined)).toBe('')
    expect(renderMarkdown('')).toBe('')
  })
})

describe('toPlainText', () => {
  it('strips markup down to readable text', () => {
    expect(toPlainText('# Heading')).toBe('Heading')
    expect(toPlainText('**bold** and _italic_')).toBe('bold and italic')
    expect(toPlainText('[a link](https://example.com)')).toBe('a link')
  })

  it('truncates with an ellipsis and honours the limit', () => {
    const text = toPlainText('a'.repeat(50), 10)
    expect(text).toHaveLength(11)
    expect(text.endsWith('…')).toBe(true)
  })

  it('handles empty input', () => {
    expect(toPlainText(null)).toBe('')
    expect(toPlainText(undefined)).toBe('')
  })
})
