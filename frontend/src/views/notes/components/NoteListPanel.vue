<script setup lang="ts">
import { Top, StarFilled } from '@element-plus/icons-vue'
import type { NoteListItemVO } from '@/types/note'
import { formatDateTime } from '@/utils/date'
import type { SidebarFilter } from '../note-ui'

defineProps<{
  filter: SidebarFilter
  notes: NoteListItemVO[]
  loading: boolean
  selectedId: number | null
  keyword: string
  total: number
  pageNum: number
  pageSize: number
}>()

const emit = defineEmits<{
  'update:keyword': [value: string]
  'update:pageNum': [value: number]
  select: [id: number]
  create: []
  importMd: []
  search: []
  restore: [id: number]
  permanentDelete: [id: number]
}>()

function onKeywordInput(e: Event) {
  emit('update:keyword', (e.target as HTMLInputElement).value)
}
</script>

<template>
  <section class="note-list-panel">
    <div class="toolbar">
      <el-input
        :model-value="keyword"
        placeholder="全文检索（Enter 搜索）"
        clearable
        @input="onKeywordInput"
        @keyup.enter="emit('search')"
      />
      <div class="btn-row">
        <el-button type="primary" size="small" @click="emit('create')">+ 新建笔记</el-button>
        <el-button size="small" @click="emit('importMd')">导入 .md</el-button>
      </div>
    </div>

    <div v-loading="loading" class="list-body">
      <div
        v-for="note in notes"
        :key="note.id"
        class="note-card"
        :class="{ selected: selectedId === note.id }"
        @click="emit('select', note.id)"
      >
        <div class="card-title">
          <el-icon v-if="note.isPinned" class="pin-icon"><Top /></el-icon>
          <el-icon v-if="note.isFavorite" class="fav-icon"><StarFilled /></el-icon>
          <span>{{ note.title || '无标题' }}</span>
        </div>
        <div v-if="note.notebookName" class="badge">{{ note.notebookName }}</div>
        <div v-if="note.tags?.length" class="tags">
          <span
            v-for="t in note.tags"
            :key="t.id"
            class="tag-pill"
            :style="{ borderColor: t.color || '#2563eb', color: t.color || '#2563eb' }"
          >
            {{ t.name }}
          </span>
        </div>
        <div class="meta">
          <span>{{ note.wordCount }} 字</span>
          <span v-if="filter === 'trash' && note.deletedAt">删除于 {{ formatDateTime(note.deletedAt) }}</span>
          <span v-else>更新 {{ formatDateTime(note.updatedAt) }}</span>
        </div>
        <div v-if="filter === 'trash'" class="trash-actions" @click.stop>
          <el-button size="small" type="primary" link @click="emit('restore', note.id)">恢复</el-button>
          <el-button size="small" type="danger" link @click="emit('permanentDelete', note.id)">彻底删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && !notes.length" description="暂无笔记" />
    </div>

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        small
        @current-change="emit('update:pageNum', $event)"
      />
    </div>
  </section>
</template>

<style scoped>
.note-list-panel {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--ssp-border);
  background: #fafbfc;
}

.toolbar {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: 1px solid var(--ssp-border);
}

.btn-row {
  display: flex;
  gap: 8px;
}

.list-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.note-card {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 10px;
  border: 1px solid var(--ssp-border);
  background: var(--ssp-surface);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.note-card:hover,
.note-card.selected {
  border-color: var(--ssp-primary);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  font-size: 0.92rem;
  margin-bottom: 6px;
}

.pin-icon {
  color: #f59e0b;
}

.fav-icon {
  color: #eab308;
}

.badge {
  display: inline-block;
  font-size: 0.72rem;
  padding: 2px 6px;
  border-radius: 4px;
  background: #e0e7ff;
  color: #3730a3;
  margin-bottom: 4px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
}

.tag-pill {
  font-size: 0.7rem;
  padding: 1px 6px;
  border-radius: 999px;
  border: 1px solid;
  background: #fff;
}

.meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  color: var(--ssp-muted);
}

.trash-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.pager {
  padding: 8px;
  border-top: 1px solid var(--ssp-border);
  display: flex;
  justify-content: center;
}
</style>