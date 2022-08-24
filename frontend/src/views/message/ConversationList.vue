<script setup lang="ts">
import dayjs from 'dayjs'

import type { Conversation } from '@/types'

defineProps<{
  conversations: Conversation[]
  activeId: string | null
}>()

const emit = defineEmits<{
  (event: 'select', conversationId: string): void
  (event: 'remove', conversationId: string): void
}>()

function preview(conversation: Conversation): string {
  return conversation.lastMessagePreview ?? 'No messages yet'
}

function when(conversation: Conversation): string {
  return conversation.lastMessageTime ? dayjs(conversation.lastMessageTime).format('MMM D') : ''
}

/**
 * One spoken sentence per row. The visual row leans on truncation, a colour
 * dot and a bare number, none of which survive a screen reader.
 */
function rowLabel(conversation: Conversation): string {
  const name = conversation.targetNickname ?? 'Unknown user'
  const unread = conversation.unreadCount > 0 ? `, ${conversation.unreadCount} unread` : ''
  return `${name}, ${conversation.targetOnline ? 'online' : 'offline'}. ${preview(conversation)}${unread}`
}
</script>

<template>
  <div class="conversation-list">
    <div v-if="!conversations.length" class="state-message">
      <v-icon icon="mdi-message-text-outline" size="28" />
      <span class="state-message__title">No conversations yet</span>
      <p class="muted mb-0">Open a user profile to start one.</p>
    </div>

    <div
      v-for="conversation in conversations"
      :key="conversation.id"
      class="row"
      :class="{ 'row--active': conversation.id === activeId }"
      role="button"
      tabindex="0"
      :aria-current="conversation.id === activeId ? 'true' : undefined"
      :aria-label="rowLabel(conversation)"
      @click="emit('select', conversation.id)"
      @keydown.enter="emit('select', conversation.id)"
      @keydown.space.prevent="emit('select', conversation.id)"
    >
      <v-badge dot :color="conversation.targetOnline ? 'success' : 'transparent'" class="presence mr-1">
        <v-avatar size="36">
          <v-img v-if="conversation.targetAvatarUrl" :src="conversation.targetAvatarUrl" alt="" />
          <span v-else>{{ conversation.targetNickname?.charAt(0) ?? '?' }}</span>
        </v-avatar>
      </v-badge>

      <div class="flex-grow-1 min-width-0">
        <div class="d-flex align-center">
          <span class="name text-truncate">{{ conversation.targetNickname ?? 'Unknown user' }}</span>
          <v-spacer />
          <span class="when muted">{{ when(conversation) }}</span>
        </div>
        <div class="d-flex align-center mt-1">
          <span class="preview text-truncate muted">{{ preview(conversation) }}</span>
          <v-spacer />
          <v-badge
            v-if="conversation.unreadCount > 0"
            :content="conversation.unreadCount"
            color="error"
            inline
          />
        </div>
      </div>

      <v-btn
        icon
        size="x-small"
        variant="text"
        title="Delete conversation"
        :aria-label="`Delete the conversation with ${conversation.targetNickname ?? 'this user'}`"
        @click.stop="emit('remove', conversation.id)"
      >
        <v-icon icon="mdi-close" size="16" />
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.conversation-list {
  overflow-y: auto;
  height: 100%;
}

.row {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  padding: var(--md-sys-space-2) var(--md-sys-space-3);
  cursor: pointer;
  border-bottom: 1px solid $border;
  transition: background-color var(--md-sys-motion-short) var(--md-sys-motion-easing);
}

.row:hover {
  background: $surface-muted;
}

/* Selection is a filled container plus a heavier name, so it does not rely on
   colour alone. */
.row--active {
  background: $accent-soft;
}

.row--active .name {
  font-weight: 600;
}

.name {
  font-size: var(--md-sys-typescale-body-md);
  font-weight: 500;
}

.preview {
  max-width: 180px;
  font-size: var(--md-sys-typescale-label-sm);
}

.when {
  flex: none;
  margin-left: var(--md-sys-space-2);
  font-size: var(--md-sys-typescale-label-xs);
}

.row .presence {
  flex: none;
}

.min-width-0 {
  min-width: 0;
}
</style>
