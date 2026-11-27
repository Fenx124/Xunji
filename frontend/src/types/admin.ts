/** M8 管理员治理后台，对齐 docs/api-contract.md §8 */

export interface AdminUserVO {
  id: number
  username: string
  nickname: string
  email: string | null
  status: 0 | 1
  roles: string[]
  aiDailyLimit: number
  createdAt: string
}

export interface AdminUserQueryParams {
  keyword?: string
  status?: 0 | 1
  pageNum?: number
  pageSize?: number
}

export interface UpdateUserStatusRequest {
  status: 0 | 1
}

export interface UpdateUserRolesRequest {
  roleCodes: string[]
}

export interface UpdateUserAiLimitRequest {
  aiDailyLimit: number
}

export interface AdminAiFeatureStat {
  featureType: string
  calls: number
  avgLatencyMs: number
}

export interface AdminAiUserStat {
  userId: number
  username: string
  nickname: string
  calls: number
  todayCalls: number
  aiDailyLimit: number
}

export interface AdminAiDailyTrend {
  date: string
  calls: number
  successCalls: number
}

export interface AdminAiStatsVO {
  totalCalls: number
  successCalls: number
  successRate: number
  avgLatencyMs: number
  totalTokens: number
  byFeature: AdminAiFeatureStat[]
  byUser: AdminAiUserStat[]
  dailyTrend: AdminAiDailyTrend[]
}

export interface AuditLogVO {
  id: number
  userId: number | null
  username: string | null
  module: string
  action: string
  httpMethod: string
  requestUri: string
  clientIp: string
  costMs: number
  status: number
  errorMsg: string | null
  createdAt: string
}

export interface AuditLogQueryParams {
  module?: string
  username?: string
  status?: number
  pageNum?: number
  pageSize?: number
}