<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { listCategories } from '@/api/category'
import { useThemeToggle } from '@/composables/useThemeToggle'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/message'
import type { Category } from '@/types'

const auth = useAuthStore()
const messages = useMessageStore()
const router = useRouter()
const route = useRoute()
const { isDark, toggle } = useThemeToggle()

const categories = ref<Category[]>([])
const navOpen = ref(false)

const wsUrl = computed(() => import.meta.env.VITE_WS_URL ?? `${location.origin}/ws`)

/** Highlights the board currently being read in the nav menu. */
const activeCategoryId = computed(() => {
  if (route.name === 'category') {
    return typeof route.params.id === 'string' ? route.params.id : null
  }
  return null
})

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

/**
 * The mobile drawer closes itself after a choice, otherwise it stays open on
 * top of the page the user just asked for.
 */
function closeNav() {
  navOpen.value = false
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
    <!--
      Secondary navigation lives in a drawer on small screens (MDesign.md
      section 6), so the bar itself only carries the brand and account actions.
    -->
    <v-navigation-drawer v-model="navOpen" temporary class="nav-drawer">
      <nav aria-label="Boards">
        <p class="nav-drawer__heading">Boards</p>
        <v-list dense>
          <v-list-item
            :to="{ name: 'home' }"
            :active="route.name === 'home'"
            @click="closeNav"
          >
            <v-list-item-header>
              <v-list-item-title>All posts</v-list-item-title>
            </v-list-item-header>
          </v-list-item>

          <v-list-item
            v-for="category in categories"
            :key="category.id"
            :to="{ name: 'category', params: { id: category.id } }"
            :active="activeCategoryId === category.id"
            @click="closeNav"
          >
            <!-- beta.0 needs the header to stack the name above the count. -->
            <v-list-item-header>
              <v-list-item-title>{{ category.name }}</v-list-item-title>
              <v-list-item-subtitle class="text-caption">
                {{ category.postCount }} posts
              </v-list-item-subtitle>
            </v-list-item-header>
          </v-list-item>
        </v-list>
      </nav>

      <template #append>
        <div class="nav-drawer__footer">
          <v-btn variant="text" size="small" block @click="closeNav">Close</v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <v-btn
      v-if="categories.length"
      icon
      variant="text"
      class="ml-1 nav-drawer__trigger"
      aria-label="Open board menu"
      @click="navOpen = true"
    >
      <v-icon>mdi-menu</v-icon>
    </v-btn>

    <v-container class="header-inner">
      <router-link :to="{ name: 'home' }" class="brand text-truncate">Forum</router-link>

      <v-spacer />

      <!-- Desktop only; the drawer above carries the same links on mobile. -->
      <v-menu v-if="categories.length" class="header-boards">
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
            :active="activeCategoryId === category.id"
          >
            <!-- beta.0 needs the header to stack the name above the count. -->
            <v-list-item-header>
              <v-list-item-title>{{ category.name }}</v-list-item-title>
              <v-list-item-subtitle class="text-caption">
                {{ category.postCount }} posts
              </v-list-item-subtitle>
            </v-list-item-header>
          </v-list-item>
        </v-list>
      </v-menu>

      <!--
        Icon-only control, so it carries both an accessible name and a title.
      -->
      <v-btn
        icon
        variant="text"
        :aria-label="messages.unread ? `Messages, ${messages.unread} unread` : 'Messages'"
        :title="messages.unread ? `Messages, ${messages.unread} unread` : 'Messages'"
        @click="openMessages"
      >
        <v-badge :content="messages.unread" :model-value="messages.unread > 0" color="error" overlap>
          <v-icon>mdi-message-text-outline</v-icon>
        </v-badge>
      </v-btn>

      <v-btn
        icon
        variant="text"
        :aria-label="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
        :title="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
        @click="toggle"
      >
        <v-icon>{{ isDark ? 'mdi-weather-sunny' : 'mdi-weather-night' }}</v-icon>
      </v-btn>

      <template v-if="auth.isAuthenticated">
        <v-btn
          color="primary"
          size="small"
          :to="{ name: 'post-create' }"
          class="ml-2 header-new-post"
        >
          New post
        </v-btn>
        <v-menu>
          <template #activator="{ props: activatorProps }">
            <v-btn
              icon
              variant="text"
              v-bind="activatorProps"
              :aria-label="`Account menu for ${auth.profile?.nickname ?? 'your account'}`"
            >
              <v-avatar size="28">
                <v-img v-if="auth.profile?.avatarUrl" :src="auth.profile.avatarUrl" :alt="''" />
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
        <v-btn color="primary" size="small" :to="{ name: 'register' }" class="ml-2">
          Register
        </v-btn>
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
  font-size: var(--md-sys-typescale-title-md);
  text-decoration: none;
  color: $text-primary;
}

.header-inner {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0;
}

/* MD3 elevates a top app bar on scroll; the flat bar here uses a border
   instead so content is not fighting a shadow. */
.nav-drawer {
  background: $surface;
}

.nav-drawer__heading {
  margin: var(--md-sys-space-4) var(--md-sys-space-4) var(--md-sys-space-2);
  font-size: var(--md-sys-typescale-label-sm);
  font-weight: 600;
  color: $text-muted;
}

.nav-drawer__footer {
  padding: var(--md-sys-space-2) var(--md-sys-space-3) var(--md-sys-space-3);
}

/* Selection state is carried by a filled container and a heavier weight, not
   by colour alone (MDesign.md section 12). */
.nav-drawer :deep(.v-list-item--active) {
  background: $accent-soft;
  color: $text-primary;
}

.nav-drawer :deep(.v-list-item--active .v-list-item__title) {
  font-weight: 600;
}

/* The drawer trigger and the desktop boards menu are mutually exclusive: the
   menu is hidden once the drawer takes over. */
@include above($bp-md) {
  .nav-drawer__trigger {
    display: none;
  }
}

@include below($bp-md) {
  .header-boards,
  .header-new-post {
    display: none;
  }
}
</style>
