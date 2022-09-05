<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { listMyCollections } from '@/api/post'
import PostList from '@/components/PostList.vue'
import type { Post } from '@/types'

const posts = ref<Post[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const total = ref(0)

const PAGE_SIZE = 20

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
const summary = computed(() => {
  if (loading.value || !total.value) {
    return ''
  }
  return total.value === 1 ? '1 saved post' : `${total.value} saved posts`
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listMyCollections(page.value, PAGE_SIZE)
    posts.value = result.records
    total.value = result.total
  } catch (err) {
    // The list is the whole page here, so a failed load empties it rather
    // than leaving stale rows next to an error message.
    error.value = err instanceof Error ? err.message : 'your saved posts could not be loaded'
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(page, () => void load())
</script>

<template>
  <v-container class="page-container">
    <header class="collections-header">
      <h1 class="page-title">Saved posts</h1>
      <p v-if="summary" class="muted collections-header__summary">{{ summary }}</p>
    </header>

    <div class="panel collections-card">
      <PostList
        :posts="posts"
        :loading="loading"
        :error="error"
        empty-text="You have not saved any posts yet."
        @retry="load"
      >
        <template #empty-action>
          <v-btn color="primary" size="small" class="mt-2" :to="{ name: 'home' }">
            Browse posts
          </v-btn>
        </template>
      </PostList>
    </div>

    <div v-if="total > PAGE_SIZE" class="d-flex justify-center mt-4">
      <v-pagination v-model="page" :length="pageCount" total-visible="7" />
    </div>
  </v-container>
</template>

<style scoped lang="scss">
.collections-header {
  margin-bottom: var(--md-sys-space-4);
}

.collections-header__summary {
  margin: var(--md-sys-space-1) 0 0;
  font-size: var(--md-sys-typescale-body-md);
}

/* The card is only as tall as its contents, so the list inside has to clip
   its own row borders rather than let the rounded corners cut them off. */
.collections-card {
  overflow: hidden;
}
</style>
