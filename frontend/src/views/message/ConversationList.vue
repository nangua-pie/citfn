<script setup lang="ts">
import dayjs from 'dayjs'

import type { Conversation } from '@/types'

defineProps<{
  conversations: Conversation[]
  activeId: number | null
}>()

const emit = defineEmits<{
  (event: 'select', conversationId: number): void
  (event: 'remove', conversationId: number): void
}>()

function preview(conversation: Conversation): string {
  return conversation.lastMessagePreview ?? 'No messages yet'
}

function when(conversation: Conversation): string {
  return conversation.lastMessageTime ? dayjs(conversation.lastMessageTime).format('MMM D') : ''
}
</script>

<template>
  <div class="conversation-list">
    <div v-if="!conversations.length" class="empty pa-4 text-center">
      <p class="mb-0">No conversations yet.</p>
      <p class="muted mb-0">Open a user profile to start one.</p>
    </div>

    <div
      v-for="conversation in conversations"
      :key="conversation.id"
      class="row"
      :class="{ 'row--active': conversation.id === activeId }"
      role="button"
      tabindex="0"
      @click="emit('select', conversation.id)"
      @keydown.enter="emit('select', conversation.id)"
    >
      <v-badge dot :color="conversation.targetOnline ? 'success' : 'transparent'" class="presence mr-1">
        <v-avatar size="36">
          <v-img v-if="conversation.targetAvatarUrl" :src="conversation.targetAvatarUrl" />
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
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid $border;
}

.row:hover {
  background: $surface-muted;
}

.row--active {
  background: $accent-soft;
}

.name {
  font-size: 14px;
  font-weight: 500;
}

.preview {
  font-size: 12px;
  max-width: 180px;
}

.when {
  font-size: 11px;
}

.empty {
  color: $text-muted;
  font-size: 13px;
}

.min-width-0 {
  min-width: 0;
}
</style>
