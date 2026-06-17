<template>
  <div class="container">
    <h2 class="homeTitle">會員統計</h2>
    <div class="charBox">
      <div id="usermain" style="width: 100%; height: 320px"></div>
      <ul class="orderListLine user">
        <li class="one"><span></span>會員總量（個）</li>
        <li class="three"><span></span>新增會員（個）</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import * as echarts from 'echarts'
@Component({
  name: 'UserStatistics',
})
export default class extends Vue {
  @Prop() private userdata!: any
  @Watch('userdata')
  getData() {
    this.$nextTick(() => {
      this.initChart()
    })
  }
  initChart() {
    type EChartsOption = echarts.EChartsOption
    const chartDom = document.getElementById('usermain') as any
    const myChart = echarts.init(chartDom)
    var option: any
    option = {
      // legend: {
      //   itemHeight: 3, // 圖例高
      //   itemWidth: 12, // 圖例寬
      //   icon: 'rect', // 圖例
      //   show: true,
      //   top: 'bottom',
      //   data: ['會員總量', '新增會員'],
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
        data: this.userdata.dateList, // 後端回傳的動態資料
      },
      yAxis: [
        {
          type: 'value',
          min: 0,
          //max: 500,
          //interval: 100,
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
          name: '會員總量',
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

          data: this.userdata.totalUserList,
        },
        {
          name: '新增會員',
          type: 'line',
          // stack: 'Total',
          smooth: false, // 是否平滑曲線
          showSymbol: false, // 不顯示滑鼠移入圓點
          symbolSize: 10, // 圓點大小
          // symbol:"circle", // 設定折線點定位實心點
          itemStyle: {
            normal: {
              color: '#FD7F7F',
              fontWeigth: 300,
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

          data: this.userdata.newUserList,
        },
      ],
    }
    option && myChart.setOption(option)
  }
}
</script>
<style scoped>
</style>
