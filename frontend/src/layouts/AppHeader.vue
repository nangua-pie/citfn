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
        <!--
          Vuetify 3.0.0-beta.0 exposes the activator handlers as one `props`
          object. The stable 3.x `{ on, attrs }` pair does not exist in this
          version, and binding `undefined` would leave the trigger inert.
        -->
        <template #activator="{ props: activatorProps }">
          <v-btn variant="text" size="small" v-bind="activatorProps" class="mr-2">Boards</v-btn>
        </template>
        <v-list dense>
          <v-list-item
            v-for="category in categories"
            :key="category.id"
            :to="{ name: 'category', params: { id: category.id } }"
          >
            <!-- beta.0 needs the header to stack the name above the count. -->
            <v-list-item-header>
              <v-list-item-title>{{ category.name }}</v-list-item-title>
              <v-list-item-subtitle>{{ category.postCount }} posts</v-list-item-subtitle>
            </v-list-item-header>
          </v-list-item>
        </v-list>
      </v-menu>

      <template v-if="auth.isAuthenticated">
        <v-btn icon variant="text" :title="messages.unread ? `${messages.unread} unread` : 'Messages'" @click="openMessages">
          <v-badge :content="messages.unread" :model-value="messages.unread > 0" color="error" overlap>
            <v-icon>mdi-message-text-outline</v-icon>
          </v-badge>
        </v-btn>
        <v-btn variant="text" size="small" :to="{ name: 'post-create' }" class="mr-2">New post</v-btn>
        <v-menu>
          <template #activator="{ props: activatorProps }">
            <v-btn icon variant="text" v-bind="activatorProps">
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
        <v-btn variant="text" size="small" :to="{ name: 'login' }">Sign in</v-btn>
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
