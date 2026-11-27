<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { getAdminAiStats } from '@/api/admin'
import type { AdminAiStatsVO } from '@/types/admin'
import { useEchart } from '@/composables/useEchart'
import type { EChartsOption } from 'echarts'

const daysOptions = [7, 14, 30, 90] as const
const days = ref<number>(7)
const loading = ref(false)
const stats = ref<AdminAiStatsVO | null>(null)

const trendRef = ref<HTMLElement | null>(null)
const featureRef = ref<HTMLElement | null>(null)
const { setOption: setTrend } = useEchart(trendRef)
const { setOption: setFeature } = useEchart(featureRef)

const summary = computed(
  () =>
    stats.value ?? {
      totalCalls: 0,
      successCalls: 0,
      successRate: 0,
      avgLatencyMs: 0,
      totalTokens: 0,
      byFeature: [],
      byUser: [],
      dailyTrend: [],
    },
)

const FEATURE_LABELS: Record<string, string> = {
  NOTE_QA: '笔记问答',
  NOTE_SUMMARY: '笔记摘要',
  SCHEDULE_PARSE: '日程解析',
}

function featureLabel(ft: string) {
  return FEATURE_LABELS[ft] ?? ft
}

function buildTrendOption(data: AdminAiStatsVO): EChartsOption {
  if (!data.dailyTrend.length) {
    return {
      title: {
        text: '暂无调用数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#9ca3af', fontSize: 14, fontWeight: 'normal' },
      },
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['总调用', '成功'], bottom: 0 },
    grid: { left: 48, right: 24, top: 32, bottom: 48 },
    xAxis: { type: 'category', data: data.dailyTrend.map((d) => d.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '总调用',
        type: 'line',
        smooth: true,
        data: data.dailyTrend.map((d) => d.calls),
        itemStyle: { color: '#3b82f6' },
      },
      {
        name: '成功',
        type: 'line',
        smooth: true,
        data: data.dailyTrend.map((d) => d.successCalls),
        itemStyle: { color: '#10b981' },
      },
    ],
  }
}

function buildFeatureOption(data: AdminAiStatsVO): EChartsOption {
  if (!data.byFeature.length) {
    return {
      title: {
        text: '暂无功能分布',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#9ca3af', fontSize: 14, fontWeight: 'normal' },
      },
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 24, bottom: 48 },
    xAxis: {
      type: 'category',
      data: data.byFeature.map((f) => featureLabel(f.featureType)),
      axisLabel: { interval: 0, rotate: data.byFeature.length > 4 ? 20 : 0 },
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        data: data.byFeature.map((f) => f.calls),
        itemStyle: { color: '#8b5cf6' },
        barMaxWidth: 48,
      },
    ],
  }
}

function renderCharts() {
  if (!stats.value) return
  setTrend(buildTrendOption(stats.value))
  setFeature(buildFeatureOption(stats.value))
}

async function loadStats() {
  loading.value = true
  try {
    const res = await getAdminAiStats(days.value)
    stats.value = res.data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

watch(days, loadStats)

onMounted(loadStats)
</script>

<template>
  <div v-loading="loading" class="admin-page page-shell">
    <div class="toolbar">
      <span class="toolbar-label">统计周期</span>
      <el-radio-group v-model="days">
        <el-radio-button v-for="d in daysOptions" :key="d" :value="d">{{ d }} 天</el-radio-button>
      </el-radio-group>
    </div>

    <div class="kpi-row">
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-num">{{ summary.totalCalls }}</div>
        <div class="kpi-txt">总调用次数</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-num">{{ summary.successRate }}%</div>
        <div class="kpi-txt">成功率</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-num">{{ summary.avgLatencyMs }}</div>
        <div class="kpi-txt">平均耗时 (ms)</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-num">{{ summary.totalTokens }}</div>
        <div class="kpi-txt">累计 Token</div>
      </el-card>
    </div>

    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>每日调用趋势</template>
        <div ref="trendRef" class="chart-box" />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>按功能类型分布</template>
        <div ref="featureRef" class="chart-box" />
      </el-card>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>用户 AI 用量</template>
      <el-table :data="summary.byUser" stripe empty-text="暂无用户调用数据">
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="calls" label="周期调用数" width="110" align="center" />
        <el-table-column label="今日 / 配额" min-width="220">
          <template #default="{ row }">
            <div class="quota-cell">
              <span>{{ row.todayCalls }} / {{ row.aiDailyLimit }}</span>
              <el-progress
                :percentage="
                  row.aiDailyLimit > 0
                    ? Math.min(100, Math.round((row.todayCalls / row.aiDailyLimit) * 100))
                    : 0
                "
                :stroke-width="8"
                :show-text="false"
                style="flex: 1; max-width: 140px"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-label {
  font-size: 0.9rem;
  color: var(--ssp-muted);
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  border-radius: 12px;
  border: 1px solid var(--ssp-border);
  text-align: center;
}

.kpi-num {
  font-size: 1.5rem;
  font-weight: 700;
}

.kpi-txt {
  font-size: 0.85rem;
  color: var(--ssp-muted);
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}

.chart-card,
.table-card {
  border-radius: 12px;
  border: 1px solid var(--ssp-border);
}

.chart-box {
  height: 260px;
  width: 100%;
}

.quota-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>