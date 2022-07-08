<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'

import { deletePost, getPost, toggleCollect, toggleLike } from '@/api/post'
import { useAuthStore } from '@/stores/auth'
import { renderMarkdown } from '@/utils/markdown'
import CommentThread from '@/components/CommentThread.vue'
import type { Post } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref<Post | null>(null)
const loading = ref(true)
const notFound = ref(false)
const error = ref('')

const postId = () => Number(route.params.id)
const isOwner = computed(() => post.value !== null && post.value.userId === auth.profile?.id)

/**
 * Sanitised HTML. AGENTS.md section 5 requires DOMPurify to run before any
 * value reaches v-html.
 */
const renderedBody = computed(() => renderMarkdown(post.value?.contentMd))

async function load() {
  loading.value = true
  notFound.value = false
  try {
    post.value = await getPost(postId())
  } catch (err) {
    notFound.value = true
    error.value = err instanceof Error ? err.message : 'this post is unavailable'
  } finally {
    loading.value = false
  }
}

async function onLike() {
  if (!post.value) return
  const result = await toggleLike(post.value.id)
  post.value.liked = result.active
  post.value.likeCount = result.count
}

async function onCollect() {
  if (!post.value) return
  const result = await toggleCollect(post.value.id)
  post.value.collected = result.active
  post.value.collectCount = result.count
}

async function onDelete() {
  if (!post.value) return
  await deletePost(post.value.id)
  await router.push({ name: 'home' })
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <v-container class="page-container">
    <div v-if="loading" class="pa-6 text-center">
      <v-progress-circular indeterminate size="22" />
    </div>

    <v-alert v-else-if="notFound" type="warning" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <template v-else-if="post">
      <v-card class="post-card pa-5">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="d-flex align-center flex-wrap meta mt-2 mb-4">
          <v-avatar size="28" class="mr-2">
            <v-img v-if="post.authorAvatarUrl" :src="post.authorAvatarUrl" />
          </v-avatar>
          <router-link :to="{ name: 'user-profile', params: { id: post.userId } }" class="author mr-3">
            {{ post.authorNickname ?? 'Unknown' }}
          </router-link>
          <span class="muted">{{ dayjs(post.createTime).format('MMM D, YYYY HH:mm') }}</span>
          <v-spacer />
          <span class="muted mr-3">{{ post.viewCount }} views</span>
        </div>

        <!-- Rendered markdown, sanitised by DOMPurify in utils/markdown.ts. -->
        <div class="markdown-body" v-html="renderedBody" />

        <div v-if="post.tags?.length" class="d-flex flex-wrap mt-4">
          <v-chip v-for="tagName in post.tags" :key="tagName" size="small" class="mr-1 mb-1">
            {{ tagName }}
          </v-chip>
        </div>

        <v-divider class="my-4" />

        <div class="d-flex align-center flex-wrap">
          <v-btn
            size="small"
            variant="text"
            :color="post.liked ? 'primary' : undefined"
            class="mr-2"
            @click="onLike"
          >
            <v-icon icon="mdi-heart-outline" size="16" class="mr-1" />
            {{ post.likeCount }}
          </v-btn>
          <v-btn size="small" variant="text" :color="post.collected ? 'primary' : undefined" class="mr-2" @click="onCollect">
            <v-icon icon="mdi-bookmark-outline" size="16" class="mr-1" />
            {{ post.collectCount }}
          </v-btn>
          <span class="muted mr-3">{{ post.commentCount }} comments</span>
          <v-spacer />
          <v-btn v-if="isOwner" size="small" variant="text" :to="{ name: 'post-edit', params: { id: post.id } }">
            Edit
          </v-btn>
          <v-btn v-if="isOwner" size="small" variant="text" color="error" @click="onDelete">Delete</v-btn>
        </div>
      </v-card>

      <CommentThread class="mt-4" :post-id="post.id" :post-author-id="post.userId" />
    </template>
  </v-container>
</template>

<style scoped>
.post-card {
  border: 1px solid $border;
}

.post-title {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.35;
}

.meta {
  font-size: 13px;
}

.author {
  font-weight: 500;
  color: $text-primary;
  text-decoration: none;
}
</style>
