<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { getCategory } from '@/api/category'
import { listPosts } from '@/api/post'
import { routeId } from '@/utils/snowflake'
import PostList from '@/components/PostList.vue'
import type { Category, Post } from '@/types'

const route = useRoute()

const category = ref<Category | null>(null)
const posts = ref<Post[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const PAGE_SIZE = 20
// The raw route string, never a number: a category id is a 64 bit snowflake
// and `Number()` would round it away.
const categoryId = () => routeId(route.params.id)

async function load() {
  const id = categoryId()
  if (!id) {
    posts.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await listPosts({ categoryId: id, page: page.value, size: PAGE_SIZE })
    posts.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/** Loads the board header and the first page together. */
async function loadBoard() {
  const id = categoryId()
  if (!id) {
    category.value = null
    posts.value = []
    total.value = 0
    return
  }
  try {
    category.value = await getCategory(id)
  } catch {
    category.value = null
  }
  await load()
}

onMounted(loadBoard)

watch(
  () => route.params.id,
  async () => {
    page.value = 1
    await loadBoard()
  },
)

watch(page, () => void load())
</script>

<template>
  <v-container class="page-container">
    <h1 class="page-title mb-1">{{ category?.name ?? 'Board' }}</h1>
    <p class="muted text-body-2 mb-4">{{ category?.description ?? '' }}</p>

    <v-card class="feed-card">
      <PostList :posts="posts" :loading="loading" empty-text="This board has no posts yet." />
    </v-card>

    <div v-if="total > PAGE_SIZE" class="d-flex justify-center mt-4">
      <v-pagination v-model="page" :length="Math.ceil(total / PAGE_SIZE)" total-visible="7" />
    </div>
  </v-container>
</template>

<style scoped>
.feed-card {
  border: 1px solid $border;
  overflow: hidden;
}
</style>
