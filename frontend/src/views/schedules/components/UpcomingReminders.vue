<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import type { ScheduleVO } from '@/types/schedule'
import { listUpcomingReminders, patchScheduleStatus } from '@/api/schedule'
import { formatDateTime } from '@/utils/date'

const props = withDefaults(
  defineProps<{
    compact?: boolean
    drawerOpen?: boolean
  }>(),
  { compact: false, drawerOpen: false },
)

const emit = defineEmits<{
  'update:drawerOpen': [value: boolean]
  changed: []
}>()

const router = useRouter()
const items = ref<ScheduleVO[]>([])
const visible = computed({
  get: () => props.drawerOpen,
  set: (v) => emit('update:drawerOpen', v),
})

async function load() {
  try {
    const res = await listUpcomingReminders()
    items.value = res.data ?? []
  } catch {
    items.value = []
  }
}

async function complete(s: ScheduleVO) {
  await patchScheduleStatus(s.id, { status: 2 })
  await load()
  emit('changed')
}

function goSchedules() {
  router.push('/schedules')
  visible.value = false
}

function goNote(noteId: number | null) {
  if (noteId) {
    router.push({ path: '/notes', query: { noteId: String(noteId) } })
    visible.value = false
  }
}

onMounted(load)

defineExpose({ load, items })
</script>

<template>
  <el-popover v-if="compact" placement="bottom-end" :width="320" trigger="click" @show="load">
    <template #reference>
      <el-badge :value="items.length || undefined" :hidden="!items.length" class="bell-badge">
        <el-button text circle aria-label="临期提醒">
          <el-icon :size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>
    <div class="reminder-pop">
      <div class="pop-head">临期提醒</div>
      <el-empty v-if="!items.length" description="暂无临期日程" :image-size="56" />
      <ul v-else class="reminder-list">
        <li v-for="s in items" :key="s.id">
          <div class="title">{{ s.title }}</div>
          <div class="sub">{{ formatDateTime(s.remindTime || s.startTime) }}</div>
          <div class="actions">
            <el-button size="small" link @click="complete(s)">完成</el-button>
            <el-button v-if="s.noteId" size="small" link @click="goNote(s.noteId)">笔记</el-button>
            <el-button size="small" link type="primary" @click="goSchedules">日程</el-button>
          </div>
        </li>
      </ul>
    </div>
  </el-popover>

  <el-drawer v-else v-model="visible" title="临期提醒" size="360px">
    <el-empty v-if="!items.length" description="暂无临期日程" />
    <ul v-else class="reminder-list">
      <li v-for="s in items" :key="s.id">
        <div class="title">{{ s.title }}</div>
        <div class="sub">{{ formatDateTime(s.remindTime || s.startTime) }}</div>
        <div class="actions">
          <el-button size="small" @click="complete(s)">标记完成</el-button>
          <el-button v-if="s.noteId" size="small" @click="goNote(s.noteId)">关联笔记</el-button>
        </div>
      </li>
    </ul>
  </el-drawer>
</template>

<style scoped>
.bell-badge :deep(.el-badge__content) {
  font-size: 10px;
}

.reminder-pop {
  max-height: 360px;
  overflow-y: auto;
}

.pop-head {
  font-weight: 600;
  margin-bottom: 8px;
}

.reminder-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.reminder-list li {
  padding: 8px 0;
  border-bottom: 1px solid var(--ssp-border);
}

.title {
  font-size: 0.9rem;
  font-weight: 500;
}

.sub {
  font-size: 0.78rem;
  color: var(--ssp-muted);
  margin: 2px 0 4px;
}

.actions {
  display: flex;
  gap: 4px;
}
</style>