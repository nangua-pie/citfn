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
    <div class="queue-toolbar">
      <h1 class="page-title">Reported content</h1>
      <v-spacer />
      <v-select
        v-model="status"
        :items="statusChoices"
        hide-details
        aria-label="Filter reports by status"
        class="queue-toolbar__filter"
        @change="load"
      />
    </div>

    <div v-if="error" class="state-message panel mb-4" role="alert">
      <v-icon icon="mdi-alert-circle-outline" size="28" />
      <span class="state-message__title">Could not load the queue</span>
      <span>{{ error }}</span>
      <v-btn size="small" variant="outlined" class="mt-2" @click="load">
        <v-icon icon="mdi-refresh" size="16" class="mr-1" />
        Try again
      </v-btn>
    </div>

    <div class="queue-card panel">
      <template v-if="loading && !reports.length">
        <div v-for="row in 3" :key="row" class="report-skeleton" aria-hidden="true">
          <div class="skeleton skeleton--line" />
          <div class="skeleton skeleton--short" />
        </div>
        <p class="sr-only" role="status">Loading reports</p>
      </template>

      <p v-else-if="!reports.length" class="state-message">The queue is empty.</p>

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
            <v-btn size="small" variant="outlined" @click="decide(report, false)">Dismiss</v-btn>
            <v-btn size="small" color="error" @click="decide(report, true)">Remove</v-btn>
          </div>
        </v-list-item>
      </v-list>
    </div>
  </v-container>
</template>

<style scoped lang="scss">
.queue-toolbar {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  margin-bottom: var(--md-sys-space-4);
}

/* The filter must not stretch with the toolbar. */
.queue-toolbar__filter {
  max-width: 180px;
}

.queue-card {
  overflow: hidden;
}

.report-row {
  border-bottom: 1px solid $border;
  min-width: 0;
  padding: var(--md-sys-space-3) var(--md-sys-space-4);
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--md-sys-space-2);
  flex: 0 0 auto;
  white-space: nowrap;
}

.chip {
  flex: none;
  height: 20px;
  padding: 0 var(--md-sys-space-2);
  border-radius: var(--md-sys-shape-corner-full);
  background: var(--md-sys-color-surface-container);
  color: var(--md-sys-color-on-surface-variant);
  font-size: var(--md-sys-typescale-label-xs);
  font-weight: 600;
  line-height: 20px;
  text-transform: lowercase;
}

/* Target ids are 19 digit snowflakes and must not widen the row. */
.target {
  min-width: 0;
  @include overflow-wrap-anywhere;
}

.detail {
  font-size: var(--md-sys-typescale-body-sm);
  color: $text-muted;
}

.report-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--md-sys-space-2);
  padding: var(--md-sys-space-3) var(--md-sys-space-4);
  border-bottom: 1px solid $border;
}

.skeleton--line {
  width: 100%;
  height: 14px;
}

.skeleton--short {
  width: 30%;
  height: 12px;
}

@include below($bp-sm) {
  .queue-toolbar {
    flex-wrap: wrap;
  }

  /* The heading takes its own line; the filter spans the width beneath it. */
  .queue-toolbar > .page-title {
    flex: 1 0 100%;
  }

  .queue-toolbar__filter {
    max-width: none;
  }

  .report-row {
    flex-direction: column;
    align-items: stretch;
  }

  .actions {
    margin-top: var(--md-sys-space-2);
  }
}
</style>
