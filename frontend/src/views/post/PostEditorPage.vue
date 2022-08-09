<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { listCategories } from '@/api/category'
import { createPost, getPost, updatePost } from '@/api/post'
import { renderMarkdown } from '@/utils/markdown'
import { routeId } from '@/utils/snowflake'
import { selectOptions } from '@/utils/select'
import type { Category } from '@/types'

const route = useRoute()
const router = useRouter()

const categories = ref<Category[]>([])
// The board is a plain string rather than `string | null` because beta.0's
// select `modelValue` prop does not accept null; an empty string is the
// "nothing chosen yet" value and is already treated as unset by submit().
const form = ref({ categoryId: '', title: '', content: '', tags: '' })
const preview = ref(false)
const submitting = ref(false)
const error = ref('')

// The raw route string: a post id is a 64 bit snowflake, so `Number()` would
// round it and the edit request would fail against the wrong row.
const postId = computed(() => routeId(route.params.id))
const isEdit = computed(() => postId.value !== null)

/**
 * Vuetify 3.0.0-beta.0's select reads `title` and `value` off each item and
 * has no `item-title` / `item-value` props, so the board list is reshaped
 * here instead of in the template.
 */
const boardChoices = computed(() => selectOptions(categories.value.map((c) => ({ title: c.name, value: c.id }))))

/** Sanitised before rendering, exactly as on the detail page. */
const renderedPreview = computed(() => renderMarkdown(form.value.content))

function parseTags(raw: string): string[] {
  const seen = new Set<string>()
  for (const part of raw.split(/[,\s]+/)) {
    const tag = part.trim().toLowerCase()
    if (tag && !seen.has(tag)) {
      seen.add(tag)
    }
    if (seen.size === 5) break
  }
  return [...seen]
}

async function submit() {
  if (!form.value.categoryId) {
    error.value = 'choose a board first'
    return
  }
  const existingId = postId.value
  submitting.value = true
  error.value = ''
  try {
    const payload = {
      categoryId: form.value.categoryId,
      title: form.value.title,
      content: form.value.content,
      tags: parseTags(form.value.tags),
    }
    const saved = existingId ? await updatePost(existingId, payload) : await createPost(payload)
    await router.push({ name: 'post-detail', params: { id: saved.id } })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'the post could not be saved'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories()
  const existingId = postId.value
  if (existingId) {
    const existing = await getPost(existingId)
    form.value = {
      categoryId: existing.categoryId,
      title: existing.title,
      content: existing.contentMd ?? '',
      tags: (existing.tags ?? []).join(', '),
    }
  } else {
    form.value.categoryId = categories.value[0]?.id ?? ''
  }
})
</script>

<template>
  <v-container class="page-container">
    <div class="d-flex align-center mb-4">
      <h1 class="page-title">{{ isEdit ? 'Edit post' : 'New post' }}</h1>
      <v-spacer />
      <v-btn size="small" variant="text" class="mr-2" @click="preview = !preview">
        {{ preview ? 'Write' : 'Preview' }}
      </v-btn>
      <v-btn color="primary" size="small" :loading="submitting" @click="submit">
        {{ isEdit ? 'Save changes' : 'Publish' }}
      </v-btn>
    </div>

    <v-card class="editor-card pa-5">
      <v-select
        v-model="form.categoryId"
        :items="boardChoices"
        label="Board"
      />

      <v-text-field v-model="form.title" label="Title" maxlength="128" counter />
      <v-textarea v-model="form.content" label="Content (Markdown)" rows="14" class="mt-2" />
      <v-text-field
        v-model="form.tags"
        label="Tags"
        hint="Up to five, separated by commas or spaces"
        persistent-hint
        class="mt-2"
      />

      <v-alert v-if="error" type="error" variant="outlined" density="compact" class="mt-3">{{ error }}</v-alert>

      <template v-if="preview">
        <v-divider class="my-4" />
        <h2 class="section-title">Preview</h2>
        <div class="markdown-body" v-html="renderedPreview" />
      </template>
    </v-card>
  </v-container>
</template>

<style scoped>
.editor-card {
  border: 1px solid $border;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
