<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NoteSidebar from './components/NoteSidebar.vue'
import type { SidebarFilter } from './note-ui'
import NoteListPanel from './components/NoteListPanel.vue'
import NoteEditorPanel from './components/NoteEditorPanel.vue'
import type { NoteDetailVO, NoteListItemVO, NotebookVO, TagVO } from '@/types/note'
import {
  createNote,
  getNote,
  listNotebooks,
  listNotes,
  listTags,
  listTrashNotes,
  permanentDeleteNote,
  restoreNote,
} from '@/api/note'

const route = useRoute()
const router = useRouter()

const filter = ref<SidebarFilter>('all')
const selectedNotebookId = ref<number | null>(null)
const selectedTagId = ref<number | null>(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const notes = ref<NoteListItemVO[]>([])
const listLoading = ref(false)

const notebooks = ref<NotebookVO[]>([])
const tags = ref<TagVO[]>([])
const selectedId = ref<number | null>(null)
const currentNote = ref<NoteDetailVO | null>(null)
const editorReadonly = ref(false)

async function loadMeta() {
  const [nbRes, tagRes] = await Promise.all([listNotebooks(), listTags()])
  notebooks.value = nbRes.data
  tags.value = tagRes.data
}

async function loadNotes() {
  listLoading.value = true
  try {
    if (filter.value === 'trash') {
      const res = await listTrashNotes({ pageNum: pageNum.value, pageSize: pageSize.value })
      notes.value = res.data.records
      total.value = res.data.total
      editorReadonly.value = true
    } else {
      editorReadonly.value = false
      const res = await listNotes({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        notebookId: selectedNotebookId.value ?? undefined,
        tagId: selectedTagId.value ?? undefined,
        keyword: keyword.value.trim() || undefined,
        isFavorite: filter.value === 'favorite' ? true : undefined,
      })
      notes.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    listLoading.value = false
  }
}

async function openNote(id: number) {
  selectedId.value = id
  const res = await getNote(id)
  currentNote.value = res.data
  router.replace({ query: { ...route.query, noteId: String(id) } })
}

async function selectNote(id: number) {
  await openNote(id)
}

async function onCreate() {
  const res = await createNote({ title: '无标题', content: '' })
  ElMessage.success('已创建笔记')
  await loadMeta()
  await loadNotes()
  await openNote(res.data.id)
}

function onImportMd() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.md,text/markdown'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    const text = await file.text()
    const baseName = file.name.replace(/\.md$/i, '')
    const res = await createNote({ title: baseName || '导入笔记', content: text })
    ElMessage.success('导入成功')
    await loadMeta()
    await loadNotes()
    await openNote(res.data.id)
  }
  input.click()
}

async function onRestore(id: number) {
  await restoreNote(id)
  ElMessage.success('已恢复')
  await loadMeta()
  await loadNotes()
  if (selectedId.value === id) {
    filter.value = 'all'
    editorReadonly.value = false
    await openNote(id)
  }
}

async function onPermanentDelete(id: number) {
  await ElMessageBox.confirm('彻底删除后无法恢复，确定继续？', '危险操作', { type: 'error' })
  await permanentDeleteNote(id)
  ElMessage.success('已彻底删除')
  if (selectedId.value === id) {
    selectedId.value = null
    currentNote.value = null
    const q = { ...route.query }
    delete q.noteId
    router.replace({ query: q })
  }
  await loadMeta()
  await loadNotes()
}

function onNoteSaved(note: NoteDetailVO) {
  currentNote.value = note
  loadNotes()
  loadMeta()
}

async function onReloadNote() {
  if (currentNote.value) await openNote(currentNote.value.id)
}

function onNoteDeleted() {
  selectedId.value = null
  currentNote.value = null
  const q = { ...route.query }
  delete q.noteId
  router.replace({ query: q })
  loadMeta()
  loadNotes()
}

watch([filter, selectedNotebookId, selectedTagId], () => {
  pageNum.value = 1
  loadNotes()
})

watch(pageNum, () => loadNotes())

onMounted(async () => {
  await loadMeta()
  await loadNotes()
  const noteIdParam = route.query.noteId
  if (noteIdParam) {
    const id = Number(noteIdParam)
    if (!Number.isNaN(id)) {
      await openNote(id)
    }
  }
})

watch(
  () => route.query.noteId,
  async (val) => {
    if (!val) return
    const id = Number(val)
    if (!Number.isNaN(id) && id !== selectedId.value) {
      await openNote(id)
    }
  },
)
</script>

<template>
  <div class="notes-workspace">
    <NoteSidebar
      v-model:filter="filter"
      v-model:selected-notebook-id="selectedNotebookId"
      v-model:selected-tag-id="selectedTagId"
      :notebooks="notebooks"
      :tags="tags"
      @refresh="loadMeta"
    />
    <NoteListPanel
      :filter="filter"
      :notes="notes"
      :loading="listLoading"
      :selected-id="selectedId"
      v-model:keyword="keyword"
      v-model:page-num="pageNum"
      :total="total"
      :page-size="pageSize"
      @search="loadNotes"
      @select="selectNote"
      @create="onCreate"
      @import-md="onImportMd"
      @restore="onRestore"
      @permanent-delete="onPermanentDelete"
    />
    <NoteEditorPanel
      :note="currentNote"
      :notebooks="notebooks"
      :tags="tags"
      :readonly="editorReadonly"
      @saved="onNoteSaved"
      @deleted="onNoteDeleted"
      @pin-favorite-changed="loadNotes"
      @reload="onReloadNote"
      @tags-changed="loadMeta"
    />
  </div>
</template>

<style scoped>
.notes-workspace {
  display: flex;
  height: calc(100vh - 56px);
  min-height: 480px;
  overflow: hidden;
}
</style>