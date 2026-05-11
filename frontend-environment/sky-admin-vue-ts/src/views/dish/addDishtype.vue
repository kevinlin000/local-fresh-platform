<template>
  <div :key="vueRest"
       class="addBrand-container">
    <div :key="restKey"
         class="container">
      <el-form ref="ruleForm"
               :model="ruleForm"
               :rules="rules"
               :inline="true"
               label-width="180px"
               class="demo-ruleForm">
        <div>
          <el-form-item label="單品名稱:"
                        prop="productName">
            <el-input v-model="ruleForm.productName"
                      placeholder="請輸入單品名稱"
                      maxlength="20" />
          </el-form-item>
          <el-form-item label="單品分類:"
                        prop="categoryId">
            <el-select v-model="ruleForm.categoryId"
                       placeholder="請選擇單品分類">
              <el-option v-for="(item, index) in dishList"
                         :key="index"
                         :label="item.name"
                         :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <div>
          <el-form-item label="單品價格:"
                        prop="price">
            <el-input v-model="ruleForm.price"
                      placeholder="請設定單品價格" />
          </el-form-item>
        </div>
        <el-form-item label="口味做法配置:">
          <el-form-item>
            <div class="flavorBox">
              <span v-if="dishFlavors.length == 0"
                    class="addBut"
                    @click="addFlavore">
                + 新增口味</span>
              <div v-if="dishFlavors.length != 0"
                   class="flavor">
                <div class="title">
                  <span>口味名稱（3 個字內）</span>
                  <!-- <span class="des-box">口味标签（输入标签回车添加）</span> -->
                </div>
                <div class="cont">
                  <div v-for="(item, index) in dishFlavors"
                       :key="index"
                       class="items">
                    <div class="itTit">
                      <!-- :dish-flavors-data="filterDishFlavorsData()" -->
                      <SelectInput :dish-flavors-data="leftDishFlavors"
                                   :index="index"
                                   :value="item.name"
                                   @select="selectHandle" />
                    </div>
                    <div class="labItems"
                         style="display: flex">
                      <span v-for="(it, ind) in item.value"
                            :key="ind">{{ it }}
                        <i @click="delFlavorLabel(index, ind)">X</i></span>
                      <div class="inputBox"
                           :style="inputStyle" />
                    </div>
                    <span class="delFlavor delBut non"
                          @click="delFlavor(item.name)">刪除</span>
                  </div>
                </div>
                <div v-if="
                       !!this.leftDishFlavors.length &&
                         this.dishFlavors.length < this.dishFlavorsData.length
                     "
                     class="addBut"
                     @click="addFlavore">
                  新增口味
                </div>
              </div>
            </div>
          </el-form-item>
        </el-form-item>
        <div>
          <el-form-item label="單品圖片:"
                        prop="image">
            <image-upload :prop-image-url="imageUrl"
                          @imageChange="imageChange">
              圖片大小不超過 2MB<br>僅能上傳 PNG、JPEG、JPG 類型圖片<br>建議上傳 200*200 或 300*300 尺寸的圖片
            </image-upload>
          </el-form-item>
        </div>
        <div class="address">
          <el-form-item label="單品描述:"
                        prop="region">
            <el-input v-model="ruleForm.description"
                      type="textarea"
                      :rows="3"
                      maxlength="200"
                      placeholder="單品描述，最長 200 字" />
          </el-form-item>
        </div>
        <div class="subBox address">
          <el-button @click="() => $router.back()">
            取消
          </el-button>
          <el-button type="primary"
                     :class="{ continue: actionType === 'add' }"
                     @click="submitForm('ruleForm')">
            儲存
          </el-button>
          <el-button v-if="actionType == 'add'"
                     type="primary"
                     @click="submitForm('ruleForm', 'goAnd')">
            儲存並繼續新增
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import SelectInput from './components/SelectInput.vue'
import ImageUpload from '@/components/ImgUpload/index.vue'
// getFlavorList口味列表暫時不做 getDishTypeList
import {
  queryDishById,
  addDish,
  editDish,
  getCategoryList,
  commonDownload
} from '@/api/dish'
import { baseUrl } from '@/config.json'
import { getToken } from '@/utils/cookies'
@Component({
  name: 'addShop',
  components: {
    HeadLable,
    SelectInput,
    ImageUpload
  }
})
export default class extends Vue {
  private restKey: number = 0
  private textarea: string = ''
  private value: string = ''
  private imageUrl: string = ''
  private actionType: string = ''
  private dishList: string[] = []
  private dishFlavorsData: any[] = [] //原始口味資料
  private dishFlavors: any[] = [] //待上傳口味的資料
  private leftDishFlavors: any[] = [] //下拉框剩余可选择的口味資料
  private vueRest = '1'
  private index = 0
  private inputStyle = { flex: 1 }
  private headers = {
    token: getToken()
  }
  private ruleForm = {
    productName: '',
    id: '',
    price: '',
    code: '',
    image: '',
    description: '',
    dishFlavors: [],
    status: true,
    categoryId: ''
  }

