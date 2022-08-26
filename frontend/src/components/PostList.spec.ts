import { describe, expect, it } from 'vitest'

import PostList from '@/components/PostList.vue'
import { makePost, mountWithApp } from '@/test/mount'

describe('PostList content', () => {
  it('shows the title, author and board of a post', () => {
    const wrapper = mountWithApp(PostList, { props: { posts: [makePost()] } })
    expect(wrapper.text()).toContain('A perfectly ordinary post title')
    expect(wrapper.text()).toContain('ada')
    expect(wrapper.text()).toContain('General')
  })

  it('keeps the full title available even where the row clamps it', () => {
    const title = 'A really quite long title that has to survive a two line clamp'
    const wrapper = mountWithApp(PostList, { props: { posts: [makePost({ title })] } })
    expect(wrapper.find('.title').attributes('title')).toBe(title)
  })

  it('spells out pinned and essential instead of showing a bare icon', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [makePost({ isTop: 1, isEssential: 1 })] },
    })
    // The word, not the colour, is what carries the state.
    expect(wrapper.text()).toContain('Pinned')
    expect(wrapper.text()).toContain('Essential')
  })

  it('labels a post awaiting moderation and shows no flag when published', () => {
    const pending = mountWithApp(PostList, { props: { posts: [makePost({ status: 2 })] } })
    expect(pending.text()).toContain('Pending review')

    const published = mountWithApp(PostList, { props: { posts: [makePost({ status: 1 })] } })
    expect(published.text()).not.toContain('Pending review')
    expect(published.find('.flag').exists()).toBe(false)
  })

  it('caps the tag count so a heavily tagged post cannot crowd out the title', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [makePost({ tags: ['vue', 'css', 'a11y', 'extra', 'ignored'] })] },
    })
    expect(wrapper.text()).toContain('#vue')
    expect(wrapper.text()).toContain('#a11y')
    expect(wrapper.text()).not.toContain('#ignored')
  })

  it('names each statistic for a screen reader, not just an icon', () => {
    const wrapper = mountWithApp(PostList, { props: { posts: [makePost()] } })
    const labels = wrapper.findAll('.stat').map((stat) => stat.text())
    expect(labels).toHaveLength(3)
    expect(wrapper.text()).toContain('views')
    expect(wrapper.text()).toContain('replies')
    expect(wrapper.text()).toContain('likes')
  })

  it('emits a datetime attribute the browser will accept', () => {
    // The API sends `MM/dd/yyyy HH:mm:ss`, which is not a valid value for
    // the datetime attribute, so it must be converted rather than passed on.
    const wrapper = mountWithApp(PostList, {
      props: { posts: [makePost({ createTime: '06/20/2022 10:21:00' })] },
    })
    const datetime = wrapper.find('time').attributes('datetime')
    expect(datetime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)
  })

  it('omits the datetime attribute rather than emitting an invalid one', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [makePost({ createTime: 'whenever' })] },
    })
    expect(wrapper.find('time').attributes('datetime')).toBeUndefined()
  })
})

describe('PostList states', () => {
  it('shows placeholders while the first page loads, not an empty message', () => {
    const wrapper = mountWithApp(PostList, { props: { posts: [], loading: true } })
    expect(wrapper.findAll('.post-skeleton').length).toBeGreaterThan(0)
    expect(wrapper.text()).not.toContain('Nothing here yet')
  })

  it('keeps the existing rows visible while a later page loads', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [makePost()], loading: true },
    })
    expect(wrapper.text()).toContain('A perfectly ordinary post title')
    expect(wrapper.findAll('.post-skeleton')).toHaveLength(0)
  })

  it('explains a failure and offers a retry that emits once', async () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [], error: 'the server said no' },
    })
    expect(wrapper.text()).toContain('Could not load posts')
    expect(wrapper.text()).toContain('the server said no')

    const retry = wrapper.findAll('button').find((b) => b.text().includes('Try again'))
    expect(retry).toBeDefined()
    await retry!.trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('prefers the error state over an empty one', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [], error: 'boom', emptyText: 'nothing here' },
    })
    expect(wrapper.text()).not.toContain('nothing here')
  })

  it('shows the empty message and the call to action when there is nothing to list', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [], emptyText: 'This board has no posts yet.' },
      slots: { 'empty-action': '<button>Write the first post</button>' },
    })
    expect(wrapper.text()).toContain('This board has no posts yet.')
    expect(wrapper.text()).toContain('Write the first post')
  })

  it('treats a whitespace-only error as no error', () => {
    const wrapper = mountWithApp(PostList, {
      props: { posts: [], error: '   ', emptyText: 'Nothing here.' },
    })
    expect(wrapper.text()).toContain('Nothing here.')
  })
})
