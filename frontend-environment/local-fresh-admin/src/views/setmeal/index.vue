<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <label style="margin-right: 5px">直送箱名稱</label>
        <el-input v-model="name" placeholder="請輸入直送箱名稱" style="width: 15%" clearable />
        <label style="margin-left: 20px; margin-right: 5px">直送箱分類</label>
        <el-select v-model="categoryId" placeholder="請選擇直送箱分類" clearable>
          <el-option
            v-for="item in categoryList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <label style="margin-left: 20px; margin-right: 5px">販售狀態</label>
        <el-select v-model="status" placeholder="請選擇" clearable>
          <el-option
            v-for="item in statusList"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" style="margin-left: 20px" @click="pageQuery()">
          搜索
        </el-button>
        <div style="float: right">
          <el-button type="danger" @click="handleDeleteSetmeal('B')">
            批次刪除
          </el-button>
          <el-button type="info" @click="handleAddSetmeal()">
            +新建直送箱
          </el-button>
        </div>
      </div>
      <el-table :data="records" border stripe class="tableBox" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="70px" />
        <el-table-column prop="boxName" label="直送箱名稱" />
        <el-table-column label="直送箱圖片">
          <template slot-scope="scope">
            <el-image style="width: 80px; height: 40px; border: none" :src="scope.row.image" />
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="直送箱分類" />
        <el-table-column prop="price" label="直送箱價" />
        <el-table-column label="販售狀態">
          <template slot-scope="scope">
            <div class="tableColumn-status" :class="{ 'stop-use': scope.row.status === 0 }">
              {{ scope.row.status === 1 ? '啟售' : '停售' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最後操作時間" />
        <el-table-column label="操作" width="250px">
          <template slot-scope="scope">
            <el-button type="text" @click="handleUpdateSetmeal(scope.row.id)">
              修改
            </el-button>
            <el-button type="text" @click="handleDeleteSetmeal('S', scope.row.id)">
              刪除
            </el-button>
            <el-button type="text" @click="handleEnableOrDisable(scope.row)">
              {{ scope.row.status === 1 ? '停售' : '啟售' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pageList"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="page"
        :page-sizes="[10, 20, 30, 40]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { getCategoryByType } from '@/api/category'
import { getSetmealPage, enableOrDisableSetmeal, deleteSetmeal } from '@/api/setMeal'

export default {
  data () {
    return {
      name: '',
      page: 1,
      pageSize: 10,
      categoryList: [],
      categoryId: '', // 分類id
      statusList: [{
          value: 0,
          label: '停售'
        }, {
          value: 1,
          label: '啟售'
        }],
        status: '', // 販售狀態
        total: 0,
        records: [],
        multipleSelection: [] // 表格選擇的元素
      }
    },
    created() {
      this.getCategoryList()
      this.pageQuery()
    },
    methods: {
      // 取得直送箱分類列表
      getCategoryList() {
        const params = { type: 2 }
        getCategoryByType(params).then(res => {
          if (res.data.code === 1) {
            this.categoryList = res.data.data
          }
        }).catch(err => {
          this.$message.error('請求發生錯誤：' + err.message)
        })
      },
      // 分頁查詢
      pageQuery() {
        const params = {
          boxName: this.name,
          categoryId: this.categoryId,
          status: this.status,
          page: this.page,
          pageSize: this.pageSize
        }
        getSetmealPage(params).then(res => {
          if (res.data.code === 1) {
            this.total = res.data.data.total
            this.records = res.data.data.records
          }
        }).catch(err => {
          this.$message.error('請求發生錯誤：' + err.message)
        })
      },
      // 每页筆數發生改变時觸發
      handleSizeChange(pageSize) {
        this.pageSize = pageSize
        this.pageQuery()
      },
      // 頁碼發生改变時觸發
      handleCurrentChange(page) {
        this.page = page
        this.pageQuery()
      },
      // 更新套装狀態
      handleEnableOrDisable(row) {
        const status = row.status === 1 ? '停售' : '啟售'
        this.$confirm('您確定要<span style="color:red">' + status + '</span>此直送箱的狀態嗎？', '提示', {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '確定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          const params = { id: row.id, status: row.status === 1 ? 0 : 1 }
          enableOrDisableSetmeal(params).then(res => {
            if (res.data.code === 1) {
              this.$message.success('直送箱狀態更新成功！')
              this.pageQuery()
            }
          }).catch(err => {
            this.$message.error('請求發生錯誤：' + err.message)
          })
        })
      },
      // 全選/取消選擇
      handleSelectionChange(val) {
        this.multipleSelection = val
      },
      // 刪除直送箱
      handleDeleteSetmeal(type: string, id: string) {
        if (type === 'B') {
          if (this.multipleSelection.length === 0) {
            this.$message.error('請至少選擇一個直送箱！')
            return
          }
        }

        this.$confirm('您確定要刪除所選的直送箱嗎？', '提示', {
          confirmButtonText: '確定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          let param = ''
          // 若是批次刪除
          if (type === 'B') {
            const arr = []
            this.multipleSelection.forEach(element => {
              arr.push(element.id)
            })
            param = arr.join(',')
          } else { // 若是单筆刪除
            param = id
          }
          deleteSetmeal(param).then(res => {
            if (res.data.code === 1) {
              this.$message.success('直送箱刪除成功！')
              this.pageQuery()
            } else {
              this.$message.error(res.data.msg)
            }
          }).catch(err => {
            this.$message.error('請求發生錯誤：' + err.message)
          })
        })
      },
      // 新增直送箱
      handleAddSetmeal() {
        this.$router.push('/setmeal/add')
      },
      // 更新直送箱
      handleUpdateSetmeal(id) {
        this.$router.push({
          path: '/setmeal/add',
          query: { id: id }
        })
      }
    }
}
</script>
<style lang="scss">
.el-table-column--selection .cell {
  padding-left: 10px;
}
</style>
<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;

    .container {
      background: #fff;
      position: relative;
      z-index: 1;
      padding: 30px 28px;
      border-radius: 4px;

      .tableBar {
        margin-bottom: 20px;
        .tableLab {
          float: right;
          span {
            cursor: pointer;
            display: inline-block;
            font-size: 14px;
            padding: 0 20px;
            color: $gray-2;
          }
        }
      }

      .tableBox {
        width: 100%;
        border: 1px solid $gray-5;
        border-bottom: 0;
      }

      .pageList {
        text-align: center;
        margin-top: 30px;
      }
      //查詢黑色按鈕樣式
      .normal-btn {
        background: #333333;
        color: white;
        margin-left: 20px;
      }
    }
  }
}
</style>
