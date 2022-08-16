<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'

import { handleReport, listReports } from '@/api/report'
import { selectOptions } from '@/utils/select'
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

// beta.0's select reads `title` / `value` off each option; there is no
// `items` prop for object shapes in its published types, see utils/select.ts.
const statusChoices = selectOptions([
  { title: 'Pending', value: 0 },
  { title: 'Accepted', value: 1 },
  { title: 'Rejected', value: 2 },
  { title: 'All', value: undefined },
])

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
        :items="statusChoices"
        hide-details
        style="max-width: 160px"
        @change="load"
      />
    </div>

    <v-alert v-if="error" type="error" variant="outlined" class="mb-4">{{ error }}</v-alert>

    <v-card class="queue-card">
      <div v-if="loading" class="pa-6 text-center">
        <v-progress-circular indeterminate size="22" />
      </div>
      <p v-else-if="!reports.length" class="pa-6 text-center muted mb-0">The queue is empty.</p>

      <v-list v-else lines="three">
        <v-list-item v-for="report in reports" :key="report.id" class="report-row">
          <!--
            beta.0 does not stack bare title and subtitle children of a list
            item, and it has no v-list-item-action component at all, so this
            row used to render as one wide horizontal band.
          -->
          <v-list-item-header>
            <v-list-item-title class="d-flex align-center">
              <span class="chip mr-2">{{ report.targetType }}</span>
              <span class="text-truncate target">#{{ report.targetId }}</span>
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
          </v-list-item-header>
          <div v-if="report.status === 0" class="actions">
            <v-btn size="small" variant="text" @click="decide(report, false)">Dismiss</v-btn>
            <v-btn size="small" color="error" variant="text" @click="decide(report, true)">Remove</v-btn>
          </div>
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
  min-width: 0;
}

.actions {
  flex: 0 0 auto;
  white-space: nowrap;
}

.chip {
  font-size: 11px;
  height: 18px;
}

/* Target ids are 19 digit snowflakes and must not widen the row. */
.target {
  min-width: 0;
}

.detail {
  font-size: 13px;
  color: $text-muted;
}
</style>
