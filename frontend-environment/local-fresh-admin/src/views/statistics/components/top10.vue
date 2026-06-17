<template>
  <div class="container top10">
    <h2 class="homeTitle">銷量排名 TOP10</h2>
    <div class="charBox">
      <div id="top" style="width: 100%; height: 380px"></div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import * as echarts from 'echarts'
@Component({
  name: 'Top',
})
export default class extends Vue {
  @Prop() private top10data!: any
  @Watch('top10data')
  getData() {
    this.$nextTick(() => {
      this.initChart()
    })
  }
  initChart() {
    type EChartsOption = echarts.EChartsOption
    const chartDom = document.getElementById('top') as any
    const myChart = echarts.init(chartDom)
    var option: any
    option = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#fff', // 背景顏色
        borderRadius: 2, // 邊框圓角
        textStyle: {
          color: '#333', //字型顏色
          fontSize: 12, //字型大小
          fontWeight: 300,
        },
      },
      grid: {
        top: '-10px',
        left: '0',
        right: '0',
        bottom: '0',
        containLabel: true,
      },
      xAxis: {
        show: false,
      },
      yAxis: {
        // 隱藏 y 軸座標軸
        axisLine: {
          show: false,
        },
        // 隱藏 y 軸刻度線
        axisTick: {
          show: false,
          alignWithLabel: true,
        },
        type: 'category',
        // interval: 100,
        axisLabel: {
          textStyle: {
            color: '#666',
            fontSize: '12px',
          },
          // formatter: "{value} ml", // 單位
        },
        data: this.top10data.nameList,
      },
      series: [
        {
          data: this.top10data.numberList,
          type: 'bar',
          showBackground: true,
          backgroundStyle: {
            color: '#F3F4F7',
          },
          barWidth: 20,
          barGap: '80%' /*多筆並排柱子設定柱子之間的間距*/,
          barCategoryGap: '80%' /*多筆並排柱子設定柱子之間的間距*/,

          itemStyle: {
            emphasis: {
              barBorderRadius: 30,
            },
            normal: {
              barBorderRadius: [0, 10, 10, 0], // 圓角
              color: new echarts.graphic.LinearGradient( // 漸層色
                1,
                0,
                0,
                0, // 漸層色的起止位置, 右/下/左/上
                [
                  // offset 位置
                  { offset: 0, color: '#FFBD00' },
                  { offset: 1, color: '#FFD000' },
                ]
              ),
              label: {
                //內容樣式
                show: true,
                formatter: '{@score}',
                color: '#333',
                // position: "insideLeft", // 內部左對齊
                position: ['8', '5'], // 自訂位置第一個參數為 x 軸方向，第二個參數為 y 軸方向
              },
            },
          },
          // label: {
          //   show: true,
          //   position: "left",
          //   valueAnimation: true,
          // },
        },
      ],
    }
    option && myChart.setOption(option)
  }
}
</script>
