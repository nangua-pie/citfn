/**
 * Text transformations behind the markdown toolbar.
 *
 * Every function is pure: it takes the textarea contents and the selection,
 * and returns the new contents plus where the caret should end up. The
 * component applies the result and restores the selection, which keeps the
 * fiddly part, offsets after an insert, testable without a DOM.
 */

export interface EditorState {
  value: string
  selectionStart: number
  selectionEnd: number
}

/** A line prefix that can be toggled off again, such as a heading or a quote. */
export interface LinePrefix {
  label: string
  icon: string
  title: string
  prefix: string
}

/** Clamps an offset into the document, so a stale selection cannot throw. */
function clamp(offset: number, value: string): number {
  if (offset < 0) return 0
  return offset > value.length ? value.length : offset
}

/** Clamps a number into an explicit range. */
function clampTo(offset: number, min: number, max: number): number {
  if (offset < min) return min
  return offset > max ? max : offset
}

/** Normalises a possibly reversed or out of range selection. */
function normalize(state: EditorState): EditorState {
  const start = clamp(Math.min(state.selectionStart, state.selectionEnd), state.value)
  const end = clamp(Math.max(state.selectionStart, state.selectionEnd), state.value)
  return { value: state.value, selectionStart: start, selectionEnd: end }
}

/**
 * Wraps the selection in `marker`, or unwraps it when it is already wrapped.
 *
 * With nothing selected the markers are inserted and the caret is placed
 * between them, which is what a keyboard user expects from Ctrl+B.
 */
export function toggleWrap(state: EditorState, marker: string): EditorState {
  const { value, selectionStart: start, selectionEnd: end } = normalize(state)
  const size = marker.length
  const selected = value.slice(start, end)

  if (selected.length >= size * 2 && selected.startsWith(marker) && selected.endsWith(marker)) {
    const inner = selected.slice(size, selected.length - size)
    return {
      value: value.slice(0, start) + inner + value.slice(end),
      selectionStart: start,
      selectionEnd: start + inner.length,
    }
  }

  return {
    value: value.slice(0, start) + marker + selected + marker + value.slice(end),
    selectionStart: start + size,
    selectionEnd: start + size + selected.length,
  }
}

interface Block {
  start: number
  end: number
  lines: string[]
}

/** The lines touched by the selection, and where that block starts and ends. */
function selectedBlock(state: EditorState): Block {
  const { value, selectionStart, selectionEnd } = normalize(state)
  const start = value.lastIndexOf('\n', selectionStart - 1) + 1
  const newlineAt = value.indexOf('\n', selectionEnd)
  const end = newlineAt === -1 ? value.length : newlineAt
  return { start, end, lines: value.slice(start, end).split('\n') }
}

/**
 * Replaces the touched lines with `transformed`, keeping the caret on the
 * same column of the same line by shifting it by the first line's delta, and
 * clamping back inside the line. Stripping a prefix shortens the line, so both
 * offsets have to be pulled in rather than left pointing past the text.
 */
function applyBlock(state: EditorState, transformed: string[]): EditorState {
  const { value, selectionStart, selectionEnd } = normalize(state)
  const block = selectedBlock(state)
  const next = transformed.join('\n')
  const firstAfter = transformed[0].length
  const delta = firstAfter - block.lines[0].length
  const caret = block.start + clampTo(selectionStart - block.start + delta, 0, firstAfter)
  const selection = clampTo(caret + (selectionEnd - selectionStart), caret, block.start + next.length)
  return {
    value: value.slice(0, block.start) + next + value.slice(block.end),
    selectionStart: caret,
    selectionEnd: selection,
  }
}

/**
 * Adds `prefix` to every touched line, or removes it when all of them carry
 * it already. A blank line stays blank rather than gaining a lone marker.
 */
export function toggleLinePrefix(state: EditorState, prefix: string): EditorState {
  const block = selectedBlock(state)
  const written = block.lines.filter((line) => line.trim().length > 0)
  const strip = written.length > 0 && written.every((line) => line.startsWith(prefix))
  return applyBlock(
    state,
    block.lines.map((line) => {
      if (line.trim().length === 0) return line
      return strip ? line.slice(prefix.length) : prefix + line
    }),
  )
}

const ORDERED_ITEM = /^\s*\d+\.\s/

