<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ScheduleVO, SchedulePriority } from '@/types/schedule'
import { PRIORITY_COLORS, PRIORITY_LABELS, STATUS_LABELS } from '@/types/schedule'
import { deleteSchedule, patchScheduleStatus } from '@/api/schedule'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{
  schedules: ScheduleVO[]
}>()

const emit = defineEmits<{
  edit: [schedule: ScheduleVO]
  changed: []
}>()

const router = useRouter()
const priorities: SchedulePriority[] = [1, 2, 3, 4]

const quadrants = computed(() => {
  const buckets: Record<SchedulePriority, ScheduleVO[]> = { 1: [], 2: [], 3: [], 4: [] }
  for (const s of props.schedules) {
    buckets[s.priority].push(s)
  }
  return buckets
})

async function toggleComplete(s: ScheduleVO) {
  const next = s.status === 2 ? 0 : 2
  await patchScheduleStatus(s.id, { status: next })
  emit('changed')
}

async function remove(s: ScheduleVO) {
  await ElMessageBox.confirm(`删除日程「${s.title}」？`, '确认', { type: 'warning' })
  await deleteSchedule(s.id)
  ElMessage.success('已删除')
  emit('changed')
}

function goNote(noteId: number | null) {
  if (noteId) router.push({ path: '/notes', query: { noteId: String(noteId) } })
}
</script>

<template>
  <div class="quadrant-grid">
    <div
      v-for="p in priorities"
      :key="p"
      class="quadrant"
      :style="{ borderTopColor: PRIORITY_COLORS[p] }"
    >
      <h3>{{ PRIORITY_LABELS[p] }}</h3>
      <el-empty v-if="!quadrants[p].length" description="暂无" :image-size="48" />
      <div v-for="s in quadrants[p]" :key="s.id" class="sch-card">
        <div class="row-top">
          <el-checkbox :model-value="s.status === 2" @change="toggleComplete(s)">
            <span :class="{ done: s.status === 2 }">{{ s.title }}</span>
          </el-checkbox>
          <el-tag v-if="s.sourceType === 'EBBINGHAUS'" size="small" type="warning">复习</el-tag>
        </div>
        <div class="meta">
          <el-tag size="small">{{ STATUS_LABELS[s.status] }}</el-tag>
          <span>{{ formatDateTime(s.startTime) }}</span>
        </div>
        <a v-if="s.noteTitle && s.noteId" class="note-link" @click.prevent="goNote(s.noteId)">
          📒 {{ s.noteTitle }}
        </a>
        <div class="actions">
          <el-button size="small" link type="primary" @click="emit('edit', s)">编辑</el-button>
          <el-button size="small" link type="danger" @click="remove(s)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.quadrant-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px;
}

.quadrant {
  background: var(--ssp-surface);
  border: 1px solid var(--ssp-border);
  border-top-width: 3px;
  border-radius: 12px;
  padding: 12px;
  min-height: 200px;
}

.quadrant h3 {
  margin: 0 0 10px;
  font-size: 0.95rem;
}

.sch-card {
  padding: 8px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #f9fafb;
  border: 1px solid var(--ssp-border);
}

.row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.done {
  text-decoration: line-through;
  color: var(--ssp-muted);
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 0.78rem;
  color: var(--ssp-muted);
}

.note-link {
  display: block;
  margin-top: 4px;
  font-size: 0.82rem;
  cursor: pointer;
}

.actions {
  margin-top: 4px;
}
</style>