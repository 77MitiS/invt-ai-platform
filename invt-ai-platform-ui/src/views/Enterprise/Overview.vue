<template>
  <div class="overview">
    <div class="metric-strip">
      <div class="metric" v-for="m in metrics" :key="m.key" :class="m.tone">
        <div class="metric-label">{{ m.label }}</div>
        <div class="metric-value">{{ m.value }}</div>
        <div class="metric-delta" :class="m.deltaTone">{{ m.delta }}</div>
      </div>
    </div>

    <div class="overview-grid">
      <article class="panel work-queue mc-surface-card">
        <header class="panel-head">
          <div>
            <h3 class="panel-title">{{ t('enterprise.overview.queueTitle') }}</h3>
            <p class="panel-desc">{{ t('enterprise.overview.queueDesc') }}</p>
          </div>
          <div class="filter-row">
            <button v-for="f in queueFilters" :key="f.key"
                    class="chip" :class="{ active: activeFilter === f.key }"
                    @click="activeFilter = f.key">
              {{ f.label }}
              <span class="chip-count">{{ f.count }}</span>
            </button>
          </div>
        </header>

        <ul class="queue-list">
          <li v-for="item in filteredQueue" :key="item.id" class="queue-item">
            <span class="risk-pill" :class="`risk-${item.risk}`">{{ riskLabel(item.risk) }}</span>
            <div class="queue-main">
              <div class="queue-title">{{ item.title }}</div>
              <div class="queue-meta">
                <span>{{ item.type }}</span>
                <span class="dot"></span>
                <span>{{ item.owner }}</span>
                <span class="dot"></span>
                <span>{{ item.eta }}</span>
              </div>
            </div>
            <div class="queue-status" :class="`status-${item.status}`">
              {{ statusLabel(item.status) }}
            </div>
          </li>
        </ul>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardApi } from '@/api/index'

const { t } = useI18n()

type QueueFilterKey = 'all' | 'high' | 'pending' | 'today'
const activeFilter = ref<QueueFilterKey>('all')

// 从 API 获取数据，带降级默认值
const metricData = ref<{ pending: number; risk: number; agents: number; tasks: number } | null>(null)
const queueItems = ref<QueueItem[]>([])

const metrics = computed(() => {
  const d = metricData.value
  return [
    { key: 'pending', label: t('enterprise.overview.metricPending'), value: String(d?.pending ?? 0), delta: null, deltaTone: 'up', tone: '' },
    { key: 'risk',    label: t('enterprise.overview.metricHighRisk'), value: String(d?.risk ?? 0), delta: null, deltaTone: 'up bad', tone: 'tone-warn' },
    { key: 'agents',  label: '活跃智能体', value: String(d?.agents ?? 0), delta: null, deltaTone: 'up', tone: '' },
    { key: 'tasks',   label: '已完成任务', value: String(d?.tasks ?? 0), delta: null, deltaTone: 'up', tone: 'tone-good' },
  ]
})

const queueFilters = computed<{ key: QueueFilterKey; label: string; count: number }[]>(() => {
  const q = queueItems.value
  return [
    { key: 'all', label: t('enterprise.overview.filterAll'), count: q.length },
    { key: 'high', label: t('enterprise.overview.filterHighRisk'), count: q.filter(i => i.risk === 'high').length },
    { key: 'pending', label: t('enterprise.overview.filterPending'), count: q.filter(i => i.status === 'pending_legal' || i.status === 'ai_reviewed').length },
    { key: 'today', label: t('enterprise.overview.filterToday'), count: q.filter(i => i.eta === '今天' || i.eta?.includes('小时')).length },
  ]
})

interface QueueItem {
  id: string
  title: string
  type: string
  owner: string
  eta: string
  risk: 'high' | 'medium' | 'low'
  status: 'ai_reviewed' | 'pending_legal' | 'approved' | 'signal'
}


// 从 API 加载数据
onMounted(async () => {
  try {
    const res: any = await dashboardApi.overview()
    const data = res?.data ?? res ?? {}
    metricData.value = {
      pending: data.pendingTasks ?? data.pending ?? 0,
      risk: data.highRiskTasks ?? data.risk ?? 0,
      agents: data.activeAgents ?? data.agents ?? 0,
      tasks: data.completedTasks ?? data.tasks ?? 0,
    }
    queueItems.value = (data.recentTasks ?? data.queue ?? []) as QueueItem[]
  } catch { /* 使用默认值 */ }
})

