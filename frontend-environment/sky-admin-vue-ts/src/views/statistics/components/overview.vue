<template>
  <div class="container">
    <h2 class="homeTitle">資料概覽</h2>
    <div class="overviewBox">
      <ul>
        <li>
          <p class="tit">營業額</p>
          <p class="num">{{ overviewData.turnover }}</p>
          <p class="tip">
            同比成長<span v-if="overviewData.turnoverGrowth">
              <span
                class="red"
                :class="overviewData.turnoverGrowth > 0 ? '' : 'green'"
                >{{
                  overviewData.turnoverGrowth > 0
                    ? '+' + overviewData.turnoverGrowth
                    : overviewData.turnoverGrowth
                }}%</span
              >
            </span>
            <span v-else>-</span>
          </p>
        </li>
        <li>
          <p class="tit">有效訂單</p>
          <p class="num">{{ overviewData.validOrderCount }}</p>
          <p class="tip">
            同比成長<span v-if="overviewData.validOrderCountGrowth">
              <span
                class="red"
                :class="overviewData.validOrderCountGrowth > 0 ? '' : 'green'"
                >{{
                  overviewData.validOrderCountGrowth > 0
                    ? '+' + overviewData.validOrderCountGrowth
                    : overviewData.validOrderCountGrowth
                }}%</span
              >
            </span>
            <span v-else>-</span>
          </p>
        </li>
        <li>
          <p class="tit">訂單完成率</p>
          <p class="num">
            {{ (overviewData.orderCompletionRate * 100).toFixed(2) }}%
          </p>
          <p class="tip">
            同比成長<span v-if="overviewData.orderCompletionRateGrowth">
              <span
                class="red"
                :class="
                  overviewData.orderCompletionRateGrowth > 0 ? '' : 'green'
                "
                >{{
                  overviewData.orderCompletionRateGrowth > 0
                    ? '+' + overviewData.orderCompletionRateGrowth
                    : overviewData.orderCompletionRateGrowth
                }}%</span
              >
            </span>
            <span v-else>-</span>
          </p>
        </li>
        <li>
          <p class="tit">平均客單價</p>
          <p class="num">{{ overviewData.unitPrice.toFixed(2) }}</p>
          <p class="tip">
            同比成長<span v-if="overviewData.unitPriceGrowth">
              <span
                class="red"
                :class="overviewData.unitPriceGrowth > 0 ? '' : 'green'"
                >{{
                  overviewData.unitPriceGrowth > 0
                    ? '+' + overviewData.unitPriceGrowth
                    : overviewData.unitPriceGrowth
                }}%</span
              >
            </span>
            <span v-else>-</span>
          </p>
        </li>
        <li>
          <p class="tit">會員總量</p>
          <p class="num">{{ overviewData.totalUsers }}</p>
          <!-- <p class="tip">
            同比增长：<span v-if="overviewData.newUsersGrowth">
              <span
                class="red"
                :class="overviewData.newUsersGrowth > 0 ? '' : 'green'"
                >{{
                  overviewData.newUsersGrowth > 0
                    ? '+' + overviewData.newUsersGrowth
                    : overviewData.newUsersGrowth
                }}</span
              ></span
            >
            <span v-else>-</span>
          </p> -->
        </li>
        <li>
          <p class="tit">新增會員</p>
          <p class="num">{{ overviewData.newUsers }}</p>
          <p class="tip">
            同比增长<span v-if="overviewData.newUsersGrowth">
              <span
                class="red"
                :class="overviewData.newUsersGrowth > 0 ? '' : 'green'"
                >{{
                  overviewData.newUsersGrowth > 0
                    ? '+' + overviewData.newUsersGrowth
                    : overviewData.newUsersGrowth
                }}%</span
              >
            </span>
            <span v-else>-</span>
          </p>
        </li>
      </ul>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator'
import { getday } from '@/utils/formValidate'
@Component({
  name: 'Overview',
})
export default class extends Vue {
  @Prop() private overviewData!: any
  get days() {
    return getday()
  }
}
</script>
