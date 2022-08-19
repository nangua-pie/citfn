<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

import { listCategories } from '@/api/category'
import { listPosts } from '@/api/post'
import type { PostQuery } from '@/api/post'
import { useAuthStore } from '@/stores/auth'
import PostList from '@/components/PostList.vue'
import type { Category, Post } from '@/types'

const auth = useAuthStore()

const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const error = ref('')
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const activeCategoryId = ref<string | null>(null)

const PAGE_SIZE = 20

async function load() {
  loading.value = true
  error.value = ''
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
  } catch (err) {
    // Kept on screen rather than swallowed: the retry affordance in PostList
    // needs something to explain.
    error.value = err instanceof Error ? err.message : 'the posts could not be loaded'
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function selectCategory(id: string | null) {
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
      <!--
        The feed leads the source order so the h1 precedes the board rail's
        h2; the rail is placed into the left column with grid instead, and
        drops below the feed on a narrow screen.
      -->
      <main class="feed">
        <div class="feed-toolbar">
          <h1 class="page-title">Posts</h1>
          <v-text-field
            v-model="keyword"
            density="compact"
            hide-details
            aria-label="Search post titles and summaries"
            placeholder="Search titles and summaries"
            class="search"
            @keyup.enter="submitSearch"
          />
          <v-btn size="small" @click="submitSearch">Search</v-btn>
        </div>

        <div class="feed-card panel">
          <PostList
            :posts="posts"
            :loading="loading"
            :error="error"
            empty-text="No posts match this filter."
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
          <v-pagination v-model="page" :length="pageCount()" total-visible="7" />
        </div>
      </main>

      <aside class="boards" aria-label="Boards">
        <h2 class="section-title boards__title">Boards</h2>
        <v-list density="compact" nav>
          <v-list-item
            active-class="board--active"
            :active="activeCategoryId === null"
            @click="selectCategory(null)"
          >
            <v-list-item-title class="text-body-2">All posts</v-list-item-title>
          </v-list-item>
          <v-list-item
            v-for="category in categories"
            :key="category.id"
            active-class="board--active"
            :active="activeCategoryId === category.id"
            @click="selectCategory(category.id)"
          >
            <!--
              beta.0 lays bare title and subtitle children of a list item out
              side by side, which ran the post count into the board name. The
              header is what stacks them.
            -->
            <v-list-item-header>
              <v-list-item-title class="text-body-2">{{ category.name }}</v-list-item-title>
              <v-list-item-subtitle class="text-caption">{{ category.postCount }}</v-list-item-subtitle>
            </v-list-item-header>
          </v-list-item>
        </v-list>
      </aside>
    </div>
  </v-container>
</template>

<style scoped>
.home-layout {
  display: grid;
  /*
   * minmax(0, 1fr) rather than a bare 1fr: a 1fr track keeps its automatic
   * minimum size, so one long unbreakable title could stretch the feed column
   * past the viewport and the page would clip instead of truncating.
   */
  grid-template-columns: 220px minmax(0, 1fr);
  grid-template-areas: 'boards feed';
  gap: var(--md-sys-space-6);
  align-items: start;
}

.boards,
.feed {
  min-width: 0;
}

.feed {
  grid-area: feed;
}

.boards {
  grid-area: boards;
  position: sticky;
  top: calc(#{$header-height} + var(--md-sys-space-4));
  background: $surface;
  border: 1px solid $border;
  border-radius: $radius-md;
  padding: var(--md-sys-space-3);
}

.boards__title {
  margin-bottom: var(--md-sys-space-2);
  padding: 0 var(--md-sys-space-3);
  font-size: var(--md-sys-typescale-label-sm);
  color: $text-muted;
}

.feed-card {
  overflow: hidden;
}

/* A fixed max-width keeps the search box from stretching with the feed. */
.feed-toolbar {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-3);
}

/* The title absorbs the free space, pushing search to the trailing edge. */
.feed-toolbar > .page-title {
  flex: 1 1 auto;
}

.search {
  max-width: 280px;
}

:deep(.board--active) {
  background: $accent-soft;
  color: $text-primary;
  font-weight: 600;
}

/* Below the tablet range the rail is no longer worth its own column: it moves
   under the feed and stops being sticky. */
@include below($bp-md) {
  .home-layout {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas:
      'feed'
      'boards';
  }

  .boards {
    position: static;
  }
}

/* On a phone the search takes the full width and drops below the title. */
@include below($bp-sm) {
  .feed-toolbar {
    flex-wrap: wrap;
  }

  /* The title keeps its own line; the field and button share the next one. */
  .feed-toolbar > .page-title {
    flex: 1 0 100%;
  }

  .search {
    flex: 1 1 auto;
    max-width: none;
  }
}
</style>
