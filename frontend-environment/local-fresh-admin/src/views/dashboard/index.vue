<template>
  <div class="dashboard-container home">
    <!-- 營業資料 -->
    <Overview :overviewData="overviewData" />
    <!-- end -->
    <!-- 訂單管理 -->
    <Orderview :orderviewData="orderviewData" />
    <!-- end -->
    <div class="homeMain">
      <!-- 商品總覽 -->
      <CuisineStatistics :dishesData="dishesData" />
      <!-- end -->
      <!-- 直送箱總覽 -->
      <SetMealStatistics :setMealData="setMealData" />
      <!-- end -->
    </div>
    <!-- 訂單資訊 -->
    <OrderList
      :order-statics="orderStatics"
      @getOrderListBy3Status="getOrderListBy3Status"
    />
    <!-- end -->
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import {
  getBusinessData,
  getDataOverView, //營業資料
  getOrderData, //訂單管理今日訂單
  getOverviewDishes, //商品總覽
  getSetMealStatistics, //直送箱總覽
} from '@/api/index'
import { getOrderListBy } from '@/api/order'
// 元件
// 營業資料
import Overview from './components/overview.vue'
// 訂單管理
import Orderview from './components/orderview.vue'
// 商品總覽
import CuisineStatistics from './components/cuisineStatistics.vue'
// 直送箱總覽
import SetMealStatistics from './components/setMealStatistics.vue'
// 訂單列表
import OrderList from './components/orderList.vue'
@Component({
  name: 'Dashboard',
  components: {
    Overview,
    Orderview,
    CuisineStatistics,
    SetMealStatistics,
    OrderList,
  },
})
export default class extends Vue {
  private todayData = {} as any
  private overviewData = {}
  private orderviewData = {} as any
  private flag = 2
  private tateData = []
  private dishesData = {} as any
  private setMealData = {}
  private orderListData = []
  private counts = 0
  private page: number = 1
  private pageSize: number = 10
  private status = 2
  private orderStatics = {} as any
  created() {
    this.init()
  }
  init() {
    this.$nextTick(() => {
      this.getBusinessData()
      this.getOrderStatisticsData()
      this.getOverStatisticsData()
      this.getSetMealStatisticsData()
    })
  }
  // 取得營業資料
  async getBusinessData() {
    const data = await getBusinessData()
    this.overviewData = data.data.data
  }
  // 取得今日訂單
  async getOrderStatisticsData() {
    const data = await getOrderData()
    this.orderviewData = data.data.data
  }
  // 取得商品總覽資料
  async getOverStatisticsData() {
    const data = await getOverviewDishes()
    this.dishesData = data.data.data
  }
  // 取得直送箱總覽資料
  async getSetMealStatisticsData() {
    const data = await getSetMealStatistics()
    this.setMealData = data.data.data
  }
  //取得待處理，待配送，配送中數量
  getOrderListBy3Status() {
    getOrderListBy({})
      .then((res) => {
        if (res.data.code === 1) {
          this.orderStatics = res.data.data
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error('請求發生錯誤：' + err.message)
      })
  }
}
</script>

<style lang="scss">
</style>
