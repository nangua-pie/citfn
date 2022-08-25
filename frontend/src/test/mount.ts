import { createRouter, createMemoryHistory } from 'vue-router'
import { mount, type MountingOptions } from '@vue/test-utils'

import vuetify from '@/plugins/vuetify'
import type { Post } from '@/types'

/**
 * Mounting helper for component tests.
 *
 * Components are mounted against the app's real Vuetify plugin and a memory
 * router, so a `router-link` resolves the way it does in the browser instead
 * of hitting a stub. A fresh router per test keeps route state from leaking.
 */
export function mountWithApp(component: unknown, options: MountingOptions<Record<string, unknown>> = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/posts/:id', name: 'post-detail', component: { template: '<div />' } },
      { path: '/posts/new', name: 'post-create', component: { template: '<div />' } },
    ],
  })

  return mount(component as never, {
    ...options,
    global: {
      plugins: [vuetify, router],
      ...options.global,
    },
  })
}

/** A post with sensible defaults, so each test states only what it asserts. */
export function makePost(overrides: Partial<Post> = {}): Post {
  return {
    id: '1001',
    categoryId: '10',
    categoryName: 'General',
    userId: '2001',
    authorNickname: 'ada',
    authorAvatarUrl: null,
    title: 'A perfectly ordinary post title',
    summary: 'A short summary of the post.',
    coverUrl: null,
    viewCount: 12,
    commentCount: 3,
    likeCount: 5,
    collectCount: 1,
    reportCount: 0,
    isTop: 0,
    isEssential: 0,
    status: 1,
    liked: false,
    collected: false,
    tags: [],
    createTime: '2022-06-20 10:21:00',
    updateTime: '2022-06-20 10:21:00',
    ...overrides,
  }
}
