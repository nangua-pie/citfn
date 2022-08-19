<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'

import { postStatusLabel, postStatusTone } from '@/utils/post'
import type { Post } from '@/types'

const props = withDefaults(
  defineProps<{
    posts: Post[]
    loading?: boolean
    /** Non-empty when the last load failed; renders the retry affordance. */
    error?: string
    emptyText?: string
    /** How many placeholder rows to draw while the first page loads. */
    skeletonRows?: number
  }>(),
  {
    loading: false,
    error: '',
    emptyText: 'Nothing here yet.',
    skeletonRows: 4,
  },
)

const emit = defineEmits<{
  (event: 'retry'): void
}>()

const hasError = computed(() => props.error.trim().length > 0)

/** Only the first few tags fit beside the title without crowding it. */
const TAG_LIMIT = 3
</script>

<template>
  <div class="post-list">
    <!--
      Skeletons hold the row height while the request is in flight, so the list
      does not jump when the content lands.
    -->
    <template v-if="loading && !posts.length">
      <div v-for="row in skeletonRows" :key="row" class="post-skeleton" aria-hidden="true">
        <div class="skeleton skeleton--title" />
        <div class="skeleton skeleton--meta" />
      </div>
      <p class="sr-only" role="status">Loading posts</p>
    </template>

    <div v-else-if="hasError" class="state-message" role="alert">
      <v-icon icon="mdi-alert-circle-outline" size="28" />
      <span class="state-message__title">Could not load posts</span>
      <span>{{ error }}</span>
      <v-btn size="small" variant="outlined" class="mt-2" @click="emit('retry')">
        <v-icon icon="mdi-refresh" size="16" class="mr-1" />
        Try again
      </v-btn>
    </div>

    <div v-else-if="!posts.length" class="state-message">
      <v-icon icon="mdi-forum-outline" size="28" />
      <span class="state-message__title">{{ emptyText }}</span>
      <slot name="empty-action" />
    </div>

    <v-list v-else lines="three">
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
          <v-list-item-title class="post-title">
            <!--
              Status is spelled out rather than left to a bare icon, so the
              state does not depend on colour or on recognising a glyph.
            -->
            <span v-if="post.isTop" class="flag flag--pinned">
              <v-icon icon="mdi-pin" size="12" class="flag__icon" />
              Pinned
            </span>
            <span v-if="post.isEssential" class="flag flag--essential">
              <v-icon icon="mdi-star" size="12" class="flag__icon" />
              Essential
            </span>
            <span
              v-if="postStatusLabel(post.status)"
              class="flag"
              :class="`flag--${postStatusTone(post.status)}`"
            >
              {{ postStatusLabel(post.status) }}
            </span>
            <span class="title" :title="post.title">{{ post.title }}</span>
          </v-list-item-title>

          <v-list-item-subtitle class="post-meta">
            <span class="post-meta__author">{{ post.authorNickname ?? 'Unknown' }}</span>
            <span v-if="post.categoryName" class="post-meta__board">
              {{ post.categoryName }}
            </span>
            <time :datetime="post.createTime" class="post-meta__time">
              {{ dayjs(post.createTime).format('MMM D, YYYY') }}
            </time>
            <span v-for="tag in post.tags?.slice(0, TAG_LIMIT)" :key="tag" class="post-meta__tag">
              #{{ tag }}
            </span>
          </v-list-item-subtitle>

          <v-list-item-subtitle v-if="post.summary" class="summary">
            {{ post.summary }}
          </v-list-item-subtitle>
        </v-list-item-header>

        <div class="stats">
          <span class="stat" :title="`${post.viewCount} views`">
            <v-icon icon="mdi-eye-outline" size="14" class="stat__icon" />
            <span class="stat__value">{{ post.viewCount }}</span>
            <span class="sr-only">views</span>
          </span>
          <span class="stat" :title="`${post.commentCount} replies`">
            <v-icon icon="mdi-comment-outline" size="14" class="stat__icon" />
            <span class="stat__value">{{ post.commentCount }}</span>
            <span class="sr-only">replies</span>
          </span>
          <span class="stat" :title="`${post.likeCount} likes`">
            <v-icon icon="mdi-heart-outline" size="14" class="stat__icon" />
            <span class="stat__value">{{ post.likeCount }}</span>
            <span class="sr-only">likes</span>
          </span>
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
  padding: var(--md-sys-space-3) var(--md-sys-space-4);
  transition: background-color var(--md-sys-motion-short) var(--md-sys-motion-easing);
}

