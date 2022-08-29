<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { getCategory } from '@/api/category'
import { listPosts } from '@/api/post'
import { useAuthStore } from '@/stores/auth'
import { routeId } from '@/utils/snowflake'
import PostList from '@/components/PostList.vue'
import type { Category, Post } from '@/types'

const route = useRoute()
const auth = useAuthStore()

const category = ref<Category | null>(null)
const posts = ref<Post[]>([])
const loading = ref(false)
const error = ref('')
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
    error.value = ''
    return
  }
  loading.value = true
  error.value = ''
  try {
    const result = await listPosts({ categoryId: id, page: page.value, size: PAGE_SIZE })
    posts.value = result.records
    total.value = result.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'the posts could not be loaded'
    posts.value = []
    total.value = 0
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
    <header class="board-header">
      <h1 class="page-title">{{ category?.name ?? 'Board' }}</h1>
      <p v-if="category?.description" class="muted board-header__description">
        {{ category.description }}
      </p>
    </header>

    <div class="feed-card panel">
      <PostList
        :posts="posts"
        :loading="loading"
        :error="error"
        empty-text="This board has no posts yet."
        @retry="load"
      >
        <template #empty-action>
          <v-btn
            v-if="auth.isAuthenticated"
            color="primary"
            size="small"
            class="mt-2"
            :to="{ name: 'post-create' }"
          >
            Write the first post
          </v-btn>
        </template>
      </PostList>
    </div>

    <div v-if="total > PAGE_SIZE" class="d-flex justify-center mt-4">
      <v-pagination v-model="page" :length="Math.ceil(total / PAGE_SIZE)" total-visible="7" />
    </div>
  </v-container>
</template>

<style scoped lang="scss">
.board-header {
  margin-bottom: var(--md-sys-space-4);
}

.board-header__description {
  margin: var(--md-sys-space-1) 0 0;
  font-size: var(--md-sys-typescale-body-md);
}

.feed-card {
  overflow: hidden;
}
</style>
