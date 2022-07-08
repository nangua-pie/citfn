<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { listCategories } from '@/api/category'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/message'
import type { Category } from '@/types'

const auth = useAuthStore()
const messages = useMessageStore()
const router = useRouter()

const categories = ref<Category[]>([])

const wsUrl = computed(() => import.meta.env.VITE_WS_URL ?? `${location.origin}/ws`)

async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch {
    categories.value = []
  }
}

function openMessages() {
  void router.push({ name: 'messages' })
}

function openProfile() {
  if (auth.profile) {
    void router.push({ name: 'user-profile', params: { id: auth.profile.id } })
  }
}

async function signOut() {
  messages.disconnect()
  await auth.logout()
  void router.push({ name: 'home' })
}

// The socket lives for as long as a session does: opened on sign in, torn
// down on sign out.
watch(
  () => auth.isAuthenticated,
  (signedIn) => {
    if (signedIn) {
      messages.connect(wsUrl.value)
    } else {
      messages.disconnect()
    }
  },
  { immediate: true },
)

onMounted(loadCategories)
</script>

<template>
  <v-app-bar flat height="56" class="app-header">
    <v-container class="d-flex align-center py-0">
      <router-link :to="{ name: 'home' }" class="brand text-truncate">Forum</router-link>

      <v-spacer />

      <v-menu v-if="categories.length" open-on-hover>
        <template #activator="{ on, attrs }">
          <v-btn text small v-bind="attrs" v-on="on" class="mr-2">Boards</v-btn>
        </template>
        <v-list dense>
          <v-list-item
            v-for="category in categories"
            :key="category.id"
            :to="{ name: 'category', params: { id: category.id } }"
          >
            <v-list-item-title>{{ category.name }}</v-list-item-title>
            <v-list-item-subtitle>{{ category.postCount }} posts</v-list-item-subtitle>
          </v-list-item>
        </v-list>
      </v-menu>

      <template v-if="auth.isAuthenticated">
        <v-btn icon variant="text" :title="messages.unread ? `${messages.unread} unread` : 'Messages'" @click="openMessages">
          <v-badge :content="messages.unread" :model-value="messages.unread > 0" color="error" overlap>
            <v-icon>mdi-message-text-outline</v-icon>
          </v-badge>
        </v-btn>
        <v-btn text small :to="{ name: 'post-create' }" class="mr-2">New post</v-btn>
        <v-menu>
          <template #activator="{ on, attrs }">
            <v-btn icon variant="text" v-bind="attrs" v-on="on">
              <v-avatar size="28">
                <v-img v-if="auth.profile?.avatarUrl" :src="auth.profile.avatarUrl" />
                <span v-else>{{ auth.profile?.nickname?.charAt(0) }}</span>
              </v-avatar>
            </v-btn>
          </template>
          <v-list dense>
            <v-list-item @click="openProfile">
              <v-list-item-title>Profile</v-list-item-title>
            </v-list-item>
            <v-list-item :to="{ name: 'settings' }">
              <v-list-item-title>Settings</v-list-item-title>
            </v-list-item>
            <v-divider />
            <v-list-item @click="signOut">
              <v-list-item-title>Sign out</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </template>
      <template v-else>
        <v-btn text small :to="{ name: 'login' }">Sign in</v-btn>
        <v-btn color="primary" small :to="{ name: 'register' }" class="ml-2">Register</v-btn>
      </template>
    </v-container>
  </v-app-bar>
</template>

<style scoped>
.app-header {
  border-bottom: 1px solid $border;
  background: $surface;
}

.brand {
  font-weight: 600;
  font-size: 16px;
  text-decoration: none;
  color: $text-primary;
}
</style>
