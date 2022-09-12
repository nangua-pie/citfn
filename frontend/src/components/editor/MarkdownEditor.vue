<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'

import { uploadImage } from '@/api/file'
import {
  LINE_PREFIXES,
  TABLE_SNIPPET,
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

const props = withDefaults(
  defineProps<{
    modelValue: string
    label?: string
    placeholder?: string
    rows?: number
  }>(),
  { label: 'Body (Markdown)', placeholder: '', rows: 16 },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const field = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadError = ref('')
const dragging = ref(false)

/**
 * Mirrors the server's own rules so a rejected file fails immediately instead
 * of after the upload round trip. See FileServiceImpl.
 */
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const MAX_IMAGE_BYTES = 5 * 1024 * 1024

const hasContent = computed(() => props.modelValue.trim().length > 0)

function textareaEl(): HTMLTextAreaElement | null {
  return field.value?.querySelector('textarea') ?? null
}

/**
 * Reads the live selection out of the textarea rather than a stale ref, so a
 * click on the toolbar does not act on where the caret used to be.
 */
function currentState(): EditorState {
  const el = textareaEl()
  return {
    value: props.modelValue,
    selectionStart: el ? el.selectionStart : props.modelValue.length,
    selectionEnd: el ? el.selectionEnd : props.modelValue.length,
  }
}

/**
 * Writes a transformation back and puts the caret where the helper asked.
 *
 * The textarea is patched by Vue on the same flush, so the selection can only
 * be restored once that has happened.
 */
function apply(next: EditorState) {
  emit('update:modelValue', next.value)
  const wanted = { start: next.selectionStart, end: next.selectionEnd }
  void nextTick(() => {
    const el = textareaEl()
    if (!el) return
    el.focus()
    el.setSelectionRange(wanted.start, wanted.end)
  })
}

function wrap(marker: string) {
  apply(toggleWrap(currentState(), marker))
}

function prefix(prefixText: string) {
  apply(toggleLinePrefix(currentState(), prefixText))
}

/** Inserts a block on its own lines, with the caret on the first blank spot. */
function insertBlock(snippet: string, caretOffset: number) {
  const pad = blockPadding(currentState())
  const padded = pad.before + snippet + pad.after
  apply(insertSnippet(currentState(), padded, pad.before.length + caretOffset))
}

function insertTable() {
  insertBlock(TABLE_SNIPPET, 2)
}

function insertCodeBlock() {
  // The caret lands on the blank line between the fences.
  insertBlock(codeBlockSnippet(), 4)
}

function openFilePicker() {
  uploadError.value = ''
  fileInput.value?.click()
}

function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) {
    void uploadImageFile(file)
  }
}

function validateImage(file: File): string {
  if (!IMAGE_TYPES.includes(file.type)) {
    return 'Only JPEG, PNG, GIF and WebP images can be embedded.'
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return 'Images must be 5 MB or smaller.'
  }
  return ''
}

async function uploadImageFile(file: File) {
  const problem = validateImage(file)
  if (problem) {
    uploadError.value = problem
    return
  }
  uploading.value = true
  uploadError.value = ''
  try {
    const uploaded = await uploadImage(file)
    apply(insertImage(currentState(), uploaded.url, uploaded.originalName || file.name))
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'the image could not be uploaded'
  } finally {
    uploading.value = false
  }
}

/** An image pasted from the clipboard is inserted the same way as a picked one. */
function onPaste(event: ClipboardEvent) {
  const file = Array.from(event.clipboardData?.files ?? [])[0]
  if (!file || !file.type.startsWith('image/')) {
    return
  }
  event.preventDefault()
  void uploadImageFile(file)
}

function onDrop(event: DragEvent) {
  dragging.value = false
  const file = Array.from(event.dataTransfer?.files ?? [])[0]
  if (!file || !file.type.startsWith('image/')) {
    return
  }
  event.preventDefault()
  void uploadImageFile(file)
}

function onKeydown(event: KeyboardEvent) {
  // Tab indents, which is what a list needs; the textarea keeps focus so the
  // caret does not jump out of the editor mid sentence.
  if (event.key === 'Tab' && !event.shiftKey && !event.ctrlKey && !event.metaKey) {
    event.preventDefault()
    apply(insertSnippet(currentState(), '  '))
    return
  }
  if (!event.ctrlKey && !event.metaKey) {
    return
  }
  const shortcuts: Record<string, () => void> = {
    b: () => wrap('**'),
    i: () => wrap('*'),
    e: () => wrap('`'),
    k: () => apply(insertLink(currentState())),
  }
  const action = shortcuts[event.key.toLowerCase()]
  if (action) {
    event.preventDefault()
    action()
  }
}
</script>

