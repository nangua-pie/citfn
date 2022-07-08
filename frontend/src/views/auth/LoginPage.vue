<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const showPassword = ref(false)

async function submit() {
  error.value = ''
  try {
    await auth.login({ username: username.value.trim(), password: password.value })
    const redirect = route.query.redirect
    await router.push(typeof redirect === 'string' ? redirect : { name: 'home' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'sign in failed'
  }
}
</script>

<template>
  <v-container class="auth-container">
    <v-card class="auth-card pa-6">
      <h1 class="page-title mb-1">Sign in</h1>
      <p class="muted text-body-2 mb-4">Welcome back.</p>

      <v-form @submit.prevent="submit">
        <v-text-field v-model="username" label="Username or email" autocomplete="username" />
        <v-text-field
          v-model="password"
          label="Password"
          :type="showPassword ? 'text' : 'password'"
          :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
          autocomplete="current-password"
          @click:append-inner="showPassword = !showPassword"
        />
        <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">{{ error }}</v-alert>
        <v-btn type="submit" color="primary" block :loading="auth.loading">Sign in</v-btn>
      </v-form>

      <p class="muted text-body-2 mt-4 mb-0">
        No account yet?
        <router-link :to="{ name: 'register' }">Create one</router-link>
      </p>
    </v-card>
  </v-container>
</template>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  padding-top: 48px;
}

.auth-card {
  width: 100%;
  max-width: 380px;
  border: 1px solid $border;
}
</style>
