<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { ScheduleVO } from '@/types/schedule'
import { STATUS_LABELS } from '@/types/schedule'
import { patchScheduleStatus } from '@/api/schedule'
import { formatDateTime, todayDateString, datePart } from '@/utils/date'

const props = defineProps<{
  schedules: ScheduleVO[]
}>()

const emit = defineEmits<{
  edit: [schedule: ScheduleVO]
  changed: []
}>()

const router = useRouter()
const today = todayDateString()

const todayList = computed(() =>
  props.schedules
    .filter((s) => datePart(s.startTime) === today)
    .sort((a, b) => a.startTime.localeCompare(b.startTime)),
)

const overdueList = computed(() =>
  props.schedules
    .filter((s) => s.status !== 2 && datePart(s.startTime) < today)
    .sort((a, b) => a.startTime.localeCompare(b.startTime)),
)

async function toggleComplete(s: ScheduleVO) {
  const next = s.status === 2 ? 0 : 2
  await patchScheduleStatus(s.id, { status: next })
  emit('changed')
}

function goNote(noteId: number | null) {
  if (noteId) router.push({ path: '/notes', query: { noteId: String(noteId) } })
}
</script>

<template>
  <div class="today-view">
    <section v-if="overdueList.length" class="block overdue">
      <h3>已过期未完成（{{ overdueList.length }}）</h3>
      <div v-for="s in overdueList" :key="'o-' + s.id" class="row">
        <el-checkbox :model-value="s.status === 2" @change="toggleComplete(s)">{{ s.title }}</el-checkbox>
        <span class="time">{{ formatDateTime(s.startTime) }}</span>
        <el-tag size="small">{{ STATUS_LABELS[s.status] }}</el-tag>
        <el-button v-if="s.noteId" size="small" link @click="goNote(s.noteId)">{{ s.noteTitle || '笔记' }}</el-button>
        <el-button size="small" link @click="emit('edit', s)">编辑</el-button>
      </div>
    </section>
    <section class="block">
      <h3>今日待办（{{ todayList.length }}）</h3>
      <el-empty v-if="!todayList.length" description="今日暂无日程" />
      <div v-for="s in todayList" :key="s.id" class="row">
        <el-checkbox :model-value="s.status === 2" @change="toggleComplete(s)">{{ s.title }}</el-checkbox>
        <span class="time">{{ formatDateTime(s.startTime) }}</span>
        <el-tag size="small">{{ STATUS_LABELS[s.status] }}</el-tag>
        <el-button v-if="s.noteId" size="small" link @click="goNote(s.noteId)">{{ s.noteTitle || '笔记' }}</el-button>
        <el-button size="small" link @click="emit('edit', s)">编辑</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.today-view {
  padding: 12px;
}

.block {
  background: var(--ssp-surface);
  border: 1px solid var(--ssp-border);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.block.overdue {
  border-color: #fecaca;
  background: #fef2f2;
}

.block h3 {
  margin: 0 0 10px;
  font-size: 0.95rem;
}

.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--ssp-border);
}

.row:last-child {
  border-bottom: none;
}

.time {
  font-size: 0.8rem;
  color: var(--ssp-muted);
}
</style>