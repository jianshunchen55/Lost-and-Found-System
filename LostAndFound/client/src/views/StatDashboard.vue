<template>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;padding:24px;background:#f0f2f5;min-height:calc(100vh - 64px)">
    
    <!-- Chart 1: Categories -->
    <div style="background:#fff;padding:20px;border-radius:8px;box-shadow:0 1px 2px rgba(0,0,0,0.1)">
      <h3 style="margin:0 0 16px 0;color:#333;border-left:4px solid #1890ff;padding-left:12px">失物类别统计</h3>
      <div id="pie" style="height:300px"></div>
      <p style="color:#666;font-size:13px;margin-top:10px;text-align:center">
        展示各类物品的丢失占比，帮助了解哪些物品更容易丢失。
      </p>
    </div>

    <!-- Chart 2: 7-Day Trend -->
    <div style="background:#fff;padding:20px;border-radius:8px;box-shadow:0 1px 2px rgba(0,0,0,0.1)">
      <h3 style="margin:0 0 16px 0;color:#333;border-left:4px solid #52c41a;padding-left:12px">近七日动态趋势</h3>
      <div id="line" style="height:300px"></div>
      <p style="color:#666;font-size:13px;margin-top:10px;text-align:center">
        统计最近一周的失物发布、拾物发布及成功认领数量。
      </p>
    </div>

    <!-- Chart 3: Hot Locations -->
    <div style="grid-column: span 2;background:#fff;padding:20px;border-radius:8px;box-shadow:0 1px 2px rgba(0,0,0,0.1)">
      <h3 style="margin:0 0 16px 0;color:#333;border-left:4px solid #faad14;padding-left:12px">高频丢失地点 TOP 10</h3>
      <div id="bar" style="height:350px"></div>
      <p style="color:#666;font-size:13px;margin-top:10px;text-align:center">
        展示物品丢失最频繁的区域，管理人员可加强巡逻或设立提示牌。
      </p>
    </div>

  </div>
</template>
<script setup lang="ts">
import * as echarts from 'echarts'
import { onMounted, onUnmounted } from 'vue'
import http from '../api/http'

let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const resizeHandler = () => {
  pieChart?.resize()
  lineChart?.resize()
  barChart?.resize()
}

onMounted(async ()=>{
  pieChart = echarts.init(document.getElementById('pie') as HTMLElement)
  lineChart = echarts.init(document.getElementById('line') as HTMLElement)
  barChart = echarts.init(document.getElementById('bar') as HTMLElement)

  window.addEventListener('resize', resizeHandler)

  try {
    // 1. Categories
    const resType = await http.get('/stat/lostType')
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { top: '5%', left: 'center' },
      series: [{
        name: '物品类别',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: '20', fontWeight: 'bold' } },
        labelLine: { show: false },
        data: resType.data // Expecting [{name, value}]
      }]
    })

    // 2. Trend
    const resTrend = await http.get('/stat/trend')
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['丢失发布', '拾物发布', '成功认领'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: resTrend.data.dates },
      yAxis: { type: 'value' },
      series: [
        { name: '丢失发布', type: 'line', data: resTrend.data.lost, smooth: true, itemStyle:{color:'#ff4d4f'} },
        { name: '拾物发布', type: 'line', data: resTrend.data.found, smooth: true, itemStyle:{color:'#1890ff'} },
        { name: '成功认领', type: 'line', data: resTrend.data.claimed, smooth: true, itemStyle:{color:'#52c41a'} }
      ]
    })

    // 3. Hot Locations
    const resLoc = await http.get('/stat/hotLocation')
    const locNames = resLoc.data.map((i:any) => i.name)
    const locValues = resLoc.data.map((i:any) => i.value)
    
    barChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: locNames, axisLabel: { interval: 0, rotate: 30 } },
      yAxis: { type: 'value' },
      series: [{
        name: '丢失数量',
        type: 'bar',
        barWidth: '40%',
        data: locValues,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        }
      }]
    })

  } catch (e) {
    console.error('Failed to load stats', e)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  pieChart?.dispose()
  lineChart?.dispose()
  barChart?.dispose()
})
</script>
