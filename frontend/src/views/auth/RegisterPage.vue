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
    <v-card class="auth-card panel">
      <div class="auth-body">
        <h1 class="page-title">Create an account</h1>
        <p class="muted auth-subtitle">Takes about a minute.</p>

        <v-form @submit.prevent="submit">
          <div class="auth-field">
            <v-text-field v-model="form.username" label="Username" autocomplete="username" required />
          </div>
          <div class="auth-field">
            <v-text-field v-model="form.nickname" label="Display name (optional)" />
          </div>
          <div class="auth-field">
            <v-text-field v-model="form.email" label="Email" type="email" autocomplete="email" required />
          </div>
          <!--
            The code field and its button are one control: the button keeps a
            full-width target on a phone rather than shrinking to a stub.
          -->
          <div class="code-row">
            <v-text-field
              v-model="form.code"
              label="Verification code"
              maxlength="6"
              inputmode="numeric"
              class="code-row__field"
            />
            <v-btn
              variant="outlined"
              size="default"
              :loading="sendingCode"
              :disabled="!form.email || sendingCode"
              class="code-row__button"
              @click="requestCode"
            >
              {{ codeSent ? 'Resend' : 'Send code' }}
            </v-btn>
          </div>
          <div class="auth-field">
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
          </div>
          <div class="auth-field">
            <v-text-field
              v-model="form.confirm"
              label="Confirm password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
            />
          </div>
          <v-alert v-if="error" type="error" variant="outlined" density="compact" class="mb-4">
            {{ error }}
          </v-alert>
          <v-btn
            type="submit"
            color="primary"
            block
            :loading="auth.loading"
            :disabled="auth.loading"
          >
            Create account
          </v-btn>
        </v-form>

        <p class="muted auth-footer">
          Already registered?
          <router-link :to="{ name: 'login' }">Sign in</router-link>
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
  max-width: 420px;
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

.code-row {
  display: flex;
  align-items: flex-start;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-2);
}

.code-row__field {
  flex: 1 1 auto;
  min-width: 0;
}

.code-row__button {
  /* Offsets the field's own top padding so the button lines up with the
     input rather than sitting a little high. */
  margin-top: 6px;
  white-space: nowrap;
}

/* Stacked on a phone: a half-width button next to a field is too small a
   target and clips the label. */
@include below($bp-sm) {
  .auth-container {
    padding-top: var(--md-sys-space-8);
  }

  .auth-body {
    padding: var(--md-sys-space-4);
  }

  .code-row {
    flex-direction: column;
    align-items: stretch;
  }

  .code-row__button {
    margin-top: 0;
  }
}
</style>
