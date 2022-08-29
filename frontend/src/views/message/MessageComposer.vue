<script setup lang="ts">
import { ref } from 'vue'

import { uploadFile, uploadImage } from '@/api/file'
import type { UploadedFile } from '@/types'

const emit = defineEmits<{
  (event: 'send', content: string): void
}>()

const draft = ref('')
const sending = ref(false)
const uploading = ref(false)
const error = ref('')
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

/** Matches the server side cap so the error surfaces before a round trip. */
const MAX_LENGTH = 2000

function send() {
  const content = draft.value.trim()
  if (!content || sending.value) {
    return
  }
  sending.value = true
  error.value = ''
  emit('send', content)
  draft.value = ''
  sending.value = false
}

async function onFilePicked(event: Event, isImage: boolean) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  uploading.value = true
  error.value = ''
  try {
    const uploaded: UploadedFile = isImage ? await uploadImage(file) : await uploadFile(file)
    emit('send', isImage ? `[image] ${uploaded.originalName}` : `[file] ${uploaded.originalName}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'the upload failed'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="composer">
    <v-textarea
      v-model="draft"
      rows="2"
      auto-grow
      hide-details
      :maxlength="MAX_LENGTH"
      aria-label="Write a message"
      placeholder="Write a message. Enter to send, Shift+Enter for a new line."
      @keydown.enter.exact.prevent="send"
    />
    <div class="composer-actions">
      <input ref="imageInput" type="file" accept="image/*" class="d-none" @change="onFilePicked($event, true)" />
      <input ref="fileInput" type="file" class="d-none" @change="onFilePicked($event, false)" />
      <v-btn
        icon
        size="small"
        variant="text"
        :loading="uploading"
        title="Attach an image"
        aria-label="Attach an image"
        @click="imageInput?.click()"
      >
        <v-icon icon="mdi-image-outline" size="18" />
      </v-btn>
      <v-btn
        icon
        size="small"
        variant="text"
        :loading="uploading"
        title="Attach a file"
        aria-label="Attach a file"
        @click="fileInput?.click()"
      >
        <v-icon icon="mdi-paperclip" size="18" />
      </v-btn>
      <v-spacer />
      <span class="counter">{{ draft.length }}/{{ MAX_LENGTH }}</span>
      <v-btn
        color="primary"
        size="small"
        :disabled="!draft.trim() || sending"
        :loading="sending"
        @click="send"
      >
        Send
      </v-btn>
    </div>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped lang="scss">
.composer {
  padding: var(--md-sys-space-3);
  border-top: 1px solid $border;
  background: $surface;
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-1);
  margin-top: var(--md-sys-space-2);
}

.counter {
  margin-right: var(--md-sys-space-2);
  font-size: var(--md-sys-typescale-label-xs);
  color: $text-muted;
  font-variant-numeric: tabular-nums;
}

.error {
  margin: var(--md-sys-space-1) 0 0;
  font-size: var(--md-sys-typescale-label-sm);
  color: $danger;
}
</style>
