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
      <div class="chat-header">
        <v-badge dot :color="conversation.targetOnline ? 'success' : 'transparent'" class="mr-2">
          <v-avatar size="32">
            <v-img v-if="conversation.targetAvatarUrl" :src="conversation.targetAvatarUrl" alt="" />
            <span v-else>{{ conversation.targetNickname?.charAt(0) ?? '?' }}</span>
          </v-avatar>
        </v-badge>
        <div class="min-width-0">
          <div class="text-truncate font-weight-medium">{{ conversation.targetNickname ?? 'Unknown user' }}</div>
          <!-- The word states the status; the dot only reinforces it. -->
          <div class="muted chat-header__status">
            {{ conversation.targetOnline ? 'Online' : 'Offline' }}
          </div>
        </div>
      </div>

      <div ref="scroller" class="chat-body">
        <div v-if="hasMore" class="d-flex justify-center mb-3">
          <v-btn size="x-small" variant="outlined" :loading="loading" @click="emit('load-older')">
            Load earlier messages
          </v-btn>
        </div>

        <div v-if="!messages.length && !loading" class="state-message">
          <v-icon icon="mdi-message-text-outline" size="28" />
          <span class="state-message__title">No messages yet</span>
          <span>Say something to start the conversation.</span>
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

    <div v-else class="state-message h-100">
      <v-icon icon="mdi-message-text-outline" size="36" />
      <span class="state-message__title">Select a conversation</span>
      <span>Choose a thread on the left to start reading.</span>
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
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  padding: var(--md-sys-space-2) var(--md-sys-space-4);
  border-bottom: 1px solid $border;
}

.chat-header__status {
  font-size: var(--md-sys-typescale-label-sm);
}

.chat-body {
  flex: 1;
  padding: var(--md-sys-space-3) var(--md-sys-space-4);
  /* The transcript scrolls here, so it must be allowed to shrink rather than
     pushing the composer off screen. */
  min-height: 0;
  overflow-y: auto;
  background: $surface-muted;
}

.min-width-0 {
  min-width: 0;
}

@include below($bp-sm) {
  .chat-body {
    padding: var(--md-sys-space-3) var(--md-sys-space-2);
  }
}
</style>
