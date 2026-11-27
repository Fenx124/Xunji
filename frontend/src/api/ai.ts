import request from '@/utils/request'
import type { PageResult, Result } from '@/types/api'
import type {
  AiChatLogVO,
  AiLogQueryParams,
  AiParseScheduleRequest,
  AiParsedScheduleVO,
  AiQaStreamRequest,
  AiSseFrame,
  AiSummarizeRequest,
  AiSummarizeVO,
} from '@/types/ai'
import { streamPostSse, type StreamPostSseCallbacks } from '@/utils/sse'

const AI_TIMEOUT = 45000

export function summarizeNote(data: AiSummarizeRequest) {
  return request.post<unknown, Result<AiSummarizeVO>>('/ai/summarize', data, {
    timeout: AI_TIMEOUT,
  })
}

export function parseScheduleByAi(data: AiParseScheduleRequest) {
  return request.post<unknown, Result<AiParsedScheduleVO>>('/ai/parse-schedule', data, {
    timeout: AI_TIMEOUT,
  })
}

export function listAiLogs(params?: AiLogQueryParams) {
  return request.get<unknown, Result<PageResult<AiChatLogVO>>>('/ai/logs', { params })
}

export function streamAiQa(
  data: AiQaStreamRequest,
  callbacks: Omit<StreamPostSseCallbacks<AiSseFrame>, 'onEvent'> & {
    onEvent: (frame: AiSseFrame) => void
  },
) {
  return streamPostSse<AiSseFrame>('/ai/qa/stream', data, callbacks)
}