<template>
  <div class="markdown-editor" :class="{ 'markdown-editor--dropping': dragging }">
    <div class="markdown-editor__toolbar" role="toolbar" aria-label="Formatting">
      <div class="markdown-editor__group">
        <v-btn icon size="small" variant="text" title="Bold (Ctrl+B)" aria-label="Bold" @click="wrap('**')">
          <v-icon icon="mdi-format-bold" size="18" />
        </v-btn>
        <v-btn icon size="small" variant="text" title="Italic (Ctrl+I)" aria-label="Italic" @click="wrap('*')">
          <v-icon icon="mdi-format-italic" size="18" />
        </v-btn>
        <v-btn
          icon
          size="small"
          variant="text"
          title="Strikethrough"
          aria-label="Strikethrough"
          @click="wrap('~~')"
        >
          <v-icon icon="mdi-format-strikethrough-variant" size="18" />
        </v-btn>
        <v-btn
          icon
          size="small"
          variant="text"
          title="Inline code (Ctrl+E)"
          aria-label="Inline code"
          @click="wrap('`')"
        >
          <v-icon icon="mdi-code-tags" size="18" />
        </v-btn>
      </div>

      <div class="markdown-editor__group">
        <v-btn
          v-for="item in LINE_PREFIXES"
          :key="item.prefix"
          icon
          size="small"
          variant="text"
          :title="item.title"
          :aria-label="item.title"
          @click="prefix(item.prefix)"
        >
          <v-icon :icon="item.icon" size="18" />
        </v-btn>
        <v-btn
          icon
          size="small"
          variant="text"
          title="Numbered list"
          aria-label="Numbered list"
          @click="apply(toggleOrderedList(currentState()))"
        >
          <v-icon icon="mdi-format-list-numbered" size="18" />
        </v-btn>
      </div>

      <div class="markdown-editor__group">
        <v-btn
          icon
          size="small"
          variant="text"
          title="Link (Ctrl+K)"
          aria-label="Insert link"
          @click="apply(insertLink(currentState()))"
        >
          <v-icon icon="mdi-link-variant" size="18" />
        </v-btn>
        <v-btn
          icon
          size="small"
          variant="text"
          :loading="uploading"
          title="Insert image"
          aria-label="Insert image"
          @click="openFilePicker"
        >
          <v-icon icon="mdi-image-plus" size="18" />
        </v-btn>
        <v-btn
          icon
          size="small"
          variant="text"
          title="Code block"
          aria-label="Insert code block"
          @click="insertCodeBlock"
        >
          <v-icon icon="mdi-code-braces" size="18" />
        </v-btn>
        <v-btn
          icon
          size="small"
          variant="text"
          title="Table"
          aria-label="Insert table"
          @click="insertTable"
        >
          <v-icon icon="mdi-table" size="18" />
        </v-btn>
      </div>

      <span class="markdown-editor__hint">Markdown supported</span>
    </div>

    <div ref="field" class="markdown-editor__field">
      <v-textarea
        :model-value="modelValue"
        :label="label"
        :placeholder="placeholder"
        :rows="rows"
        hide-details
        @update:model-value="emit('update:modelValue', $event)"
        @keydown="onKeydown"
        @paste="onPaste"
        @drop="onDrop"
        @dragover.prevent="dragging = true"
        @dragleave="dragging = false"
      />
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/png,image/jpeg,image/gif,image/webp"
      class="markdown-editor__file"
      @change="onFileChosen"
    />

    <p v-if="uploadError" class="markdown-editor__error" role="alert">{{ uploadError }}</p>
    <p v-else-if="dragging" class="markdown-editor__drop-hint">Drop the image to embed it</p>
    <p v-else-if="!hasContent" class="markdown-editor__empty">Nothing written yet.</p>
  </div>
</template>

<style scoped lang="scss">
.markdown-editor {
  border: 1px solid $border;
  border-radius: $radius-md;
  background: $surface;
  overflow: hidden;
}

.markdown-editor--dropping {
  border-color: $accent;
}

/* A sticky row of controls, split into groups so twelve buttons read as a
   shape rather than a queue. */
.markdown-editor__toolbar {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  flex-wrap: wrap;
  padding: var(--md-sys-space-2) var(--md-sys-space-3);
  border-bottom: 1px solid $border;
  background: $surface-muted;
}

.markdown-editor__group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.markdown-editor__group + .markdown-editor__group {
  padding-left: var(--md-sys-space-2);
  border-left: 1px solid $border;
}

.markdown-editor__hint {
  margin-left: auto;
  font-size: var(--md-sys-typescale-label-sm);
  color: $text-muted;
}

/* The textarea is the drop target, so the field reaches the card edges. */
.markdown-editor__field :deep(.v-field) {
  border-radius: 0;
}

.markdown-editor__file {
  display: none;
}

.markdown-editor__error {
  margin: 0;
  padding: var(--md-sys-space-2) var(--md-sys-space-4);
  font-size: var(--md-sys-typescale-body-sm);
  color: $danger;
}

.markdown-editor__drop-hint,
.markdown-editor__empty {
  margin: 0;
  padding: var(--md-sys-space-2) var(--md-sys-space-4);
  font-size: var(--md-sys-typescale-body-sm);
  color: $text-muted;
}

@include below($bp-sm) {
  .markdown-editor__hint {
    display: none;
  }
}
</style>
