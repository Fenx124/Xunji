/** M5 番茄钟专注模块，对齐 docs/api-contract.md §6 */

export type FocusStatus = 'COMPLETED' | 'ABORTED'

export interface FocusRecordVO {
  id: number
  noteId: number | null
  noteTitle: string | null
  scheduleId: number | null
  scheduleTitle: string | null
  plannedMinutes: number
  actualMinutes: number
  status: FocusStatus
  startedAt: string
  endedAt: string
  remark: string | null
  createdAt: string
}

export interface CreateFocusRecordRequest {
  noteId?: number
  scheduleId?: number
  plannedMinutes: number
  actualMinutes: number
  status: FocusStatus
  startedAt: string
  endedAt: string
  remark?: string
}

export interface FocusTodayStatsVO {
  completedCount: number
  totalMinutes: number
  totalSessions: number
}

export interface FocusQueryParams {
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}