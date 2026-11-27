<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Delete, Folder, Plus, PriceTag, Star } from '@element-plus/icons-vue'
import type { NotebookVO, TagVO } from '@/types/note'
import {
  createNotebook,
  createTag,
  deleteNotebook,
  deleteTag,
  updateNotebook,
  updateTag,
} from '@/api/note'

import type { SidebarFilter } from '../note-ui'

const props = defineProps<{
  filter: SidebarFilter
  notebooks: NotebookVO[]
  tags: TagVO[]
  selectedNotebookId: number | null
  selectedTagId: number | null
}>()

const emit = defineEmits<{
  'update:filter': [value: SidebarFilter]
  'update:selectedNotebookId': [value: number | null]
  'update:selectedTagId': [value: number | null]
  refresh: []
}>()

const notebookDialogVisible = ref(false)
const notebookForm = ref({ id: 0, name: '', description: '' })
const tagDialogVisible = ref(false)
const tagForm = ref({ id: 0, name: '', color: '#2563eb' })

function setFilter(f: SidebarFilter) {
  emit('update:filter', f)
  if (f === 'trash') {
    emit('update:selectedNotebookId', null)
    emit('update:selectedTagId', null)
  }
}

function selectNotebook(id: number | null) {
  emit('update:filter', 'all')
  emit('update:selectedNotebookId', id)
  emit('update:selectedTagId', null)
}

function selectTag(id: number | null) {
  emit('update:filter', 'all')
  emit('update:selectedTagId', id)
  emit('update:selectedNotebookId', null)
}

function openNotebookDialog(nb?: NotebookVO) {
  if (nb) {
    notebookForm.value = { id: nb.id, name: nb.name, description: nb.description || '' }
  } else {
    notebookForm.value = { id: 0, name: '', description: '' }
  }
  notebookDialogVisible.value = true
}

async function saveNotebook() {
  const name = notebookForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入笔记本名称')
    return
  }
  if (notebookForm.value.id) {
    await updateNotebook(notebookForm.value.id, {
      name,
      description: notebookForm.value.description || null,
    })
    ElMessage.success('笔记本已更新')
  } else {
    await createNotebook({ name, description: notebookForm.value.description || null })
    ElMessage.success('笔记本已创建')
  }
  notebookDialogVisible.value = false
  emit('refresh')
}

async function removeNotebook(nb: NotebookVO) {
  await ElMessageBox.confirm(`确定删除笔记本「${nb.name}」？其下笔记将变为未分类。`, '删除确认', {
    type: 'warning',
  })
  await deleteNotebook(nb.id)
  if (props.selectedNotebookId === nb.id) {
    emit('update:selectedNotebookId', null)
  }
  ElMessage.success('已删除')
  emit('refresh')
}

function openTagDialog(tag?: TagVO) {
  if (tag) {
    tagForm.value = { id: tag.id, name: tag.name, color: tag.color || '#2563eb' }
  } else {
    tagForm.value = { id: 0, name: '', color: '#2563eb' }
  }
  tagDialogVisible.value = true
}

async function saveTag() {
  const name = tagForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (tagForm.value.id) {
    await updateTag(tagForm.value.id, { name, color: tagForm.value.color })
    ElMessage.success('标签已更新')
  } else {
    await createTag({ name, color: tagForm.value.color })
    ElMessage.success('标签已创建')
  }
  tagDialogVisible.value = false
  emit('refresh')
}

async function removeTag(tag: TagVO) {
  await ElMessageBox.confirm(`确定删除标签「${tag.name}」？`, '删除确认', { type: 'warning' })
  await deleteTag(tag.id)
  if (props.selectedTagId === tag.id) {
    emit('update:selectedTagId', null)
  }
  ElMessage.success('已删除')
  emit('refresh')
}
</script>

<template>
  <aside class="note-sidebar">
    <div class="filter-group">
      <div
        class="filter-item"
        :class="{ active: filter === 'all' && !selectedNotebookId && !selectedTagId }"
        @click="setFilter('all'); selectNotebook(null)"
      >
        <el-icon><Collection /></el-icon>
        <span>全部笔记</span>
      </div>
      <div class="filter-item" :class="{ active: filter === 'favorite' }" @click="setFilter('favorite')">
        <el-icon><Star /></el-icon>
        <span>我的收藏</span>
      </div>
      <div class="filter-item" :class="{ active: filter === 'trash' }" @click="setFilter('trash')">
        <el-icon><Delete /></el-icon>
        <span>回收站</span>
      </div>
    </div>

    <div class="section">
      <div class="section-head">
        <span>笔记本</span>
        <el-button text size="small" @click="openNotebookDialog()">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <div
        v-for="nb in notebooks"
        :key="nb.id"
        class="list-item"
        :class="{ active: selectedNotebookId === nb.id }"
        @click="selectNotebook(nb.id)"
      >
        <el-icon><Folder /></el-icon>
        <span class="item-name">{{ nb.name }}</span>
        <span class="count">{{ nb.noteCount }}</span>
        <el-dropdown trigger="click" @click.stop>
          <el-button text size="small" class="more-btn">···</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openNotebookDialog(nb)">编辑</el-dropdown-item>
              <el-dropdown-item divided @click="removeNotebook(nb)">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="section">
      <div class="section-head">
        <span>标签</span>
        <el-button text size="small" @click="openTagDialog()">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <div
        v-for="tag in tags"
        :key="tag.id"
        class="list-item"
        :class="{ active: selectedTagId === tag.id }"
        @click="selectTag(tag.id)"
      >
        <span class="tag-dot" :style="{ background: tag.color || '#2563eb' }" />
        <el-icon><PriceTag /></el-icon>
        <span class="item-name">{{ tag.name }}</span>
        <span class="count">{{ tag.noteCount }}</span>
        <el-dropdown trigger="click" @click.stop>
          <el-button text size="small" class="more-btn">···</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openTagDialog(tag)">编辑</el-dropdown-item>
              <el-dropdown-item divided @click="removeTag(tag)">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-dialog v-model="notebookDialogVisible" :title="notebookForm.id ? '编辑笔记本' : '新建笔记本'" width="400px">
      <el-form label-width="72px">
        <el-form-item label="名称">
          <el-input v-model="notebookForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="notebookForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notebookDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNotebook">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tagDialogVisible" :title="tagForm.id ? '编辑标签' : '新建标签'" width="400px">
      <el-form label-width="72px">
        <el-form-item label="名称">
          <el-input v-model="tagForm.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>
  </aside>
</template>

<style scoped>
.note-sidebar {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--ssp-border);
  background: var(--ssp-surface);
  overflow-y: auto;
  padding: 12px 8px;
}

.filter-group {
  margin-bottom: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: var(--ssp-text);
}

.filter-item:hover,
.filter-item.active {
  background: #eff6ff;
  color: var(--ssp-primary);
}

.section {
  margin-bottom: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ssp-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.88rem;
}

.list-item:hover,
.list-item.active {
  background: #f3f4f6;
}

.item-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.count {
  font-size: 0.75rem;
  color: var(--ssp-muted);
}

.more-btn {
  padding: 0 4px;
  opacity: 0;
}

.list-item:hover .more-btn {
  opacity: 1;
}

.tag-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>