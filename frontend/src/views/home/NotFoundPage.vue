<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <v-container class="page-container">
    <div class="not-found">
      <p class="not-found__code">404</p>
      <h1 class="page-title">This page does not exist</h1>
      <p class="muted not-found__detail">
        The link may be broken, or the content may have been removed.
      </p>
      <v-btn color="primary" class="mt-4" @click="router.push({ name: 'home' })">
        Back to the forum
      </v-btn>
    </div>
  </v-container>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--md-sys-space-2);
  /* Vertically centred in the remaining viewport without a fixed height. */
  min-height: 50vh;
  text-align: center;
}

.not-found__code {
  margin: 0;
  font-size: 3rem;
  font-weight: 700;
  line-height: 1;
  color: $border-strong;
}

.not-found__detail {
  max-width: 44ch;
  margin: 0;
}
</style>
