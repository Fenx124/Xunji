<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bell, MagicStick, Plus } from '@element-plus/icons-vue'
import AiScheduleParseModal from './components/AiScheduleParseModal.vue'
import type { AiParsedScheduleVO } from '@/types/ai'
import ScheduleCalendar from './components/ScheduleCalendar.vue'
import ScheduleQuadrant from './components/ScheduleQuadrant.vue'
import ScheduleToday from './components/ScheduleToday.vue'
import ScheduleFormDialog from './components/ScheduleFormDialog.vue'
import UpcomingReminders from './components/UpcomingReminders.vue'
import type { ScheduleVO } from '@/types/schedule'
import { listSchedules } from '@/api/schedule'

type ViewMode = 'calendar' | 'quadrant' | 'today'
type StatusFilter = 'all' | 'pending' | 'done'

const viewMode = ref<ViewMode>('calendar')
const statusFilter = ref<StatusFilter>('all')
const schedules = ref<ScheduleVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editing = ref<ScheduleVO | null>(null)
const presetStart = ref<string | null>(null)
const aiParseVisible = ref(false)
const aiPreset = ref<AiParsedScheduleVO | null>(null)
const createSourceType = ref<'MANUAL' | 'AI'>('MANUAL')

const reminderDrawer = ref(false)
const reminderRef = ref<InstanceType<typeof UpcomingReminders> | null>(null)

const filteredSchedules = computed(() => {
  if (statusFilter.value === 'all') return schedules.value
  if (statusFilter.value === 'done') return schedules.value.filter((s) => s.status === 2)
  return schedules.value.filter((s) => s.status !== 2)
})

async function loadSchedules() {
  loading.value = true
  try {
    const start = new Date()
    start.setMonth(start.getMonth() - 1)
    const end = new Date()
    end.setMonth(end.getMonth() + 3)
    const fmt = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    const res = await listSchedules({
      startDate: fmt(start),
      endDate: fmt(end),
    })
    schedules.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate(start?: string) {
  editing.value = null
  presetStart.value = start ?? null
  aiPreset.value = null
  createSourceType.value = 'MANUAL'
  dialogVisible.value = true
}

function onAiParsed(data: AiParsedScheduleVO) {
  editing.value = null
  presetStart.value = null
  aiPreset.value = data
  createSourceType.value = 'AI'
  dialogVisible.value = true
}

function openEdit(s: ScheduleVO) {
  editing.value = s
  presetStart.value = null
  aiPreset.value = null
  createSourceType.value = 'MANUAL'
  dialogVisible.value = true
}

onMounted(loadSchedules)
</script>

<template>
  <div v-loading="loading" class="schedules-page page-shell">
    <div class="toolbar">
      <el-radio-group v-model="viewMode">
        <el-radio-button value="calendar">日历视图</el-radio-button>
        <el-radio-button value="quadrant">四象限视图</el-radio-button>
        <el-radio-button value="today">今日待办</el-radio-button>
      </el-radio-group>
      <el-select v-model="statusFilter" style="width: 120px">
        <el-option label="全部" value="all" />
        <el-option label="待办" value="pending" />
        <el-option label="已完成" value="done" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openCreate()">新建日程</el-button>
      <el-button :icon="MagicStick" @click="aiParseVisible = true">AI 一句话建日程</el-button>
      <el-badge
        :value="reminderRef?.items?.length || undefined"
        :hidden="!reminderRef?.items?.length"
      >
        <el-button :icon="Bell" @click="reminderDrawer = true; reminderRef?.load()">临期提醒</el-button>
      </el-badge>
      <UpcomingReminders
        ref="reminderRef"
        v-model:drawer-open="reminderDrawer"
        :compact="false"
        @changed="loadSchedules"
      />
    </div>

    <ScheduleCalendar
      v-if="viewMode === 'calendar'"
      :schedules="filteredSchedules"
      @date-click="openCreate"
      @event-click="openEdit"
    />
    <ScheduleQuadrant
      v-else-if="viewMode === 'quadrant'"
      :schedules="filteredSchedules"
      @edit="openEdit"
      @changed="loadSchedules"
    />
    <ScheduleToday
      v-else
      :schedules="filteredSchedules"
      @edit="openEdit"
      @changed="loadSchedules"
    />

    <AiScheduleParseModal v-model:visible="aiParseVisible" @parsed="onAiParsed" />
    <ScheduleFormDialog
      v-model:visible="dialogVisible"
      :editing="editing"
      :preset-start="presetStart"
      :ai-preset="aiPreset"
      :create-source-type="createSourceType"
      @saved="loadSchedules"
    />
  </div>
</template>

<style scoped>
.schedules-page {
  padding-top: 12px;
  min-height: calc(100vh - 56px);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
</style>