<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Promotion, VideoPause } from '@element-plus/icons-vue'
import { listNotes } from '@/api/note'
import { createSchedule } from '@/api/schedule'
import { listAiLogs, parseScheduleByAi, streamAiQa } from '@/api/ai'
import type { NoteListItemVO } from '@/types/note'
import {
  AI_FEATURE_LABELS,
  type AiChatLogVO,
  type AiFeatureType,
  type AiParsedScheduleVO,
  type AiSseFrame,
} from '@/types/ai'
import { PRIORITY_LABELS, type SchedulePriority } from '@/types/schedule'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()

const noteOptions = ref<NoteListItemVO[]>([])
const selectedNoteId = ref<number | null>(null)

const logFeatureFilter = ref<AiFeatureType | ''>('')
const logs = ref<AiChatLogVO[]>([])
const logPageNum = ref(1)
const logPageSize = ref(15)
const logTotal = ref(0)
const logLoading = ref(false)
const logDetail = ref<AiChatLogVO | null>(null)
const logDrawer = ref(false)

type ChatRole = 'user' | 'assistant'
interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  streaming?: boolean
  fallback?: boolean
  latencyMs?: number
}

const activeTab = ref('qa')
const messages = ref<ChatMessage[]>([])
const questionInput = ref('')
const streaming = ref(false)
const streamFallback = ref(false)
const abortRef = ref<AbortController | null>(null)
const chatBoxRef = ref<HTMLElement | null>(null)

const qaChips = [
  '提炼这篇笔记的核心考点',
  '基于笔记出 3 道复习自测题',
  '用通俗例子解释难点',
]

const scheduleText = ref('')
const scheduleParsing = ref(false)
const parsedSchedule = ref<AiParsedScheduleVO | null>(null)
const confirmForm = ref({
  title: '',
  description: '',
  noteId: null as number | null,
  startTime: '',
  endTime: '' as string | null,
  remindTime: '' as string | null,
  priority: 2 as SchedulePriority,
})
const confirmFallback = ref(false)
const creatingSchedule = ref(false)

const scheduleChips = [
  '明天晚上8点前交软件体系结构报告，重要且紧急',
  '下周三上午10点复习数据库事务与锁',
]

async function loadNotes() {
  const res = await listNotes({ pageNum: 1, pageSize: 100 })
  noteOptions.value = res.data.records
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await listAiLogs({
      pageNum: logPageNum.value,
      pageSize: logPageSize.value,
      featureType: logFeatureFilter.value || undefined,
      noteId: selectedNoteId.value ?? undefined,
    })
    logs.value = res.data.records
    logTotal.value = res.data.total
  } finally {
    logLoading.value = false
  }
}

function openLog(item: AiChatLogVO) {
  logDetail.value = item
  logDrawer.value = true
}

function scrollChatToBottom() {
  nextTick(() => {
    const el = chatBoxRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function stopStream() {
  abortRef.value?.abort()
  abortRef.value = null
  streaming.value = false
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'assistant' && last.streaming) {
    last.streaming = false
  }
}

async function sendQuestion(prefill?: string) {
  const q = (prefill ?? questionInput.value).trim()
  if (!q || streaming.value) return
  questionInput.value = ''
  const userMsg: ChatMessage = { id: `u-${Date.now()}`, role: 'user', content: q }
  const assistantMsg: ChatMessage = {
    id: `a-${Date.now()}`,
    role: 'assistant',
    content: '',
    streaming: true,
  }
  messages.value.push(userMsg, assistantMsg)
  scrollChatToBottom()

  streaming.value = true
  streamFallback.value = false
  const controller = new AbortController()
  abortRef.value = controller

  await streamAiQa(
    {
      noteId: selectedNoteId.value,
      question: q,
    },
    {
      signal: controller.signal,
      onEvent: (frame: AiSseFrame) => {
        if (frame.error) {
          ElMessage.error(frame.error)
          return
        }
        if (frame.fallback) streamFallback.value = true
        if (frame.chunk) {
          assistantMsg.content += frame.chunk
          scrollChatToBottom()
        }
        if (frame.done) {
          assistantMsg.streaming = false
          assistantMsg.fallback = frame.fallback ?? streamFallback.value
          assistantMsg.latencyMs = frame.latencyMs
          streaming.value = false
          abortRef.value = null
          loadLogs()
        }
      },
      onError: (message, code) => {
        if (controller.signal.aborted) return
        assistantMsg.streaming = false
        if (!assistantMsg.content) {
          assistantMsg.content = message || '问答失败，请稍后重试'
        }
        streaming.value = false
        abortRef.value = null
        if (code === 42901) {
          ElMessage.warning(message || 'AI 调用过于频繁，请稍后再试')
        } else if (code === 50301) {
          ElMessage.warning(message || 'AI 服务暂不可用')
        } else if (code !== undefined) {
          ElMessage.error(message)
        } else {
          ElMessage.error(message)
        }
      },
      onComplete: () => {
        if (controller.signal.aborted) {
          streaming.value = false
          abortRef.value = null
        }
      },
    },
  )
}

function onQuestionKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    void sendQuestion()
  }
}

