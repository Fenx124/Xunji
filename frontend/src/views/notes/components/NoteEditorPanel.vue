<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  Calendar,
  ChatDotRound,
  Clock,
  Delete,
  Download,
  MagicStick,
  Star,
  StarFilled,
  Top,
} from '@element-plus/icons-vue'
import NoteHistoryDrawer from './NoteHistoryDrawer.vue'
import { summarizeNote } from '@/api/ai'
import { createTag } from '@/api/note'
import type { NoteDetailVO, NotebookVO, TagVO } from '@/types/note'
import { STATUS_LABELS } from '@/types/schedule'
import { deleteNote, toggleNoteFavorite, toggleNotePin, updateNote } from '@/api/note'
import { generateEbbinghaus } from '@/api/schedule'
import { downloadMarkdown } from '@/utils/download'
import { formatDateTime } from '@/utils/date'
import type { Result } from '@/types/api'

const props = defineProps<{
  note: NoteDetailVO | null
  notebooks: NotebookVO[]
  tags: TagVO[]
  readonly: boolean
}>()

const emit = defineEmits<{
  saved: [note: NoteDetailVO]
  deleted: []
  pinFavoriteChanged: []
  reload: []
  restored: [note: NoteDetailVO]
  'tags-changed': []
}>()

const router = useRouter()
const title = ref('')
const content = ref('')
const notebookId = ref<number | null>(null)
const tagIds = ref<number[]>([])
const isPinned = ref(false)
const isFavorite = ref(false)
const activeCollapse = ref<string[]>(['schedules'])
const saving = ref(false)
const summarizing = ref(false)
const latestSummary = ref<string | null>(null)
const summaryFallback = ref(false)
const suggestedTags = ref<string[]>([])
const historyOpen = ref(false)

const canShowHistory = computed(() => props.note != null && props.note.id > 0)

const tagOptions = computed(() => props.tags.map((t) => ({ value: t.id, label: t.name })))
const relatedSchedules = computed(() => props.note?.relatedSchedules ?? [])

watch(
  () => props.note,
  (n) => {
    if (!n) {
      title.value = ''
      content.value = ''
      notebookId.value = null
      tagIds.value = []
      isPinned.value = false
      isFavorite.value = false
      return
    }
    title.value = n.title
    content.value = n.content
    notebookId.value = n.notebookId
    tagIds.value = n.tags.map((t) => t.id)
    isPinned.value = n.isPinned
    isFavorite.value = n.isFavorite
    latestSummary.value = null
    summaryFallback.value = false
    suggestedTags.value = []
  },
  { immediate: true },
)

const displaySummary = computed(
  () => latestSummary.value ?? props.note?.aiSummary ?? null,
)

async function onAiSummarize() {
  if (!props.note || props.readonly) return
  summarizing.value = true
  try {
    const res = await summarizeNote({ noteId: props.note.id })
    latestSummary.value = res.data.summary
    summaryFallback.value = res.data.fallback
    suggestedTags.value = res.data.suggestedTags ?? []
    ElMessage.success(
      res.data.fallback ? '摘要已生成（本地降级）' : `摘要已生成（${res.data.latencyMs} ms）`,
    )
    emit('reload')
  } finally {
    summarizing.value = false
  }
}

async function applySuggestedTag(name: string) {
  if (!props.note || props.readonly) return
  const trimmed = name.trim()
  if (!trimmed) return
  let tagId = props.tags.find((t) => t.name === trimmed)?.id
  if (!tagId) {
    const created = await createTag({ name: trimmed })
    tagId = created.data.id
    emit('tags-changed')
  }
  if (!tagIds.value.includes(tagId)) {
    tagIds.value = [...tagIds.value, tagId]
    await save()
  }
}

function goAiQa() {
  if (!props.note) return
  void router.push({ path: '/ai', query: { noteId: String(props.note.id) } })
}

async function save() {
  if (!props.note || props.readonly) return
  saving.value = true
  try {
    const res = await updateNote(props.note.id, {
      title: title.value.trim() || '无标题',
      content: content.value,
      notebookId: notebookId.value,
      tagIds: tagIds.value,
      isPinned: isPinned.value,
      isFavorite: isFavorite.value,
      version: props.note.version,
    })
    ElMessage.success('已保存')
    emit('saved', res.data)
  } catch (err) {
    const body = err as Result<unknown>
    if (body?.code === 40901) {
      await ElMessageBox.alert(
        '笔记已被其他会话修改，请重新加载后再编辑，避免覆盖他人更改。',
        '版本冲突',
        { type: 'warning', confirmButtonText: '重新加载' },
      )
      emit('reload')
    }
  } finally {
    saving.value = false
  }
}

async function onTogglePin() {
  if (!props.note || props.readonly) return
  const res = await toggleNotePin(props.note.id, !isPinned.value)
  isPinned.value = res.data.isPinned
  isFavorite.value = res.data.isFavorite
  emit('pinFavoriteChanged')
}

async function onToggleFavorite() {
  if (!props.note || props.readonly) return
  const res = await toggleNoteFavorite(props.note.id, !isFavorite.value)
  isPinned.value = res.data.isPinned
  isFavorite.value = res.data.isFavorite
  emit('pinFavoriteChanged')
}

async function moveToTrash() {
  if (!props.note || props.readonly) return
  await ElMessageBox.confirm('移入回收站后可从侧边栏恢复。', '移入回收站', { type: 'warning' })
  await deleteNote(props.note.id)
  ElMessage.success('已移入回收站')
  emit('deleted')
}

function exportMd() {
  if (!props.note) return
  downloadMarkdown(title.value || 'note', content.value)
}

