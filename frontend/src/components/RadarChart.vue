<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { init, use, type ECharts, type ComposeOption } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import {
  LegendComponent,
  RadarComponent,
  type LegendComponentOption,
  type RadarComponentOption,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { RadarSeriesOption } from 'echarts/charts'
import { useThemeStore } from '@/stores/theme'

use([RadarChart, RadarComponent, LegendComponent, CanvasRenderer])

type RadarChartOption = ComposeOption<RadarComponentOption | LegendComponentOption | RadarSeriesOption>
type RadarSeriesData = NonNullable<RadarSeriesOption['data']>

const props = defineProps<{
  current: { dimension: string; label: string; avgScore: number }[]
  previous?: { dimension: string; label: string; avgScore: number }[]
  hasPrevious?: boolean
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null
const theme = useThemeStore()

function render() {
  if (!chartRef.value) return
  if (!chart) chart = init(chartRef.value)
  const isDark = theme.isDark
  const textColor = isDark ? '#8a8a84' : '#4a4a44'
  const axisColor = isDark ? '#272725' : '#eae8e4'

  const indicator = props.current.map(d => ({ name: d.label, max: 100 }))
  const currentData = { name: '本学期', value: props.current.map(d => d.avgScore), lineStyle: { color: '#7C3AED', width: 2 }, areaStyle: { color: 'rgba(124,58,237,0.06)' }, itemStyle: { color: '#7C3AED' } }

  const series: RadarSeriesOption[] = [{ type: 'radar', data: [currentData] as RadarSeriesData, symbol: 'circle', symbolSize: 6 }]
  const legendData = ['本学期']

  if (props.hasPrevious && props.previous?.length) {
    const prevData = { name: '上学期', value: props.previous.map(d => d.avgScore), lineStyle: { color: '#F97316', width: 2, type: 'dashed' as const }, areaStyle: { color: 'rgba(249,115,22,0.04)' }, itemStyle: { color: '#F97316' } }
    ;(series[0].data as RadarSeriesData).push(prevData)
    legendData.push('上学期')
  }

  const option = {
    radar: {
      indicator,
      center: ['50%', '54%'],
      radius: '55%',
      nameGap: 8,
      axisName: { color: textColor, fontSize: 13, overflow: 'truncate', width: 70 },
      splitArea: { areaStyle: { color: [isDark ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.01)', 'transparent'] } },
      axisLine: { lineStyle: { color: axisColor } },
      splitLine: { lineStyle: { color: axisColor } },
    },
    series,
    legend: { data: legendData, bottom: 0, textStyle: { color: textColor, fontSize: 13 } },
  } as RadarChartOption

  chart.setOption(option, true)
}

watch(() => theme.isDark, render)
watch(() => props.current, render, { deep: true })
onMounted(render)
onUnmounted(() => { chart?.dispose(); chart = null })

function handleResize() { chart?.resize() }
onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => window.removeEventListener('resize', handleResize))
</script>

<template>
  <div ref="chartRef" class="radar-chart" />
</template>

<style scoped>
.radar-chart { width: 100%; height: 360px; }
</style>
