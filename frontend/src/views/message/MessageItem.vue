<script setup lang="ts">
import { computed, ref } from 'vue'
import dayjs from 'dayjs'

import type { ImMessage } from '@/types'

const props = defineProps<{
  message: ImMessage
  own: boolean
}>()

const emit = defineEmits<{
  (event: 'recall', messageId: string): void
  (event: 'read', messageId: string): void
}>()

const menu = ref(false)

const recalled = computed(() => props.message.status === 'RECALLED')

/** A recall is only offered inside the same two minute window the server allows. */
const canRecall = computed(() => {
  if (!props.own || recalled.value) {
    return false
  }
  const sentAt = dayjs(props.message.sendTime)
  return dayjs().diff(sentAt, 'second') <= 120
})

const time = computed(() => dayjs(props.message.sendTime).format('HH:mm'))

function onRecall() {
  menu.value = false
  emit('recall', props.message.messageId)
}
</script>

<template>
  <div class="d-flex mb-2" :class="own ? 'justify-end' : 'justify-start'">
    <div class="bubble" :class="own ? 'bubble--own' : 'bubble--peer'">
      <div v-if="message.messageType === 'IMAGE' && message.attachments?.length" class="mb-1">
        <v-img
          :src="message.attachments[0].url"
          :alt="message.attachments[0].fileName ?? 'image'"
          max-width="220"
          max-height="220"
          cover
          rounded
        />
      </div>
      <a
        v-else-if="message.messageType === 'FILE' && message.attachments?.length"
        :href="message.attachments[0].url"
        target="_blank"
        rel="noopener"
        class="file-link d-flex align-center"
      >
        <v-icon icon="mdi-paperclip" size="16" class="mr-1" />
        {{ message.attachments[0].fileName ?? 'attachment' }}
      </a>
      <p v-else-if="recalled" class="text--recalled mb-0">
        <v-icon icon="mdi-undo-variant" size="14" class="mr-1" />Message recalled
      </p>
      <p v-else class="mb-0">{{ message.content }}</p>

      <div class="meta d-flex align-center justify-end mt-1">
        <span>{{ time }}</span>
        <v-icon v-if="own && !recalled" icon="mdi-check" size="12" class="ml-1" />
      </div>
    </div>

    <v-menu v-if="canRecall" v-model="menu" location="top end">
      <!--
        Vuetify 3.0.0-beta.0 hands the activator a single `props` object that
        already carries the click, focus and hover handlers. The stable 3.x
        `{ on, attrs }` pair does not exist here, and destructuring `on` would
        leave the button with no click handler at all.
      -->
      <template #activator="{ props: activatorProps }">
        <v-btn icon size="x-small" variant="text" class="ml-1" v-bind="activatorProps">
          <v-icon icon="mdi-dots-vertical" size="16" />
        </v-btn>
      </template>
      <v-list dense>
        <v-list-item @click="onRecall">
          <v-list-item-title>Recall</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<style scoped>
.bubble {
  max-width: 68%;
  padding: 8px 10px;
  border-radius: $radius-md;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.bubble--own {
  background: $accent-soft;
}

.bubble--peer {
  background: $surface;
  border: 1px solid $border;
}

.meta {
  font-size: 11px;
  color: $text-muted;
}

.text--recalled {
  font-style: italic;
  color: $text-muted;
}

.file-link {
  color: $accent;
  text-decoration: none;
}
</style>
