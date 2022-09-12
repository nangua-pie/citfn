import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'

import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import { uploadImage } from '@/api/file'
import { mountWithApp } from '@/test/mount'

vi.mock('@/api/file', () => ({ uploadImage: vi.fn() }))

/**
 * The editor is a controlled component, so the test drives it through a host
 * that owns the text. Typing really does update the prop, which is what a
 * toolbar action then reads.
 */
const Harness = defineComponent({
  components: { MarkdownEditor },
  setup() {
    return { text: ref('') }
  },
  template: '<MarkdownEditor v-model="text" />',
})

type Mounted = Awaited<ReturnType<typeof render>>

async function render(initial = '') {
  const wrapper = mountWithApp(Harness, { attachTo: document.body })
  await flushPromises()
  if (initial) {
    await typeInto(wrapper, initial)
  }
  return wrapper
}

/** Types into the textarea so the host's ref, and so the prop, really change. */
async function typeInto(wrapper: Mounted, value: string, start?: number, end?: number) {
  const field = wrapper.find('textarea')
  await field.setValue(value)
  await flushPromises()
  if (start !== undefined) {
    const el = wrapper.find('textarea').element as HTMLTextAreaElement
    el.setSelectionRange(start, end ?? start)
  }
  return wrapper
}

function content(wrapper: Mounted): string {
  return String((wrapper.vm as unknown as { text: string }).text)
}

function clickControl(wrapper: Mounted, label: string) {
  return wrapper.find(`[aria-label="${label}"]`).trigger('click')
}

async function chooseFile(wrapper: Mounted, file: File) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await flushPromises()
}

function png(name = 'diagram.png', size = 32) {
  return new File([new Uint8Array(size)], name, { type: 'image/png' })
}

describe('MarkdownEditor formatting', () => {
  it('wraps the selection in bold markers', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'hello world', 0, 5)

    await clickControl(wrapper, 'Bold')

    expect(content(wrapper)).toBe('**hello** world')
  })

  it('prefixes the caret line with a quote marker', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'hello', 3)

    await clickControl(wrapper, 'Quote')

    expect(content(wrapper)).toBe('> hello')
  })

  it('numbers the selected lines', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'one\ntwo', 0, 7)

    await clickControl(wrapper, 'Numbered list')

    expect(content(wrapper)).toBe('1. one\n2. two')
  })

  it('inserts a fenced code block on its own lines', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'before', 6)

    await clickControl(wrapper, 'Insert code block')

    expect(content(wrapper)).toBe('before\n```\n\n```')
  })

  it('inserts a table skeleton', async () => {
    const wrapper = await render()

    await clickControl(wrapper, 'Insert table')

    expect(content(wrapper)).toContain('| Column 1 | Column 2 |')
  })

  it('uses the selected text as the link label', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'read the docs', 5, 13)

    await clickControl(wrapper, 'Insert link')

    expect(content(wrapper)).toBe('read [the docs](url)')
  })

  it('applies the bold shortcut rather than letting the browser act on it', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'hello', 0, 5)

    await wrapper.find('textarea').trigger('keydown', { key: 'b', ctrlKey: true })
    await flushPromises()

    expect(content(wrapper)).toBe('**hello**')
  })

  it('indents with Tab instead of moving focus out of the editor', async () => {
    const wrapper = await render()
    await typeInto(wrapper, 'hello', 5)

    await wrapper.find('textarea').trigger('keydown', { key: 'Tab' })
    await flushPromises()

    expect(content(wrapper)).toBe('hello  ')
  })
})

describe('MarkdownEditor images', () => {
  beforeEach(() => {
    vi.mocked(uploadImage).mockReset()
  })

  it('embeds an uploaded image as markdown and uses the file name as alt text', async () => {
    vi.mocked(uploadImage).mockResolvedValue({
      id: '9',
      originalName: 'diagram.png',
      url: '/files/diagram.png',
      contentType: 'image/png',
      size: 32,
      createTime: '2022-08-31 10:00:00',
    })
    const wrapper = await render()

    await chooseFile(wrapper, png())

    expect(uploadImage).toHaveBeenCalledTimes(1)
    expect(content(wrapper)).toBe('![diagram.png](/files/diagram.png)')
    wrapper.unmount()
  })

  it('refuses a file the server would refuse, without uploading it', async () => {
    const wrapper = await render()

    await chooseFile(wrapper, new File(['x'], 'notes.pdf', { type: 'application/pdf' }))

    expect(uploadImage).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Only JPEG, PNG, GIF and WebP images can be embedded.')
    wrapper.unmount()
  })

  it('refuses an image over the size limit before spending the upload', async () => {
    const wrapper = await render()

    await chooseFile(wrapper, png('huge.png', 6 * 1024 * 1024))

    expect(uploadImage).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Images must be 5 MB or smaller.')
    wrapper.unmount()
  })

  it('surfaces a rejected upload instead of failing silently', async () => {
    vi.mocked(uploadImage).mockRejectedValue(new Error('the file is not allowed'))
    const wrapper = await render()

    await chooseFile(wrapper, png())

    expect(wrapper.text()).toContain('the file is not allowed')
    wrapper.unmount()
  })

  it('embeds an image pasted from the clipboard', async () => {
    vi.mocked(uploadImage).mockResolvedValue({
      id: '9',
      originalName: 'pasted.png',
      url: '/files/pasted.png',
      contentType: 'image/png',
      size: 32,
      createTime: '2022-08-31 10:00:00',
    })
    const wrapper = await render()
    const clipboardData = { files: [png('pasted.png')] }

    await wrapper.find('textarea').trigger('paste', { clipboardData })
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledTimes(1)
    expect(content(wrapper)).toBe('![pasted.png](/files/pasted.png)')
    wrapper.unmount()
  })

  it('leaves a pasted block of text alone', async () => {
    const wrapper = await render()

    await wrapper.find('textarea').trigger('paste', { clipboardData: { files: [] } })
    await flushPromises()

    expect(uploadImage).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
