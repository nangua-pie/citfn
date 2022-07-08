<script setup lang="ts">
import { onMounted } from 'vue'

import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/message'
import ChatPanel from './ChatPanel.vue'
import ConversationList from './ConversationList.vue'

const auth = useAuthStore()
const messages = useMessageStore()

function onSend(content: string) {
  const conversation = messages.activeConversation
  if (!conversation) {
    return
  }
  void messages.send(conversation.targetUserId, content)
}

function onRemove(conversationId: number) {
  void messages.removeConversation(conversationId)
}

onMounted(() => {
  void messages.loadConversations()
  void messages.refreshUnread()
})
</script>

<template>
  <v-container class="page-container">
    <div class="message-layout">
      <aside class="sidebar">
        <div class="sidebar-header d-flex align-center px-3 py-2">
          <span class="page-title">Messages</span>
          <v-spacer />
          <v-btn icon size="x-small" variant="text" title="Reload" @click="messages.loadConversations()">
            <v-icon icon="mdi-refresh" size="18" />
          </v-btn>
        </div>
        <ConversationList
          :conversations="messages.conversations"
          :active-id="messages.activeConversationId"
          @select="messages.selectConversation($event)"
          @remove="onRemove"
        />
      </aside>

      <section class="panel">
        <ChatPanel
          :conversation="messages.activeConversation"
          :messages="messages.messages"
          :current-user-id="auth.profile?.id ?? null"
          :loading="messages.loading"
          :has-more="messages.hasMore"
          @send="onSend"
          @recall="messages.recall($event)"
          @read="messages.markRead($event)"
          @load-older="messages.loadOlderMessages()"
        />
      </section>
    </div>
  </v-container>
</template>

<style scoped>
.message-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  height: calc(100vh - 56px - 48px);
  border: 1px solid $border;
  border-radius: $radius-md;
  overflow: hidden;
  background: $surface;
}

.sidebar {
  border-right: 1px solid $border;
  min-height: 0;
}

.sidebar-header {
  border-bottom: 1px solid $border;
}

.panel {
  min-width: 0;
}

@media (max-width: 840px) {
  .message-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: none;
    border-bottom: 1px solid $border;
    max-height: 220px;
  }
}
</style>
