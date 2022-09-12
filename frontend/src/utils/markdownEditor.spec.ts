import { describe, expect, it } from 'vitest'

import {
  blockPadding,
  codeBlockSnippet,
  insertImage,
  insertLink,
  insertSnippet,
  toggleLinePrefix,
  toggleOrderedList,
  toggleWrap,
} from '@/utils/markdownEditor'
import type { EditorState } from '@/utils/markdownEditor'

/** Builds a state whose selection spans the given offsets. */
function state(value: string, start: number, end = start): EditorState {
  return { value, selectionStart: start, selectionEnd: end }
}

describe('toggleWrap', () => {
  it('wraps the selection and keeps it selected', () => {
    const result = toggleWrap(state('hello world', 0, 5), '**')
    expect(result.value).toBe('**hello** world')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('hello')
  })

  it('unwraps a selection that is already wrapped', () => {
    const result = toggleWrap(state('**hello**', 0, 9), '**')
    expect(result.value).toBe('hello')
    expect(result.selectionStart).toBe(0)
    expect(result.selectionEnd).toBe(5)
  })

  it('drops a bare caret between the markers', () => {
    const result = toggleWrap(state('', 0), '**')
    expect(result.value).toBe('****')
    expect(result.selectionStart).toBe(2)
    expect(result.selectionEnd).toBe(2)
  })

  it('leaves a half wrapped selection alone rather than stripping one marker', () => {
    const result = toggleWrap(state('*hello*', 0, 7), '**')
    expect(result.value).toBe('***hello***')
  })

  it('treats a reversed selection as its own mirror', () => {
    const result = toggleWrap(state('hello world', 5, 0), '**')
    expect(result.value).toBe('**hello** world')
  })
})

describe('toggleLinePrefix', () => {
  it('prefixes the line the caret is on', () => {
    const result = toggleLinePrefix(state('hello world', 6), '## ')
    expect(result.value).toBe('## hello world')
    // Column six of the line, pushed along by the three characters it gained.
    expect(result.selectionStart).toBe(9)
  })

  it('strips the prefix again when every touched line has it', () => {
    const result = toggleLinePrefix(state('## hello world', 6), '## ')
    expect(result.value).toBe('hello world')
    expect(result.selectionStart).toBeGreaterThanOrEqual(0)
    expect(result.selectionEnd).toBeLessThanOrEqual(result.value.length)
  })

  it('prefixes every line the selection touches', () => {
    const result = toggleLinePrefix(state('one\ntwo\nthree', 0, 13), '- ')
    expect(result.value).toBe('- one\n- two\n- three')
  })

  it('leaves a blank line blank', () => {
    const result = toggleLinePrefix(state('one\n\ntwo', 0, 8), '- ')
    expect(result.value).toBe('- one\n\n- two')
  })
})

describe('toggleOrderedList', () => {
  it('numbers the lines from one', () => {
    const result = toggleOrderedList(state('alpha\nbeta\ngamma', 0, 16))
    expect(result.value).toBe('1. alpha\n2. beta\n3. gamma')
  })

  it('numbers every line when only some of them were already numbered', () => {
    // The button toggles between "not a list" and "a list", so a block that is
    // only partly numbered is renumbered rather than cleared or doubled.
    const result = toggleOrderedList(state('1. alpha\nbeta', 0, 13))
    expect(result.value).toBe('1. alpha\n2. beta')
  })

  it('strips the numbering on a second press', () => {
    const numbered = toggleOrderedList(state('alpha\nbeta', 0, 10)).value
    const result = toggleOrderedList(state(numbered, 0, numbered.length))
    expect(result.value).toBe('alpha\nbeta')
  })
})

describe('insertSnippet', () => {
  it('replaces the selection', () => {
    const result = insertSnippet(state('hello world', 0, 5), 'goodbye')
    expect(result.value).toBe('goodbye world')
    expect(result.selectionStart).toBe(7)
  })

  it('puts the caret where the caller asked', () => {
    const result = insertSnippet(state('', 0), 'a(b)c', 2)
    expect(result.value).toBe('a(b)c')
    expect(result.selectionStart).toBe(2)
  })
})

describe('insertLink', () => {
  it('keeps the selected text as the label and selects the url', () => {
    const result = insertLink(state('see docs here', 4, 8))
    expect(result.value).toBe('see [docs](url) here')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('url')
  })

  it('offers a placeholder label when nothing is selected', () => {
    const result = insertLink(state('', 0))
    expect(result.value).toBe('[link text](url)')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('url')
  })
})

describe('insertImage', () => {
  it('writes image markdown and uses the file name as alt text', () => {
    const result = insertImage(state('', 0), 'http://host/a.png', 'diagram.png')
    expect(result.value).toBe('![diagram.png](http://host/a.png)')
  })

  it('falls back to a generic label when there is no file name', () => {
    expect(insertImage(state('', 0), 'http://host/a.png', '  ').value).toBe('![image](http://host/a.png)')
  })

  it('drops the caret after the inserted image so typing continues below it', () => {
    const result = insertImage(state('', 0), 'http://host/a.png', 'a.png')
    expect(result.selectionStart).toBe(result.value.length)
  })
})

describe('block helpers', () => {
  it('adds no leading newline when the caret already starts a line', () => {
    expect(blockPadding(state('one\ntwo', 4))).toEqual({ before: '', after: '\n' })
  })

  it('wraps the block in newlines when the caret is in the middle of a line', () => {
    expect(blockPadding(state('before', 3))).toEqual({ before: '\n', after: '\n' })
  })

  it('adds only a leading newline at the end of the document', () => {
    expect(blockPadding(state('one\ntwo', 7))).toEqual({ before: '\n', after: '' })
  })

  it('builds a fenced block with a blank line inside', () => {
    expect(codeBlockSnippet()).toBe('```\n\n```')
    expect(codeBlockSnippet('js')).toBe('```js\n\n```')
  })
})
