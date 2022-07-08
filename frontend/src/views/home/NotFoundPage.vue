<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <v-container class="page-container text-center py-16">
    <p class="muted mb-2">404</p>
    <h1 class="page-title mb-3">This page does not exist</h1>
    <p class="muted mb-6">The link may be broken, or the content may have been removed.</p>
    <v-btn color="primary" @click="router.push({ name: 'home' })">Back to the forum</v-btn>
  </v-container>
</template>
