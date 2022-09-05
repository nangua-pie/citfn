import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MyCollectionsPage from '@/views/post/MyCollectionsPage.vue'
import { listMyCollections } from '@/api/post'
import type { PageResult, Post } from '@/types'

vi.mock('@/api/post', () => ({
  listMyCollections: vi.fn(),
}))

const vuetify = createVuetify({ components, directives })

function post(id: string, title: string): Post {
  return {
    id,
    categoryId: '3',
    userId: '5',
    title,
    summary: null,
    coverUrl: null,
    viewCount: 0,
    commentCount: 0,
    likeCount: 0,
    collectCount: 0,
    reportCount: 0,
    isTop: 0,
    isEssential: 0,
    status: 1,
    liked: false,
    collected: true,
    tags: [],
    createTime: '2022-08-27 10:00:00',
    updateTime: '2022-08-27 10:00:00',
  }
}

function page(records: Post[], total: number): PageResult<Post> {
  return { records, total, page: 1, size: 20, pages: 1 }
}

function render() {
  return mount(MyCollectionsPage, {
    global: { plugins: [vuetify] },
  })
}

describe('MyCollectionsPage', () => {
  beforeEach(() => {
    vi.mocked(listMyCollections).mockReset()
  })

  it('renders the saved posts the API returns and asks for the first page', async () => {
    vi.mocked(listMyCollections).mockResolvedValue(page([post('1', 'Saved one'), post('2', 'Saved two')], 2))

    const wrapper = render()
    await flushPromises()

    expect(listMyCollections).toHaveBeenCalledWith(1, 20)
    expect(wrapper.text()).toContain('Saved one')
    expect(wrapper.text()).toContain('Saved two')
    expect(wrapper.text()).toContain('2 saved posts')
  })

  it('offers a way back to browsing when nothing is saved', async () => {
    vi.mocked(listMyCollections).mockResolvedValue(page([], 0))

    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('You have not saved any posts yet.')
    expect(wrapper.text()).toContain('Browse posts')
  })

  it('says how many are saved when there is exactly one', async () => {
    vi.mocked(listMyCollections).mockResolvedValue(page([post('1', 'Only one')], 1))

    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('1 saved post')
    expect(wrapper.text()).not.toContain('1 saved posts')
  })

  it('reports a failed load and retries on demand', async () => {
    vi.mocked(listMyCollections)
      .mockRejectedValueOnce(new Error('the list is unavailable'))
      .mockResolvedValue(page([post('1', 'Recovered')], 1))

    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('the list is unavailable')

    const retry = wrapper.findAll('button').find((b) => b.text().includes('Try again'))
    expect(retry).toBeDefined()
    await retry!.trigger('click')
    await flushPromises()

    expect(listMyCollections).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Recovered')
    expect(wrapper.text()).not.toContain('the list is unavailable')
  })
})
