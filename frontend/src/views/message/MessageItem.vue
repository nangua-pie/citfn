<script setup lang="ts">
import { computed, ref } from 'vue'
import dayjs from 'dayjs'

import { machineDate } from '@/utils/date'
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
const machineTime = computed(() => machineDate(props.message.sendTime))

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

      <!--
        The tick alone is ambiguous, so the timestamp is what a screen reader
        announces; aria-hidden keeps the decorative tick out of the way.
      -->
      <div class="meta d-flex align-center justify-end mt-1">
        <time :datetime="machineTime">{{ time }}</time>
        <v-icon v-if="own && !recalled" icon="mdi-check" size="12" class="ml-1" aria-hidden="true" />
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
        <v-btn
          icon
          size="x-small"
          variant="text"
          class="ml-1"
          v-bind="activatorProps"
          :aria-label="`Options for the message sent at ${time}`"
        >
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
  /* A bubble never takes more than this much of the row, so a long message
     wraps instead of stretching the transcript. */
  max-width: min(68%, 520px);
  padding: var(--md-sys-space-2) var(--md-sys-space-3);
  border-radius: $radius-md;
  font-size: var(--md-sys-typescale-body-md);
  line-height: 1.55;
  @include overflow-wrap-anywhere;
}

.bubble--own {
  background: $accent-soft;
  color: var(--md-sys-color-on-primary-container);
}

.bubble--peer {
  background: $surface;
  border: 1px solid $border;
}

.meta {
  font-size: var(--md-sys-typescale-label-xs);
  color: $text-muted;
}

.text--recalled {
  font-style: italic;
  color: $text-muted;
}

.file-link {
  color: $accent;
  text-decoration: none;
  /* A long attachment name must not widen the bubble. */
  @include overflow-wrap-anywhere;
}
</style>
