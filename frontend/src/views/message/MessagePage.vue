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

function onRemove(conversationId: string) {
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
      <aside class="sidebar" aria-label="Conversations">
        <div class="sidebar-header">
          <span class="page-title">Messages</span>
          <v-spacer />
          <v-btn
            icon
            size="x-small"
            variant="text"
            aria-label="Reload conversations"
            title="Reload conversations"
            @click="messages.loadConversations()"
          >
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

<style scoped lang="scss">
.message-layout {
  display: grid;
  /*
   * minmax(0, 1fr) rather than a bare 1fr: the panel keeps its automatic
   * minimum size otherwise, so a long nickname or an unbroken message would
   * stretch the chat column past the viewport instead of wrapping.
   */
  grid-template-columns: 300px minmax(0, 1fr);
  /*
   * The row is pinned to the container height instead of being left to size
   * itself. The grid has a fixed height, but an implicit auto row still grows
   * to fit its content, which pushed the composer past the bottom of the
   * viewport. minmax(0, ...) lets the row shrink below that content size so the
   * transcript scrolls and the composer stays where it belongs.
   */
  grid-template-rows: minmax(0, 1fr);
  /*
   * The transcript scrolls inside this box, so it has to be bounded to the
   * space the header and page padding leave. dvh keeps it correct while a
   * mobile browser's chrome slides in and out; the vh line is the fallback for
   * engines without dvh.
   */
  height: calc(100vh - #{$header-height} - (2 * var(--md-sys-space-6)));
  height: calc(100dvh - #{$header-height} - (2 * var(--md-sys-space-6)));
  border: 1px solid $border;
  border-radius: $radius-md;
  overflow: hidden;
  background: $surface;
}

.sidebar {
  border-right: 1px solid $border;
  min-height: 0;
  min-width: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  padding: var(--md-sys-space-2) var(--md-sys-space-3);
  border-bottom: 1px solid $border;
}

.panel {
  min-width: 0;
}

/* Below the tablet range the two panes stack: the list is capped so the
   transcript keeps the larger share of the screen. */
@include below($bp-md) {
  .message-layout {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: minmax(0, auto) minmax(0, 1fr);
  }

  .sidebar {
    border-right: none;
    border-bottom: 1px solid $border;
    max-height: 38vh;
  }
}

@include below($bp-xs) {
  /* Reclaim the gutters: the two panes are the whole screen here. */
  .page-container {
    padding-left: 0;
    padding-right: 0;
  }
}
</style>
