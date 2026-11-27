<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Clock,
  Document,
  Collection,
  Refresh,
} from '@element-plus/icons-vue'
import { getDashboardOverview } from '@/api/dashboard'
import type { DashboardOverviewVO } from '@/types/dashboard'
import { useEchart } from '@/composables/useEchart'
import type { EChartsOption } from 'echarts'

const router = useRouter()
const loading = ref(false)
const overview = ref<DashboardOverviewVO | null>(null)

const heatmapRef = ref<HTMLElement | null>(null)
const trendRef = ref<HTMLElement | null>(null)
const notebookRef = ref<HTMLElement | null>(null)
const tagRef = ref<HTMLElement | null>(null)

const { setOption: setHeatmap } = useEchart(heatmapRef)
const { setOption: setTrend } = useEchart(trendRef)
const { setOption: setNotebook } = useEchart(notebookRef)
const { setOption: setTag } = useEchart(tagRef)

const summary = computed(
  () =>
    overview.value?.summary ?? {
      totalNotes: 0,
      todayPendingSchedules: 0,
      monthCompletionRate: 0,
      weekFocusMinutes: 0,
      todayFocusMinutes: 0,
    },
)

function heatmapRange(): [string, string] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 89)
  const fmt = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return [fmt(start), fmt(end)]
}

function buildHeatmapOption(data: DashboardOverviewVO): EChartsOption {
  const [rangeStart, rangeEnd] = heatmapRange()
  const map = new Map(data.heatmap.map((h) => [h.date, h.count]))
  const seriesData: [string, number][] = []
  const cursor = new Date(rangeStart)
  const end = new Date(rangeEnd)
  while (cursor <= end) {
    const key = `${cursor.getFullYear()}-${String(cursor.getMonth() + 1).padStart(2, '0')}-${String(cursor.getDate()).padStart(2, '0')}`
    seriesData.push([key, map.get(key) ?? 0])
    cursor.setDate(cursor.getDate() + 1)
  }
  const maxVal = Math.max(1, ...seriesData.map(([, v]) => v))

  return {
    tooltip: {
      formatter: (p: unknown) => {
        const params = p as { value: [string, number] }
        const [date, count] = params.value
        return `${date}<br/>活跃度：<b>${count}</b>`
      },
    },
    visualMap: {
      min: 0,
      max: maxVal,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: {
        color: ['#ebedf0', '#c6e6f5', '#6bb8e8', '#2d9cdb', '#10b981'],
      },
    },
    calendar: {
      top: 50,
      left: 40,
      right: 20,
      bottom: 56,
      range: [rangeStart, rangeEnd],
      cellSize: ['auto', 14],
      itemStyle: { borderWidth: 2, borderColor: '#fff' },
      yearLabel: { show: false },
      dayLabel: { firstDay: 1, nameMap: 'cn' },
      monthLabel: { nameMap: 'cn' },
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: seriesData,
      },
    ],
  }
}

function buildTrendOption(data: DashboardOverviewVO): EChartsOption {
  const dates = data.weeklyTrend.map((d) => d.date.slice(5))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['笔记更新', '日程完成', '专注分钟'], bottom: 0 },
    grid: { left: 48, right: 48, top: 40, bottom: 48 },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '次数', minInterval: 1 },
      { type: 'value', name: '分钟', minInterval: 1 },
    ],
    series: [
      {
        name: '笔记更新',
        type: 'bar',
        data: data.weeklyTrend.map((d) => d.notesUpdated),
        itemStyle: { color: '#3b82f6' },
      },
      {
        name: '日程完成',
        type: 'bar',
        data: data.weeklyTrend.map((d) => d.schedulesCompleted),
        itemStyle: { color: '#8b5cf6' },
      },
      {
        name: '专注分钟',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: data.weeklyTrend.map((d) => d.focusMinutes),
        itemStyle: { color: '#10b981' },
      },
    ],
  }
}

