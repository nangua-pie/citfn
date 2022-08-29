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
    <v-card class="auth-card panel">
      <div class="auth-body">
        <h1 class="page-title">Sign in</h1>
        <p class="muted auth-subtitle">Welcome back.</p>

        <v-form @submit.prevent="submit">
          <div class="auth-field">
            <v-text-field
              v-model="username"
              label="Username or email"
              autocomplete="username"
              required
            />
          </div>
          <div class="auth-field">
            <v-text-field
              v-model="password"
              label="Password"
              :type="showPassword ? 'text' : 'password'"
              :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
              autocomplete="current-password"
              @click:append-inner="showPassword = !showPassword"
            />
          </div>
          <v-alert v-if="error" type="error" variant="outlined" density="compact" class="mb-4">
            {{ error }}
          </v-alert>
          <v-btn type="submit" color="primary" block :loading="auth.loading" :disabled="auth.loading">
            Sign in
          </v-btn>
        </v-form>

        <p class="muted auth-footer">
          No account yet?
          <router-link :to="{ name: 'register' }">Create one</router-link>
        </p>
      </div>
    </v-card>
  </v-container>
</template>

<style scoped lang="scss">
.auth-container {
  display: flex;
  justify-content: center;
  padding: var(--md-sys-space-12) var(--md-sys-layout-gutter);
}

.auth-card {
  width: 100%;
  max-width: 380px;
}

.auth-body {
  padding: var(--md-sys-space-6);
}

.auth-subtitle {
  margin: 0 0 var(--md-sys-space-5);
  font-size: var(--md-sys-typescale-body-md);
}

.auth-field {
  margin-bottom: var(--md-sys-space-2);
}

.auth-footer {
  margin: var(--md-sys-space-5) 0 0;
  font-size: var(--md-sys-typescale-body-md);
}

@include below($bp-sm) {
  .auth-container {
    padding-top: var(--md-sys-space-8);
  }

  .auth-body {
    padding: var(--md-sys-space-4);
  }
}
</style>
