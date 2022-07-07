import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({
  gfm: true,
  breaks: true,
})

/**
 * Renders markdown to HTML.
 *
 * AGENTS.md section 5 forbids handing unsanitised HTML to `v-html`, so every
 * result passes through DOMPurify. Scripts, event handlers, `javascript:`
 * URLs, iframes and object tags are all stripped here.
 */
export function renderMarkdown(markdown: string | null | undefined): string {
  if (!markdown) {
    return ''
  }
  const raw = marked.parse(markdown) as string
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: [
      'p', 'br', 'hr',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'strong', 'em', 'del', 'code', 'pre', 'blockquote',
      'ul', 'ol', 'li',
      'a', 'img',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'span', 'div',
    ],
    ALLOWED_ATTR: ['href', 'title', 'alt', 'src', 'width', 'height', 'class', 'align'],
    ALLOW_DATA_ATTR: false,
  })
}

/** Plain-text preview for list rows and meta descriptions. */
export function toPlainText(markdown: string | null | undefined, maxLength = 200): string {
  if (!markdown) {
    return ''
  }
  const text = markdown
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
    .replace(/^\s{0,3}#{1,6}\s*/gm, '')
    .replace(/[*_`~#>|-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length <= maxLength ? text : `${text.slice(0, maxLength)}…`
}
