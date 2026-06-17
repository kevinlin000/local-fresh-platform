<template>
  <div class="addBrand-container">
    <div class="container">
      <el-form ref="ruleForm"
               :model="ruleForm"
               :rules="rules"
               :inline="true"
               label-width="180px"
               class="demo-ruleForm">
        <div>
          <el-form-item label="直送箱名稱:"
                        prop="boxName">
            <el-input v-model="ruleForm.boxName"
                      placeholder="請輸入直送箱名稱"
                      maxlength="14" />
          </el-form-item>
          <el-form-item label="直送箱分類:"
                        prop="idType">
            <el-select v-model="ruleForm.idType"
                       placeholder="請選擇直送箱分類"
                       @change="$forceUpdate()">
              <el-option v-for="(item, index) in setMealList"
                         :key="index"
                         :label="item.name"
                         :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <div>
          <el-form-item label="直送箱價格:"
                        prop="price">
            <el-input v-model="ruleForm.price"
                      placeholder="請設定直送箱價格" />
          </el-form-item>
        </div>
        <div>
          <el-form-item label="直送箱單品:"
                        required>
            <el-form-item>
              <div class="addDish">
                <span v-if="dishTable.length == 0"
                      class="addBut"
                      @click="openAddDish('new')">
                  + 新增單品</span>
                <div v-if="dishTable.length != 0"
                     class="content">
                  <div class="addBut"
                       style="margin-bottom: 20px"
                       @click="openAddDish('change')">
                    + 新增單品
                  </div>
                  <div class="table">
                    <el-table :data="dishTable"
                              style="width: 100%">
                      <el-table-column prop="productName"
                                       label="名稱"
                                       width="180"
                                       align="center" />
                      <el-table-column prop="price"
                                       label="原價"
                                       width="180"
                                       align="center">
                        <template slot-scope="scope">
                          {{ (Number(scope.row.price).toFixed(2) * 100) / 100 }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="address"
                                       label="份數"
                                       align="center">
                        <template slot-scope="scope">
                          <el-input-number v-model="scope.row.copies"
                                           size="small"
                                           :min="1"
                                           :max="99"
                                           label="描述文字" />
                        </template>
                      </el-table-column>
                      <el-table-column prop="address"
                                       label="操作"
                                       width="180px;"
                                       align="center">
                        <template slot-scope="scope">
                          <el-button type="text"
                                     size="small"
                                     class="delBut non"
                                     @click="delDishHandle(scope.$index)">
                            刪除
                          </el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-form-item>
        </div>
        <div>
          <el-form-item label="直送箱圖片:"
                        required
                        prop="image">
            <image-upload :prop-image-url="imageUrl"
                          @imageChange="imageChange">
              圖片大小不超過 2MB<br>僅能上傳 PNG、JPEG、JPG 類型圖片<br>建議上傳 200*200 或 300*300 尺寸的圖片
            </image-upload>
          </el-form-item>
        </div>
        <div class="address">
          <el-form-item label="直送箱描述:">
            <el-input v-model="ruleForm.description"
                      type="textarea"
                      :rows="3"
                      maxlength="200"
                      placeholder="直送箱描述，最長 200 字" />
          </el-form-item>
        </div>
        <div class="subBox address">
          <el-form-item>
            <el-button @click="() => $router.back()">
              取消
            </el-button>
            <el-button type="primary"
                       :class="{ continue: actionType === 'add' }"
                       @click="submitForm('ruleForm', false)">
              儲存
            </el-button>
            <el-button v-if="actionType == 'add'"
                       type="primary"
                       @click="submitForm('ruleForm', true)">
              儲存並繼續新增
            </el-button>
          </el-form-item>
        </div>
      </el-form>
    </div>
    <el-dialog v-if="dialogVisible"
               title="新增單品"
               class="addDishList"
               :visible.sync="dialogVisible"
               width="60%"
               :before-close="handleClose">
      <AddDish v-if="dialogVisible"
               ref="adddish"
               :check-list="checkList"
               :seach-key="seachKey"
               :dish-list="dishList"
               @checkList="getCheckList" />
      <span slot="footer"
            class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary"
                   @click="addTableList">添 加</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import ImageUpload from '@/components/ImgUpload/index.vue'
import AddDish from './components/AddDish.vue'
import { querySetmealById, addSetmeal, editSetmeal } from '@/api/setMeal'
import { getCategoryList, getDishPage } from '@/api/dish'
import { baseUrl } from '@/config.json'

@Component({
  name: 'addShop',
  components: {
    HeadLable,
    AddDish,
    ImageUpload
  }
})
export default class extends Vue {
  private value: string = ''
  private setMealList: any[] = []
  private seachKey: string = ''
  private dishList: any[] = []
  private imageUrl: string = ''
  private actionType: string = ''
  private dishTable: any[] = []
  private dialogVisible: boolean = false
  private checkList: any[] = []
  private ruleForm = {
    boxName: '',
    categoryId: '',
    price: '',
    code: '',
    image: '',
    description: '',
    dishList: [],
    status: true,
    idType: ''
  }

  get rules() {
    return {
      boxName: {
        required: true,
        validator: (rule: any, value: string, callback: Function) => {
          if (!value) {
            callback(new Error('請輸入直送箱名稱'))
          } else {
            const reg = /^([A-Za-z0-9\u4e00-\u9fa5]){2,20}$/
            if (!reg.test(value)) {
              callback(new Error('直送箱名稱格式不符，請輸入 2-20 個字元'))
            } else {
              callback()
            }
          }
        },
        trigger: 'blur'
      },
      idType: {
        required: true,
        message: '請選擇直送箱分類',
        trigger: 'change'
      },
      image: {
        required: true,
        message: '直送箱圖片不能為空'
      },
      price: {
        required: true,
        // 'message': '請輸入直送箱價格',
        validator: (rules: any, value: string, callback: Function) => {
          const reg = /^([1-9]\d{0,5}|0)(\.\d{1,2})?$/
          if (!reg.test(value) || Number(value) <= 0) {
            callback(
              new Error(
                '直送箱價格格式有誤，請輸入大於零且最多保留兩位小數的金額'
              )
            )
          } else {
            callback()
          }
        },
        trigger: 'blur'
      },
      code: { required: true, message: '請輸入商品碼', trigger: 'blur' }
    }
  }

  created() {
    this.getDishTypeList()
    this.actionType = this.$route.query.id ? 'edit' : 'add'
    if (this.actionType == 'edit') {
      this.init()
    } else {
      this.resetForm()
    }
  }

  @Watch('$route.query.id')
  onRouteIdChange(value: string | (string | null)[] | undefined) {
    this.actionType = value ? 'edit' : 'add'
    if (value) {
      this.init()
    } else {
      this.resetForm()
    }
  }

  private resetForm() {
    this.ruleForm = {
      boxName: '',
      categoryId: '',
      price: '',
      code: '',
      image: '',
      description: '',
      dishList: [],
      status: true,
      idType: ''
    } as any
    this.imageUrl = ''
    this.dishTable = []
    this.checkList = []
    this.dialogVisible = false
    this.seachKey = ''
    this.$nextTick(() => {
      if (this.$refs.ruleForm) {
        ;(this.$refs.ruleForm as any).clearValidate()
      }
    })
  }

  private async init() {
    querySetmealById(this.$route.query.id).then(async res => {
      if (res && res.data && res.data.code === 1) {
        this.ruleForm = res.data.data
        this.ruleForm.status = res.data.data.status == '1'
        ;(this.ruleForm as any).price = res.data.data.price
        // this.imageUrl = `http://172.17.2.120:8080/common/download?name=${res.data.data.image}`
        this.imageUrl = res.data.data.image
        const giftBoxProducts = await this.hydrateGiftBoxProducts(
          res.data.data.giftBoxProducts || []
        )
        this.checkList = giftBoxProducts
        this.dishTable = [...giftBoxProducts].reverse()
        this.ruleForm.idType = res.data.data.categoryId
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }

  private async hydrateGiftBoxProducts(giftBoxProducts: any[]) {
    const res = await getDishPage({
      page: 1,
      pageSize: 1000
    })
    const records: any[] = (res && res.data && res.data.data && res.data.data.records) || []
    const productMap: Map<number, any> = new Map(records.map((item: any) => [item.id, item]))
    return giftBoxProducts.map((item: any) => {
      const product: any = productMap.get(item.productId) || {}
      const productName = item.name || product.productName || ''
      return {
        ...item,
        dishId: item.productId,
        productId: item.productId,
        productName,
        name: productName,
        price: item.price != null ? item.price : (product.price != null ? product.price : 0)
      }
    })
  }
  private seachHandle() {
    this.seachKey = this.value
  }
  // 取得直送箱分類
  private getDishTypeList() {
    getCategoryList({ type: 2, page: 1, pageSize: 1000 }).then(res => {
      if (res && res.data && res.data.code === 1) {
        this.setMealList = res.data.data.map((obj: any) => ({
          ...obj,
          idType: obj.id
        }))
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }

  // 透過直送箱ID取得單品列表分類
  // private getDishList (id:number) {
  //   getDishListType({id}).then(res => {
  //     if (res.data.code == 200) {
  //       const { data } = res.data
  //       this.dishList = data
  //     } else {
  //       this.$message.error(res.data.desc)
  //     }
  //   })
  // }

  // 刪除直送箱單品
  delDishHandle(index: any) {
    this.dishTable.splice(index, 1)
    this.checkList = this.dishTable
    // this.checkList.splice(index, 1)
  }

  // 取得新增單品資料，確認後倒序展示
  private getCheckList(value: any) {
    this.checkList = [...value].reverse()
  }

  // 新增單品
  openAddDish(st: string) {
    this.seachKey = ''
    this.dialogVisible = true
  }
  // 取消新增單品
  handleClose(done: any) {
    // this.$refs.adddish.close()
    this.dialogVisible = false
    this.checkList = JSON.parse(JSON.stringify(this.dishTable))
    // this.dialogVisible = false
  }

  // 儲存新增單品清單
  public addTableList() {
    this.dishTable = JSON.parse(JSON.stringify(this.checkList))
    this.dishTable.forEach((n: any) => {
      n.copies = 1
    })
    this.dialogVisible = false
  }

  public submitForm(formName: any, st: any) {
    ;(this.$refs[formName] as any).validate((valid: any) => {
      if (valid) {
        if (this.dishTable.length === 0) {
          return this.$message.error('直送箱內單品不能為空')
        }
        if (!this.ruleForm.image) return this.$message.error('直送箱圖片不能為空')
        let prams = { ...this.ruleForm } as any
        prams.giftBoxProducts = this.dishTable.map((obj: any) => ({
          copies: obj.copies,
          productId: obj.productId || obj.dishId,
          name: obj.productName || obj.name,
          price: obj.price
        }))
        ;(prams as any).status =
          this.actionType === 'add' ? 0 : this.ruleForm.status ? 1 : 0
        prams.categoryId = this.ruleForm.idType
        // delete prams.dishList
        if (this.actionType == 'add') {
          delete prams.id
          addSetmeal(prams)
            .then(res => {
              if (res && res.data && res.data.code === 1) {
                this.$message.success('直送箱新增成功！')
                if (!st) {
                  this.$router.push({ path: '/setmeal' })
                } else {
                  this.resetForm()
                }
              } else {
                this.$message.error(res.data.msg)
              }
            })
            .catch(err => {
              this.$message.error('請求發生錯誤：' + err.message)
            })
        } else {
          delete prams.updateTime
          editSetmeal(prams)
            .then(res => {
              if (res.data.code === 1) {
                this.$message.success('直送箱修改成功！')
                this.$router.push({ path: '/setmeal' })
              } else {
                // this.$message.error(res.data.desc || res.data.message)
              }
            })
            .catch(err => {
              this.$message.error('請求發生錯誤：' + err.message)
            })
        }
      } else {
        // console.log('error submit!!')
        return false
      }
    })
  }

  imageChange(value: any) {
    this.ruleForm.image = value
  }
}
</script>
<style>
.avatar-uploader .el-icon-plus:after {
  position: absolute;
  display: inline-block;
  content: ' ' !important;
  left: calc(50% - 20px);
  top: calc(50% - 40px);
  width: 40px;
  height: 40px;
  background: url('./../../assets/icons/icon_upload@2x.png') center center
    no-repeat;
  background-size: 20px;
}
</style>
<style lang="scss">
// .el-form-item__error {
//   top: 90%;
// }
.addBrand-container {
  .avatar-uploader .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }

  .avatar-uploader .el-upload:hover {
    border-color: #4A7C3A;
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 200px;
    height: 160px;
    line-height: 160px;
    text-align: center;
  }

  .avatar {
    width: 200px;
    height: 160px;
    display: block;
  }

  // .el-form--inline .el-form-item__content {
  //   width: 293px;
  // }

  .el-input {
    width: 293px;
  }

  .address {
    .el-form-item__content {
      width: 777px !important;
    }
  }
  .el-input__prefix {
    top: 2px;
  }

  .addDish {
    .el-input {
      width: 130px;
    }

    .el-input-number__increase {
      border-left: solid 1px #fbe396;
      background: #fffbf0;
    }

    .el-input-number__decrease {
      border-right: solid 1px #fbe396;
      background: #fffbf0;
    }

    input {
      border: 1px solid #fbe396;
    }

    .table {
      border: solid 1px #ebeef5;
      border-radius: 3px;

      th {
        padding: 5px 0;
      }

      td {
        padding: 7px 0;
      }
    }
  }

  .addDishList {
    .seachDish {
      position: absolute;
      top: 12px;
      right: 20px;
    }

    .el-dialog__footer {
      padding-top: 27px;
    }

    .el-dialog__body {
      padding: 0;
      border-bottom: solid 1px #efefef;
    }
    .seachDish {
      .el-input__inner {
        height: 40px;
        line-height: 40px;
      }
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
      .el-input {
        width: 350px;
      }
      .addDish {
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

        .content {
          background: #fafafb;
          padding: 20px;
          border: solid 1px #d8dde3;
          border-radius: 3px;
        }
      }
    }
  }
}
</style>
