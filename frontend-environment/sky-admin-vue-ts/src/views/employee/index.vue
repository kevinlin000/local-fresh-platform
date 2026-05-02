<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <label style="margin-right: 5px">员工姓名</label>
        <el-input v-model="name" placeholder="请输入员工姓名" style="width: 15%" clearable></el-input>
        <el-button type="primary" style="margin-left: 20px" @click="pageQuery()">搜索</el-button>
        <el-button type="primary" style="float: right" @click="addEmployee()">+添加员工</el-button>
      </div>
      <el-table :data="records" border stripe style="width: 100%">
        <el-table-column prop="name" label="员工姓名" width="180">
        </el-table-column>
        <el-table-column prop="username" label="账号" width="180">
        </el-table-column>
        <el-table-column prop="phone" label="手机号"> </el-table-column>
        <el-table-column label="账号状态" width="180">
          <template slot-scope="scope">
            <div class="tableColumn-status" :class="{ 'stop-use': scope.row.status === 0 }">
              {{scope.row.status === 1 ? '启用' : '禁用'}}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最后操作时间" width="180">
        </el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <el-button type="text" @click="updateEmployee(scope.row)" v-if="scope.row.username !== 'admin'">修改</el-button>
            <el-button type="text" @click="updateEmployee(scope.row)" disabled v-else>修改</el-button>
            <el-button type="text" @click="handleEnableOrDisable(scope.row)" v-if="scope.row.username !== 'admin'">
              {{scope.row.status === 1 ? '禁用' : '启用'}}
            </el-button>
            <el-button type="text" disabled v-else>
              {{scope.row.status === 1 ? '禁用' : '启用'}}
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
    // 分页查询
    pageQuery(){
      // 准备请求参数
      const params = {name: this.name, page: this.page, pageSize: this.pageSize}
      getEmployeeList(params).then(res => {
          if(res.data.code === 1){
            this.total = res.data.data.total
            this.records = res.data.data.records
          }
      }).catch(err => {
        this.$message.error('请求出错了' + err.message)
      })
    },

    // 每页数量发生变化时调用
    handleSizeChange(pageSize){
      this.pageSize = pageSize
      this.pageQuery()
    },

    // 页码发生变化时调用
    handleCurrentChange(page){
      this.page = page
      this.pageQuery()
    },

    // 更新员工账号状态
    handleEnableOrDisable(row){
      const status = row.status === 1 ? '禁用' : '启用'
      this.$confirm('您确定要<span style="color:red">' + status + '</span>此员工账号的状态吗？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() =>{
        const params = {id: row.id, status: row.status == 1 ? 0 : 1}
        enableOrDisable(params).then(res =>{
          if(res.data.code === 1){
            this.$message.success('员工账号状态更新成功')
            this.pageQuery()
          }
        }).catch(err =>{
          this.$message.error('请求出错了' + err.message)
        })
      })
    },

    // 添加员工
    addEmployee(){
      this.$router.push('/employee/add')
    },

    // 修改员工
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
