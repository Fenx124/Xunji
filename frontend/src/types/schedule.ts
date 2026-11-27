/** M3 日程中心类型，对齐 docs/api-contract.md §4 */

export type SchedulePriority = 1 | 2 | 3 | 4
export type ScheduleStatus = 0 | 1 | 2
export type ScheduleSourceType = 'MANUAL' | 'EBBINGHAUS' | 'AI'

export interface ScheduleVO {
  id: number
  noteId: number | null
  noteTitle: string | null
  title: string
  description: string | null
  startTime: string
  endTime: string | null
  remindTime: string | null
  reminded: boolean
  priority: SchedulePriority
  status: ScheduleStatus
  sourceType: ScheduleSourceType
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ScheduleQueryParams {
  startDate?: string
  endDate?: string
  status?: ScheduleStatus
  priority?: SchedulePriority
  noteId?: number
}

export interface CreateScheduleRequest {
  title: string
  description?: string | null
  noteId?: number | null
  startTime: string
  endTime?: string | null
  remindTime?: string | null
  priority?: SchedulePriority
  sourceType?: 'MANUAL' | 'AI'
}

export interface UpdateScheduleRequest {
  title: string
  description?: string | null
  noteId?: number | null
  startTime: string
  endTime?: string | null
  remindTime?: string | null
  priority?: SchedulePriority
  status?: ScheduleStatus
}

export interface PatchScheduleStatusRequest {
  status: ScheduleStatus
}

export interface EbbinghausRequest {
  noteId: number
  baseDate?: string
}

export const PRIORITY_LABELS: Record<SchedulePriority, string> = {
  1: '重要且紧急',
  2: '重要不紧急',
  3: '紧急不重要',
  4: '不重要不紧急',
}

export const PRIORITY_COLORS: Record<SchedulePriority, string> = {
  1: '#dc2626',
  2: '#d97706',
  3: '#2563eb',
  4: '#6b7280',
}

export const STATUS_LABELS: Record<ScheduleStatus, string> = {
  0: '待办',
  1: '进行中',
  2: '已完成',
}