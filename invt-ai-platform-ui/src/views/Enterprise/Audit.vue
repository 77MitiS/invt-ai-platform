<template>
  <div class="audit-shell">
    <article class="panel mc-surface-card">
      <header class="panel-head">
        <div>
          <h3 class="panel-title">{{ t('enterprise.audit.title') }}</h3>
          <p class="panel-desc">{{ t('enterprise.audit.desc') }}</p>
        </div>
      </header>

      <div v-if="loading" class="loading-hint">{{ t('common.loading') }}</div>

      <ol v-else class="audit-trail">
        <li v-for="e in events" :key="e.id" class="audit-event">
          <div class="event-time">
            <div class="time-stamp">{{ e.time }}</div>
            <div class="time-date">{{ e.date }}</div>
          </div>
          <div class="event-marker" :class="`marker-${e.kind}`"></div>
          <div class="event-body">
            <div class="event-head">
              <span class="event-kind" :class="`kind-${e.kind}`">{{ t(`enterprise.audit.kind.${e.kind}`) }}</span>
              <span class="event-actor">{{ e.actor }}</span>
              <span v-if="e.system" class="event-system">{{ e.system }}</span>
            </div>
            <div class="event-summary">{{ e.summary }}</div>
            <div v-if="e.evidence" class="event-evidence">
              <span class="ev-label">{{ t('enterprise.audit.evidence') }}</span>
              <span>{{ e.evidence }}</span>
            </div>
          </div>
        </li>
      </ol>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { auditApi } from '@/api/index'

const { t } = useI18n()

interface AuditEvent {
  id: string; time: string; date: string; kind: string
  actor: string; system?: string; summary: string; evidence?: string
}

const events = ref<AuditEvent[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res: any = await auditApi.listEvents({ page: 1, size: 50 })
    const records = (res.data as any)?.records || res.data || []
    if (!Array.isArray(records)) return
    events.value = records.map((e: any) => ({
      id: String(e.id),
      time: new Date(e.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      date: isToday(e.createTime) ? '今天' : isYesterday(e.createTime) ? '昨天' : formatDate(e.createTime),
      kind: mapKind(e.action),
      actor: e.username || '系统',
      system: e.resourceName || undefined,
      summary: e.resourceName ? `${actionLabel(e.action)} ${e.resourceName}` : String(e.detailJson || ''),
      evidence: e.detailJson ? String(e.detailJson).substring(0, 120) : undefined,
    }))
  } catch { /* use empty */ }
  loading.value = false
})

function isToday(d: string) { const n = new Date(); const dd = new Date(d); return dd.toDateString() === n.toDateString() }
function isYesterday(d: string) { const n = new Date(); n.setDate(n.getDate()-1); return new Date(d).toDateString() === n.toDateString() }
function formatDate(d: string) { return new Date(d).toLocaleDateString('zh-CN') }

function mapKind(a: string): string {
  if (!a) return 'review'
  const u = a.toUpperCase()
  if (u.includes('APPROVE') || u.includes('GRANT')) return 'approve'
  if (u.includes('REJECT') || u.includes('DENY')) return 'reject'
  if (u.includes('UPDATE') || u.includes('MODIFY')) return 'modify'
  if (u.includes('LOGIN') || u.includes('LOGOUT')) return 'access'
  return 'tool'
}
function actionLabel(a: string): string {
  return t(`enterprise.audit.action.${a}`) || a
}
</script>

<style scoped>
.audit-shell { display: flex; flex-direction: column; flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; padding-bottom: 16px; }
.audit-shell > .panel { flex-shrink: 0; }
.panel { padding: 18px 20px; display: flex; flex-direction: column; gap: 14px; }
.panel-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.panel-title { font-size: 16px; font-weight: 700; color: var(--mc-text-primary); margin: 0 0 2px; }
.panel-desc { font-size: 12px; color: var(--mc-text-secondary); margin: 0; max-width: 480px; line-height: 1.5; }
.loading-hint { padding: 24px; text-align: center; font-size: 13px; color: var(--mc-text-tertiary); }
.audit-trail { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.audit-event { display: grid; grid-template-columns: 72px 16px 1fr; gap: 14px; padding: 14px 0; border-bottom: 1px solid var(--mc-border-light); position: relative; }
.audit-event:last-child { border-bottom: none; }
.audit-event::before { content: ''; position: absolute; left: 79px; top: 0; bottom: 0; width: 2px; background: var(--mc-border-light); }
.audit-event:first-child::before { top: 14px; }
.audit-event:last-child::before { bottom: calc(100% - 24px); }
.event-time { text-align: right; padding-top: 2px; }
.time-stamp { font-size: 13px; font-weight: 600; color: var(--mc-text-primary); font-family: var(--mc-font-mono); }
.time-date { font-size: 11px; color: var(--mc-text-tertiary); }
.event-marker { width: 12px; height: 12px; border-radius: 50%; margin-top: 6px; border: 3px solid var(--mc-bg); position: relative; z-index: 1; }
.marker-review { background: var(--mc-text-secondary); }
.marker-approve { background: #15803d; }
.marker-reject { background: #b91c1c; }
.marker-tool { background: #f59e0b; }
.marker-access { background: #1e40af; }
.marker-modify { background: var(--mc-primary); }
.event-body { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.event-head { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.event-kind { font-size: 10px; font-weight: 700; padding: 3px 8px; border-radius: 4px; text-transform: uppercase; letter-spacing: 0.05em; }
.kind-review { background: var(--mc-bg-sunken); color: var(--mc-text-secondary); }
.kind-approve { background: #dcfce7; color: #15803d; }
.kind-reject { background: #fee2e2; color: #b91c1c; }
.kind-tool { background: #fef3c7; color: #b45309; }
.kind-access { background: #dbeafe; color: #1e40af; }
.kind-modify { background: var(--mc-primary-bg); color: var(--mc-primary-hover); }
.event-actor { font-size: 13px; font-weight: 600; color: var(--mc-text-primary); }
.event-system { font-size: 12px; color: var(--mc-text-tertiary); padding: 2px 8px; background: var(--mc-bg-muted); border-radius: 4px; }
.event-summary { font-size: 13px; color: var(--mc-text-primary); line-height: 1.55; }
.event-evidence { font-size: 11px; color: var(--mc-text-tertiary); display: flex; gap: 6px; align-items: baseline; line-height: 1.5; flex-wrap: wrap; }
.ev-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--mc-text-secondary); white-space: nowrap; }
</style>
