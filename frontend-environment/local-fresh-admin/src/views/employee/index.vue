<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <label style="margin-right: 5px">員工姓名</label>
        <el-input v-model="name" placeholder="請輸入員工姓名" style="width: 15%" clearable></el-input>
        <el-button type="primary" style="margin-left: 20px" @click="pageQuery()">搜尋</el-button>
        <el-button type="primary" style="float: right" @click="addEmployee()">+新增員工</el-button>
      </div>
      <el-table :data="records" border stripe style="width: 100%">
        <el-table-column prop="name" label="員工姓名" width="180">
        </el-table-column>
        <el-table-column prop="username" label="帳號" width="180">
        </el-table-column>
        <el-table-column prop="phone" label="手機號碼"> </el-table-column>
        <el-table-column label="帳號狀態" width="180">
          <template slot-scope="scope">
            <div class="tableColumn-status" :class="{ 'stop-use': scope.row.status === 0 }">
              {{scope.row.status === 1 ? '啟用' : '停用'}}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最後操作時間" width="180">
        </el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <el-button type="text" @click="updateEmployee(scope.row)" v-if="scope.row.username !== 'admin'">修改</el-button>
            <el-button type="text" @click="updateEmployee(scope.row)" disabled v-else>修改</el-button>
            <el-button type="text" @click="handleEnableOrDisable(scope.row)" v-if="scope.row.username !== 'admin'">
              {{scope.row.status === 1 ? '停用' : '啟用'}}
            </el-button>
            <el-button type="text" disabled v-else>
              {{scope.row.status === 1 ? '停用' : '啟用'}}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pageList"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page=page
        :page-sizes="[10, 20, 30, 40]"
        :page-size=pageSize
        layout="total, sizes, prev, pager, next, jumper"
        :total=total
      >
      </el-pagination>
    </div>
  </div>
</template>

<script lang="ts">
import {getEmployeeList, enableOrDisable} from '@/api/employee'

export default {
  data() {
    return {
      name: '',
      page: 1,
      pageSize: 10,
      total: 0,
      records: []
    }
  },
  created() {
    this.pageQuery()
  },
  methods: {
    // 分頁查詢
    pageQuery(){
      // 準備請求參數
      const params = {name: this.name, page: this.page, pageSize: this.pageSize}
      getEmployeeList(params).then(res => {
          if(res.data.code === 1){
            this.total = res.data.data.total
            this.records = res.data.data.records
          }
      }).catch(err => {
        this.$message.error('請求發生錯誤：' + err.message)
      })
    },

    // 每页數量發生變化時呼叫
    handleSizeChange(pageSize){
      this.pageSize = pageSize
      this.pageQuery()
    },

    // 頁碼發生變化時呼叫
    handleCurrentChange(page){
      this.page = page
      this.pageQuery()
    },

    // 更新員工帳號狀態
    handleEnableOrDisable(row){
      const status = row.status === 1 ? '停用' : '啟用'
      this.$confirm('您確定要<span style="color:red">' + status + '</span>此員工帳號的狀態嗎？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() =>{
        const params = {id: row.id, status: row.status == 1 ? 0 : 1}
        enableOrDisable(params).then(res =>{
          if(res.data.code === 1){
            this.$message.success('員工帳號狀態更新成功')
            this.pageQuery()
          }
        }).catch(err =>{
          this.$message.error('請求發生錯誤：' + err.message)
        })
      })
    },

    // 添加員工
    addEmployee(){
      this.$router.push('/employee/add')
    },

    // 修改員工
    updateEmployee(row){
      this.$router.push({
        path: '/employee/add',
        query: {id: row.id}
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.disabled-text {
  color: #bac0cd !important;
}

</style>
