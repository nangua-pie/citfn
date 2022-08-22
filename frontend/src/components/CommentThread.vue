<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'

import { createComment, deleteComment, listComments, toggleCommentLike } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'
import type { Comment } from '@/types'

const props = defineProps<{
  postId: string
  postAuthorId: string
}>()

const auth = useAuthStore()

const comments = ref<Comment[]>([])
const loading = ref(false)
const draft = ref('')
const replyTo = ref<Comment | null>(null)
const error = ref('')
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    comments.value = await listComments(props.postId)
  } finally {
    loading.value = false
  }
}

async function submit() {
  const content = draft.value.trim()
  if (!content || submitting.value) {
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await createComment({
      postId: props.postId,
      parentId: replyTo.value ? replyTo.value.id : null,
      content,
    })
    draft.value = ''
    replyTo.value = null
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'the comment could not be posted'
  } finally {
    submitting.value = false
  }
}

async function toggleLike(comment: Comment) {
  const result = await toggleCommentLike(comment.id)
  comment.liked = result.active
  comment.likeCount = result.count
}

async function remove(comment: Comment) {
  await deleteComment(comment.id)
  await load()
}

function startReply(comment: Comment) {
  replyTo.value = comment
  draft.value = `@${comment.authorNickname ?? 'user'} `
}

onMounted(load)
</script>

<template>
  <section class="comment-card panel">
    <h2 class="section-title comment-card__title">
      {{ comments.length }} {{ comments.length === 1 ? 'comment' : 'comments' }}
    </h2>

    <template v-if="loading && !comments.length">
      <div v-for="row in 3" :key="row" class="comment-skeleton" aria-hidden="true">
        <div class="skeleton skeleton--line" />
        <div class="skeleton skeleton--short" />
      </div>
      <p class="sr-only" role="status">Loading comments</p>
    </template>

    <p v-else-if="!comments.length" class="state-message">No comments yet.</p>

    <article v-for="comment in comments" :key="comment.id" class="comment">
      <div class="d-flex align-center mb-1">
        <router-link :to="{ name: 'user-profile', params: { id: comment.userId } }" class="author">
          {{ comment.authorNickname ?? 'Unknown' }}
        </router-link>
        <span v-if="comment.userId === postAuthorId" class="chip ml-2">author</span>
        <span class="muted ml-2 text-caption">{{ dayjs(comment.createTime).format('MMM D, HH:mm') }}</span>
      </div>

      <p class="comment-body mb-2">{{ comment.content }}</p>

      <div class="d-flex align-center actions">
        <v-btn
          size="x-small"
          :variant="comment.liked ? 'contained' : 'text'"
          :color="comment.liked ? 'primary' : undefined"
          :aria-pressed="comment.liked"
          @click="toggleLike(comment)"
        >
          <v-icon icon="mdi-heart-outline" size="14" class="mr-1" />{{ comment.likeCount }}
        </v-btn>
        <v-btn size="x-small" variant="text" @click="startReply(comment)">Reply</v-btn>
        <v-btn
          v-if="comment.userId === auth.profile?.id"
          size="x-small"
          variant="text"
          color="error"
          :aria-label="`Delete the comment by ${comment.authorNickname ?? 'this user'}`"
          @click="remove(comment)"
        >
          Delete
        </v-btn>
      </div>

      <div v-if="comment.replies?.length" class="replies">
        <article v-for="reply in comment.replies" :key="reply.id" class="reply">
          <div class="d-flex align-center mb-1">
            <router-link :to="{ name: 'user-profile', params: { id: reply.userId } }" class="author">
              {{ reply.authorNickname ?? 'Unknown' }}
            </router-link>
            <span v-if="reply.replyToNickname" class="muted ml-1 text-caption">
              → @{{ reply.replyToNickname }}
            </span>
            <span class="muted ml-2 text-caption">{{ dayjs(reply.createTime).format('MMM D, HH:mm') }}</span>
          </div>
          <p class="comment-body mb-1">{{ reply.content }}</p>
          <v-btn
            size="x-small"
            :variant="reply.liked ? 'contained' : 'text'"
            :color="reply.liked ? 'primary' : undefined"
            :aria-pressed="reply.liked"
            @click="toggleLike(reply)"
          >
            <v-icon icon="mdi-heart-outline" size="13" class="mr-1" />{{ reply.likeCount }}
          </v-btn>
        </article>
      </div>
    </article>

    <v-divider class="comment-divider" />

    <template v-if="auth.isAuthenticated">
      <v-textarea v-model="draft" rows="2" auto-grow hide-details placeholder="Add a comment" />
      <div class="d-flex align-center mt-2">
        <v-chip v-if="replyTo" size="small" class="mr-2" closable @click:close="replyTo = null">
          Replying to {{ replyTo.authorNickname }}
        </v-chip>
        <v-spacer />
        <v-btn color="primary" size="small" :loading="submitting" :disabled="!draft.trim()" @click="submit">
          Comment
        </v-btn>
      </div>
      <v-alert v-if="error" type="error" variant="outlined" density="compact" class="mt-2">{{ error }}</v-alert>
    </template>

    <p v-else class="muted text-body-2 mb-0">
      <router-link :to="{ name: 'login' }">Sign in</router-link> to join the discussion.
    </p>
  </section>
</template>

<style scoped>
.comment-card {
  padding: var(--md-sys-space-5);
}

.comment-card__title {
  margin-bottom: var(--md-sys-space-4);
}

.comment {
  padding-bottom: var(--md-sys-space-4);
  border-bottom: 1px solid $border;
  margin-bottom: var(--md-sys-space-4);
}

.comment:last-of-type {
  border-bottom: none;
}

.author {
  font-size: var(--md-sys-typescale-body-sm);
  font-weight: 500;
  color: $text-primary;
  text-decoration: none;
}

.chip {
  height: 20px;
  font-size: var(--md-sys-typescale-label-xs);
}

.comment-body {
  font-size: var(--md-sys-typescale-body-md);
  line-height: var(--md-sys-typescale-body-line);
  white-space: pre-wrap;
  @include overflow-wrap-anywhere;
}

.actions {
  margin-top: -4px;
}

.replies {
  padding-left: var(--md-sys-space-3);
  border-left: 2px solid $border;
}

.reply {
  margin-bottom: var(--md-sys-space-3);
}

.reply:last-child {
  margin-bottom: 0;
}

.comment-divider {
  margin: var(--md-sys-space-4) 0;
}

.comment-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-4);
}

.skeleton--line {
  width: 100%;
  height: 14px;
}

.skeleton--short {
  width: 30%;
  height: 12px;
}

@include below($bp-sm) {
  .comment-card {
    padding: var(--md-sys-space-4);
  }
}
</style>
