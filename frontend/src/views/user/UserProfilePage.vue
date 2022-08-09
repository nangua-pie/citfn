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
const error = ref('')

// The raw route string: a user id is a 64 bit snowflake, and `Number()`
// would round it and address the wrong account.
const userId = () => routeId(route.params.id)
const isSelf = () => profile.value?.id === auth.profile?.id

async function load() {
  loading.value = true
  error.value = ''
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
    const result = await listPosts({ userId: id, page: 1, size: 10 })
    posts.value = result.records
  } catch (err) {
    profile.value = null
    error.value = err instanceof Error ? err.message : 'this profile is unavailable'
  } finally {
    loading.value = false
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
      <v-card class="profile-card pa-5 mb-4">
        <div class="d-flex align-center flex-wrap">
          <v-avatar size="56" class="mr-4">
            <v-img v-if="profile.avatarUrl" :src="profile.avatarUrl" />
            <span v-else class="text-h5">{{ profile.nickname?.charAt(0) }}</span>
          </v-avatar>
          <div class="min-width-0">
            <div class="d-flex align-center">
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
      </v-card>

      <v-card class="feed-card">
        <PostList :posts="posts" empty-text="This user has not published anything yet." />
      </v-card>
    </template>
  </v-container>
</template>

<style scoped>
.profile-card,
.feed-card {
  border: 1px solid $border;
}

.feed-card {
  overflow: hidden;
}

.level-chip {
  font-size: 11px;
  height: 20px;
}

.bio {
  font-size: 13px;
  color: $text-muted;
  max-width: 520px;
}

.min-width-0 {
  min-width: 0;
}
</style>
