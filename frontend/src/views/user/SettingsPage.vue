<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { uploadImage } from '@/api/file'
import { updateMyProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const form = ref({ nickname: '', bio: '', avatarUrl: '' })
const password = ref({ current: '', next: '', confirm: '' })
const savingProfile = ref(false)
const savingPassword = ref(false)
const uploading = ref(false)
const profileMessage = ref('')
const profileError = ref('')
const passwordMessage = ref('')
const passwordError = ref('')
const avatarInput = ref<HTMLInputElement | null>(null)

async function saveProfile() {
  savingProfile.value = true
  profileMessage.value = ''
  profileError.value = ''
  try {
    auth.profile = await updateMyProfile({
      nickname: form.value.nickname.trim() || undefined,
      bio: form.value.bio.trim() || undefined,
      avatarUrl: form.value.avatarUrl.trim() || undefined,
    })
    profileMessage.value = 'Profile updated.'
  } catch (err) {
    profileError.value = err instanceof Error ? err.message : 'the profile could not be saved'
  } finally {
    savingProfile.value = false
  }
}

async function pickAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  uploading.value = true
  profileError.value = ''
  try {
    const uploaded = await uploadImage(file)
    form.value.avatarUrl = uploaded.url
  } catch (err) {
    profileError.value = err instanceof Error ? err.message : 'the avatar could not be uploaded'
  } finally {
    uploading.value = false
  }
}

async function savePassword() {
  passwordError.value = ''
  passwordMessage.value = ''
  if (password.value.next !== password.value.confirm) {
    passwordError.value = 'the two passwords do not match'
    return
  }
  savingPassword.value = true
  try {
    await auth.updatePassword(password.value.current, password.value.next)
    // Every session was revoked server side, so return to the signed out state.
    await router.push({ name: 'login' })
  } catch (err) {
    passwordError.value = err instanceof Error ? err.message : 'the password could not be changed'
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  form.value = {
    nickname: auth.profile?.nickname ?? '',
    bio: auth.profile?.bio ?? '',
    avatarUrl: auth.profile?.avatarUrl ?? '',
  }
})
</script>

<template>
  <v-container class="page-container">
    <h1 class="page-title mb-4">Settings</h1>

    <div class="settings-layout">
      <v-card class="settings-card pa-5">
        <h2 class="section-title">Profile</h2>

        <div class="d-flex align-center mb-4">
          <v-avatar size="64" class="mr-4">
            <v-img v-if="form.avatarUrl" :src="form.avatarUrl" />
          </v-avatar>
          <div>
            <input ref="avatarInput" type="file" accept="image/*" class="d-none" @change="pickAvatar" />
            <v-btn size="small" :loading="uploading" @click="avatarInput?.click()">
              Upload avatar
            </v-btn>
            <div class="muted text-caption mt-1">PNG, JPG, GIF or WebP, up to 5 MB.</div>
          </div>
        </div>

        <v-text-field v-model="form.nickname" label="Display name" maxlength="64" />
        <v-textarea v-model="form.bio" label="Bio" rows="3" maxlength="255" />

        <v-alert v-if="profileError" type="error" variant="outlined" density="compact" class="mb-3">
          {{ profileError }}
        </v-alert>
        <v-alert v-if="profileMessage" type="success" variant="outlined" density="compact" class="mb-3">
          {{ profileMessage }}
        </v-alert>

        <v-btn color="primary" size="small" :loading="savingProfile" @click="saveProfile">Save profile</v-btn>
      </v-card>

      <v-card class="settings-card pa-5">
        <h2 class="section-title">Password</h2>
        <p class="muted text-body-2">Changing your password signs out every device.</p>

        <v-text-field v-model="password.current" label="Current password" type="password" autocomplete="current-password" />
        <v-text-field
          v-model="password.next"
          label="New password"
          type="password"
          autocomplete="new-password"
          hint="At least 8 characters, with a letter and a digit"
          persistent-hint
        />
        <v-text-field
          v-model="password.confirm"
          label="Confirm new password"
          type="password"
          autocomplete="new-password"
        />

        <v-alert v-if="passwordError" type="error" variant="outlined" density="compact" class="mb-3">
          {{ passwordError }}
        </v-alert>

        <v-btn color="primary" size="small" :loading="savingPassword" @click="savePassword">Change password</v-btn>
      </v-card>
    </div>
  </v-container>
</template>

<style scoped>
.settings-layout {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
  align-items: start;
}

.settings-card {
  border: 1px solid $border;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
