/** M2 笔记中心类型，对齐 docs/api-contract.md §3 */

export interface NotebookVO {
  id: number
  name: string
  description: string | null
  icon: string | null
  sortOrder: number
  noteCount: number
  createdAt: string
  updatedAt: string
}

export interface TagVO {
  id: number
  name: string
  color: string | null
  noteCount: number
  createdAt: string
}

export interface TagSimpleVO {
  id: number
  name: string
  color: string | null
}

export interface ScheduleBriefVO {
  id: number
  title: string
  startTime: string
  status: 0 | 1 | 2
  priority: 1 | 2 | 3 | 4
  sourceType: 'MANUAL' | 'EBBINGHAUS' | 'AI'
}

export interface NoteListItemVO {
  id: number
  notebookId: number | null
  notebookName: string | null
  title: string
  aiSummary: string | null
  isPinned: boolean
  isFavorite: boolean
  wordCount: number
  version: number
  tags: TagSimpleVO[]
  deletedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface NoteDetailVO extends NoteListItemVO {
  content: string
  relatedSchedules: ScheduleBriefVO[]
}

export interface CreateNoteRequest {
  notebookId?: number | null
  title: string
  content: string
  tagIds?: number[]
  isPinned?: boolean
  isFavorite?: boolean
}

export interface UpdateNoteRequest {
  notebookId?: number | null
  title: string
  content: string
  tagIds?: number[]
  isPinned?: boolean
  isFavorite?: boolean
  version: number
}

export interface NoteQueryParams {
  notebookId?: number
  tagId?: number
  keyword?: string
  isFavorite?: boolean
  pageNum?: number
  pageSize?: number
}

export interface NotePinFavoriteResult {
  id: number
  isPinned: boolean
  isFavorite: boolean
}

export interface CreateNotebookRequest {
  name: string
  description?: string | null
  icon?: string | null
  sortOrder?: number
}

export interface UpdateNotebookRequest {
  name: string
  description?: string | null
  icon?: string | null
  sortOrder?: number
}

export interface CreateTagRequest {
  name: string
  color?: string | null
}

export interface UpdateTagRequest {
  name: string
  color?: string | null
}