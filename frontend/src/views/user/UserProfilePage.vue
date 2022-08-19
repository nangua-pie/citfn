<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { listPosts } from '@/api/post'
import { getProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/message'
import { routeId } from '@/utils/snowflake'
import PostList from '@/components/PostList.vue'
import type { Post, UserProfile } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const messages = useMessageStore()

const profile = ref<UserProfile | null>(null)
const posts = ref<Post[]>([])
const loading = ref(true)
/** Profile load failure: the page itself cannot render. */
const error = ref('')
/** Post list failure: the profile is still worth showing above the retry. */
const postsError = ref('')
/** Tracked apart from `loading` so the list keeps its skeleton while the
 *  post query runs after the profile has already rendered. */
const postsLoading = ref(false)

// The raw route string: a user id is a 64 bit snowflake, and `Number()`
// would round it and address the wrong account.
const userId = () => routeId(route.params.id)
const isSelf = () => profile.value?.id === auth.profile?.id

async function load() {
  loading.value = true
  error.value = ''
  postsError.value = ''
  const id = userId()
  if (!id) {
    profile.value = null
    posts.value = []
    error.value = 'this profile is unavailable'
    loading.value = false
    return
  }
  try {
    profile.value = await getProfile(id)
  } catch (err) {
    profile.value = null
    posts.value = []
    error.value = err instanceof Error ? err.message : 'this profile is unavailable'
    loading.value = false
    return
  }
  loading.value = false

  // Loaded after the profile so a slow post query never delays the header.
  postsLoading.value = true
  try {
    const result = await listPosts({ userId: id, page: 1, size: 10 })
    posts.value = result.records
  } catch (err) {
    posts.value = []
    postsError.value = err instanceof Error ? err.message : 'their posts could not be loaded'
  } finally {
    postsLoading.value = false
  }
}

async function retryPosts() {
  const id = userId()
  if (!id) {
    return
  }
  postsError.value = ''
  postsLoading.value = true
  try {
    const result = await listPosts({ userId: id, page: 1, size: 10 })
    posts.value = result.records
  } catch (err) {
    postsError.value = err instanceof Error ? err.message : 'their posts could not be loaded'
  } finally {
    postsLoading.value = false
  }
}

async function startConversation() {
  if (!profile.value) return
  await messages.openWith(profile.value.id)
  await router.push({ name: 'messages' })
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <v-container class="page-container">
    <div v-if="loading" class="pa-6 text-center">
      <v-progress-circular indeterminate size="22" />
    </div>

    <v-alert v-else-if="error" type="warning" variant="outlined">{{ error }}</v-alert>

    <template v-else-if="profile">
      <section class="profile-card panel">
        <div class="profile-card__body">
          <v-avatar size="56" class="mr-4">
            <v-img v-if="profile.avatarUrl" :src="profile.avatarUrl" />
            <span v-else class="text-h5">{{ profile.nickname?.charAt(0) }}</span>
          </v-avatar>
          <div class="profile-card__identity">
            <div class="d-flex align-center flex-wrap">
              <h1 class="page-title mr-2">{{ profile.nickname }}</h1>
              <v-chip size="small" class="level-chip">Level {{ profile.level }}</v-chip>
            </div>
            <div class="muted text-body-2">@{{ profile.username }} · {{ profile.points }} points</div>
            <div v-if="profile.bio" class="bio mt-1">{{ profile.bio }}</div>
          </div>
          <v-spacer />
          <v-btn v-if="!isSelf()" color="primary" size="small" class="mt-3 mt-md-0" @click="startConversation">
            <v-icon icon="mdi-message-text-outline" size="16" class="mr-1" />
            Message
          </v-btn>
          <v-btn v-else size="small" class="mt-3 mt-md-0" :to="{ name: 'settings' }">Edit profile</v-btn>
        </div>
      </section>

      <h2 class="section-title section-title--spaced">Posts</h2>
      <div class="feed-card panel">
        <PostList
          :posts="posts"
          :loading="postsLoading"
          :error="postsError"
          empty-text="This user has not published anything yet."
          @retry="retryPosts"
        />
      </div>
    </template>
  </v-container>
</template>

<style scoped>
.profile-card {
  margin-bottom: var(--md-sys-space-6);
  padding: var(--md-sys-space-5);
}

.profile-card__body {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--md-sys-space-2) var(--md-sys-space-4);
}

.profile-card__identity {
  /* min-width: 0 is what lets a long nickname wrap instead of widening the
     row past the viewport. */
  min-width: 0;
  flex: 1 1 240px;
}

.feed-card {
  overflow: hidden;
}

.section-title--spaced {
  margin-bottom: var(--md-sys-space-2);
}

.level-chip {
  height: 22px;
  font-size: var(--md-sys-typescale-label-xs);
}

.bio {
  font-size: var(--md-sys-typescale-body-md);
  color: $text-muted;
  max-width: 60ch;
}
</style>
