<template>
  <div>
    <div class="container homecon">
      <h2 class="homeTitle homeTitleBtn">
        訂單資訊
        <ul class="conTab">
          <li
            v-for="(item, index) in tabList"
            :key="index"
            :class="activeIndex === index ? 'active' : ''"
            @click="handleClass(index)"
          >
            <el-badge
              class="item"
              :class="item.num >= 10 ? 'badgeW' : ''"
              :value="item.num > 99 ? '99+' : item.num"
              :hidden="!([2, 3].includes(item.value) && item.num)"
              >{{ item.label }}</el-badge
            >
          </li>
        </ul>
      </h2>
      <div class="">
        <div v-if="orderData.length > 0">
          <el-table
            :data="orderData"
            stripe
            class="tableBox"
            style="width: 100%"
            @row-click="handleTable"
          >
            <el-table-column prop="number" label="訂單編號"> </el-table-column>
            <el-table-column label="訂單單品">
              <template slot-scope="scope">
                <div class="ellipsisHidden">
                  <el-popover
                    placement="top-start"
                    title=""
                    width="200"
                    trigger="hover"
                    :content="scope.row.orderDishes"
                  >
                    <span slot="reference">{{ scope.row.orderDishes }}</span>
                  </el-popover>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              label="地址"
              :class-name="dialogOrderStatus === 2 ? 'address' : ''"
            >
              <template slot-scope="scope">
                <div class="ellipsisHidden">
                  <el-popover
                    placement="top-start"
                    title=""
                    width="200"
                    trigger="hover"
                    :content="scope.row.address"
                  >
                    <span slot="reference">{{ scope.row.address }}</span>
                  </el-popover>
                </div>
              </template>
            </el-table-column>

            <el-table-column
              prop="estimatedDeliveryTime"
              label="預計送達時間"
              sortable
              class-name="orderTime"
              min-width="130"
            >
            </el-table-column>
            <el-table-column prop="amount" label="實收金額"> </el-table-column>
            <el-table-column label="備註">
              <template slot-scope="scope">
                <div class="ellipsisHidden">
                  <el-popover
                    placement="top-start"
                    title=""
                    width="200"
                    trigger="hover"
                    :content="scope.row.remark"
                  >
                    <span slot="reference">{{ scope.row.remark }}</span>
                  </el-popover>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              prop="tablewareNumber"
              label="餐具數量"
              min-width="80"
              align="center"
              v-if="status === 3"
            >
            </el-table-column>
            <el-table-column
              label="操作"
              align="center"
              :class-name="dialogOrderStatus === 0 ? 'operate' : 'otherOperate'"
              :min-width="
                [2, 3].includes(dialogOrderStatus)
                  ? 130
                  : [0].includes(dialogOrderStatus)
                  ? 140
                  : 'auto'
              "
            >
              <template slot-scope="{ row }">
                <!-- <el-divider direction="vertical" /> -->
                <div class="before">
                  <el-button
                    v-if="row.status === 2"
                    type="text"
                    class="blueBug"
                    @click="
                      orderAccept(row, $event), (isTableOperateBtn = true)
                    "
                  >
                    接單
                  </el-button>
                  <el-button
                    v-if="row.status === 3"
                    type="text"
                    class="blueBug"
                    @click="cancelOrDeliveryOrComplete(3, row.id, $event)"
                  >
                    派送
                  </el-button>
                </div>
                <div class="middle">
                  <el-button
                    v-if="row.status === 2"
                    type="text"
                    class="delBut"
                    @click="
                      orderReject(row, $event), (isTableOperateBtn = true)
                    "
                  >
                    拒單
                  </el-button>
                  <el-button
                    v-if="[1, 3, 4, 5].includes(row.status)"
                    type="text"
                    class="delBut"
                    @click="cancelOrder(row, $event)"
                  >
                    取消
                  </el-button>
                </div>
                <div class="after">
                  <el-button
                    type="text"
                    class="blueBug non"
                    @click="goDetail(row.id, row.status, row, $event)"
                  >
                    查看
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <Empty v-else :is-search="isSearch" />
        <el-pagination
          v-if="counts > 10"
          class="pageList"
          :page-sizes="[10, 20, 30, 40]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="counts"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    <!-- 查看弹框部分 -->
    <el-dialog
      title="訂單資訊"
      :visible.sync="dialogVisible"
      width="53%"
      :before-close="handleClose"
      class="order-dialog"
    >
      <el-scrollbar style="height: 100%">
        <div class="order-top">
          <div>
            <div style="display: inline-block">
              <label style="font-size: 16px">訂單編號：</label>
              <div class="order-num">
                {{ diaForm.number }}
              </div>
            </div>
            <div
              style="display: inline-block"
              class="order-status"
              :class="{ status3: [3, 4].includes(dialogOrderStatus) }"
            >
              {{
                orderList.filter((item) => item.value === dialogOrderStatus)[0]
                  .label
              }}
            </div>
          </div>
          <p><label>下單時間：</label>{{ diaForm.orderTime }}</p>
        </div>

        <div class="order-middle">
          <div class="user-info">
            <div class="user-info-box">
              <div class="user-name">
                <label>會員名稱：</label>
                <span>{{ diaForm.consignee }}</span>
              </div>
              <div class="user-phone">
                <label>手機號碼：</label>
                <span>{{ diaForm.phone }}</span>
              </div>
              <div
                v-if="[2, 3, 4, 5].includes(dialogOrderStatus)"
                class="user-getTime"
              >
                <label>{{
                  dialogOrderStatus === 5 ? '送達時間：' : '預計送達時間：'
                }}</label>
                <span>{{
                  dialogOrderStatus === 5
                    ? diaForm.deliveryTime
                    : diaForm.estimatedDeliveryTime
                }}</span>
              </div>
              <div class="user-address">
                <label>地址：</label>
                <span>{{ diaForm.address }}</span>
              </div>
            </div>
            <div
              class="user-remark"
              :class="{ orderCancel: dialogOrderStatus === 6 }"
            >
              <div>{{ dialogOrderStatus === 6 ? '取消原因' : '備註' }}</div>
              <span>{{
                dialogOrderStatus === 6
                  ? diaForm.cancelReason || diaForm.rejectionReason
                  : diaForm.remark
              }}</span>
            </div>
          </div>

          <div class="dish-info">
            <div class="dish-label">單品</div>
            <div class="dish-list">
              <div
                v-for="(item, index) in diaForm.orderDetailList"
                :key="index"
                class="dish-item"
              >
                <span class="dish-name">{{ item.name }}</span>
                <span class="dish-num">x{{ item.number }}</span>
                <span class="dish-price"
                  >NT$ {{ item.amount ? item.amount.toFixed(2) : '' }}</span
                >
              </div>
            </div>
            <div class="dish-all-amount">
              <label>單品小計</label>
              <span
                >NT$ {{
                  (diaForm.amount - 6 - diaForm.packAmount).toFixed(2)
                }}</span
              >
            </div>
          </div>
        </div>

        <div class="order-bottom">
          <div class="amount-info">
            <div class="amount-label">費用</div>
            <div class="amount-list">
              <div class="dish-amount">
                <span class="amount-name">單品小計：</span>
                <span class="amount-price"
                  >NT$ {{
                    ((diaForm.amount - 6 - diaForm.packAmount).toFixed(2) *
                      100) /
                    100
                  }}</span
                >
              </div>
              <div class="send-amount">
                <span class="amount-name">派送費：</span>
                <span class="amount-price">NT$ {{ 6 }}</span>
              </div>
              <div class="package-amount">
                <span class="amount-name">打包費：</span>
                <span class="amount-price"
                  >NT$ {{
                    diaForm.packAmount
                      ? (diaForm.packAmount.toFixed(2) * 100) / 100
                      : ''
                  }}</span
                >
              </div>
              <div class="all-amount">
                <span class="amount-name">合計：</span>
                <span class="amount-price"
                  >NT$ {{
                    diaForm.amount
                      ? (diaForm.amount.toFixed(2) * 100) / 100
                      : ''
                  }}</span
                >
              </div>
              <div class="pay-type">
                <span class="pay-name">支付管道：</span>
                <span class="pay-value">{{
                  diaForm.payMethod === 1 ? '第三方付款' : '其他付款'
                }}</span>
              </div>
              <div class="pay-time">
                <span class="pay-name">支付時間：</span>
                <span class="pay-value">{{ diaForm.checkoutTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>
      <span v-if="dialogOrderStatus !== 6" slot="footer" class="dialog-footer">
        <el-checkbox
          v-if="dialogOrderStatus === 2 && status === 2"
          v-model="isAutoNext"
          >處理完自動跳轉下一筆</el-checkbox
        >
        <el-button
          v-if="dialogOrderStatus === 2"
          @click="orderReject(row, $event), (isTableOperateBtn = false)"
          >拒 單</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 2"
          type="primary"
          @click="orderAccept(row, $event), (isTableOperateBtn = false)"
          >接 單</el-button
        >

        <el-button
          v-if="[1, 3, 4, 5].includes(dialogOrderStatus)"
          @click="dialogVisible = false"
          >返 回</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 3"
          type="primary"
          @click="cancelOrDeliveryOrComplete(3, row.id, $event)"
          >派 送</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 4"
          type="primary"
          @click="cancelOrDeliveryOrComplete(4, row.id, $event)"
          >完 成</el-button
        >
        <el-button
          v-if="[1].includes(dialogOrderStatus)"
          type="primary"
          @click="cancelOrder(row, $event)"
          >取消訂單</el-button
        >
      </span>
    </el-dialog>
    <!-- end -->
    <!-- 拒單、取消彈窗 -->
    <el-dialog
      :title="cancelDialogTitle + '原因'"
      :visible.sync="cancelDialogVisible"
      width="42%"
      :before-close="() => ((cancelDialogVisible = false), (cancelReason = ''))"
      class="cancelDialog"
    >
      <el-form label-width="90px">
        <el-form-item :label="cancelDialogTitle + '原因：'">
          <el-select
            v-model="cancelReason"
            :placeholder="'請選擇' + cancelDialogTitle + '原因'"
          >
            <el-option
              v-for="(item, index) in cancelDialogTitle === '取消'
                ? cancelrReasonList
                : cancelOrderReasonList"
              :key="index"
              :label="item.label"
              :value="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="cancelReason === '自訂原因'" label="原因：">
          <el-input
            v-model.trim="remark"
            type="textarea"
            :placeholder="'請填寫您' + cancelDialogTitle + '的原因（限 20 字內）'"
            maxlength="20"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click=";(cancelDialogVisible = false), (cancelReason = '')"
          >取消</el-button
        >
          <el-button type="primary" @click="confirmCancel">確定</el-button>
      </span>
    </el-dialog>
    <!-- end -->
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator'
import Empty from '@/components/Empty/index.vue'
import {
  getOrderDetailPage,
  queryOrderDetailById,
  completeOrder,
  deliveryOrder,
  orderCancel,
  orderReject,
  orderAccept,
  getOrderListBy,
} from '@/api/order'
@Component({
  name: 'Orderview',
  components: {
    Empty,
  },
})
export default class extends Vue {
  @Prop({ default: '' }) orderStatics!: any

  private orderId = '' // 訂單號
  private dialogOrderStatus = 0 // 彈窗所需訂單狀態，用於詳情展示欄位
  private activeIndex = 0

  private dialogVisible = false // 詳情彈窗
  private cancelDialogVisible = false // 取消、拒單彈窗
  private cancelDialogTitle = '' // 取消、拒單彈窗標題
  private cancelReason = ''
  private remark = '' // 自訂原因
  private diaForm = []
  private row = {}
  private isAutoNext = true
  private isSearch: boolean = false
  private counts = 0
  private page: number = 1
  private pageSize: number = 10
  private status = 2
  private orderData = []
  private isTableOperateBtn = true
  private cancelOrderReasonList = [
    {
      value: 1,
      label: '訂單量較多，暫時無法接單',
    },
    {
      value: 2,
      label: '單品已售完，暫時無法接單',
    },
    {
      value: 3,
      label: '店休中，暫時無法接單',
    },
    {
      value: 0,
      label: '自訂原因',
    },
  ]

  private cancelrReasonList = [
    {
      value: 1,
      label: '訂單量較多，暫時無法接單',
    },
    {
      value: 2,
      label: '單品已售完，暫時無法接單',
    },
    {
      value: 3,
      label: '配送員不足無法配送',
    },
    {
      value: 4,
      label: '顧客來電取消',
    },
    {
      value: 0,
      label: '自訂原因',
    },
  ]
  private orderList = [
    {
      label: '全部訂單',
      value: 0,
    },
    {
      label: '待付款',
      value: 1,
    },
    {
      label: '待接單',
      value: 2,
    },
    {
      label: '待派送',
      value: 3,
    },
    {
      label: '派送中',
      value: 4,
    },
    {
      label: '已完成',
      value: 5,
    },
    {
      label: '已取消',
      value: 6,
    },
  ]
  get tabList() {
    return [
      {
        label: '待接單',
        value: 2,
        num: this.orderStatics.toBeConfirmed,
      },
      {
        label: '待派送',
        value: 3,
        num: this.orderStatics.confirmed,
      },
    ]
  }
  created() {
    this.getOrderListData(this.status)
  }
  // // 取得訂單資料
  async getOrderListData(status) {
    const params = {
      page: this.page,
      pageSize: this.pageSize,
      status: status,
    }
    const data = await getOrderDetailPage(params)
    this.orderData = data.data.data.records
    this.counts = data.data.data.total
    this.$emit('getOrderListBy3Status')
    if (
      this.dialogOrderStatus === 2 &&
      this.status === 2 &&
      this.isAutoNext &&
      !this.isTableOperateBtn &&
      data.data.records.length > 1
    ) {
      const row = data.data.records[0]
      this.goDetail(row.id, row.status, row, row)
    } else {
      return null
    }
  }

  //接單
  orderAccept(row: any, event) {
    event.stopPropagation()
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    orderAccept({ id: this.orderId })
      .then((res) => {
        if (res.data.code === 1) {
          this.$message.success('操作成功')
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.dialogVisible = false
          this.getOrderListData(this.status)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error('請求發生錯誤：' + err.message)
      })
  }
  // 開啟取消訂單彈窗
  cancelOrder(row: any, event) {
    event.stopPropagation()
    this.cancelDialogVisible = true
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.cancelDialogTitle = '取消'
    this.dialogVisible = false
    this.cancelReason = ''
  }
  // 開啟拒單彈窗
  orderReject(row: any, event) {
    event.stopPropagation()
    this.cancelDialogVisible = true
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.cancelDialogTitle = '拒绝'
    this.dialogVisible = false
    this.cancelReason = ''
  }
  // 確認取消或拒單訂單並填寫原因
  confirmCancel(type) {
    if (!this.cancelReason) {
      return this.$message.error(`請選擇${this.cancelDialogTitle}原因`)
    } else if (this.cancelReason === '自訂原因' && !this.remark) {
      return this.$message.error(`請輸入${this.cancelDialogTitle}原因`)
    }

    ;(this.cancelDialogTitle === '取消' ? orderCancel : orderReject)({
      id: this.orderId,
      // eslint-disable-next-line standard/computed-property-even-spacing
      [this.cancelDialogTitle === '取消' ? 'cancelReason' : 'rejectionReason']:
        this.cancelReason === '自訂原因' ? this.remark : this.cancelReason,
    })
      .then((res) => {
        if (res.data.code === 1) {
          this.$message.success('操作成功')
          this.cancelDialogVisible = false
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.getOrderListData(this.status)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error('請求發生錯誤：' + err.message)
      })
  }

  // 派送，完成
  cancelOrDeliveryOrComplete(status: number, id: string, event) {
    event.stopPropagation()
    const params = {
      status,
      id,
    }
    ;(status === 3 ? deliveryOrder : completeOrder)(params)
      .then((res) => {
        if (res.data.code === 1) {
          this.$message.success('操作成功')
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.dialogVisible = false
          this.getOrderListData(this.status)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error('請求發生錯誤：' + err.message)
      })
  }
  // 查看詳情
  async goDetail(id: any, status: number, row: any, event) {
    event.stopPropagation()
    // console.log(111, index, row)
    this.diaForm = []
    this.dialogVisible = true
    this.dialogOrderStatus = status
    const { data } = await queryOrderDetailById({ orderId: id })
    this.diaForm = data.data
    this.row = row
  }
  // 關閉彈層
  handleClose() {
    this.dialogVisible = false
  }
  // tab切换
  handleClass(index) {
    this.activeIndex = index
    if (index === 0) {
      this.status = 2
      this.getOrderListData(2)
    } else {
      this.status = 3
      this.getOrderListData(3)
    }
  }
  // 触发table某一行
  handleTable(row, column, event) {
    event.stopPropagation()
    this.goDetail(row.id, row.status, row, event)
  }
  // 分頁
  private handleSizeChange(val: any) {
    this.pageSize = val
    this.getOrderListData(this.status)
  }

  private handleCurrentChange(val: any) {
    this.page = val
    this.getOrderListData(this.status)
  }
}
</script>
<style  lang="scss" scoped >
.dashboard-container.home .homecon {
  margin-bottom: 0;
}
.order-top {
  // height: 80px;
  border-bottom: 1px solid #e7e6e6;
  padding-bottom: 26px;
  padding-left: 22px;
  padding-right: 22px;
  // margin: 0 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .order-status {
    width: 57.25px;
    height: 27px;
    background: #333333;
    border-radius: 13.5px;
    color: white;
    margin-left: 19px;
    text-align: center;
    line-height: 27px;
  }
  .status3 {
    background: #f56c6c;
  }
  p {
    color: #333;
    label {
      color: #666;
    }
  }
  .order-num {
    font-size: 16px;
    color: #2a2929;
    font-weight: bold;
    display: inline-block;
  }
}

.order-middle {
  .user-info {
    min-height: 140px;
    background: #fbfbfa;
    margin-top: 23px;

    padding: 20px 43px;
    color: #333;
    .user-info-box {
      min-height: 55px;
      display: flex;
      flex-wrap: wrap;
      .user-name {
        flex: 67%;
      }
      .user-phone {
        flex: 33%;
      }
      .user-getTime {
        margin-top: 14px;
        flex: 80%;
        label {
          margin-right: 3px;
        }
      }
      label {
        margin-right: 17px;
        color: #666;
      }

      .user-address {
        margin-top: 14px;
        flex: 80%;
        label {
          margin-right: 30px;
        }
      }
    }
    .user-remark {
      height: 43px;
      line-height: 43px;
      background: #f5f0e6;
      border: 1px solid rgba(74, 124, 58, 0.24);
      border-radius: 4px;
      margin-top: 10px;
      padding: 6px;
      display: flex;
      align-items: center;
      div {
        display: inline-block;
        min-width: 53px;
        height: 32px;
        background: rgba(74, 124, 58, 0.14);
        border-radius: 4px;
        text-align: center;
        line-height: 32px;
        color: #333;
        margin-right: 30px;
        // padding: 12px 6px;
      }
      span {
        color: #4A7C3A;
      }
    }
    .orderCancel {
      background: #ffffff;
      border: 1px solid #b6b6b6;

      div {
        padding: 0 10px;
        background-color: #e5e4e4;
      }
      span {
        color: #f56c6c;
      }
    }
  }
  .dish-info {
    // min-height: 180px;
    display: flex;
    flex-wrap: wrap;
    padding: 20px 40px;
    border-bottom: 1px solid #e7e6e6;
    .dish-label {
      color: #666;
    }
    .dish-list {
      flex: 80%;
      display: flex;
      flex-wrap: wrap;
      .dish-item {
        flex: 50%;
        margin-bottom: 14px;
        color: #333;
        .dish-num {
          margin-right: 51px;
        }
      }
      // .dish-item:nth-child(odd) {
      //   flex: 60%;
      // }
      // .dish-item:nth-child(even) {
      //   flex: 40%;
      // }
    }
    .dish-label {
      margin-right: 65px;
    }
    .dish-all-amount {
      flex: 1;
      padding-left: 92px;
      margin-top: 10px;
      label {
        color: #333333;
        font-weight: bold;
        margin-right: 5px;
      }
      span {
        color: #f56c6c;
      }
    }
  }
}
.order-bottom {
  .amount-info {
    // min-height: 180px;
    display: flex;
    flex-wrap: wrap;
    padding: 20px 40px;
    padding-bottom: 0px;
    .amount-label {
      color: #666;
      margin-right: 65px;
    }
    .amount-list {
      flex: 80%;
      display: flex;
      flex-wrap: wrap;
      color: #333;
      // height: 65px;
      .dish-amount,
      .package-amount,
      .pay-type {
        display: inline-block;
        width: 300px;
        margin-bottom: 14px;
        flex: 50%;
      }
      .send-amount,
      .all-amount,
      .pay-time {
        display: inline-block;
        flex: 50%;
        padding-left: 10%;
      }
      .package-amount {
        .amount-name {
          margin-right: 14px;
        }
      }
      .all-amount {
        .amount-name {
          margin-right: 24px;
        }
        .amount-price {
          color: #f56c6c;
        }
      }
      .send-amount {
        .amount-name {
          margin-right: 10px;
        }
      }
    }
  }
}
</style>
<style  lang="scss">
.dashboard-container {
  .cancelTime {
    padding-left: 30px;
  }
  .orderTime {
    padding-left: 30px;
  }
  td.operate .cell {
    .before,
    .middle,
    .after {
      height: 39px;
      width: 48px;
    }
  }
  td.operate .cell,
  td.otherOperate .cell {
    display: flex;
    flex-wrap: nowrap;
    justify-content: center;
  }
}
</style>
