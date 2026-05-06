<template>
  <div class="container">
    <h2 class="homeTitle">
      今日資料<i>{{ days[1] }}</i
      ><span><router-link to="statistics">詳細資料</router-link></span>
    </h2>
    <div class="overviewBox">
      <ul>
        <li>
          <p class="tit">營業額</p>
          <p class="num">¥ {{ overviewData.turnover }}</p>
        </li>
        <li>
          <p class="tit">有效訂單</p>
          <p class="num">{{ overviewData.validOrderCount }}</p>
        </li>
        <li>
          <p class="tit">訂單完成率</p>
          <p class="num">
            {{ (overviewData.orderCompletionRate * 100).toFixed(0) }}%
          </p>
        </li>
        <li>
          <p class="tit">平均客單價</p>
          <p class="num">¥ {{ overviewData.unitPrice }}</p>
        </li>

        <li>
          <p class="tit">新增會員</p>
          <p class="num">{{ overviewData.newUsers }}</p>
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
