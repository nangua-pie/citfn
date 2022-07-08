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
      placeholder="Write a message. Enter to send, Shift+Enter for a new line."
      @keydown.enter.exact.prevent="send"
    />
    <div class="d-flex align-center mt-2">
      <input ref="imageInput" type="file" accept="image/*" class="d-none" @change="onFilePicked($event, true)" />
      <input ref="fileInput" type="file" class="d-none" @change="onFilePicked($event, false)" />
      <v-btn icon size="small" variant="text" :loading="uploading" title="Attach an image" @click="imageInput?.click()">
        <v-icon icon="mdi-image-outline" size="18" />
      </v-btn>
      <v-btn icon size="small" variant="text" :loading="uploading" title="Attach a file" @click="fileInput?.click()">
        <v-icon icon="mdi-paperclip" size="18" />
      </v-btn>
      <v-spacer />
      <span class="counter mr-3">{{ draft.length }}/{{ MAX_LENGTH }}</span>
      <v-btn color="primary" size="small" :disabled="!draft.trim()" @click="send">Send</v-btn>
    </div>
    <p v-if="error" class="error mt-1 mb-0">{{ error }}</p>
  </div>
</template>

<style scoped>
.composer {
  padding: 12px;
  border-top: 1px solid $border;
  background: $surface;
}

.counter {
  font-size: 11px;
  color: $text-muted;
}

.error {
  font-size: 12px;
  color: $danger;
}
</style>
