import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { DashboardOverviewVO } from '@/types/dashboard'

export function getDashboardOverview() {
  return request.get<unknown, Result<DashboardOverviewVO>>('/dashboard/overview')
}