function buildNotebookOption(data: DashboardOverviewVO): EChartsOption {
  const items = data.notebookDistribution
  if (!items.length || summary.value.totalNotes === 0) {
    return {
      title: {
        text: '暂无笔记数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#9ca3af', fontSize: 14, fontWeight: 'normal' },
      },
    }
  }
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 8, top: 16, bottom: 16 },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: items.map((n) => ({ name: n.name, value: n.count })),
      },
    ],
  }
}

function buildTagOption(data: DashboardOverviewVO): EChartsOption {
  const items = [...data.tagDistribution].sort((a, b) => a.count - b.count)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 24, top: 16, bottom: 24 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: items.map((t) => t.name),
    },
    series: [
      {
        type: 'bar',
        data: items.map((t) => ({
          value: t.count,
          itemStyle: { color: t.color || '#409EFF' },
        })),
        barMaxWidth: 24,
      },
    ],
  }
}

function renderCharts() {
  if (!overview.value) return
  const v = overview.value
  setHeatmap(buildHeatmapOption(v))
  setTrend(buildTrendOption(v))
  setNotebook(buildNotebookOption(v))
  setTag(buildTagOption(v))
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await getDashboardOverview()
    overview.value = res.data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

watch(overview, () => {
  void nextTick().then(renderCharts)
})

function go(path: string) {
  void router.push(path)
}

onMounted(loadOverview)
</script>

<template>
  <div v-loading="loading" class="dashboard-page page-shell">
    <div class="kpi-row">
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-icon" style="background: #dbeafe"><el-icon :size="22" color="#2563eb"><Collection /></el-icon></div>
        <div>
          <div class="kpi-num">{{ summary.totalNotes }}</div>
          <div class="kpi-txt">累计笔记</div>
        </div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-icon" style="background: #fef3c7"><el-icon :size="22" color="#d97706"><Calendar /></el-icon></div>
        <div>
          <div class="kpi-num">{{ summary.todayPendingSchedules }}</div>
          <div class="kpi-txt">今日待办</div>
        </div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-icon" style="background: #ede9fe"><el-icon :size="22" color="#7c3aed"><Document /></el-icon></div>
        <div>
          <div class="kpi-num">{{ summary.monthCompletionRate }}%</div>
          <div class="kpi-txt">本月日程完成率</div>
        </div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-icon" style="background: #d1fae5"><el-icon :size="22" color="#059669"><Clock /></el-icon></div>
        <div>
          <div class="kpi-num">{{ summary.weekFocusMinutes }}</div>
          <div class="kpi-txt">本周专注（分钟）</div>
          <div class="kpi-sub">今日 {{ summary.todayFocusMinutes }} 分钟</div>
        </div>
      </el-card>
    </div>

    <div class="quick-actions">
      <el-button type="primary" @click="go('/notes')">新建笔记</el-button>
      <el-button @click="go('/schedules')">管理日程</el-button>
      <el-button @click="go('/focus')">开启番茄钟</el-button>
      <el-button :icon="Refresh" @click="loadOverview">刷新数据</el-button>
    </div>

    <div class="charts-grid">
      <el-card shadow="never" class="chart-card wide">
        <template #header>近 90 天学习活跃度</template>
        <div ref="heatmapRef" class="chart-box tall" />
      </el-card>
      <el-card shadow="never" class="chart-card wide">
        <template #header>近 7 天学习趋势</template>
        <div ref="trendRef" class="chart-box" />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>笔记本分布</template>
        <div ref="notebookRef" class="chart-box" />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>热门标签分布</template>
        <div ref="tagRef" class="chart-box" />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1024px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .kpi-row {
    grid-template-columns: 1fr;
  }
}

.kpi-card {
  border-radius: 12px;
  border: 1px solid var(--ssp-border);
}

.kpi-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
}

.kpi-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-num {
  font-size: 1.5rem;
  font-weight: 700;
}

.kpi-txt {
  font-size: 0.85rem;
  color: var(--ssp-muted);
}

.kpi-sub {
  font-size: 0.75rem;
  color: var(--ssp-muted);
  margin-top: 2px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  border-radius: 12px;
  border: 1px solid var(--ssp-border);
}

.chart-card.wide {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

.chart-box {
  height: 280px;
  width: 100%;
}

.chart-box.tall {
  height: 200px;
}
</style>