async function parseSchedule() {
  const text = scheduleText.value.trim()
  if (!text) return
  scheduleParsing.value = true
  parsedSchedule.value = null
  try {
    const res = await parseScheduleByAi({ text })
    parsedSchedule.value = res.data
    confirmFallback.value = res.data.fallback
    confirmForm.value = {
      title: res.data.title,
      description: res.data.description || '',
      noteId: selectedNoteId.value,
      startTime: res.data.startTime,
      endTime: res.data.endTime,
      remindTime: res.data.remindTime,
      priority: res.data.priority,
    }
  } finally {
    scheduleParsing.value = false
  }
}

async function confirmCreateSchedule() {
  if (!confirmForm.value.title.trim() || !confirmForm.value.startTime) return
  creatingSchedule.value = true
  try {
    await createSchedule({
      title: confirmForm.value.title.trim(),
      description: confirmForm.value.description || null,
      noteId: confirmForm.value.noteId,
      startTime: confirmForm.value.startTime,
      endTime: confirmForm.value.endTime || null,
      remindTime: confirmForm.value.remindTime || null,
      priority: confirmForm.value.priority,
      sourceType: 'AI',
    })
    ElMessage.success({
      message: '日程已创建',
      duration: 4000,
    })
    void router.push('/schedules')
  } finally {
    creatingSchedule.value = false
  }
}

watch(selectedNoteId, () => {
  logPageNum.value = 1
  void loadLogs()
})

watch(logFeatureFilter, () => {
  logPageNum.value = 1
  void loadLogs()
})

watch(logPageNum, () => void loadLogs())

onMounted(async () => {
  await loadNotes()
  const q = route.query.noteId
  if (q) {
    const id = Number(q)
    if (!Number.isNaN(id)) selectedNoteId.value = id
  }
  await loadLogs()
})
</script>