async function onEbbinghaus() {
  if (!props.note) return
  const res = await generateEbbinghaus({ noteId: props.note.id })
  const count = res.data.length
  try {
    await ElMessageBox.confirm(
      `已生成 ${count} 条艾宾浩斯复习日程，是否前往日程中心查看？`,
      '复习计划已创建',
      { confirmButtonText: '前往日程', cancelButtonText: '留在笔记', type: 'success' },
    )
    await router.push('/schedules')
  } catch {
    emit('reload')
  }
}

function goSchedule() {
  router.push('/schedules')
}

function onHistoryRestored(note: NoteDetailVO) {
  emit('restored', note)
}
</script>

<template>
  <section class="editor-panel">
    <div v-if="!note" class="empty-editor">
      <el-empty description="选择或新建一篇笔记" />
    </div>
    <template v-else>
      <header v-if="!readonly" class="editor-toolbar">
        <el-input v-model="title" class="title-input" placeholder="笔记标题" />
        <el-select v-model="notebookId" clearable placeholder="笔记本" class="field-sm">
          <el-option v-for="nb in notebooks" :key="nb.id" :label="nb.name" :value="nb.id" />
        </el-select>
        <el-select
          v-model="tagIds"
          multiple
          collapse-tags
          filterable
          placeholder="标签"
          class="field-md"
        >
          <el-option v-for="t in tagOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-button :type="isPinned ? 'warning' : 'default'" @click="onTogglePin">
          <el-icon><Top /></el-icon>
        </el-button>
        <el-button :type="isFavorite ? 'warning' : 'default'" @click="onToggleFavorite">
          <el-icon><component :is="isFavorite ? StarFilled : Star" /></el-icon>
        </el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button :icon="Clock" :disabled="!canShowHistory" @click="historyOpen = true">
          历史版本
        </el-button>
        <el-button :icon="MagicStick" :loading="summarizing" @click="onAiSummarize">
          AI 摘要与标签
        </el-button>
        <el-button :icon="ChatDotRound" @click="goAiQa">AI 问答</el-button>
        <el-button @click="onEbbinghaus">艾宾浩斯</el-button>
        <el-button :icon="Download" @click="exportMd" />
        <el-button type="danger" plain :icon="Delete" @click="moveToTrash" />
      </header>
      <div v-else class="readonly-hint">
        <el-alert type="info" :closable="false" title="回收站中的笔记只读，请先恢复后再编辑。" />
      </div>

      <div v-if="displaySummary" class="ai-summary-banner">
        <div class="banner-head">
          <strong>AI 摘要</strong>
          <el-tag v-if="summaryFallback" size="small" type="warning">本地降级模式</el-tag>
        </div>
        <p class="summary-text">{{ displaySummary }}</p>
        <div v-if="suggestedTags.length" class="tag-suggest">
          <span class="muted-label">推荐标签：</span>
          <el-tag
            v-for="t in suggestedTags"
            :key="t"
            class="tag-pill"
            effect="plain"
            @click="applySuggestedTag(t)"
          >
            + {{ t }}
          </el-tag>
        </div>
      </div>

      <el-collapse v-model="activeCollapse" class="schedules-collapse">
        <el-collapse-item name="schedules">
          <template #title>
            <span class="collapse-title"><el-icon><Calendar /></el-icon> 关联复习日程（{{ relatedSchedules.length }}）</span>
          </template>
          <ul v-if="relatedSchedules.length" class="schedule-links">
            <li v-for="s in relatedSchedules" :key="s.id" @click="goSchedule">
              <el-tag size="small" :type="s.status === 2 ? 'success' : 'info'">{{ STATUS_LABELS[s.status] }}</el-tag>
              <span class="sch-title">{{ s.title }}</span>
              <span class="sch-time">{{ formatDateTime(s.startTime) }}</span>
            </li>
          </ul>
          <span v-else class="muted">暂无关联日程</span>
        </el-collapse-item>
      </el-collapse>

      <MdEditor
        v-model="content"
        language="zh-CN"
        :readonly="readonly"
        preview-theme="github"
        code-theme="atom"
        class="md-editor"
      />

      <NoteHistoryDrawer
        v-model="historyOpen"
        :note-id="note?.id ?? null"
        :current-title="title"
        :current-content="content"
        @restored="onHistoryRestored"
      />
    </template>
  </section>
</template>

<style scoped>
.editor-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--ssp-surface);
}

.empty-editor {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--ssp-border);
}

.title-input {
  flex: 1 1 180px;
  min-width: 120px;
}

.field-sm {
  width: 130px;
}

.field-md {
  width: 160px;
}

.readonly-hint {
  padding: 8px 12px;
}

.ai-summary-banner {
  margin: 0 12px;
  padding: 10px 12px;
  border: 1px solid #e0e7ff;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  border-radius: 8px;
}

.banner-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.summary-text {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.5;
  color: #334155;
}

.tag-suggest {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}

.muted-label {
  font-size: 0.8rem;
  color: var(--ssp-muted);
}

.tag-pill {
  cursor: pointer;
}

.schedules-collapse {
  border-bottom: 1px solid var(--ssp-border);
  padding: 0 12px;
}

.collapse-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
}

.schedule-links {
  list-style: none;
  margin: 0;
  padding: 0 0 8px;
}

.schedule-links li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 4px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
}

.schedule-links li:hover {
  background: #f3f4f6;
}

.sch-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sch-time {
  color: var(--ssp-muted);
  font-size: 0.78rem;
}

.muted {
  color: var(--ssp-muted);
  font-size: 0.85rem;
}

.md-editor {
  flex: 1;
  min-height: 320px;
}

:deep(.md-editor) {
  height: 100%;
  border: none;
}
</style>