<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { parseScheduleByAi } from '@/api/ai'
import type { AiParsedScheduleVO } from '@/types/ai'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  parsed: [data: AiParsedScheduleVO]
}>()

const text = ref('')
const loading = ref(false)

const examples = [
  '明天晚上8点前交软件体系结构报告，重要且紧急',
  '下周三上午10点复习数据库事务与锁',
]

watch(
  () => props.visible,
  (open) => {
    if (open) text.value = ''
  },
)

async function onParse() {
  const t = text.value.trim()
  if (!t) return
  loading.value = true
  try {
    const res = await parseScheduleByAi({ text: t })
    emit('parsed', res.data)
    emit('update:visible', false)
    if (res.data.fallback) {
      ElMessage.info('已使用本地降级解析，请核对时间字段')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="AI 一句话建日程"
    width="520px"
    @update:model-value="emit('update:visible', $event)"
  >
    <p class="hint">描述你的待办，AI 会解析标题、时间与优先级。</p>
    <el-input v-model="text" type="textarea" :rows="4" placeholder="输入自然语言…" />
    <div class="chips">
      <el-tag
        v-for="ex in examples"
        :key="ex"
        class="chip"
        effect="plain"
        @click="text = ex"
      >
        {{ ex }}
      </el-tag>
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="onParse">智能解析</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  margin: 0 0 10px;
  color: var(--ssp-muted);
  font-size: 0.88rem;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.chip {
  cursor: pointer;
}
</style>