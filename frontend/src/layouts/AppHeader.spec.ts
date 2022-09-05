import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { VApp } from 'vuetify/components'

import AppHeader from '@/layouts/AppHeader.vue'
import { tokenStorage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useThemeToggle } from '@/composables/useThemeToggle'
import { mountWithApp } from '@/test/mount'

vi.mock('@/api/category', () => ({
  listCategories: vi.fn(async () => [
    { id: '10', name: 'General', description: null, icon: null, sortOrder: 1, postCount: 4, status: 1, createTime: '2022-06-20 10:21:00' },
    { id: '11', name: 'Announcements', description: null, icon: null, sortOrder: 2, postCount: 2, status: 1, createTime: '2022-06-20 10:21:00' },
  ]),
}))

vi.mock('@/utils/ws', () => ({
  imSocket: {
    connect: vi.fn(),
    disconnect: vi.fn(),
    onEvent: vi.fn(),
    onStateChange: vi.fn(),
    send: vi.fn(() => false),
  },
}))

// The header opens the socket as soon as a session exists, which then asks
// for the conversation list and the unread total.
vi.mock('@/api/message', () => ({
  listConversations: vi.fn(async () => []),
  unreadCount: vi.fn(async () => ({ unreadCount: 0 })),
}))

/**
 * The header inside a real v-app. App.vue owns the shell in production and the
 * theme class lands on that element, so the tests need it to observe a change.
 */
const Shell = defineComponent({
  components: { AppHeader, VApp },
  setup() {
    // Mirrors App.vue: the shell owns the theme and passes it down as a prop,
    // because the injected Vuetify theme is read only from inside the app.
    const { themeName } = useThemeToggle()
    return { themeName }
  },
  render() {
    return h(VApp, { theme: this.themeName }, { default: () => [h(AppHeader)] })
  },
})

/** Lets setup effects and the board fetch settle. */
async function settle() {
  await nextTick()
  await nextTick()
  await nextTick()
}

/** Finds a control by the accessible name it exposes. */
function buttonWithLabel(wrapper: any, label: string) {
  return wrapper
    .findAll('button')
    .find((b: any) => (b.attributes('aria-label') || '').toLowerCase().includes(label))
}

describe('AppHeader theme control', () => {
  beforeEach(() => {
    localStorage.clear()
    // The composable holds its state at module scope, so each test starts from
    // a known value rather than inheriting the previous one.
    useThemeToggle().set('light')
  })

  it('starts from the light theme and says so on the button', async () => {
    const wrapper = mountWithApp(Shell)
    await settle()
    expect(buttonWithLabel(wrapper, 'theme')?.attributes('aria-label')).toBe(
      'Switch to dark theme',
    )
  })

  it('flips the theme class on the app shell, not just its own state', async () => {
    const wrapper = mountWithApp(Shell, { attachTo: document.body })
    await settle()

    expect(document.querySelector('.v-application')?.className).toContain('v-theme--light')

    const toggle = buttonWithLabel(wrapper, 'theme')
    expect(toggle).toBeDefined()
    await toggle.trigger('click')
    await settle()

    expect(document.querySelector('.v-application')?.className).toContain('v-theme--dark')
    wrapper.unmount()
  })

  it('persists the choice so a reload does not flash the wrong theme', async () => {
    const wrapper = mountWithApp(Shell)
    await settle()
    await buttonWithLabel(wrapper, 'theme').trigger('click')
    await settle()
    expect(localStorage.getItem('forum-theme')).toBe('dark')
  })
})

describe('AppHeader board navigation', () => {
  beforeEach(() => {
    localStorage.clear()
    useThemeToggle().set('light')
  })

  it('keeps the drawer out of the app bar so it is not clipped away', async () => {
    const wrapper = mountWithApp(Shell, { attachTo: document.body })
    await settle()

    const drawer = document.querySelector('.v-navigation-drawer')
    expect(drawer).not.toBeNull()
    // The original bug nested the drawer inside the bar, where it collapsed.
    expect(drawer?.closest('.app-header')).toBeNull()
    wrapper.unmount()
  })

  it('opens the drawer when the menu button is pressed', async () => {
    const wrapper = mountWithApp(Shell, { attachTo: document.body })
    await settle()

    const before = document.querySelector('.v-navigation-drawer')?.className ?? ''
    await buttonWithLabel(wrapper, 'board menu').trigger('click')
    await settle()

    const after = document.querySelector('.v-navigation-drawer')?.className ?? ''
    // The exact modifier is an internal detail of this build; what matters is
    // that the drawer reacts to the button at all.
    expect(after).not.toBe(before)
    wrapper.unmount()
  })
})

describe('AppHeader saved posts entry', () => {
  beforeEach(() => {
    localStorage.clear()
    useThemeToggle().set('light')
  })

  /**
   * Seeds a session so the header renders its signed-in controls. The store is
   * created against the same Pinia instance the component mounts with.
   */
  function signIn() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.profile = {
      id: '5',
      username: 'ada',
      nickname: 'Ada',
      avatarUrl: null,
      bio: null,
      level: 1,
      points: 0,
      status: 1,
      createTime: '2022-08-27 10:00:00',
    }
    tokenStorage.set('access-token', 'refresh-token')
    return pinia
  }

  it('hides the saved posts entry while signed out', async () => {
    const wrapper = mountWithApp(Shell, { attachTo: document.body })
    await settle()

    const entries = wrapper.findAll('.v-list-item-title').filter((t) => t.text() === 'Saved posts')
    expect(entries).toHaveLength(0)
    wrapper.unmount()
  })

  it('puts saved posts in the account menu once signed in', async () => {
    const pinia = signIn()
    const wrapper = mountWithApp(Shell, { attachTo: document.body }, pinia)
    await settle()

    const accountButton = buttonWithLabel(wrapper, 'account menu')
    expect(accountButton).toBeDefined()
    await accountButton!.trigger('click')
    await settle()

    const entry = wrapper.findAll('.v-list-item-title').find((t) => t.text() === 'Saved posts')
    expect(entry).toBeDefined()
    wrapper.unmount()
  })

  it('puts saved posts in the drawer, where a phone can reach it', async () => {
    const pinia = signIn()
    const wrapper = mountWithApp(Shell, { attachTo: document.body }, pinia)
    await settle()

    const drawer = document.querySelector('.v-navigation-drawer')
    const entries = Array.from(drawer?.querySelectorAll('.v-list-item-title') ?? []).filter(
      (t) => t.textContent === 'Saved posts',
    )
    expect(entries.length).toBeGreaterThan(0)
    wrapper.unmount()
  })
})
