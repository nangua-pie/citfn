<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'

import { handleReport, listReports } from '@/api/report'
import type { ReportRecord } from '@/api/report'

const reports = ref<ReportRecord[]>([])
const loading = ref(false)
const status = ref<number | undefined>(0)
const error = ref('')

const statusLabels: Record<number, string> = {
  0: 'Pending',
  1: 'Accepted',
  2: 'Rejected',
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listReports(status.value, 1, 50)
    reports.value = result.records
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'the queue could not be loaded'
  } finally {
    loading.value = false
  }
}

async function decide(report: ReportRecord, accept: boolean) {
  await handleReport(report.id, accept, accept ? 'Removed after review' : 'No violation found')
  await load()
}

onMounted(load)
</script>

<template>
  <v-container class="page-container">
    <div class="d-flex align-center mb-4">
      <h1 class="page-title">Reported content</h1>
      <v-spacer />
      <v-select
        v-model="status"
        :items="[
          { title: 'Pending', value: 0 },
          { title: 'Accepted', value: 1 },
          { title: 'Rejected', value: 2 },
          { title: 'All', value: undefined },
        ]"
        density="compact"
        hide-details
        style="max-width: 160px"
        @change="load"
      />
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>

    <v-card class="queue-card">
      <div v-if="loading" class="pa-6 text-center">
        <v-progress-circular indeterminate size="22" />
      </div>
      <p v-else-if="!reports.length" class="pa-6 text-center muted mb-0">The queue is empty.</p>

      <v-list v-else lines="three">
        <v-list-item v-for="report in reports" :key="report.id" class="report-row">
          <v-list-item-title class="d-flex align-center">
            <span class="chip mr-2">{{ report.targetType }}</span>
            <span class="text-truncate">#{{ report.targetId }}</span>
            <v-spacer />
            <v-chip size="small" :color="report.status === 0 ? 'warning' : report.status === 1 ? 'error' : 'success'">
              {{ statusLabels[report.status] ?? report.status }}
            </v-chip>
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ report.reasonCode }}
            <span v-if="report.reporterNickname"> · by {{ report.reporterNickname }}</span>
            · {{ dayjs(report.createTime).format('MMM D, HH:mm') }}
          </v-list-item-subtitle>
          <v-list-item-subtitle v-if="report.reasonDetail" class="detail">
            {{ report.reasonDetail }}
          </v-list-item-subtitle>
          <v-list-item-action v-if="report.status === 0" class="actions">
            <v-btn size="small" variant="text" @click="decide(report, false)">Dismiss</v-btn>
            <v-btn size="small" color="error" variant="text" @click="decide(report, true)">Remove</v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
  </v-container>
</template>

<style scoped>
.queue-card {
  border: 1px solid $border;
}

.report-row {
  border-bottom: 1px solid $border;
}

.chip {
  font-size: 11px;
  height: 18px;
}

.detail {
  font-size: 13px;
  color: $text-muted;
}
</style>
