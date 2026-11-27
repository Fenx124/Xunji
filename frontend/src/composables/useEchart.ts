import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

/** 挂载 ECharts 实例，自动 ResizeObserver + dispose */
export function useEchart(containerRef: Ref<HTMLElement | null>) {
  let chart: echarts.ECharts | null = null
  let ro: ResizeObserver | null = null

  const ready = ref(false)

  function handleResize() {
    chart?.resize()
  }

  function ensureChart() {
    if (!containerRef.value) return null
    if (!chart) {
      chart = echarts.init(containerRef.value)
      ro = new ResizeObserver(handleResize)
      ro.observe(containerRef.value)
      window.addEventListener('resize', handleResize)
      ready.value = true
    }
    return chart
  }

  function setOption(option: EChartsOption, notMerge = true) {
    const instance = ensureChart()
    instance?.setOption(option, notMerge)
  }

  onMounted(ensureChart)

  onBeforeUnmount(() => {
    ro?.disconnect()
    window.removeEventListener('resize', handleResize)
    chart?.dispose()
    chart = null
  })

  return { setOption, ready }
}