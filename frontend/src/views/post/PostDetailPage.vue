<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'

import { deletePost, getPost, toggleCollect, toggleLike } from '@/api/post'
import { useAuthStore } from '@/stores/auth'
import { renderMarkdown } from '@/utils/markdown'
import { machineDate } from '@/utils/date'
import { postStatusLabel, postStatusTone } from '@/utils/post'
import { routeId } from '@/utils/snowflake'
import CommentThread from '@/components/CommentThread.vue'
import type { Post } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref<Post | null>(null)
const loading = ref(true)
const notFound = ref(false)
const error = ref('')

// Kept as the raw route string: the value is a 64 bit snowflake and
// `Number()` would round it, so the lookup would miss the row entirely.
const postId = () => routeId(route.params.id)
const isOwner = computed(() => post.value !== null && post.value.userId === auth.profile?.id)

/**
 * Sanitised HTML. AGENTS.md section 5 requires DOMPurify to run before any
 * value reaches v-html.
 */
const renderedBody = computed(() => renderMarkdown(post.value?.contentMd))
const statusLabel = computed(() => postStatusLabel(post.value?.status))
const statusTone = computed(() => postStatusTone(post.value?.status))

async function load() {
  const id = postId()
  if (!id) {
    notFound.value = true
    error.value = 'this post is unavailable'
    loading.value = false
    return
  }
  loading.value = true
  notFound.value = false
  try {
    post.value = await getPost(id)
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
    <div class="article">
    <div v-if="loading" class="pa-6 text-center">
      <v-progress-circular indeterminate size="22" />
    </div>

    <div v-else-if="notFound" class="state-message">
      <v-icon icon="mdi-file-question-outline" size="28" />
      <span class="state-message__title">This post is unavailable</span>
      <span>{{ error }}</span>
      <v-btn variant="outlined" size="small" class="mt-2" :to="{ name: 'home' }">
        Back to the forum
      </v-btn>
    </div>

    <template v-else-if="post">
      <article class="post-card panel">
        <h1 class="post-title">{{ post.title }}</h1>

        <div class="post-flags">
          <span v-if="post.isTop" class="flag flag--pinned">
            <v-icon icon="mdi-pin" size="12" class="flag__icon" />
            Pinned
          </span>
          <span v-if="post.isEssential" class="flag flag--essential">
            <v-icon icon="mdi-star" size="12" class="flag__icon" />
            Essential
          </span>
          <span v-if="statusLabel" class="flag" :class="`flag--${statusTone}`">
            {{ statusLabel }}
          </span>
        </div>

        <div class="post-meta">
          <v-avatar size="32" class="post-meta__avatar">
            <v-img v-if="post.authorAvatarUrl" :src="post.authorAvatarUrl" alt="" />
            <span v-else>{{ post.authorNickname?.charAt(0) }}</span>
          </v-avatar>
          <router-link :to="{ name: 'user-profile', params: { id: post.userId } }" class="author">
            {{ post.authorNickname ?? 'Unknown' }}
          </router-link>
          <time :datetime="machineDate(post.createTime)" class="muted">
            {{ dayjs(post.createTime).format('MMM D, YYYY HH:mm') }}
          </time>
          <span class="muted">·</span>
          <span class="muted">{{ post.viewCount }} views</span>
        </div>

        <!-- Rendered markdown, sanitised by DOMPurify in utils/markdown.ts. -->
        <div class="markdown-body" v-html="renderedBody" />

        <div v-if="post.tags?.length" class="post-tags">
          <v-chip v-for="tagName in post.tags" :key="tagName" size="small" class="post-tag">
            {{ tagName }}
          </v-chip>
        </div>

        <v-divider class="post-divider" />

        <!--
          Like and collect are tonal, because they are stateful but reversible.
          Edit is a text action; delete is the only destructive one and is kept
          on the trailing edge.
        -->
        <div class="post-actions">
          <v-btn
            :variant="post.liked ? 'contained' : 'outlined'"
            :color="post.liked ? 'primary' : undefined"
            size="small"
            class="post-action"
            :aria-pressed="post.liked"
            @click="onLike"
          >
            <v-icon :icon="post.liked ? 'mdi-heart' : 'mdi-heart-outline'" size="16" class="mr-1" />
            {{ post.liked ? 'Liked' : 'Like' }}
            <span class="post-action__count">{{ post.likeCount }}</span>
          </v-btn>
          <v-btn
            :variant="post.collected ? 'contained' : 'outlined'"
            :color="post.collected ? 'primary' : undefined"
            size="small"
            class="post-action"
            :aria-pressed="post.collected"
            @click="onCollect"
          >
            <v-icon :icon="post.collected ? 'mdi-bookmark' : 'mdi-bookmark-outline'" size="16" class="mr-1" />
            {{ post.collected ? 'Saved' : 'Save' }}
            <span class="post-action__count">{{ post.collectCount }}</span>
          </v-btn>

          <a class="comment-link" href="#comments">
            <v-icon icon="mdi-comment-outline" size="16" class="mr-1" />
            {{ post.commentCount }} comments
          </a>

          <v-spacer />
          <v-btn
            v-if="isOwner"
            size="small"
            variant="text"
            :to="{ name: 'post-edit', params: { id: post.id } }"
          >
            Edit
          </v-btn>
          <v-btn v-if="isOwner" size="small" variant="text" color="error" @click="onDelete">Delete</v-btn>
        </div>
      </article>

      <CommentThread
        id="comments"
        class="post-comments"
        :post-id="post.id"
        :post-author-id="post.userId"
      />
    </template>
    </div>
  </v-container>
</template>

<style scoped>
.article {
  /* A centred reading column: MDesign.md section 4 caps the measure so prose
     stays readable instead of stretching across a wide monitor. */
  max-width: 860px;
  margin: 0 auto;
}

.post-card {
  padding: var(--md-sys-space-6);
}

.post-title {
  font-size: var(--md-sys-typescale-title-lg);
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: 0;
  @include overflow-wrap-anywhere;
}

.post-flags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--md-sys-space-2);
  margin-top: var(--md-sys-space-2);
}

.author {
  font-weight: 500;
  color: $text-primary;
  text-decoration: none;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-5);
  font-size: var(--md-sys-typescale-body-sm);
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--md-sys-space-1);
  margin-top: var(--md-sys-space-5);
}

.post-divider {
  margin: var(--md-sys-space-5) 0;
}

.post-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--md-sys-space-2);
}

.post-action__count {
  margin-left: var(--md-sys-space-1);
  font-variant-numeric: tabular-nums;
  opacity: 0.75;
}

.comment-link {
  display: inline-flex;
  align-items: center;
  padding: 0 var(--md-sys-space-2);
  min-height: 32px;
  border-radius: $radius-sm;
  color: $text-muted;
  font-size: var(--md-sys-typescale-body-sm);
  text-decoration: none;
  transition: color var(--md-sys-motion-short) var(--md-sys-motion-easing);
}

.comment-link:hover {
  color: $accent;
}

.post-comments {
  margin-top: var(--md-sys-space-4);
  /* Anchoring to the thread must clear the sticky-free app bar. */
  scroll-margin-top: calc(#{$header-height} + var(--md-sys-space-4));
}

@include below($bp-sm) {
  .post-card {
    padding: var(--md-sys-space-4);
  }

  /* Actions wrap onto their own lines rather than overflowing at 320px. */
  .post-actions {
    gap: var(--md-sys-space-2);
  }
}
</style>
