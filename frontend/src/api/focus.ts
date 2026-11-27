import request from '@/utils/request'
import type { PageResult, Result } from '@/types/api'
import type {
  CreateFocusRecordRequest,
  FocusQueryParams,
  FocusRecordVO,
  FocusTodayStatsVO,
} from '@/types/focus'

export function createFocusRecord(data: CreateFocusRecordRequest) {
  return request.post<unknown, Result<FocusRecordVO>>('/focus/records', data)
}

export function listFocusRecords(params?: FocusQueryParams) {
  return request.get<unknown, Result<PageResult<FocusRecordVO>>>('/focus/records', { params })
}

export function getFocusTodayStats() {
  return request.get<unknown, Result<FocusTodayStatsVO>>('/focus/stats/today')
}