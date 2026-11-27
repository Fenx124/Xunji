import request from '@/utils/request'
import type { PageResult, Result } from '@/types/api'
import type {
  AdminAiStatsVO,
  AdminUserQueryParams,
  AdminUserVO,
  AuditLogQueryParams,
  AuditLogVO,
  UpdateUserAiLimitRequest,
  UpdateUserRolesRequest,
  UpdateUserStatusRequest,
} from '@/types/admin'

export function listAdminUsers(params?: AdminUserQueryParams) {
  return request.get<unknown, Result<PageResult<AdminUserVO>>>('/admin/users', { params })
}

export function patchAdminUserStatus(id: number, data: UpdateUserStatusRequest) {
  return request.patch<unknown, Result<AdminUserVO>>(`/admin/users/${id}/status`, data)
}

export function putAdminUserRoles(id: number, data: UpdateUserRolesRequest) {
  return request.put<unknown, Result<AdminUserVO>>(`/admin/users/${id}/roles`, data)
}

export function putAdminUserAiLimit(id: number, data: UpdateUserAiLimitRequest) {
  return request.put<unknown, Result<AdminUserVO>>(`/admin/users/${id}/ai-limit`, data)
}

export function kickAdminUser(id: number) {
  return request.post<unknown, Result<null>>(`/admin/users/${id}/kick`)
}

export function getAdminAiStats(days = 7) {
  return request.get<unknown, Result<AdminAiStatsVO>>('/admin/ai-stats', { params: { days } })
}

export function listAuditLogs(params?: AuditLogQueryParams) {
  return request.get<unknown, Result<PageResult<AuditLogVO>>>('/admin/audit-logs', { params })
}