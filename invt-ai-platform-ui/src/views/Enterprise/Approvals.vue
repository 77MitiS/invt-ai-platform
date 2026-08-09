<template>
  <div class="approvals-shell">
    <article class="panel mc-surface-card">
      <header class="panel-head">
        <div>
          <h3 class="panel-title">{{ t('enterprise.approvals.queueTitle') }}</h3>
          <p class="panel-desc">{{ t('enterprise.approvals.queueDesc') }}</p>
        </div>
      </header>

      <div v-if="loading" class="loading-hint">{{ t('common.loading') }}</div>

      <ul v-else class="approval-list">
        <li v-for="a in items" :key="a.id" class="approval-item">
          <div class="approval-main">
            <div class="approval-head-row">
              <span class="approval-kind" :class="`kind-${a.kind}`">{{ kindLabel(a.kind) }}</span>
              <span class="approval-risk" :class="`risk-${a.risk}`">{{ riskLabel(a.risk) }}</span>
              <span class="approval-eta">{{ a.sla }}</span>
            </div>
            <div class="approval-title">{{ a.title }}</div>
            <div class="approval-meta">
              <span><strong>{{ t('enterprise.approvals.requester') }}</strong> {{ a.requester }}</span>
              <span class="dot"></span>
              <span><strong>{{ t('enterprise.approvals.target') }}</strong> {{ a.target }}</span>
              <span class="dot"></span>
              <span><strong>{{ t('enterprise.approvals.reason') }}</strong> {{ a.reason }}</span>
            </div>
            <div class="approval-evidence">
              <span class="ev-label">{{ t('enterprise.approvals.evidence') }}</span>
              <span>{{ a.evidence }}</span>
            </div>
          </div>
          <div class="approval-actions">
            <button class="btn-action" @click="handleResolve(a.id, 'approved')">{{ t('enterprise.approvals.btnApprove') }}</button>
            <button class="btn-action btn-action--ghost" @click="handleResolve(a.id, 'denied')">{{ t('enterprise.approvals.btnReject') }}</button>
          </div>
        </li>
      </ul>
    </article>

    <article class="panel mc-surface-card sla-panel">
      <header class="panel-head">
        <div>
          <h3 class="panel-title">{{ t('enterprise.approvals.slaTitle') }}</h3>
          <p class="panel-desc">{{ t('enterprise.approvals.slaDesc') }}</p>
        </div>
      </header>
      <div class="sla-grid">
        <div class="sla-card">
          <div class="sla-value">{{ items.length }}</div>
          <div class="sla-label">待处理</div>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { mcToast } from '@/composables/useMcToast'
import { securityApi } from '@/api/index'

const { t } = useI18n()

interface ApprovalItem {
  id: string; kind: string; risk: string; title: string
  requester: string; target: string; reason: string; evidence: string; sla: string
}

const items = ref<ApprovalItem[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res: any = await securityApi.listApprovals({})
    const list = res.data || []
    items.value = (Array.isArray(list) ? list : []).map((a: any) => ({
      id: String(a.id || a.pendingId || ''),
      kind: a.kind || 'tool',
      risk: a.severity === 'CRITICAL' || a.severity === 'HIGH' ? 'high' : a.severity === 'MEDIUM' ? 'medium' : 'low',
      title: a.title || a.toolName || '审批请求',
      requester: a.requester || a.username || '',
      target: a.target || a.toolName || '',
      reason: a.reason || a.trigger || '',
      evidence: a.evidence || '',
      sla: a.sla || '待处理',
    }))
  } catch { /* empty */ }
  loading.value = false
})

function kindLabel(k: string): string { return t(`enterprise.approvals.kind.${k}`) || k }
function riskLabel(r: string): string { return t(`enterprise.risk.${r}`) || r }

async function handleResolve(id: string, decision: string) {
  try {
    await securityApi.resolveApproval(id, decision)
    mcToast.success(decision === 'approved' ? '已批准' : '已驳回')
    items.value = items.value.filter(i => i.id !== id)
  } catch {
    mcToast.error('操作失败')
  }
}
</script>

<style scoped>
.approvals-shell { display: flex; flex-direction: column; gap: 16px; flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; padding-bottom: 16px; }
.panel { padding: 18px 20px; display: flex; flex-direction: column; gap: 14px; }
.panel-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.panel-title { font-size: 16px; font-weight: 700; color: var(--mc-text-primary); margin: 0 0 2px; }
.panel-desc { font-size: 12px; color: var(--mc-text-secondary); margin: 0; max-width: 480px; line-height: 1.5; }
.loading-hint { padding: 24px; text-align: center; font-size: 13px; color: var(--mc-text-tertiary); }
.approval-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.approval-item { display: flex; gap: 16px; padding: 14px; border: 1px solid var(--mc-border-light); border-radius: 10px; align-items: flex-start; }
.approval-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.approval-head-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.approval-kind { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px; }
.kind-contract { background: #ede9fe; color: #6d28d9; }
.kind-tool { background: #fef3c7; color: #b45309; }
.kind-transformation { background: #dbeafe; color: #1e40af; }
.kind-access { background: #d1fae5; color: #065f46; }
.approval-risk { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; padding: 2px 6px; border-radius: 4px; }
.risk-high { background: #fee2e2; color: #b91c1c; }
.risk-medium { background: #fef3c7; color: #b45309; }
.risk-low { background: #d1fae5; color: #065f46; }
.approval-eta { font-size: 11px; color: var(--mc-text-tertiary); margin-left: auto; }
.approval-title { font-size: 14px; font-weight: 600; color: var(--mc-text-primary); }
.approval-meta { font-size: 12px; color: var(--mc-text-secondary); display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.dot { width: 3px; height: 3px; border-radius: 50%; background: var(--mc-text-tertiary); flex-shrink: 0; }
.approval-evidence { font-size: 11px; color: var(--mc-text-tertiary); display: flex; gap: 6px; align-items: baseline; line-height: 1.5; }
.ev-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--mc-text-secondary); white-space: nowrap; }
.approval-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
.btn-action { padding: 7px 16px; border-radius: 8px; border: none; background: var(--mc-primary); color: #fff; font-size: 13px; font-weight: 500; cursor: pointer; }
.btn-action--ghost { background: transparent; color: var(--mc-text-secondary); border: 1px solid var(--mc-border-light); }
.btn-action:hover { opacity: 0.85; }
.sla-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 12px; }
.sla-card { padding: 14px; border-radius: 10px; border: 1px solid var(--mc-border-light); text-align: center; }
.sla-value { font-size: 24px; font-weight: 700; color: var(--mc-text-primary); }
.sla-label { font-size: 12px; color: var(--mc-text-secondary); margin-top: 2px; }
</style>
