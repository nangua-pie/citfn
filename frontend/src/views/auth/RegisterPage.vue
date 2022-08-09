<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { sendCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const form = ref({ username: '', nickname: '', email: '', code: '', password: '', confirm: '' })
const error = ref('')
const codeSent = ref(false)
const sendingCode = ref(false)
const showPassword = ref(false)

async function requestCode() {
  error.value = ''
  sendingCode.value = true
  try {
    await sendCode(form.value.email.trim(), 'REGISTER')
    codeSent.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'the code could not be sent'
  } finally {
    sendingCode.value = false
  }
}

async function submit() {
  error.value = ''
  if (form.value.password !== form.value.confirm) {
    error.value = 'the two passwords do not match'
    return
  }
  try {
    await auth.register({
      username: form.value.username.trim(),
      password: form.value.password,
      email: form.value.email.trim(),
      code: form.value.code.trim(),
      nickname: form.value.nickname.trim() || undefined,
    })
    await router.push({ name: 'home' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'registration failed'
  }
}
</script>

<template>
  <v-container class="auth-container">
    <v-card class="auth-card pa-6">
      <h1 class="page-title mb-1">Create an account</h1>
      <p class="muted text-body-2 mb-4">Takes about a minute.</p>

      <v-form @submit.prevent="submit">
        <v-text-field v-model="form.username" label="Username" autocomplete="username" />
        <v-text-field v-model="form.nickname" label="Display name (optional)" />
        <v-text-field v-model="form.email" label="Email" type="email" autocomplete="email" />
        <div class="d-flex align-start">
          <v-text-field v-model="form.code" label="Verification code" maxlength="6" class="flex-grow-1" />
          <v-btn class="ml-2 mt-1" size="default" :loading="sendingCode" :disabled="!form.email" @click="requestCode">
            {{ codeSent ? 'Resend' : 'Send code' }}
          </v-btn>
        </div>
        <v-text-field
          v-model="form.password"
          label="Password"
          :type="showPassword ? 'text' : 'password'"
          :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
          autocomplete="new-password"
          hint="At least 8 characters, with a letter and a digit"
          persistent-hint
          @click:append-inner="showPassword = !showPassword"
        />
        <v-text-field
          v-model="form.confirm"
          label="Confirm password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
        />
        <v-alert v-if="error" type="error" variant="outlined" density="compact" class="mb-4">{{ error }}</v-alert>
        <v-btn type="submit" color="primary" block :loading="auth.loading">Create account</v-btn>
      </v-form>

      <p class="muted text-body-2 mt-4 mb-0">
        Already registered?
        <router-link :to="{ name: 'login' }">Sign in</router-link>
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
  max-width: 420px;
  border: 1px solid $border;
}
</style>
