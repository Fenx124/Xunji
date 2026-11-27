/** M4 AI 学习助手类型，对齐 docs/api-contract.md §5 */

import type { SchedulePriority } from './schedule'

export type AiFeatureType = 'SUMMARY' | 'TAG_SUGGEST' | 'NOTE_QA' | 'SCHEDULE_PARSE'

export interface AiSummarizeRequest {
  noteId: number
}

export interface AiSummarizeVO {
  noteId: number
  summary: string
  suggestedTags: string[]
  fallback: boolean
  latencyMs: number
}

export interface AiQaStreamRequest {
  noteId?: number | null
  question: string
}

export interface AiSseFrame {
  chunk: string
  done: boolean
  fallback?: boolean
  logId?: number
  latencyMs?: number
  error?: string
  code?: number
}

export interface AiParseScheduleRequest {
  text: string
}

export interface AiParsedScheduleVO {
  title: string
  description: string | null
  startTime: string
  endTime: string | null
  remindTime: string | null
  priority: SchedulePriority
  fallback: boolean
}

export interface AiChatLogVO {
  id: number
  noteId: number | null
  noteTitle: string | null
  featureType: AiFeatureType
  provider: string | null
  modelName: string | null
  promptText: string
  responseText: string
  promptTokens: number | null
  completionTokens: number | null
  latencyMs: number | null
  success: boolean
  errorMessage: string | null
  createdAt: string
}

export interface AiLogQueryParams {
  noteId?: number
  featureType?: AiFeatureType
  pageNum?: number
  pageSize?: number
}

export const AI_FEATURE_LABELS: Record<AiFeatureType, string> = {
  SUMMARY: '笔记摘要',
  TAG_SUGGEST: '标签推荐',
  NOTE_QA: '流式问答',
  SCHEDULE_PARSE: '日程解析',
}