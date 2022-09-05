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
  <!--
    The drawer is a sibling of the app bar, not a child of it. Vuetify
    positions a drawer through the layout service, which expects it to be a
    direct child of the app shell; nested inside the bar it resolved to a
    zero-height box and never appeared. Vue 3 allows a component to have
    several roots, so both live side by side here.
  -->
  <v-navigation-drawer v-model="navOpen" temporary class="nav-drawer">
    <nav aria-label="Boards">
      <p class="nav-drawer__heading">Boards</p>
      <v-list dense>
        <v-list-item :to="{ name: 'home' }" :active="route.name === 'home'" @click="closeNav">
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
            <v-list-item-subtitle class="text-caption">{{ category.postCount }} posts</v-list-item-subtitle>
          </v-list-item-header>
        </v-list-item>
      </v-list>
    </nav>

    <!--
      Saved posts has no icon control in the bar: the row of controls was
      already at the limit of a 320px bar, and the account menu only exists on
      wider screens. The drawer is where it stays reachable on a phone.
    -->
    <nav v-if="auth.isAuthenticated" aria-label="Your account">
      <p class="nav-drawer__heading">You</p>
      <v-list dense>
        <v-list-item :to="{ name: 'collections' }" @click="closeNav">
          <v-list-item-header>
            <v-list-item-title>Saved posts</v-list-item-title>
          </v-list-item-header>
        </v-list-item>
      </v-list>
    </nav>
  </v-navigation-drawer>

  <v-app-bar flat height="56" class="app-header">
    <v-btn
      v-if="categories.length"
      icon
      variant="text"
      class="header-action nav-drawer__trigger"
      aria-label="Open board menu"
      title="Open board menu"
      @click="navOpen = true"
    >
      <v-icon size="20">mdi-menu</v-icon>
    </v-btn>

    <v-container class="header-inner">
      <router-link :to="{ name: 'home' }" class="brand">Forum</router-link>

      <!-- Desktop only; the drawer carries the same links on mobile. -->
      <v-menu v-if="categories.length">
        <!--
          Vuetify 3.0.0-beta.0 exposes the activator handlers as one `props`
          object. The stable 3.x `{ on, attrs }` pair does not exist in this
          version, and binding `undefined` would leave the trigger inert.
        -->
        <template #activator="{ props: activatorProps }">
          <v-btn variant="text" class="header-action header-boards mr-2" v-bind="activatorProps">
            Boards
          </v-btn>
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

      <v-spacer />

      <!--
        One row at one height. Every control in the group is forced to the same
        box by .header-action, so the bar no longer mixes 32px and 40px buttons.
        New post is the only filled control here; everything else is a plain
        text or icon action, which is what makes it read as the primary one.
      -->
      <div class="header-actions">
        <v-btn
          icon
          variant="text"
          class="header-action"
          :aria-label="messages.unread ? `Messages, ${messages.unread} unread` : 'Messages'"
          :title="messages.unread ? `Messages, ${messages.unread} unread` : 'Messages'"
          @click="openMessages"
        >
          <v-badge :content="messages.unread" :model-value="messages.unread > 0" color="error" overlap>
            <v-icon size="20">mdi-message-text-outline</v-icon>
          </v-badge>
        </v-btn>

        <v-btn
          icon
          variant="text"
          class="header-action"
          :aria-label="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
          :title="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
          @click="toggle"
        >
          <v-icon size="20">{{ isDark ? 'mdi-weather-sunny' : 'mdi-weather-night' }}</v-icon>
        </v-btn>

        <template v-if="auth.isAuthenticated">
          <v-btn
            color="primary"
            class="header-action header-action--filled ml-2"
            :to="{ name: 'post-create' }"
          >
            <v-icon size="18" class="mr-1">mdi-pencil-outline</v-icon>
            New post
          </v-btn>

          <v-menu>
            <template #activator="{ props: activatorProps }">
              <v-btn
                icon
                variant="text"
                class="header-action ml-1"
                v-bind="activatorProps"
                :aria-label="`Account menu for ${auth.profile?.nickname ?? 'your account'}`"
              >
                <v-avatar size="28">
                  <v-img v-if="auth.profile?.avatarUrl" :src="auth.profile.avatarUrl" alt="" />
                  <span v-else>{{ auth.profile?.nickname?.charAt(0) }}</span>
                </v-avatar>
              </v-btn>
            </template>
            <v-list dense>
              <v-list-item @click="openProfile">
                <v-list-item-title>Profile</v-list-item-title>
              </v-list-item>
              <v-list-item :to="{ name: 'collections' }">
                <v-list-item-title>Saved posts</v-list-item-title>
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
          <v-btn
            variant="text"
            class="header-action header-signin ml-1"
            :to="{ name: 'login' }"
          >
            Sign in
          </v-btn>
          <v-btn
            color="primary"
            class="header-action header-action--filled ml-1"
            :to="{ name: 'register' }"
          >
            Register
          </v-btn>
        </template>
      </div>
    </v-container>
  </v-app-bar>
</template>

<style scoped lang="scss">
.app-header {
  border-bottom: 1px solid $border;
  background: $surface;
}

.brand {
  font-weight: 600;
  font-size: var(--md-sys-typescale-title-md);
  /* The name is short and identifies the site; it must never be the thing
     that gets squeezed out on a narrow bar. */
  flex: none;
  white-space: nowrap;
  text-decoration: none;
  color: $text-primary;
}

.header-inner {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  flex: none;
  gap: 2px;
}

/*
 * One box for every control in the bar. Vuetify's own size scale gives a
 * plain button 36px and an icon button a different box again, which is what
 * made the row look assembled rather than designed.
 *
 * The selector is anchored to .app-header on purpose. Vuetify sets button
 * height with `.v-btn.v-btn--density-default`, which is also two classes, so
 * a single class here ties with it and loses on source order. Descending from
 * the bar out-specifies it without resorting to !important.
 */
.app-header .header-action {
  height: 40px;
}

/*
 * Icon-only controls are square. Vuetify gives an icon button 48px of width,
 * a third more than the height set above, and the three of them are what push
 * the signed-out row past the edge of a phone.
 */
.app-header .header-action.v-btn--icon {
  width: 40px;
  min-width: 40px;
  padding: 0;
}

/* The text buttons size to their label instead. */
.app-header .header-action--filled,
.app-header .header-signin {
  min-width: 0;
}

.app-header .header-action--filled {
  padding: 0 var(--md-sys-space-4);
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
  .header-boards {
    display: none;
  }
}

/* On a 320px bar the wordmark, three controls and two buttons have to share
   one row. Reclaiming the gutters and tightening the filled button is enough
   to keep the brand whole instead of truncating it to "F...". */
@include below($bp-xs) {
  .header-inner {
    padding: 0 var(--md-sys-space-1);
  }

  .app-header .header-action--filled {
    padding: 0 var(--md-sys-space-3);
  }

  .brand {
    font-size: var(--md-sys-typescale-title-sm);
  }
}

/*
 * Below roughly 360px the signed-out row still does not fit. "Register"
 * stays because it is the primary action, and the register page links back to
 * sign in, so nothing becomes unreachable. This sits below the smallest
 * breakpoint in MDesign.md section 11 because it is a consequence of how many
 * controls fit, not a layout breakpoint in its own right.
 */
@media (max-width: 359px) {
  .header-signin {
    display: none;
  }
}
</style>