/* Vuetify already sets flex: 1 1 auto and min-width: 0 here; restated so the
   column is what shrinks when a long title is present. */
.post-header {
  min-width: 0;
}

.post-title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--md-sys-space-1) var(--md-sys-space-2);
  margin-bottom: 2px;
  @include overflow-wrap-anywhere;
}

/* MDesign.md section 4: the title leads, may run to two lines, and is not cut
   to a single truncated line purely for looks. The full value stays in the
   title attribute and on the detail page. */
.title {
  font-size: var(--md-sys-typescale-body-lg);
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: 0;
  min-width: 0;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--md-sys-space-1) var(--md-sys-space-2);
  font-size: var(--md-sys-typescale-label-sm);
  line-height: 1.5;
  color: $text-muted;
}

.post-meta__author {
  color: $text-primary;
  font-weight: 500;
}

/* Separators are pseudo-elements so the markup carries no stray punctuation
   for a screen reader to read aloud. */
.post-meta__board::before,
.post-meta__time::before,
.post-meta__tag::before {
  content: '·';
  margin-right: var(--md-sys-space-2);
  color: $border-strong;
}

.post-meta__tag {
  @include overflow-wrap-anywhere;
}

/* Status flags are a tinted pill plus a word: colour never carries the
   meaning on its own (MDesign.md section 12). */
.flag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex: none;
  padding: 1px var(--md-sys-space-2);
  border-radius: var(--md-sys-shape-corner-full);
  font-size: var(--md-sys-typescale-label-xs);
  font-weight: 600;
  line-height: 1.6;
  white-space: nowrap;
  background: $surface-sunken;
  color: $text-muted;
}

.flag__icon {
  flex: none;
}

.flag--pinned {
  background: $accent-soft;
  color: var(--md-sys-color-on-primary-container);
}

.flag--essential,
.flag--warning {
  background: var(--md-sys-color-warning-container);
  color: var(--md-sys-color-on-warning-container);
}

.flag--error {
  background: var(--md-sys-color-error-container);
  color: var(--md-sys-color-on-error-container);
}

.stats {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-4);
  flex: 0 0 auto;
  white-space: nowrap;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: var(--md-sys-typescale-label-sm);
  color: $text-muted;
  /* Tabular figures keep the column from twitching as counts change. */
  font-variant-numeric: tabular-nums;
}

.stat__icon {
  flex: none;
}

.summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-top: 2px;
  font-size: var(--md-sys-typescale-body-sm);
  line-height: 1.5;
  color: $text-muted;
  @include overflow-wrap-anywhere;
}

.post-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--md-sys-space-2);
  padding: var(--md-sys-space-3) var(--md-sys-space-4);
  border-bottom: 1px solid $border;
}

.skeleton--title {
  width: 72%;
  height: 16px;
}

.skeleton--meta {
  width: 45%;
  height: 12px;
}

/* MDesign.md section 5: on a phone the stats fold under the title and the row
   grows to fit its content rather than being clipped to a fixed height. */
@include below($bp-sm) {
  .post-row {
    flex-direction: column;
    align-items: stretch;
    gap: var(--md-sys-space-2);
    padding: var(--md-sys-space-3);
  }

  .stats {
    justify-content: flex-start;
    gap: var(--md-sys-space-5);
  }

  .title {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
