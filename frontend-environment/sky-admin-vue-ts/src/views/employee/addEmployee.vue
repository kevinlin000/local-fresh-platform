<template>
  <div class="addBrand-container">
    <div class="container">
      <el-form :model="ruleForm" :rules="rules" ref="ruleForm" label-width="180px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="ruleForm.username"></el-input>
        </el-form-item>
        <el-form-item label="员工姓名" prop="name">
          <el-input v-model="ruleForm.name"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="ruleForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
            <el-radio v-model="ruleForm.sex" label="1">男</el-radio>
            <el-radio v-model="ruleForm.sex" label="2">女</el-radio>
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="ruleForm.idNumber"></el-input>
        </el-form-item>
        <div class="subBox">
          <el-button type="primary" @click="submitForm('ruleForm',false)">保存</el-button>
          <el-button 
            v-if="this.optType === 'add'" 
            type="primary" 
            @click="submitForm('ruleForm',true)">保存并继续添加员工
          </el-button>
          <el-button @click="() => this.$router.push('/employee')">返回</el-button>
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
      // 操作类型:add-添加员工，edit-修改员工
      optType: 'add',
      rules: {
          username: [
            { required: true, message: '请输入员工账号', trigger: 'blur' }
          ],
          name: [
            { required: true, message: '请选择员工姓名', trigger: 'blur' }
          ],
          phone: [
            { required: true, trigger: 'blur', validator: (rule, value, callback) =>{
                if(value === '' || (!/^1(3|4|5|6|7|8)\d{9}$/.test(value))){
                  callback(new Error('请输入正确的手机号'))
                }else{
                  callback()
                }
              } 
            }
          ],
          idNumber: [
            { required: true, trigger: 'blur', validator: (rule, value, callback) =>{
                if(value === '' || (!/(^\d{15}$)|(^\d{18}$)|(^\d{17}(X|x)$)/.test(value))){
                  callback(new Error('请输入正确的身份证'))
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
    // 若是传过来的参数有id，则是更新页面，否则为添加页面
    this.optType = this.$route.query.id ? 'update' : 'add'
    if(this.optType === 'update'){
      // 回显员工的信息
      getEmployeeById(this.$route.query.id).then(res =>{
        if(res.data.code === 1){
          this.ruleForm = res.data.data
        }
      }).catch(err =>{
        this.$message.error('请求出错了' + err.message)
      })
    }
  },
  methods: {

    // 提交表单
    submitForm(formName, isContinued){
      // 进行表单校验
      this.$refs[formName].validate(valid =>{
        // 表单校验通过
        if(valid){
          // 若是添加员工操作
          if(this.optType === 'add'){
            addEmployee(this.ruleForm).then(res =>{
              if(res.data.code === 1){
                this.$message.success('添加员工信息成功')
                // 若点击的是"保存并继续添加员工"按钮
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
              this.$message.error('请求出错了' + err.message)
            })
          }else{ // 若是修改员工操作
            updateEmployee(this.ruleForm).then(res => {
              if(res.data.code === 1){
                this.$message.success('更新员工信息成功')
                this.$router.push('/employee')
              }
            }).catch(err =>{
              this.$message.error('请求出错了' + err.message)
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
