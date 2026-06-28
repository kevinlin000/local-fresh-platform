<template>
  <div class="dashboard-container home">
    <!-- 標題 -->
    <TitleIndex @sendTitleInd="getTitleNum" :flag="flag" :tateData="tateData" />
    <!-- end -->
    <div class="homeMain">
      <!-- 營業額統計 -->
      <TurnoverStatistics :turnoverdata="turnoverData" />
      <!-- end -->
      <!-- 會員統計 -->
      <UserStatistics :userdata="userData" />
      <!-- end -->
    </div>
    <div class="homeMain homecon">
      <!-- 訂單統計 -->
      <OrderStatistics :orderdata="orderData" :overviewData="overviewData" />
      <!-- end -->
      <!-- 銷量排名 TOP10 -->
      <Top :top10data="top10Data" />
      <!-- end -->
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import {
  get1stAndToday,
  past7Day,
  past30Day,
  pastWeek,
  pastMonth,
} from '@/utils/formValidate'
import {
  getDataOverView, //資料概覽
  getTurnoverStatistics,
  getUserStatistics,
  getOrderStatistics,
  getTop,
} from '@/api/index'
// 元件
// 標題
import TitleIndex from './components/titleIndex.vue'
// 營業額統計
import TurnoverStatistics from './components/turnoverStatistics.vue'
// 會員統計
import UserStatistics from './components/userStatistics.vue'
// 訂單統計
import OrderStatistics from './components/orderStatistics.vue'
// 排名
import Top from './components/top10.vue'
@Component({
  name: 'Dashboard',
  components: {
    TitleIndex,
    TurnoverStatistics,
    UserStatistics,
    OrderStatistics,
    Top,
  },
})
export default class extends Vue {
  private overviewData = {} as any
  private flag = 2
  private tateData = []
  private turnoverData = {} as any
  private userData = {}
  private orderData = {
    data: {},
  } as any
  private top10Data = {}
  created() {
    //this.init(this.flag)
    this.getTitleNum(2);
  }
  // 取得基本資料
  init(begin: any,end:any) {
    this.$nextTick(() => {
      this.getTurnoverStatisticsData(begin,end)
      this.getUserStatisticsData(begin,end)
      this.getOrderStatisticsData(begin,end)
      this.getTopData(begin,end)
    })
  }

  // 取得營業額統計資料
  async getTurnoverStatisticsData(begin: any ,end:any) {
    const data = await getTurnoverStatistics({ begin: begin,end:end })
    const turnoverData = data.data.data
    this.turnoverData = {
      dateList: turnoverData.dateList.split(','),
      turnoverList: turnoverData.turnoverList.split(',')
    }
    // this.tateData = this.turnoverData.date
    // const arr = []
    // this.tateData.forEach((val) => {
    //   let date = new Date()
    //   let year = date.getFullYear()
    //   arr.push(year + '-' + val)
    // })
    // this.tateData = arr
  }
  // 取得會員統計資料
  async getUserStatisticsData(begin: any ,end:any) {
    const data = await getUserStatistics({ begin: begin,end:end })
    const userData = data.data.data
    this.userData = {
      dateList: userData.dateList.split(','),
      totalUserList: userData.totalUserList.split(','),
      newUserList: userData.newUserList.split(','),
    }
  }
  // 取得訂單統計資料
  async getOrderStatisticsData(begin: any ,end:any) {
    const data = await getOrderStatistics({begin: begin,end:end })
    const orderData = data.data.data
    this.orderData = {
      data: {
        dateList: orderData.dateList.split(','),
        orderCountList: orderData.orderCountList.split(','),
        validOrderCountList: orderData.validOrderCountList.split(','),
        //orderCompletionRateList: orderData.orderCompletionRateList.split(','),
      },
      totalOrderCount: orderData.totalOrderCount,
      validOrderCount: orderData.validOrderCount,
      orderCompletionRate: orderData.orderCompletionRate
    }
  }
  // 取得排行資料
  async getTopData(begin: any ,end:any) {
    const data = await getTop({begin: begin,end:end })
    const top10Data = data.data.data
    this.top10Data = {
      nameList: top10Data.nameList.split(',').reverse(),
      numberList: top10Data.numberList.split(',').reverse(),
    }
  }
  // 取得目前选中的tab時间
  getTitleNum(data) {
    switch (data) {
      case 1:
        this.tateData = get1stAndToday()
        break
      case 2:
        this.tateData = past7Day()
        break
      case 3:
        this.tateData = past30Day()
        break
      case 4:
        this.tateData = pastWeek()
        break
      case 5:
        this.tateData = pastMonth()
        break
    }
    this.init(this.tateData[0],this.tateData[1])
  }
}
</script>

<style lang="scss">
</style>
