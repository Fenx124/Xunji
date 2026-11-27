<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNoteHistory,
  listNoteHistory,
  restoreNoteHistory,
} from '@/api/note'
import type { NoteDetailVO, NoteHistoryDetailVO, NoteHistoryItemVO } from '@/types/note'
import { lineDiff, type DiffLine } from '@/utils/textDiff'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{
  modelValue: boolean
  noteId: number | null
  currentTitle: string
  currentContent: string
}>()

const emit = defineEmits<{
  'update:modelValue': [open: boolean]
  restored: [note: NoteDetailVO]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const listLoading = ref(false)
const detailLoading = ref(false)
const restoring = ref(false)
const items = ref<NoteHistoryItemVO[]>([])
const selectedId = ref<number | null>(null)
const detail = ref<NoteHistoryDetailVO | null>(null)

const titleChanged = computed(
  () => detail.value != null && detail.value.title !== props.currentTitle,
)

const contentDiff = computed((): DiffLine[] => {
  if (!detail.value) return []
  return lineDiff(detail.value.content, props.currentContent)
})

const hasContentChanges = computed(() => contentDiff.value.some((l) => l.type !== 'equal'))

async function loadList() {
  if (props.noteId == null) return
  listLoading.value = true
  selectedId.value = null
  detail.value = null
  try {
    const res = await listNoteHistory(props.noteId)
    items.value = res.data
    if (res.data.length > 0) {
      await selectItem(res.data[0])
    }
  } finally {
    listLoading.value = false
  }
}

async function selectItem(item: NoteHistoryItemVO) {
  if (props.noteId == null) return
  selectedId.value = item.id
  detailLoading.value = true
  try {
    const res = await getNoteHistory(props.noteId, item.id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function onRestore() {
  if (props.noteId == null || selectedId.value == null || !detail.value) return
  try {
    await ElMessageBox.confirm(
      '将用该历史版本的标题与正文覆盖当前笔记，是否继续？',
      '恢复此版本',
      { type: 'warning', confirmButtonText: '恢复', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  restoring.value = true
  try {
    const res = await restoreNoteHistory(props.noteId, selectedId.value)
    ElMessage.success('已恢复为该版本')
    emit('restored', res.data)
    visible.value = false
  } finally {
    restoring.value = false
  }
}

watch(
  () => [visible.value, props.noteId] as const,
  ([open, id]) => {
    if (open && id != null) void loadList()
  },
)
</script>

<template>
  <el-drawer v-model="visible" title="历史版本" size="720px" destroy-on-close>
    <section v-loading="listLoading" class="history-layout">
      <aside class="history-list">
        <el-empty
          v-if="!listLoading && items.length === 0"
          description="还没有历史版本。下次修改标题或正文并保存后会留下快照。"
        />
        <ul v-else class="version-list">
          <li
            v-for="item in items"
            :key="item.id"
            class="version-item"
            :class="{ active: selectedId === item.id }"
            @click="selectItem(item)"
          >
            <span class="version-no">v{{ item.versionNo }}</span>
            <span class="version-title">{{ item.title || '无标题' }}</span>
            <span v-if="item.changeNote" class="version-note">{{ item.changeNote }}</span>
            <span class="version-time">{{ formatDateTime(item.createdAt) }}</span>
          </li>
        </ul>
      </aside>

      <main v-loading="detailLoading" class="history-detail">
        <template v-if="detail">
          <div v-if="titleChanged" class="title-diff">
            <span class="diff-label removed">删除</span>
            <span class="diff-text removed">{{ detail.title }}</span>
            <span class="diff-label added">新增</span>
            <span class="diff-text added">{{ currentTitle }}</span>
          </div>
          <p v-else class="muted">标题与当前一致</p>

          <h4 class="diff-heading">正文对比（相对当前编辑器）</h4>
          <div v-if="!hasContentChanges" class="muted">正文与当前一致</div>
          <pre v-else class="diff-block"><template v-for="(line, idx) in contentDiff" :key="idx"><span
            v-if="line.type !== 'equal'"
            class="diff-row"
            :class="line.type"
          ><span class="diff-label">{{ line.type === 'removed' ? '删除' : '新增' }}</span>{{ line.text }}
</span><span v-else class="diff-row equal">{{ line.text }}
</span></template></pre>

          <div class="restore-bar">
            <el-button type="primary" :loading="restoring" @click="onRestore">恢复此版本</el-button>
          </div>
        </template>
        <el-empty v-else-if="!listLoading && items.length === 0" />
        <span v-else-if="items.length && !detail" class="muted">请选择左侧版本查看差异</span>
      </main>
    </section>
  </el-drawer>
</template>

<style scoped>
.history-layout {
  display: flex;
  gap: 16px;
  min-height: 360px;
}

.history-list {
  flex: 0 0 240px;
  border-right: 1px solid var(--ssp-border);
  padding-right: 12px;
}

.version-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.version-item {
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.version-item:hover {
  background: #f3f4f6;
}

.version-item.active {
  background: #eef2ff;
  border: 1px solid #c7d2fe;
}

.version-no {
  font-size: 0.75rem;
  font-weight: 600;
  color: #6366f1;
}

.version-title {
  font-size: 0.88rem;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-note {
  font-size: 0.78rem;
  color: var(--ssp-muted);
}

.version-time {
  font-size: 0.75rem;
  color: var(--ssp-muted);
}

.history-detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.muted {
  color: var(--ssp-muted);
  font-size: 0.85rem;
}

.diff-heading {
  margin: 8px 0 0;
  font-size: 0.9rem;
}

.title-diff {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
}

.diff-label {
  font-size: 0.7rem;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.diff-label.removed {
  background: #fee2e2;
  color: #b91c1c;
}

.diff-label.added {
  background: #dcfce7;
  color: #15803d;
}

.diff-text.removed {
  text-decoration: line-through;
  color: #991b1b;
  background: #fef2f2;
  padding: 2px 6px;
  border-radius: 4px;
}

.diff-text.added {
  color: #166534;
  background: #f0fdf4;
  padding: 2px 6px;
  border-radius: 4px;
}

.diff-block {
  margin: 0;
  padding: 10px;
  border: 1px solid var(--ssp-border);
  border-radius: 8px;
  font-size: 0.82rem;
  line-height: 1.45;
  max-height: 50vh;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
}

.diff-row {
  display: block;
}

.diff-row.removed {
  background: #fef2f2;
  color: #7f1d1d;
}

.diff-row.added {
  background: #f0fdf4;
  color: #14532d;
}

.diff-row.equal {
  color: #64748b;
}

.diff-row .diff-label {
  margin-right: 6px;
}

.restore-bar {
  margin-top: auto;
  padding-top: 12px;
}
</style>