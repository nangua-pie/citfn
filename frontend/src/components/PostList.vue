<script setup lang="ts">
import dayjs from 'dayjs'

import type { Post } from '@/types'

defineProps<{
  posts: Post[]
  loading?: boolean
  emptyText?: string
}>()
</script>

<template>
  <div>
    <div v-if="loading" class="pa-6 text-center">
      <v-progress-circular indeterminate size="22" />
    </div>

    <p v-else-if="!posts.length" class="empty pa-6 text-center muted mb-0">
      {{ emptyText ?? 'Nothing here yet.' }}
    </p>

    <v-list v-else lines="three" class="post-list">
      <v-list-item
        v-for="post in posts"
        :key="post.id"
        :to="{ name: 'post-detail', params: { id: post.id } }"
        class="post-row"
      >
        <v-list-item-title class="d-flex align-center">
          <v-icon v-if="post.isTop" icon="mdi-pin" size="14" class="mr-1" color="primary" />
          <v-icon v-if="post.isEssential" icon="mdi-star" size="14" class="mr-1" color="warning" />
          <span class="title text-truncate">{{ post.title }}</span>
        </v-list-item-title>
        <v-list-item-subtitle class="text-truncate">
          {{ post.authorNickname ?? 'Unknown' }}
          <span v-if="post.categoryName"> · {{ post.categoryName }}</span>
          <span> · {{ dayjs(post.createTime).format('MMM D, YYYY') }}</span>
        </v-list-item-subtitle>
        <v-list-item-subtitle v-if="post.summary" class="summary text-truncate">
          {{ post.summary }}
        </v-list-item-subtitle>
        <v-list-item-action class="stats text-caption muted">
          <span class="mr-3"><v-icon icon="mdi-eye-outline" size="13" /> {{ post.viewCount }}</span>
          <span class="mr-3"><v-icon icon="mdi-comment-outline" size="13" /> {{ post.commentCount }}</span>
          <span><v-icon icon="mdi-heart-outline" size="13" /> {{ post.likeCount }}</span>
        </v-list-item-action>
      </v-list-item>
    </v-list>
  </div>
</template>

<style scoped>
.post-list {
  background: transparent;
  padding: 0;
}

.post-row {
  border-bottom: 1px solid $border;
}

.title {
  font-size: 15px;
  font-weight: 500;
}

.summary {
  font-size: 12px;
  max-width: 640px;
}

.stats {
  white-space: nowrap;
}

.empty {
  font-size: 13px;
}
</style>
