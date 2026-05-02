<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <label style="margin-right: 5px">套餐名称</label>
        <el-input v-model="name" placeholder="请输入套餐名称" style="width: 15%" clearable></el-input>
        <label style="margin-left: 20px; margin-right: 5px">套餐分类</label>
        <el-select v-model="categoryId" placeholder="请选择套餐分类" clearable>
          <el-option
            v-for="item in categoryList"
            :key="item.id"
            :label="item.name"
            :value="item.id">
          </el-option>
        </el-select>
        <label style="margin-left: 20px; margin-right: 5px">售卖状态</label>
        <el-select v-model="status" placeholder="请选择" clearable>
          <el-option
            v-for="item in statusList"
            :key="item.value"
            :label="item.label"
            :value="item.value">
          </el-option>
        </el-select>
        <el-button type="primary" style="margin-left: 20px" @click="pageQuery()">搜索</el-button>
        <div style="float: right">
          <el-button type="danger" @click="handleDeleteSetmeal('B')">批量删除</el-button>
          <el-button type="info" @click="handleAddSetmeal()">+新建套餐</el-button>
        </div>
      </div>
      <el-table :data="records" border stripe class="tableBox" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="70px"></el-table-column>
        <el-table-column prop="name" label="套餐名称">
        </el-table-column>
        <el-table-column label="套餐图片">
          <template slot-scope="scope">
            <el-image style="width: 80px; height: 40px; border: none" :src="scope.row.image"/>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="套餐分类"></el-table-column>
        <el-table-column prop="price" label="套餐价"></el-table-column>
        <el-table-column label="售卖状态">
          <template slot-scope="scope">
            <div class="tableColumn-status" :class="{ 'stop-use': scope.row.status === 0 }">
              {{scope.row.status === 1 ? '起售' : '停售'}}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最后操作时间">
        </el-table-column>
        <el-table-column label="操作" width="250px">
          <template slot-scope="scope">
            <el-button type="text" @click="handleUpdateSetmeal(scope.row.id)">修改</el-button>
            <el-button type="text" @click="handleDeleteSetmeal('S', scope.row.id)">删除</el-button>
            <el-button type="text" @click="handleEnableOrDisable(scope.row)">
              {{scope.row.status === 1 ? '停售' : '起售'}}
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
import {getCategoryByType} from '@/api/category'
import {getSetmealPage, enableOrDisableSetmeal, deleteSetmeal} from '@/api/setMeal'

export default {
  data () {
    return {
      name: '',
      page: 1,
      pageSize: 10,
      categoryList: [],
      categoryId: '', // 分类id
      statusList: [{
          value: 0,
          label: '停售'
        }, {
          value: 1,
          label: '起售'
        }],
        status: '', // 售卖状态
        total: 0,
        records: [],
        multipleSelection: [] // 表格选择的元素
      }
    },
    created() {
      this.getCategoryList()
      this.pageQuery()
    },
    methods: {
      // 获取套餐分类列表
      getCategoryList(){
          const params = {type: 2}
          getCategoryByType(params).then(res =>{
            if(res.data.code === 1){
              this.categoryList = res.data.data
            }
          }).catch(err =>{
            this.$message.error('请求出错了' + err.message)
          })
      },
      // 分页查询
      pageQuery(){
        const params = {
          name: this.name,
          categoryId: this.categoryId,
          status: this.status,
          page: this.page,
          pageSize: this.pageSize
        }
        getSetmealPage(params).then(res =>{
          if(res.data.code === 1){
            this.total = res.data.data.total
            this.records = res.data.data.records
          }
        }).catch(err =>{
            this.$message.error('请求出错了' + err.message)
        })
      },
      // 每页条数发生改变时触发
      handleSizeChange(pageSize){
        this.pageSize = pageSize
        this.pageQuery()
      },
      // 页码发生改变时触发
      handleCurrentChange(page){
        this.page = page
        this.pageQuery()
      },
      // 更新套装状态
      handleEnableOrDisable(row){
        const status = row.status === 1 ? '停售' : '起售'
        this.$confirm('您确定要<span style="color:red">' + status + '</span>此套餐的状态吗？', '提示', {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() =>{
          const params = {id: row.id, status: row.status == 1 ? 0 : 1}
          enableOrDisableSetmeal(params).then(res =>{
            if(res.data.code === 1){
              this.$message.success('更新套餐状态成功！')
              this.pageQuery()
            }
          }).catch(err =>{
            this.$message.error('请求出错了' + err.message)
        })
        })
      },
      // 全选/取消选择
      handleSelectionChange(val){
        this.multipleSelection = val
      },
      // 删除套餐
      handleDeleteSetmeal(type: string, id: string){
        if(type === 'B'){
          if(this.multipleSelection.length === 0){
            this.$message.error('请至少选择一个套餐信息！')
            return
          }
        }

        this.$confirm('您确定要删除所选择的套餐信息吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() =>{
          let param = ''
          // 若是批量删除
          if(type === 'B'){
            const arr = new Array
            this.multipleSelection.forEach(element => {
              arr.push(element.id)
            })
            param = arr.join(',')
          }else{ // 若是单个删除
            param = id
          }
          deleteSetmeal(param).then(res =>{
            if(res.data.code === 1){
              this.$message.success('套餐信息删除成功!')
              this.pageQuery()
            }else{
              this.$message.error(res.data.msg)
            }
          }).catch(err => {
            this.$message.error('请求出错了' + err.message)
          })
        })
      },
      // 新增套餐
      handleAddSetmeal(){
        this.$router.push('/setmeal/add')
      },
      // 更新套餐
      handleUpdateSetmeal(id){
        this.$router.push({
          path: '/setmeal/add',
          query: {id: id}
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
      //查询黑色按钮样式
      .normal-btn {
        background: #333333;
        color: white;
        margin-left: 20px;
      }
    }
  }
}
</style>
