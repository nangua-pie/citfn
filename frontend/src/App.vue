<script setup lang="ts">
import { onMounted } from 'vue'
import { useThemeToggle } from '@/composables/useThemeToggle'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/layouts/AppHeader.vue'

const auth = useAuthStore()
const { themeName } = useThemeToggle()

onMounted(() => {
  // The route guard has already asked for this on the first navigation, so in
  // the common case this resolves immediately rather than issuing a second
  // request. It is kept here so a session is still restored if the app is ever
  // mounted without a navigation.
  void auth.ensureSession()
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
