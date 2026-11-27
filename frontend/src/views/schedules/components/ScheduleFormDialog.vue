<script setup lang="ts">
import { ref, watch } from 'vue'
import type { AiParsedScheduleVO } from '@/types/ai'
import type { ScheduleVO, SchedulePriority } from '@/types/schedule'
import { PRIORITY_LABELS } from '@/types/schedule'
import { listNotes } from '@/api/note'
import type { NoteListItemVO } from '@/types/note'
import { createSchedule, updateSchedule } from '@/api/schedule'

const props = defineProps<{
  visible: boolean
  editing: ScheduleVO | null
  presetStart?: string | null
  aiPreset?: AiParsedScheduleVO | null
  createSourceType?: 'MANUAL' | 'AI'
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const form = ref({
  title: '',
  description: '',
  noteId: null as number | null,
  startTime: '',
  endTime: '' as string | null,
  remindTime: '' as string | null,
  priority: 2 as SchedulePriority,
})

const noteOptions = ref<NoteListItemVO[]>([])
const submitting = ref(false)

watch(
  () => props.visible,
  async (open) => {
    if (!open) return
    const res = await listNotes({ pageSize: 100, pageNum: 1 })
    noteOptions.value = res.data.records
    if (props.editing) {
      const s = props.editing
      form.value = {
        title: s.title,
        description: s.description || '',
        noteId: s.noteId,
        startTime: s.startTime,
        endTime: s.endTime,
        remindTime: s.remindTime,
        priority: s.priority,
      }
    } else if (props.aiPreset) {
      const p = props.aiPreset
      form.value = {
        title: p.title,
        description: p.description || '',
        noteId: null,
        startTime: p.startTime,
        endTime: p.endTime,
        remindTime: p.remindTime,
        priority: p.priority,
      }
    } else {
      form.value = {
        title: '',
        description: '',
        noteId: null,
        startTime: props.presetStart || defaultStart(),
        endTime: null,
        remindTime: null,
        priority: 2,
      }
    }
  },
)

function defaultStart(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day} 09:00:00`
}

async function submit() {
  if (!form.value.title.trim() || !form.value.startTime) return
  submitting.value = true
  try {
    const payload = {
      title: form.value.title.trim(),
      description: form.value.description || null,
      noteId: form.value.noteId,
      startTime: form.value.startTime,
      endTime: form.value.endTime || null,
      remindTime: form.value.remindTime || null,
      priority: form.value.priority,
    }
    if (props.editing) {
      await updateSchedule(props.editing.id, payload)
    } else {
      await createSchedule({
        ...payload,
        sourceType: props.createSourceType ?? 'MANUAL',
      })
    }
    emit('update:visible', false)
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="editing ? '编辑日程' : '新建日程'"
    width="520px"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-alert
      v-if="aiPreset?.fallback && !editing"
      type="warning"
      :closable="false"
      title="本地降级模式：请核对解析出的时间是否正确"
      style="margin-bottom: 12px"
    />
    <el-form label-width="88px">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="128" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="开始时间" required>
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择开始时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          clearable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="提醒时间">
        <el-date-picker
          v-model="form.remindTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          clearable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="form.priority" style="width: 100%">
          <el-option :value="1" :label="PRIORITY_LABELS[1]" />
          <el-option :value="2" :label="PRIORITY_LABELS[2]" />
          <el-option :value="3" :label="PRIORITY_LABELS[3]" />
          <el-option :value="4" :label="PRIORITY_LABELS[4]" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联笔记">
        <el-select v-model="form.noteId" filterable clearable placeholder="可选" style="width: 100%">
          <el-option
            v-for="n in noteOptions"
            :key="n.id"
            :label="n.title"
            :value="n.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>