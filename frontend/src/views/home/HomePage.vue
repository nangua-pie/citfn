<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

import { listCategories } from '@/api/category'
import { listPosts } from '@/api/post'
import type { PostQuery } from '@/api/post'
import PostList from '@/components/PostList.vue'
import type { Category, Post } from '@/types'

const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const activeCategoryId = ref<number | null>(null)

const PAGE_SIZE = 20

async function load() {
  loading.value = true
  try {
    const query: PostQuery = {
      page: page.value,
      size: PAGE_SIZE,
      categoryId: activeCategoryId.value ?? undefined,
      keyword: keyword.value.trim() || undefined,
    }
    const result = await listPosts(query)
    posts.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number | null) {
  activeCategoryId.value = activeCategoryId.value === id ? null : id
  page.value = 1
  void load()
}

function submitSearch() {
  page.value = 1
  void load()
}

const pageCount = () => Math.max(1, Math.ceil(total.value / PAGE_SIZE))

onMounted(async () => {
  try {
    categories.value = await listCategories()
  } catch {
    categories.value = []
  }
  await load()
})

watch(page, () => void load())
</script>

<template>
  <v-container class="page-container">
    <div class="home-layout">
      <aside class="boards">
        <h2 class="section-title">Boards</h2>
        <v-list density="compact" nav>
          <v-list-item active-class="board--active" @click="selectCategory(null)">
            <v-list-item-title class="text-body-2">All posts</v-list-item-title>
          </v-list-item>
          <v-list-item
            v-for="category in categories"
            :key="category.id"
            @click="selectCategory(category.id)"
          >
            <v-list-item-title class="text-body-2">{{ category.name }}</v-list-item-title>
            <v-list-item-subtitle class="text-caption">{{ category.postCount }}</v-list-item-subtitle>
          </v-list-item>
        </v-list>
      </aside>

      <main class="feed">
        <div class="d-flex align-center mb-3">
          <h1 class="page-title">Posts</h1>
          <v-spacer />
          <v-text-field
            v-model="keyword"
            density="compact"
            hide-details
            placeholder="Search titles and summaries"
            style="max-width: 280px"
            @keyup.enter="submitSearch"
          />
          <v-btn class="ml-2" size="small" @click="submitSearch">Search</v-btn>
        </div>

        <v-card class="feed-card">
          <PostList :posts="posts" :loading="loading" empty-text="No posts match this filter." />
        </v-card>

        <div v-if="total > PAGE_SIZE" class="d-flex justify-center mt-4">
          <v-pagination v-model="page" :length="pageCount()" total-visible="7" />
        </div>
      </main>
    </div>
  </v-container>
</template>

<style scoped>
.home-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  align-items: start;
}

.section-title {
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: $text-muted;
  margin-bottom: 8px;
}

.boards {
  background: $surface;
  border: 1px solid $border;
  border-radius: $radius-md;
  padding: 12px;
}

.feed-card {
  border: 1px solid $border;
  overflow: hidden;
}

@media (max-width: 840px) {
  .home-layout {
    grid-template-columns: 1fr;
  }
}
</style>
