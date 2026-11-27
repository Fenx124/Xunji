<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheck,
  VideoPause,
  VideoPlay,
  RefreshLeft,
  CloseBold,
} from '@element-plus/icons-vue'
import { listNotes } from '@/api/note'
import { listSchedules } from '@/api/schedule'
import {
  createFocusRecord,
  getFocusTodayStats,
  listFocusRecords,
} from '@/api/focus'
import type { NoteListItemVO } from '@/types/note'
import type { ScheduleVO } from '@/types/schedule'
import type { FocusRecordVO, FocusTodayStatsVO } from '@/types/focus'
import { formatDateTime, nowDateTimeString } from '@/utils/date'

type TimerPhase = 'idle' | 'running' | 'paused'

const PRESETS = [
  { label: '25 分钟 (标准番茄)', minutes: 25 },
  { label: '15 分钟 (轻量专注)', minutes: 15 },
  { label: '45 分钟 (深度沉浸)', minutes: 45 },
] as const

const router = useRouter()

const presetIndex = ref(0)
const customMinutes = ref(25)
const useCustom = ref(false)

const plannedMinutes = computed(() => {
  if (useCustom.value && customMinutes.value != null) {
    return Math.min(180, Math.max(1, customMinutes.value))
  }
  return PRESETS[presetIndex.value]?.minutes ?? 25
})

const totalSeconds = computed(() => plannedMinutes.value * 60)
const remainingSeconds = ref(25 * 60)
const phase = ref<TimerPhase>('idle')
const startedAt = ref<string | null>(null)

const noteId = ref<number | undefined>()
const scheduleId = ref<number | undefined>()
const remark = ref('')

const noteOptions = ref<NoteListItemVO[]>([])
const scheduleOptions = ref<ScheduleVO[]>([])

const todayStats = ref<FocusTodayStatsVO>({
  completedCount: 0,
  totalMinutes: 0,
  totalSessions: 0,
})

const historyLoading = ref(false)
const records = ref<FocusRecordVO[]>([])
const historyTotal = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dateRange = ref<[string, string] | null>(null)

let tickTimer: ReturnType<typeof setInterval> | null = null
let submitting = false

const bindingsDisabled = computed(() => phase.value === 'running')

const phaseLabel = computed(() => {
  if (phase.value === 'running') return '专注中'
  if (phase.value === 'paused') return '已暂停'
  return '待开始'
})

const phaseTagType = computed(() => {
  if (phase.value === 'running') return 'success'
  if (phase.value === 'paused') return 'warning'
  return 'info'
})

const displayTime = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const ringProgress = computed(() => {
  if (totalSeconds.value <= 0) return 0
  return 1 - remainingSeconds.value / totalSeconds.value
})

const RING_R = 88
const RING_C = 2 * Math.PI * RING_R

const ringDashoffset = computed(() => RING_C * (1 - ringProgress.value))

function syncRemainingFromPlan() {
  if (phase.value === 'idle') {
    remainingSeconds.value = plannedMinutes.value * 60
  }
}

watch([plannedMinutes, useCustom, presetIndex], syncRemainingFromPlan)

function clearTick() {
  if (tickTimer) {
    clearInterval(tickTimer)
    tickTimer = null
  }
}

function startTick() {
  clearTick()
  tickTimer = setInterval(() => {
    if (remainingSeconds.value <= 1) {
      remainingSeconds.value = 0
      clearTick()
      void finishSession('COMPLETED', true)
      return
    }
    remainingSeconds.value -= 1
  }, 1000)
}

function elapsedMinutes(): number {
  const elapsed = totalSeconds.value - remainingSeconds.value
  return Math.max(1, Math.ceil(elapsed / 60))
}

async function finishSession(status: 'COMPLETED' | 'ABORTED', fromAuto = false) {
  if (submitting) return
  if (!startedAt.value) return
  submitting = true
  clearTick()
  const actual =
    status === 'COMPLETED' && fromAuto && remainingSeconds.value === 0
      ? plannedMinutes.value
      : elapsedMinutes()
  const payload = {
    noteId: noteId.value,
    scheduleId: scheduleId.value,
    plannedMinutes: plannedMinutes.value,
    actualMinutes: actual,
    status,
    startedAt: startedAt.value,
    endedAt: nowDateTimeString(),
    remark: remark.value.trim() || undefined,
  }
  try {
    await createFocusRecord(payload)
    if (status === 'COMPLETED') {
      ElMessage.success({ message: '🎉 专注完成，干得漂亮！', duration: 3000 })
    } else {
      ElMessage.info('已记录本次放弃')
    }
    resetTimer()
    await Promise.all([loadTodayStats(), loadHistory()])
  } finally {
    submitting = false
  }
}