const filteredQueue = computed(() => {
  const q = queueItems.value
  switch (activeFilter.value) {
    case 'high': return q.filter(q => q.risk === 'high')
    case 'pending': return q.filter(q => q.status === 'pending_legal' || q.status === 'ai_reviewed')
    case 'today': return q.filter(q => q.eta === '今天' || q.eta?.includes('小时'))
    default: return q
  }
})

function riskLabel(r: 'high' | 'medium' | 'low'): string {
  return r === 'high' ? t('enterprise.risk.high')
       : r === 'medium' ? t('enterprise.risk.medium')
       : t('enterprise.risk.low')
}

function statusLabel(s: QueueItem['status']): string {
  switch (s) {
    case 'ai_reviewed': return t('enterprise.status.aiReviewed')
    case 'pending_legal': return t('enterprise.status.pendingLegal')
    case 'approved': return t('enterprise.status.approved')
    case 'signal': return t('enterprise.status.signal')
  }
}
</script>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 18px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  /* Inset the scrollbar so it doesn't hug the cards, and give the last
     card breathing room from the bottom edge of the body. */
  padding-right: 4px;
  padding-bottom: 16px;
}
/* Each panel inside an overflow-y flex column must opt out of the default
   flex-shrink behaviour, otherwise the browser will compress them to fit
   inside the visible body instead of letting them overflow into the
   scrollable area. Without this the pipeline panel renders at ~38px tall
   (= just its padding) and its content overflows below the visible card. */
.overview > .panel,
.overview > .metric-strip,
.overview > .overview-grid {
  flex-shrink: 0;
}

/* === metric strip === */
.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.metric {
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border-light);
  border-radius: 14px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: var(--mc-shadow-soft);
  position: relative;
  overflow: hidden;
}
.metric::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--mc-border);
}
.metric.tone-warn::before { background: #d97706; }
.metric.tone-good::before { background: #15803d; }
.metric-label { font-size: 12px; color: var(--mc-text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; }
.metric-value { font-size: 28px; font-weight: 700; color: var(--mc-text-primary); line-height: 1; }
.metric-delta { font-size: 12px; color: var(--mc-text-secondary); }
.metric-delta.up { color: #15803d; }
.metric-delta.up.bad { color: #b91c1c; }

/* === panels === */
.overview-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
.panel {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.panel-title { font-size: 16px; font-weight: 700; color: var(--mc-text-primary); margin: 0 0 2px; }
.panel-desc { font-size: 12px; color: var(--mc-text-secondary); margin: 0; max-width: 480px; line-height: 1.5; }

/* === filter chips === */
.filter-row { display: flex; gap: 6px; flex-wrap: wrap; }
.chip {
  border: 1px solid var(--mc-border-light);
  background: var(--mc-bg-elevated);
  color: var(--mc-text-secondary);
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
}
.chip:hover { color: var(--mc-text-primary); border-color: var(--mc-border); }
.chip.active { background: var(--mc-primary-bg); color: var(--mc-primary-hover); border-color: var(--mc-primary); }
.chip-count { font-size: 11px; opacity: 0.7; }

/* === queue list === */
.queue-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.queue-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--mc-border-light);
  cursor: pointer;
  transition: background 0.15s;
}
.queue-item:hover { background: var(--mc-bg-muted); margin: 0 -8px; padding: 12px 8px; border-radius: 8px; border-bottom-color: transparent; }
.queue-item:last-child { border-bottom: none; }

.risk-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  min-width: 44px;
  text-align: center;
}
.risk-high { background: #fee2e2; color: #b91c1c; }
.risk-medium { background: #fef3c7; color: #b45309; }
.risk-low { background: #dcfce7; color: #15803d; }

.queue-main { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.queue-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.queue-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--mc-text-tertiary);
}
.dot { width: 3px; height: 3px; background: currentColor; border-radius: 50%; opacity: 0.5; }

.queue-status {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 9px;
  border-radius: 6px;
  white-space: nowrap;
}
.status-ai_reviewed { background: var(--mc-primary-bg); color: var(--mc-primary-hover); }
.status-pending_legal { background: #fef3c7; color: #b45309; }
.status-approved { background: #dcfce7; color: #15803d; }
.status-signal { background: var(--mc-accent-soft); color: var(--mc-accent); }

@media (max-width: 1100px) {
  .overview-grid { grid-template-columns: 1fr; }
  .metric-strip { grid-template-columns: repeat(2, 1fr); }
}
</style>
