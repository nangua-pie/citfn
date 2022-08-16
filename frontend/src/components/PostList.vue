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
        <!--
          3.0.0-beta.0 does not group the title and subtitles on its own: as
          direct children of a flex row they were laid out side by side, which
          made a row as wide as the sum of its parts and pushed the whole page
          past the viewport. v-list-item-header is the column that stacks them.
        -->
        <v-list-item-header class="post-header">
          <v-list-item-title class="d-flex align-center">
            <v-icon v-if="post.isTop" icon="mdi-pin" size="14" class="mr-1 flag" color="primary" />
            <v-icon v-if="post.isEssential" icon="mdi-star" size="14" class="mr-1 flag" color="warning" />
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
        </v-list-item-header>
        <div class="stats text-caption muted">
          <span class="mr-3"><v-icon icon="mdi-eye-outline" size="13" /> {{ post.viewCount }}</span>
          <span class="mr-3"><v-icon icon="mdi-comment-outline" size="13" /> {{ post.commentCount }}</span>
          <span><v-icon icon="mdi-heart-outline" size="13" /> {{ post.likeCount }}</span>
        </div>
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
  min-width: 0;
}

/* Vuetify already sets flex: 1 1 auto and min-width: 0 here; restated so the
   column is what shrinks when a long title is present. */
.post-header {
  min-width: 0;
}

.title {
  font-size: 15px;
  font-weight: 500;
  min-width: 0;
}

.summary,
.post-header :deep(.v-list-item-subtitle) {
  font-size: 12px;
}

.post-header :deep(.v-list-item-title) {
  font-size: 15px;
  font-weight: 500;
}

/* Pinned and starred markers keep their size; only the text is truncated. */
.flag {
  flex: none;
}

.stats {
  flex: 0 0 auto;
  white-space: nowrap;
}

.empty {
  font-size: 13px;
}
</style>