function resetTimer() {
  clearTick()
  phase.value = 'idle'
  startedAt.value = null
  remainingSeconds.value = plannedMinutes.value * 60
}

function handleStart() {
  if (phase.value === 'idle') {
    startedAt.value = nowDateTimeString()
    phase.value = 'running'
    startTick()
    return
  }
  if (phase.value === 'paused') {
    phase.value = 'running'
    startTick()
  }
}

function handlePause() {
  if (phase.value !== 'running') return
  phase.value = 'paused'
  clearTick()
}

async function handleCompleteEarly() {
  if (!startedAt.value) return
  await finishSession('COMPLETED')
}

async function handleAbort() {
  if (!startedAt.value) {
    resetTimer()
    return
  }
  await finishSession('ABORTED')
}

async function loadBindings() {
  const [notesRes, schedRes] = await Promise.all([
    listNotes({ pageNum: 1, pageSize: 100 }),
    listSchedules({ status: 0 }),
  ])
  noteOptions.value = notesRes.data.records
  scheduleOptions.value = schedRes.data
}

async function loadTodayStats() {
  const res = await getFocusTodayStats()
  todayStats.value = res.data
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const params: {
      pageNum: number
      pageSize: number
      startDate?: string
      endDate?: string
    } = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (dateRange.value?.[0]) params.startDate = dateRange.value[0]
    if (dateRange.value?.[1]) params.endDate = dateRange.value[1]
    const res = await listFocusRecords(params)
    records.value = res.data.records
    historyTotal.value = res.data.total
  } finally {
    historyLoading.value = false
  }
}

function onDateRangeChange() {
  pageNum.value = 1
  void loadHistory()
}

function goNote(id: number) {
  void router.push({ path: '/notes', query: { noteId: String(id) } })
}

function statusTagType(status: string) {
  return status === 'COMPLETED' ? 'success' : 'danger'
}

onMounted(async () => {
  syncRemainingFromPlan()
  await Promise.all([loadBindings(), loadTodayStats(), loadHistory()])
})

onBeforeUnmount(clearTick)
</script>

