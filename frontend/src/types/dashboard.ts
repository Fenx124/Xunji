/** M7 首页学习数据看板，对齐 docs/api-contract.md §7 */

export interface DashboardSummary {
  totalNotes: number
  todayPendingSchedules: number
  monthCompletionRate: number
  weekFocusMinutes: number
  todayFocusMinutes: number
}

export interface HeatmapItem {
  date: string
  count: number
}

export interface WeeklyTrendItem {
  date: string
  notesUpdated: number
  schedulesCompleted: number
  focusMinutes: number
}

export interface NotebookDistributionItem {
  name: string
  count: number
}

export interface TagDistributionItem {
  name: string
  color: string
  count: number
}

export interface DashboardOverviewVO {
  summary: DashboardSummary
  heatmap: HeatmapItem[]
  weeklyTrend: WeeklyTrendItem[]
  notebookDistribution: NotebookDistributionItem[]
  tagDistribution: TagDistributionItem[]
}