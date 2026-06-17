<template>
  <div class="addBrand-container">
    <div class="container">
      <el-form :model="ruleForm" :rules="rules" ref="ruleForm" label-width="180px">
        <el-form-item label="帳號" prop="username">
          <el-input v-model="ruleForm.username"></el-input>
        </el-form-item>
        <el-form-item label="員工姓名" prop="name">
          <el-input v-model="ruleForm.name"></el-input>
        </el-form-item>
        <el-form-item label="手機號碼" prop="phone">
          <el-input v-model="ruleForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="性別" prop="sex">
            <el-radio v-model="ruleForm.sex" label="1">男</el-radio>
            <el-radio v-model="ruleForm.sex" label="2">女</el-radio>
        </el-form-item>
        <el-form-item label="身分證字號" prop="idNumber">
          <el-input v-model="ruleForm.idNumber"></el-input>
        </el-form-item>
        <div class="subBox">
          <el-button type="primary" @click="submitForm('ruleForm',false)">儲存</el-button>
          <el-button 
            v-if="this.optType === 'add'" 
            type="primary" 
            @click="submitForm('ruleForm',true)">儲存並繼續新增員工
          </el-button>
          <el-button @click="() => this.$router.push('/employee')">回到</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts">
import {addEmployee, getEmployeeById, updateEmployee} from '@/api/employee'

export default {
  data () {
    return {
      ruleForm: {
        username: '',
        name: '',
        phone: '',
        sex: 1,
        idNumber: ''
      },
      // 操作類型：add-新增員工，edit-修改員工
      optType: 'add',
      rules: {
          username: [
            { required: true, message: '請輸入員工帳號', trigger: 'blur' }
          ],
          name: [
            { required: true, message: '請選擇員工姓名', trigger: 'blur' }
          ],
          phone: [
            { required: true, trigger: 'blur', validator: (rule, value, callback) =>{
                if(value === '' || (!/^1(3|4|5|6|7|8)\d{9}$/.test(value))){
                  callback(new Error('請輸入正確的手機號碼'))
                }else{
                  callback()
                }
              } 
            }
          ],
          idNumber: [
            { required: true, trigger: 'blur', validator: (rule, value, callback) =>{
                if(value === '' || (!/(^\d{15}$)|(^\d{18}$)|(^\d{17}(X|x)$)/.test(value))){
                  callback(new Error('請輸入正確的身分證字號'))
                }else{
                  callback()
                }
              } 
            }
          ]
        }
    }
  },
  created() {
    // 若傳入參數含 id，則為修改頁面，否則為新增頁面
    this.optType = this.$route.query.id ? 'update' : 'add'
    if(this.optType === 'update'){
      // 回填員工資訊
      getEmployeeById(this.$route.query.id).then(res =>{
        if(res.data.code === 1){
          this.ruleForm = res.data.data
        }
      }).catch(err =>{
        this.$message.error('請求發生錯誤：' + err.message)
      })
    }
  },
  methods: {

    // 提交表單
    submitForm(formName, isContinued){
      // 進行表單校驗
      this.$refs[formName].validate(valid =>{
        // 表單校驗通過
        if(valid){
          // 若為新增員工操作
          if(this.optType === 'add'){
            addEmployee(this.ruleForm).then(res =>{
              if(res.data.code === 1){
                this.$message.success('新增員工資料成功')
                // 若點擊的是「儲存並繼續新增員工」按鈕
                if(isContinued){
                  this.ruleForm = {
                    username: '',
                    name: '',
                    phone: '',
                    sex: 1,
                    idNumber: ''
                  }
                }else{
                  this.$router.push('/employee')
                }
              }
            }).catch(err =>{
              this.$message.error('請求發生錯誤：' + err.message)
            })
          }else{ // 若是修改員工操作
            updateEmployee(this.ruleForm).then(res => {
              if(res.data.code === 1){
                this.$message.success('更新員工資訊成功')
                this.$router.push('/employee')
              }
            }).catch(err =>{
              this.$message.error('請求發生錯誤：' + err.message)
            })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.addBrand {
  &-container {
    margin: 30px;
    margin-top: 30px;
    .HeadLable {
      background-color: transparent;
      margin-bottom: 0px;
      padding-left: 0px;
    }
    .container {
      position: relative;
      z-index: 1;
      background: #fff;
      padding: 30px;
      border-radius: 4px;
      // min-height: 500px;
      .subBox {
        padding-top: 30px;
        text-align: center;
        border-top: solid 1px $gray-5;
      }
    }
    .idNumber {
      margin-bottom: 39px;
    }

    .el-form-item {
      margin-bottom: 29px;
    }
    .el-input {
      width: 293px;
    }
  }
}
</style>