<template>
  <div class="focus-page page-shell">
    <div class="focus-layout">
      <el-card class="timer-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span>专注工作室</span>
            <el-tag :type="phaseTagType" effect="dark" round>{{ phaseLabel }}</el-tag>
          </div>
        </template>

        <div class="preset-row">
          <el-radio-group v-model="useCustom" size="small">
            <el-radio-button :value="false">预设</el-radio-button>
            <el-radio-button :value="true">自定义</el-radio-button>
          </el-radio-group>
          <template v-if="!useCustom">
            <el-select
              v-model="presetIndex"
              :disabled="bindingsDisabled"
              style="width: 220px; margin-left: 12px"
            >
              <el-option
                v-for="(p, i) in PRESETS"
                :key="p.minutes"
                :label="p.label"
                :value="i"
              />
            </el-select>
          </template>
          <template v-else>
            <el-input-number
              v-model="customMinutes"
              :min="1"
              :max="180"
              :disabled="bindingsDisabled"
              style="margin-left: 12px; width: 140px"
            />
            <span class="unit-hint">分钟 (1–180)</span>
          </template>
        </div>

        <div class="ring-wrap">
          <svg class="ring-svg" viewBox="0 0 200 200" aria-hidden="true">
            <circle class="ring-bg" cx="100" cy="100" :r="RING_R" />
            <circle
              class="ring-fg"
              cx="100"
              cy="100"
              :r="RING_R"
              :stroke-dasharray="RING_C"
              :stroke-dashoffset="ringDashoffset"
            />
          </svg>
          <div class="ring-center">
            <div class="time-display">{{ displayTime }}</div>
            <div class="plan-hint">计划 {{ plannedMinutes }} 分钟</div>
          </div>
        </div>

        <el-form label-width="88px" class="bind-form">
          <el-form-item label="关联笔记">
            <el-select
              v-model="noteId"
              clearable
              filterable
              placeholder="可选"
              :disabled="bindingsDisabled"
              style="width: 100%"
            >
              <el-option
                v-for="n in noteOptions"
                :key="n.id"
                :label="n.title"
                :value="n.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关联日程">
            <el-select
              v-model="scheduleId"
              clearable
              filterable
              placeholder="可选（未完成）"
              :disabled="bindingsDisabled"
              style="width: 100%"
            >
              <el-option
                v-for="s in scheduleOptions"
                :key="s.id"
                :label="s.title"
                :value="s.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="remark"
              :disabled="bindingsDisabled"
              maxlength="200"
              show-word-limit
              placeholder="本次专注目标或感受"
            />
          </el-form-item>
        </el-form>

        <div class="controls">
          <el-button
            v-if="phase === 'idle' || phase === 'paused'"
            type="primary"
            size="large"
            :icon="VideoPlay"
            @click="handleStart"
          >
            {{ phase === 'paused' ? '继续专注' : '开始专注' }}
          </el-button>
          <el-button
            v-if="phase === 'running'"
            size="large"
            :icon="VideoPause"
            @click="handlePause"
          >
            暂停
          </el-button>
          <el-button
            v-if="phase !== 'idle'"
            type="success"
            size="large"
            :icon="CircleCheck"
            :disabled="!startedAt"
            @click="handleCompleteEarly"
          >
            提前完成
          </el-button>
          <el-button
            v-if="phase !== 'idle'"
            type="danger"
            plain
            size="large"
            :icon="CloseBold"
            @click="handleAbort"
          >
            放弃本次
          </el-button>
          <el-button
            v-if="phase === 'idle'"
            text
            :icon="RefreshLeft"
            @click="syncRemainingFromPlan"
          >
            重置倒计时
          </el-button>
        </div>
      </el-card>

      <aside class="side-panel">
        <el-card shadow="never" class="stats-card">
          <template #header>今日数据</template>
          <div class="kpi-strip">
            <div class="kpi">
              <div class="kpi-val">{{ todayStats.completedCount }}</div>
              <div class="kpi-label">完成番茄</div>
            </div>
            <div class="kpi-sep" />
            <div class="kpi">
              <div class="kpi-val">{{ todayStats.totalMinutes }}</div>
              <div class="kpi-label">专注分钟</div>
            </div>
            <div class="kpi-sep" />
            <div class="kpi">
              <div class="kpi-val">{{ todayStats.totalSessions }}</div>
              <div class="kpi-label">专注次数</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="history-card">
          <template #header>
            <div class="card-head">
              <span>专注历史</span>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始"
                end-placeholder="结束"
                style="width: 240px"
                @change="onDateRangeChange"
              />
            </div>
          </template>
          <el-table
            v-loading="historyLoading"
            :data="records"
            stripe
            size="small"
            empty-text="暂无专注记录"
          >
            <el-table-column label="开始" min-width="140">
              <template #default="{ row }">{{ formatDateTime(row.startedAt) }}</template>
            </el-table-column>
            <el-table-column label="结束" min-width="140">
              <template #default="{ row }">{{ formatDateTime(row.endedAt) }}</template>
            </el-table-column>
            <el-table-column label="计划/实际" width="100" align="center">
              <template #default="{ row }">
                {{ row.plannedMinutes }} / {{ row.actualMinutes }} 分
              </template>
            </el-table-column>
            <el-table-column label="状态" width="88" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.status)">
                  {{ row.status === 'COMPLETED' ? '完成' : '放弃' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="笔记" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link
                  v-if="row.noteId"
                  type="primary"
                  @click="goNote(row.noteId)"
                >
                  {{ row.noteTitle || `#${row.noteId}` }}
                </el-link>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="scheduleTitle"
              label="日程"
              min-width="100"
              show-overflow-tooltip
            />
            <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="historyTotal"
              :page-sizes="[10, 20]"
              layout="total, sizes, prev, pager, next"
              small
              @current-change="loadHistory"
              @size-change="
                () => {
                  pageNum = 1
                  loadHistory()
                }
              "
            />
          </div>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.focus-layout {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 1.1fr);
  gap: 20px;
  align-items: start;
}

@media (max-width: 960px) {
  .focus-layout {
    grid-template-columns: 1fr;
  }
}

.timer-card,
.stats-card,
.history-card {
  border-radius: 12px;
  border: 1px solid var(--ssp-border);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.preset-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.unit-hint {
  margin-left: 8px;
  font-size: 0.85rem;
  color: var(--ssp-muted);
}

.ring-wrap {
  position: relative;
  width: 220px;
  height: 220px;
  margin: 12px auto 20px;
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: #e5e7eb;
  stroke-width: 10;
}

.ring-fg {
  fill: none;
  stroke: #10b981;
  stroke-width: 10;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.35s ease;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.time-display {
  font-size: 2.5rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
}

.plan-hint {
  margin-top: 4px;
  font-size: 0.9rem;
  color: var(--ssp-muted);
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 8px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kpi-strip {
  display: flex;
  align-items: stretch;
  justify-content: space-around;
}

.kpi {
  text-align: center;
  flex: 1;
}

.kpi-val {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--ssp-primary);
}

.kpi-label {
  font-size: 0.85rem;
  color: var(--ssp-muted);
  margin-top: 4px;
}

.kpi-sep {
  width: 1px;
  background: var(--ssp-border);
  margin: 4px 0;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.muted {
  color: var(--ssp-muted);
}

.history-card :deep(.el-card__body) {
  padding-top: 8px;
}
</style>