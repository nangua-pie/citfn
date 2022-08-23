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
  // Guards against a second submit while the first is still in flight; the
  // button's loading state hides the problem but does not prevent it.
  if (submitting.value) {
    return
  }
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
    <div class="editor">
      <div class="editor-toolbar">
        <h1 class="page-title">{{ isEdit ? 'Edit post' : 'New post' }}</h1>
        <v-spacer />
        <!-- The one high-emphasis action in this group. -->
        <v-btn color="primary" size="small" :loading="submitting" @click="submit">
          {{ isEdit ? 'Save changes' : 'Publish' }}
        </v-btn>
      </div>

    <v-card class="editor-card panel">
        <div class="editor-field">
          <v-select v-model="form.categoryId" :items="boardChoices" label="Board" />
        </div>

        <div class="editor-field">
          <v-text-field v-model="form.title" label="Title" maxlength="128" counter />
        </div>

        <div class="editor-field">
          <div class="editor-field__header">
            <h2 class="section-title">Content</h2>
            <v-btn size="small" variant="outlined" @click="preview = !preview">
              {{ preview ? 'Back to editing' : 'Preview' }}
            </v-btn>
          </div>

          <!--
            Editor and preview occupy the same slot, so toggling between them
            does not move the fields below.
          -->
          <v-textarea
            v-show="!preview"
            v-model="form.content"
            label="Body (Markdown)"
            rows="14"
            hide-details
          />
          <div v-if="preview" class="editor-preview panel-sunken">
            <div v-if="form.content.trim()" class="markdown-body" v-html="renderedPreview" />
            <p v-else class="state-message">Nothing to preview yet.</p>
          </div>
        </div>

        <div class="editor-field">
          <v-text-field
            v-model="form.tags"
            label="Tags"
            hint="Up to five, separated by commas or spaces"
            persistent-hint
          />
        </div>

        <v-alert
          v-if="error"
          type="error"
          variant="outlined"
          density="compact"
          class="mt-3"
        >
          {{ error }}
        </v-alert>
    </v-card>
    </div>
  </v-container>
</template>

<style scoped>
.editor-card {
  padding: var(--md-sys-space-5);
}

.editor {
  max-width: 860px;
  margin: 0 auto;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-4);
}

.editor-field {
  margin-bottom: var(--md-sys-space-4);
}

.editor-field:last-of-type {
  margin-bottom: 0;
}

.editor-field__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-2);
}

.editor-preview {
  padding: var(--md-sys-space-4);
  min-height: 320px;
}

@include below($bp-sm) {
  .editor-card {
    padding: var(--md-sys-space-4);
  }
}
</style>