/** Renumbers the touched lines as an ordered list, or strips the numbering. */
export function toggleOrderedList(state: EditorState): EditorState {
  const block = selectedBlock(state)
  const written = block.lines.filter((line) => line.trim().length > 0)
  const strip = written.length > 0 && written.every((line) => ORDERED_ITEM.test(line))
  let counter = 0
  return applyBlock(
    state,
    block.lines.map((line) => {
      if (line.trim().length === 0) return line
      if (strip) return line.replace(ORDERED_ITEM, '')
      counter += 1
      // Any existing number is replaced rather than prefixed a second time, so
      // a half numbered block is renumbered instead of becoming 1. 1. alpha.
      return `${counter}. ${line.replace(ORDERED_ITEM, '')}`
    }),
  )
}

/**
 * Replaces the selection with `snippet`.
 *
 * `caretOffset` is measured from the start of the inserted text, which is how
 * a caller says to put the caret between the parentheses. Left out, the caret
 * lands after the snippet, which is where typing should continue once the
 * selection it replaced has been overwritten.
 */
export function insertSnippet(state: EditorState, snippet: string, caretOffset?: number): EditorState {
  const { value, selectionStart: start, selectionEnd: end } = normalize(state)
  const offset = caretOffset === undefined ? snippet.length : caretOffset
  const caret = start + clampTo(offset, 0, snippet.length)
  return {
    value: value.slice(0, start) + snippet + value.slice(end),
    selectionStart: caret,
    selectionEnd: caret,
  }
}

/**
 * Inserts a link, keeping any selected text as the label and landing the caret
 * on the URL so it can be typed straight away.
 */
export function insertLink(state: EditorState): EditorState {
  const { value, selectionStart: start, selectionEnd: end } = normalize(state)
  const label = value.slice(start, end) || 'link text'
  return {
    value: value.slice(0, start) + `[${label}](url)` + value.slice(end),
    selectionStart: start + label.length + 3,
    selectionEnd: start + label.length + 3 + 'url'.length,
  }
}

/**
 * Inserts an image, defaulting the alt text to the uploaded file's name so the
 * post still describes itself when the image cannot be loaded.
 */
export function insertImage(state: EditorState, url: string, alt: string): EditorState {
  const label = alt.trim() || 'image'
  const snippet = `![${label}](${url})`
  // The caret lands after the closing parenthesis: an image is a block of its
  // own, so typing should continue below it rather than inside the parens.
  return insertSnippet(state, snippet, snippet.length)
}

/** Newlines needed around a block insert, so it lands on its own lines. */
export function blockPadding(state: EditorState): { before: string; after: string } {
  const { value, selectionStart, selectionEnd } = normalize(state)
  // A leading newline is only needed when the caret is not already sitting at
  // the start of a line; the existing one already separates the block.
  const lineStart = value.lastIndexOf('\n', selectionStart - 1) + 1
  const before = selectionStart === lineStart ? '' : '\n'
  const newlineAt = value.indexOf('\n', selectionEnd)
  const lineEnd = newlineAt === -1 ? value.length : newlineAt
  return { before, after: selectionEnd >= lineEnd ? '' : '\n' }
}

/** A fenced block, with a blank line inside for the author to type into. */
export function codeBlockSnippet(language = ''): string {
  return '```' + language + '\n\n```'
}

/** A small table skeleton, enough structure to fill in. */
export const TABLE_SNIPPET = '| Column 1 | Column 2 |\n| --- | --- |\n| Cell | Cell |'

/** Line formatting the toolbar offers as a toggleable prefix. */
export const LINE_PREFIXES: LinePrefix[] = [
  { label: 'H2', icon: 'mdi-format-header-2', title: 'Heading', prefix: '## ' },
  { label: 'H3', icon: 'mdi-format-header-3', title: 'Subheading', prefix: '### ' },
  { label: 'Quote', icon: 'mdi-format-quote-open', title: 'Quote', prefix: '> ' },
  { label: 'Bullet', icon: 'mdi-format-list-bulleted', title: 'Bullet list', prefix: '- ' },
]

/** Markdown shortcuts, matched against the pressed key. */
export const SHORTCUTS: Record<string, { ctrl: boolean; key: string }> = {
  bold: { ctrl: true, key: 'b' },
  italic: { ctrl: true, key: 'i' },
  code: { ctrl: true, key: 'e' },
  link: { ctrl: true, key: 'k' },
}
