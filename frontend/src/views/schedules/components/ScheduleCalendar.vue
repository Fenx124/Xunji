<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type { ScheduleVO, SchedulePriority } from '@/types/schedule'
import { PRIORITY_COLORS } from '@/types/schedule'

const props = defineProps<{
  schedules: ScheduleVO[]
}>()

const emit = defineEmits<{
  dateClick: [start: string]
  eventClick: [schedule: ScheduleVO]
}>()

const calendarRef = ref<InstanceType<typeof FullCalendar> | null>(null)

const scheduleMap = computed(() => {
  const map = new Map<number, ScheduleVO>()
  for (const s of props.schedules) map.set(s.id, s)
  return map
})

// FullCalendar 与 vue3 封装存在重复的类型包，此处使用宽松类型以通过 vue-tsc
const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  locale: 'zh-cn',
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay',
  },
  height: 'auto',
  events: props.schedules.map((s) => ({
    id: String(s.id),
    title: `${s.sourceType === 'EBBINGHAUS' ? '📚 ' : ''}${s.title}`,
    start: toIsoLocal(s.startTime),
    end: s.endTime ? toIsoLocal(s.endTime) : undefined,
    backgroundColor: PRIORITY_COLORS[s.priority as SchedulePriority],
    borderColor: PRIORITY_COLORS[s.priority as SchedulePriority],
  })),
  dateClick: (info: { date: Date }) => {
    const d = info.date
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    emit('dateClick', `${y}-${m}-${day} 09:00:00`)
  },
  eventClick: (info: { event: { id: string } }) => {
    const id = Number(info.event.id)
    const sch = scheduleMap.value.get(id)
    if (sch) emit('eventClick', sch)
  },
}))

function toIsoLocal(value: string): string {
  return value.includes('T') ? value : value.replace(' ', 'T')
}

watch(
  () => props.schedules,
  () => {
    calendarRef.value?.getApi()?.refetchEvents()
  },
  { deep: true },
)
</script>

<template>
  <div class="calendar-wrap">
    <FullCalendar ref="calendarRef" :options="(calendarOptions as never)" />
  </div>
</template>

<style scoped>
.calendar-wrap {
  padding: 12px;
  background: var(--ssp-surface);
  border-radius: 12px;
  border: 1px solid var(--ssp-border);
}

:deep(.fc) {
  --fc-border-color: var(--ssp-border);
  font-size: 0.9rem;
}
</style>