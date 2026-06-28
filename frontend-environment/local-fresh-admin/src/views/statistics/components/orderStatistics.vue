<template>
  <div class="container">
    <h2 class="homeTitle">訂單統計</h2>
    <div class="charBox">
      <div class="orderProportion">
        <div>
          <p>訂單完成率</p>
          <p>{{ (orderdata.orderCompletionRate * 100).toFixed(1) }}%</p>
        </div>
        <div class="symbol">=</div>
        <div>
          <p>有效訂單</p>
          <p>{{ orderdata.validOrderCount }}</p>
        </div>
        <div class="symbol">/</div>
        <div>
          <p>訂單總數</p>
          <p>{{ orderdata.totalOrderCount }}</p>
        </div>
      </div>
      <div id="ordermain" style="width: 100%; height: 300px"></div>
      <ul class="orderListLine">
        <li class="one"><span></span>訂單總數（筆）</li>
        <li class="three"><span></span>有效訂單（筆）</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import * as echarts from 'echarts'
@Component({
  name: 'OrderStatistics',
})
export default class extends Vue {
  @Prop() private orderdata!: any
  @Prop() private overviewData!: any

  @Watch('orderdata')
  getData() {
    this.$nextTick(() => {
      this.initChart()
    })
  }
  initChart() {
    type EChartsOption = echarts.EChartsOption
    const chartDom = document.getElementById('ordermain') as any
    const myChart = echarts.init(chartDom)
    // // 循環遍歷出 x 軸資料
    // const baseDate = this.orderdata.list.map((item) => {
    //   return (item as any).date
    // })
    // const baseAmount = this.orderdata.list.map((item) => {
    //   return (item as any).amount
    // })
    // const baseValidNum = this.orderdata.list.map((item) => {
    //   return (item as any).accomplishNum
    // })
    // const baseAccomplishNum = this.orderdata.list.map((item) => {
    //   return (item as any).accomplishNum
    // })
    var option: any
    option = {
      // legend: {
      //   itemHeight: 3, // 圖例高
      //   itemWidth: 12, // 圖例寬
      //   icon: 'rect', // 圖例
      //   show: true,
      //   top: 'bottom',
      //   data: ['訂單完成率', '有效訂單', '訂單總數'],
      // },
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
        top: '5%',
        left: '20',
        right: '50',
        bottom: '12%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        axisLabel: {
          // X 軸字型顏色
          textStyle: {
            color: '#666',
            fontSize: '12px',
          },
        },
        axisLine: {
          // X 軸線顏色
          lineStyle: {
            color: '#E5E4E4',
            width: 1, // X 軸線寬度
          },
        },
        data: this.orderdata.data.dateList, // 後端回傳的動態資料
      },
      yAxis: [
        {
          type: 'value',
          min: 0,
          //max: 500,
          interval: 50,
          axisLabel: {
            textStyle: {
              color: '#666',
              fontSize: '12px',
            },
            // formatter: "{value} ml", // 單位
          },
        }, // 左側值
      ],
      series: [
        {
          name: '訂單總數',
          type: 'line',
          // stack: 'Total',
          smooth: false, // 是否平滑曲線
          showSymbol: false, // 不顯示滑鼠移入圓點
          symbolSize: 10,
          // symbol:"circle", // 設定折線點定位實心點
          itemStyle: {
            normal: {
              color: '#FFD000',
              lineStyle: {
                color: '#FFD000',
              },
            },
            emphasis: {
              color: '#fff',
              borderWidth: 5,
              borderColor: '#FFC100',
            },
          },

          data: this.orderdata.data.orderCountList,
        },
        {
          name: '有效訂單',
          type: 'line',
          // stack: 'Total',
          smooth: false, // 是否平滑曲線
          showSymbol: false, // 不顯示滑鼠移入圓點
          symbolSize: 10, // 圓點大小
          // symbol:"circle", // 設定折線點定位實心點
          itemStyle: {
            normal: {
              color: '#FD7F7F',
              lineStyle: {
                color: '#FD7F7F',
              },
            },
            emphasis: {
              // 圓點顏色
              color: '#fff',
              borderWidth: 5,
              borderColor: '#FD7F7F',
            },
          },

          data: this.orderdata.data.validOrderCountList,
        }
      ],
    }
    option && myChart.setOption(option)
  }
}
</script>
