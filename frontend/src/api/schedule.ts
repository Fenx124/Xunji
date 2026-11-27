import request from '@/utils/request'
import type { Result } from '@/types/api'
import type {
  CreateScheduleRequest,
  EbbinghausRequest,
  PatchScheduleStatusRequest,
  ScheduleQueryParams,
  ScheduleVO,
  UpdateScheduleRequest,
} from '@/types/schedule'

export function listSchedules(params?: ScheduleQueryParams) {
  return request.get<unknown, Result<ScheduleVO[]>>('/schedules', { params })
}

export function createSchedule(data: CreateScheduleRequest) {
  return request.post<unknown, Result<ScheduleVO>>('/schedules', data)
}

export function updateSchedule(id: number, data: UpdateScheduleRequest) {
  return request.put<unknown, Result<ScheduleVO>>(`/schedules/${id}`, data)
}

export function patchScheduleStatus(id: number, data: PatchScheduleStatusRequest) {
  return request.patch<unknown, Result<ScheduleVO>>(`/schedules/${id}/status`, data)
}

export function deleteSchedule(id: number) {
  return request.delete<unknown, Result<null>>(`/schedules/${id}`)
}

export function generateEbbinghaus(data: EbbinghausRequest) {
  return request.post<unknown, Result<ScheduleVO[]>>('/schedules/ebbinghaus', data)
}

export function listUpcomingReminders() {
  return request.get<unknown, Result<ScheduleVO[]>>('/schedules/reminders/upcoming')
}