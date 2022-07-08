<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/layouts/AppHeader.vue'

const auth = useAuthStore()

onMounted(() => {
  // Restores a session from a stored refresh token so a reload keeps the user
  // signed in without prompting.
  auth.restoreSession()
})
</script>

<template>
  <v-app>
    <AppHeader />
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>
