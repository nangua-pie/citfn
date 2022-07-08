<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'

import { createComment, deleteComment, listComments, toggleCommentLike } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'
import type { Comment } from '@/types'

const props = defineProps<{
  postId: number
  postAuthorId: number
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
  <v-card class="comment-card pa-5">
    <h2 class="section-title mb-4">{{ comments.length }} comments</h2>

    <div v-if="loading" class="pa-4 text-center">
      <v-progress-circular indeterminate size="20" />
    </div>

    <p v-else-if="!comments.length" class="muted text-body-2">No comments yet.</p>

    <div v-for="comment in comments" :key="comment.id" class="comment">
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
          variant="text"
          :color="comment.liked ? 'primary' : undefined"
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
          @click="remove(comment)"
        >
          Delete
        </v-btn>
      </div>

      <div v-if="comment.replies?.length" class="replies mt-3">
        <div v-for="reply in comment.replies" :key="reply.id" class="reply mb-3">
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
            variant="text"
            :color="reply.liked ? 'primary' : undefined"
            @click="toggleLike(reply)"
          >
            <v-icon icon="mdi-heart-outline" size="13" class="mr-1" />{{ reply.likeCount }}
          </v-btn>
        </div>
      </div>
    </div>

    <v-divider class="my-4" />

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
      <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mt-2">{{ error }}</v-alert>
    </template>

    <p v-else class="muted text-body-2 mb-0">
      <router-link :to="{ name: 'login' }">Sign in</router-link> to join the discussion.
    </p>
  </v-card>
</template>

<style scoped>
.comment-card {
  border: 1px solid $border;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.comment {
  padding-bottom: 16px;
  border-bottom: 1px solid $border;
  margin-bottom: 16px;
}

.comment:last-of-type {
  border-bottom: none;
}

.author {
  font-size: 13px;
  font-weight: 500;
  color: $text-primary;
  text-decoration: none;
}

.chip {
  font-size: 11px;
  height: 18px;
}

.comment-body {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.actions {
  margin-top: -4px;
}

.replies {
  padding-left: 14px;
  border-left: 2px solid $border;
}
</style>
