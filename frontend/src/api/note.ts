import request from '@/utils/request'
import type { PageResult, Result } from '@/types/api'
import type {
  CreateNotebookRequest,
  CreateNoteRequest,
  CreateTagRequest,
  NoteDetailVO,
  NoteHistoryDetailVO,
  NoteHistoryItemVO,
  NoteListItemVO,
  NotePinFavoriteResult,
  NoteQueryParams,
  NotebookVO,
  TagVO,
  UpdateNotebookRequest,
  UpdateNoteRequest,
  UpdateTagRequest,
} from '@/types/note'

export function listNotebooks() {
  return request.get<unknown, Result<NotebookVO[]>>('/notebooks')
}

export function createNotebook(data: CreateNotebookRequest) {
  return request.post<unknown, Result<NotebookVO>>('/notebooks', data)
}

export function updateNotebook(id: number, data: UpdateNotebookRequest) {
  return request.put<unknown, Result<NotebookVO>>(`/notebooks/${id}`, data)
}

export function deleteNotebook(id: number) {
  return request.delete<unknown, Result<null>>(`/notebooks/${id}`)
}

export function listTags() {
  return request.get<unknown, Result<TagVO[]>>('/tags')
}

export function createTag(data: CreateTagRequest) {
  return request.post<unknown, Result<TagVO>>('/tags', data)
}

export function updateTag(id: number, data: UpdateTagRequest) {
  return request.put<unknown, Result<TagVO>>(`/tags/${id}`, data)
}

export function deleteTag(id: number) {
  return request.delete<unknown, Result<null>>(`/tags/${id}`)
}

export function listNotes(params?: NoteQueryParams) {
  return request.get<unknown, Result<PageResult<NoteListItemVO>>>('/notes', { params })
}

export function listTrashNotes(params?: { pageNum?: number; pageSize?: number }) {
  return request.get<unknown, Result<PageResult<NoteListItemVO>>>('/notes/trash', { params })
}

export function getNote(id: number) {
  return request.get<unknown, Result<NoteDetailVO>>(`/notes/${id}`)
}

export function createNote(data: CreateNoteRequest) {
  return request.post<unknown, Result<NoteDetailVO>>('/notes', data)
}

export function updateNote(id: number, data: UpdateNoteRequest) {
  return request.put<unknown, Result<NoteDetailVO>>(`/notes/${id}`, data)
}

export function toggleNotePin(id: number, isPinned?: boolean) {
  return request.patch<unknown, Result<NotePinFavoriteResult>>(`/notes/${id}/pin`, {
    isPinned,
  })
}

export function toggleNoteFavorite(id: number, isFavorite?: boolean) {
  return request.patch<unknown, Result<NotePinFavoriteResult>>(`/notes/${id}/favorite`, {
    isFavorite,
  })
}

export function deleteNote(id: number) {
  return request.delete<unknown, Result<null>>(`/notes/${id}`)
}

export function restoreNote(id: number) {
  return request.post<unknown, Result<NoteDetailVO>>(`/notes/${id}/restore`)
}

export function permanentDeleteNote(id: number) {
  return request.delete<unknown, Result<null>>(`/notes/${id}/permanent`)
}

export function listNoteHistory(noteId: number) {
  return request.get<unknown, Result<NoteHistoryItemVO[]>>(`/notes/${noteId}/history`)
}

export function getNoteHistory(noteId: number, historyId: number) {
  return request.get<unknown, Result<NoteHistoryDetailVO>>(`/notes/${noteId}/history/${historyId}`)
}

export function restoreNoteHistory(noteId: number, historyId: number) {
  return request.post<unknown, Result<NoteDetailVO>>(`/notes/${noteId}/history/${historyId}/restore`)
}