  get rules() {
    return {
      productName: [
        {
          required: true,
          validator: (rule: any, value: string, callback: Function) => {
            if (!value) {
              callback(new Error('請輸入單品名稱'))
            } else {
              const reg = /^([A-Za-z0-9\u4e00-\u9fa5]){2,20}$/
              if (!reg.test(value)) {
                callback(new Error('單品名稱格式不符，請輸入 2-20 個字元'))
              } else {
                callback()
              }
            }
          },
          trigger: 'blur'
        }
      ],
      categoryId: [
        { required: true, message: '請選擇單品分類', trigger: 'change' }
      ],
      image: {
        required: true,
        message: '單品圖片不能為空'
      },
      price: [
        {
          required: true,
          // 'message': '請填寫單品價格',
          validator: (rules: any, value: string, callback: Function) => {
            const reg = /^([1-9]\d{0,5}|0)(\.\d{1,2})?$/
            if (!reg.test(value) || Number(value) <= 0) {
              callback(
                new Error(
                  '單品價格格式有誤，請輸入大於零且最多保留兩位小數的金額'
                )
              )
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ],
      code: [{ required: true, message: '請填寫商品碼', trigger: 'blur' }]
    }
  }

  created() {
    this.getDishList()
    // 口味临时資料
    this.getFlavorListHand()
    this.actionType = this.$route.query.id ? 'edit' : 'add'
    if (this.$route.query.id) {
      this.init()
    } else {
      this.resetForm()
    }
  }

  mounted() {}
  @Watch('$route.query.id')
  onRouteIdChange(value: string | (string | null)[] | undefined) {
    this.actionType = value ? 'edit' : 'add'
    if (value) {
      this.init()
    } else {
      this.resetForm()
    }
  }

  @Watch('dishFlavors')
  changeDishFlavors() {
    this.getLeftDishFlavors()
  }

  private resetForm() {
    this.ruleForm = {
      productName: '',
      id: '',
      price: '',
      code: '',
      image: '',
      description: '',
      dishFlavors: [],
      status: true,
      categoryId: ''
    }
    this.imageUrl = ''
    this.dishFlavors = []
    this.leftDishFlavors = [...this.dishFlavorsData]
    this.restKey++
    this.$nextTick(() => {
      if (this.$refs.ruleForm) {
        ;(this.$refs.ruleForm as any).clearValidate()
      }
    })
  }

  //过滤已选择的口味下拉框無法再次选择
  getLeftDishFlavors() {
    let arr = []
    this.dishFlavorsData.map(item => {
      if (
        this.dishFlavors.findIndex(item1 => item.name === item1.name) === -1
      ) {
        arr.push(item)
      }
    })
    this.leftDishFlavors = arr
  }

  private selectHandle(val: any, key: any, ind: any) {
    const arrDate = [...this.dishFlavors]
    const index = this.dishFlavorsData.findIndex(item => item.name === val)
    arrDate[key] = JSON.parse(JSON.stringify(this.dishFlavorsData[index]))
    this.dishFlavors = arrDate
  }

  private async init() {
    queryDishById(this.$route.query.id).then(res => {
      if (res && res.data && res.data.code === 1) {
        this.ruleForm = { ...res.data.data }
        this.ruleForm.price = String(res.data.data.price)
        this.ruleForm.status = res.data.data.status == '1'
        this.dishFlavors = Array.isArray(res.data.data.flavors)
          ? res.data.data.flavors.map(obj => ({
            ...obj,
            value: JSON.parse(obj.value)
          }))
          : []
        let arr = []
        this.getLeftDishFlavors()
        this.imageUrl = res.data.data.image
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }

  // 按鈕 - 新增口味
  private addFlavore() {
    this.dishFlavors.push({ name: '', value: [] }) // JSON.parse(JSON.stringify(this.dishFlavorsData))
  }

  // 按鈕 - 刪除口味
  private delFlavor(name: string) {
    let ind = this.dishFlavors.findIndex(item => item.name === name)
    this.dishFlavors.splice(ind, 1)
  }

  // 按鈕 - 刪除口味標籤
  private delFlavorLabel(index: number, ind: number) {
    this.dishFlavors[index].value.splice(ind, 1)
  }

  //口味位置记录
  private flavorPosition(index: number) {
    this.index = index
  }

  // 新增口味標籤
  private keyDownHandle(val: any) {
    if (event) {
      event.cancelBubble = true
      event.preventDefault()
      event.stopPropagation()
    }

    if (val.target.innerText.trim() != '') {
      this.dishFlavors[this.index].flavorData.push(val.target.innerText)
      val.target.innerText = ''
    }
  }

  // 取得單品分類
  private getDishList() {
    getCategoryList({ type: 1 }).then(res => {
      if (res.data.code === 1) {
        this.dishList = res && res.data && res.data.data
      } else {
        this.$message.error(res.data.msg)
      }
      // if (res.data.code == 200) {
      //   const {data} = res.data
      //   this.dishList = data
      // } else {
      //   this.$message.error(res.data.desc)
      // }
    })
  }

  // 取得口味列表
  private getFlavorListHand() {
    // flavor flavorData
    this.dishFlavorsData = [
      { name: '甜味', value: ['无糖', '少糖', '半糖', '多糖', '全糖'] },
      { name: '温度', value: ['热饮', '常温', '去冰', '少冰', '多冰'] },
      { name: '忌口', value: ['不要葱', '不要蒜', '不要香菜', '不要辣'] },
      { name: '辣度', value: ['不辣', '微辣', '中辣', '重辣'] }
    ]
  }

  private submitForm(formName: any, st: any) {
    ;(this.$refs[formName] as any).validate((valid: any) => {
      console.log(valid, 'valid')
      if (valid) {
        if (!this.ruleForm.image) return this.$message.error('單品圖片不能為空')
        let params: any = { ...this.ruleForm }
        // params.flavors = this.dishFlavors
        params.status =
          this.actionType === 'add' ? 0 : this.ruleForm.status ? 1 : 0
        // params.price *= 100
        params.categoryId = this.ruleForm.categoryId
        params.flavors = this.dishFlavors.map(obj => ({
          ...obj,
          value: JSON.stringify(obj.value)
        }))
        delete params.dishFlavors
        if (this.actionType == 'add') {
          delete params.id
          addDish(params)
            .then(res => {
              if (res.data.code === 1) {
                this.$message.success('單品新增成功！')
                if (!st) {
                  this.$router.push({ path: '/dish' })
                } else {
                  this.resetForm()
                }
              } else {
                this.$message.error(res.data.desc || res.data.msg)
              }
            })
            .catch(err => {
              this.$message.error('請求發生錯誤：' + err.message)
            })
        } else {
          delete params.createTime
          delete params.updateTime
          editDish(params)
            .then(res => {
              if (res && res.data && res.data.code === 1) {
                this.$router.push({ path: '/dish' })
                this.$message.success('單品修改成功！')
              } else {
                this.$message.error(res.data.desc || res.data.msg)
              }
              // if (res.data.code == 200) {
              //   this.$router.push({'path': '/dish'})
              //   this.$message.success('單品修改成功！')
              // } else {
              //   this.$message.error(res.data.desc || res.data.message)
              // }
            })
            .catch(err => {
              this.$message.error('請求發生錯誤：' + err.message)
            })
        }
      } else {
        return false
      }
    })
  }

  imageChange(value: any) {
    this.ruleForm.image = value
  }
}
</script>
<style lang="scss" scoped>
.addBrand-container {
  .el-form--inline .el-form-item__content {
    width: 293px;
  }

  .el-input {
    width: 350px;
  }

  .address {
    .el-form-item__content {
      width: 777px !important;
    }
  }
}
</style>
<style lang="scss" scoped>
.addBrand {
  &-container {
    margin: 30px;

    .container {
      position: relative;
      z-index: 1;
      background: #fff;
      padding: 30px;
      border-radius: 4px;
      min-height: 500px;

      .subBox {
        padding-top: 30px;
        text-align: center;
        border-top: solid 1px $gray-5;
      }
      .upload-item {
        .el-form-item__error {
          top: 90%;
        }
      }
    }
  }
}

.flavorBox {
  width: 777px;

  .addBut {
    background: #4A7C3A;
    display: inline-block;
    padding: 0px 20px;
    border-radius: 3px;
    line-height: 40px;
    cursor: pointer;
    border-radius: 4px;
    color: #333333;
    font-weight: 500;
  }

  .flavor {
    border: solid 1px #dfe2e8;
    border-radius: 3px;
    padding: 15px;
    background: #fafafb;

    .title {
      color: #606168;
      .des-box {
        padding-left: 44px;
      }
    }

    .cont {
      .items {
        display: flex;
        margin: 10px 0;

        .itTit {
          width: 150px;
          margin-right: 15px;

          input {
            width: 100%;
            // line-height: 40px;
            // border-radius: 3px;
            // padding: 0 10px;
          }
        }

        .labItems {
          flex: 1;
          display: flex;
          flex-wrap: wrap;
          border-radius: 3px;
          min-height: 39px;
          border: solid 1px #d8dde3;
          background: #fff;
          padding: 0 5px;

          span {
            display: inline-block;
            color: #4A7C3A;
            margin: 5px;
            line-height: 26px;
            padding: 0 10px;
            background: #fffbf0;
            border: 1px solid #fbe396;
            border-radius: 4px;
            font-size: 12px;

            i {
              cursor: pointer;
              font-style: normal;
            }
          }

          .inputBox {
            display: inline-block;
            width: 100%;
            height: 36px;
            line-height: 36px;
            overflow: hidden;
          }
        }

        .delFlavor {
          display: inline-block;
          padding: 0 10px;
          color: #f19c59;
          cursor: pointer;
        }
      }
    }
  }
}
</style>
