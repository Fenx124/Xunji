<script setup lang="ts">
import { ref, watch } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/vue3/daygrid'
import timeGridPlugin from '@fullcalendar/vue3/timegrid'
import interactionPlugin from '@fullcalendar/vue3/interaction'
import classicTheme from '@fullcalendar/vue3/themes/classic'
import zhCnLocale from '@fullcalendar/vue3/locales/zh-cn'
import '@fullcalendar/vue3/skeleton.css'
import '@fullcalendar/vue3/themes/classic/theme.css'
import '@fullcalendar/vue3/themes/classic/palette.css'
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

function toIsoLocal(value: string): string {
  return value.includes('T') ? value : value.replace(' ', 'T')
}

function toEvents(list: ScheduleVO[]) {
  return list.map((s) => ({
    id: String(s.id),
    title: `${s.sourceType === 'EBBINGHAUS' ? '📚 ' : ''}${s.title}`,
    start: toIsoLocal(s.startTime),
    end: s.endTime ? toIsoLocal(s.endTime) : undefined,
    backgroundColor: PRIORITY_COLORS[s.priority as SchedulePriority],
    borderColor: PRIORITY_COLORS[s.priority as SchedulePriority],
  }))
}

function findSchedule(id: string) {
  return props.schedules.find((s) => s.id === Number(id))
}

// 选项对象必须保持同一引用。vue3 封装会深度监听 options，
// 每次新建对象都会 resetOptions，和日历内部渲染互相触发，页面会卡死。
const calendarOptions = {
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, classicTheme],
  locale: zhCnLocale,
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay',
  },
  height: 680,
  events: toEvents(props.schedules),
  dateClick: (info: { date: Date }) => {
    const d = info.date
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    emit('dateClick', `${y}-${m}-${day} 09:00:00`)
  },
  eventClick: (info: { event: { id: string } }) => {
    const sch = findSchedule(info.event.id)
    if (sch) emit('eventClick', sch)
  },
}

watch(
  () => props.schedules,
  (list) => {
    const events = toEvents(list)
    const api = calendarRef.value?.getApi()
    if (api) {
      api.setOption('events', events)
      return
    }
    calendarOptions.events = events
  },
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
</style>
