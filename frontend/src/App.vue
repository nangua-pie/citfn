<script setup lang="ts">
import { onMounted } from 'vue'
import { useThemeToggle } from '@/composables/useThemeToggle'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/layouts/AppHeader.vue'

const auth = useAuthStore()
const { themeName } = useThemeToggle()

onMounted(() => {
  // Restores a session from a stored refresh token so a reload keeps the user
  // signed in without prompting.
  auth.restoreSession()
})
</script>

<template>
  <!--
    `theme` is bound rather than set through useTheme(): in this Vuetify build
    the injected theme is read only inside the app, so the prop is the only
    route that actually moves the `.v-theme--*` class.
  -->
  <v-app :theme="themeName">
    <AppHeader />
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>
