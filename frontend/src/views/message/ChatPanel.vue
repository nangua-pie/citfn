<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

import MessageComposer from './MessageComposer.vue'
import MessageItem from './MessageItem.vue'
import type { Conversation, ImMessage } from '@/types'

const props = defineProps<{
  conversation: Conversation | null
  messages: ImMessage[]
  currentUserId: string | null
  loading: boolean
  hasMore: boolean
}>()

const emit = defineEmits<{
  (event: 'send', content: string): void
  (event: 'recall', messageId: string): void
  (event: 'read', messageId: string): void
  (event: 'load-older'): void
}>()

const scroller = ref<HTMLElement | null>(null)

/** Keeps the newest message in view as the transcript grows. */
async function scrollToBottom() {
  await nextTick()
  if (scroller.value) {
    scroller.value.scrollTop = scroller.value.scrollHeight
  }
}

watch(
  () => props.messages.length,
  (count, previous) => {
    // Only auto-scroll when a message was appended, not when older history was
 // prepended, which would yank the reader away from what they were reading.
    if (count > previous && props.messages.length > 0) {
      void scrollToBottom()
    }
  },
)
</script>

<template>
  <div class="chat-panel">
    <template v-if="conversation">
      <div class="chat-header d-flex align-center px-4 py-2">
        <v-badge dot :color="conversation.targetOnline ? 'success' : 'transparent'" class="mr-2">
          <v-avatar size="32">
            <v-img v-if="conversation.targetAvatarUrl" :src="conversation.targetAvatarUrl" />
            <span v-else>{{ conversation.targetNickname?.charAt(0) ?? '?' }}</span>
          </v-avatar>
        </v-badge>
        <div class="min-width-0">
          <div class="text-truncate font-weight-medium">{{ conversation.targetNickname ?? 'Unknown user' }}</div>
          <div class="muted text-caption">{{ conversation.targetOnline ? 'Online' : 'Offline' }}</div>
        </div>
      </div>

      <div ref="scroller" class="chat-body px-4 py-3">
        <div v-if="hasMore" class="d-flex justify-center mb-3">
          <v-btn size="x-small" variant="text" :loading="loading" @click="emit('load-older')">Load earlier</v-btn>
        </div>

        <div v-if="!messages.length && !loading" class="empty text-center muted py-8">
          No messages yet. Say something.
        </div>

        <MessageItem
          v-for="message in messages"
          :key="message.messageId"
          :message="message"
          :own="message.senderId === currentUserId"
          @recall="emit('recall', $event)"
          @read="emit('read', $event)"
        />

        <div v-if="loading && !messages.length" class="text-center py-4">
          <v-progress-circular indeterminate size="20" />
        </div>
      </div>

      <MessageComposer @send="emit('send', $event)" />
    </template>

    <div v-else class="empty d-flex align-center justify-content-center h-100">
      <div class="text-center">
        <v-icon icon="mdi-message-text-outline" size="36" class="muted" />
        <p class="muted mt-2 mb-0">Select a conversation to start reading</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: $surface;
}

.chat-header {
  border-bottom: 1px solid $border;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  background: $surface-muted;
}

.empty {
  color: $text-muted;
  font-size: 13px;
}

.min-width-0 {
  min-width: 0;
}
</style>