<template>
  <div class="ai-studio page-shell">
    <aside class="ai-side">
      <h2 class="side-title"><el-icon><MagicStick /></el-icon> AI 学习工作室</h2>
      <label class="field-label">上下文笔记</label>
      <el-select
        v-model="selectedNoteId"
        filterable
        clearable
        placeholder="不限定笔记（通用学习问答）"
        class="note-select"
      >
        <el-option :value="null" label="不限定笔记（通用学习问答）" />
        <el-option v-for="n in noteOptions" :key="n.id" :label="n.title" :value="n.id" />
      </el-select>

      <div class="log-head">
        <span class="field-label">最近 AI 交互</span>
        <el-select v-model="logFeatureFilter" clearable placeholder="功能类型" size="small" style="width: 120px">
          <el-option
            v-for="(label, key) in AI_FEATURE_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
      </div>
      <el-skeleton v-if="logLoading && !logs.length" :rows="4" animated />
      <ul v-else class="log-list">
        <li v-for="item in logs" :key="item.id" @click="openLog(item)">
          <el-tag size="small" type="info">{{ AI_FEATURE_LABELS[item.featureType] }}</el-tag>
          <span class="log-preview">{{ item.promptText.slice(0, 48) }}…</span>
          <span class="log-time">{{ formatDateTime(item.createdAt) }}</span>
        </li>
        <li v-if="!logs.length" class="muted">暂无记录</li>
      </ul>
      <el-pagination
        v-if="logTotal > logPageSize"
        v-model:current-page="logPageNum"
        :page-size="logPageSize"
        :total="logTotal"
        layout="prev, pager, next"
        small
        class="log-pager"
      />
    </aside>

    <main class="ai-main">
      <el-tabs v-model="activeTab" class="main-tabs">
        <el-tab-pane label="笔记流式问答" name="qa">
          <div ref="chatBoxRef" class="chat-box">
            <el-empty v-if="!messages.length" description="输入问题开始对话，或点击下方快捷提示" />
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="bubble-row"
              :class="msg.role"
            >
              <div class="bubble" :class="msg.role">
                <p class="bubble-text">{{ msg.content }}<span v-if="msg.streaming" class="cursor">▍</span></p>
                <div v-if="msg.role === 'assistant' && !msg.streaming" class="bubble-meta">
                  <el-tag v-if="msg.fallback" size="small" type="warning">本地降级模式</el-tag>
                  <span v-if="msg.latencyMs != null" class="latency">{{ msg.latencyMs }} ms</span>
                </div>
              </div>
            </div>
          </div>
          <div class="chip-row">
            <el-tag
              v-for="c in qaChips"
              :key="c"
              class="chip"
              effect="plain"
              @click="sendQuestion(c)"
            >
              {{ c }}
            </el-tag>
          </div>
          <div class="input-row">
            <el-input
              v-model="questionInput"
              type="textarea"
              :rows="2"
              placeholder="Enter 发送，Shift+Enter 换行"
              :disabled="streaming"
              @keydown="onQuestionKeydown"
            />
            <div class="input-actions">
              <el-button
                v-if="streaming"
                type="warning"
                :icon="VideoPause"
                @click="stopStream"
              >
                停止生成
              </el-button>
              <el-button
                type="primary"
                :icon="Promotion"
                :loading="streaming"
                :disabled="streaming"
                @click="sendQuestion()"
              >
                发送
              </el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="AI 一句话建日程" name="schedule">
          <p class="hint">用自然语言描述待办，AI 将解析为可编辑的日程字段。</p>
          <el-input
            v-model="scheduleText"
            type="textarea"
            :rows="3"
            placeholder="例如：明天下午3点小组讨论需求分析"
          />
          <div class="chip-row">
            <el-tag
              v-for="c in scheduleChips"
              :key="c"
              class="chip"
              effect="plain"
              @click="scheduleText = c"
            >
              {{ c }}
            </el-tag>
          </div>
          <el-button type="primary" :loading="scheduleParsing" @click="parseSchedule">
            智能解析
          </el-button>

          <el-card v-if="parsedSchedule" class="confirm-card" shadow="never">
            <template #header>
              <span>确认日程信息</span>
              <el-tag v-if="confirmFallback" type="warning" size="small">本地降级模式</el-tag>
            </template>
            <el-form label-width="88px">
              <el-form-item label="标题" required>
                <el-input v-model="confirmForm.title" maxlength="128" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="confirmForm.description" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item label="开始时间" required>
                <el-date-picker
                  v-model="confirmForm.startTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="结束时间">
                <el-date-picker
                  v-model="confirmForm.endTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  clearable
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="提醒时间">
                <el-date-picker
                  v-model="confirmForm.remindTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  clearable
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="优先级">
                <el-select v-model="confirmForm.priority" style="width: 100%">
                  <el-option :value="1" :label="PRIORITY_LABELS[1]" />
                  <el-option :value="2" :label="PRIORITY_LABELS[2]" />
                  <el-option :value="3" :label="PRIORITY_LABELS[3]" />
                  <el-option :value="4" :label="PRIORITY_LABELS[4]" />
                </el-select>
              </el-form-item>
              <el-form-item label="关联笔记">
                <el-select v-model="confirmForm.noteId" filterable clearable style="width: 100%">
                  <el-option v-for="n in noteOptions" :key="n.id" :label="n.title" :value="n.id" />
                </el-select>
              </el-form-item>
              <el-button type="success" :loading="creatingSchedule" @click="confirmCreateSchedule">
                确认创建日程
              </el-button>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-drawer v-model="logDrawer" title="AI 交互详情" size="480px">
      <template v-if="logDetail">
        <p><strong>功能</strong>：{{ AI_FEATURE_LABELS[logDetail.featureType] }}</p>
        <p v-if="logDetail.noteTitle"><strong>笔记</strong>：{{ logDetail.noteTitle }}</p>
        <p><strong>时间</strong>：{{ formatDateTime(logDetail.createdAt) }}</p>
        <el-divider />
        <h4>提问</h4>
        <pre class="log-block">{{ logDetail.promptText }}</pre>
        <h4>回复</h4>
        <pre class="log-block">{{ logDetail.responseText }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.ai-studio {
  display: flex;
  gap: 16px;
  min-height: calc(100vh - 56px);
  padding-top: 12px;
}

.ai-side {
  width: 280px;
  flex-shrink: 0;
  background: var(--ssp-surface);
  border: 1px solid var(--ssp-border);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 80px);
}

.side-title {
  margin: 0;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-label {
  font-size: 0.82rem;
  color: var(--ssp-muted);
}

.note-select {
  width: 100%;
}

.log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.log-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
  min-height: 120px;
}

.log-list li {
  padding: 8px 6px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8rem;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-list li:hover {
  background: #f3f4f6;
}

.log-preview {
  color: #334155;
  line-height: 1.35;
}

.log-time {
  color: var(--ssp-muted);
  font-size: 0.72rem;
}

.log-pager {
  justify-content: center;
}

.muted {
  color: var(--ssp-muted);
  font-size: 0.85rem;
  padding: 8px;
}

.ai-main {
  flex: 1;
  min-width: 0;
  background: var(--ssp-surface);
  border: 1px solid var(--ssp-border);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
}

.main-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.el-tab-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 480px;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px 16px;
  min-height: 280px;
  max-height: calc(100vh - 320px);
}

.bubble-row {
  display: flex;
  margin-bottom: 12px;
}

.bubble-row.user {
  justify-content: flex-end;
}

.bubble-row.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 0.9rem;
  line-height: 1.55;
}

.bubble.user {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble.assistant {
  background: #f1f5f9;
  color: #0f172a;
  border-bottom-left-radius: 4px;
}

.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.cursor {
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.latency {
  font-size: 0.75rem;
  color: var(--ssp-muted);
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.chip {
  cursor: pointer;
}

.input-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.hint {
  color: var(--ssp-muted);
  font-size: 0.88rem;
  margin: 0 0 10px;
}

.confirm-card {
  margin-top: 16px;
}

.log-block {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  padding: 10px;
  border-radius: 8px;
  font-size: 0.85rem;
}
</style>