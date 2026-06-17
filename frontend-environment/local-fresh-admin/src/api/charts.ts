import request from '@/utils/request'
/**
 *
 * 報表資料
 *
 **/

// 取得當日銷售資料 -> 頂部資料
export const getDataes = (params: any) =>
  request({
    'url': `/report/amountCollect/${params.date}`,
    'method': 'get'
  })

// 取得當日銷售資料 -> 頂部資料 - 營收概況
export const getChartsDataes = (params: any) =>
  request({
    'url': `/report/dayCollect/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得當日銷售趨勢資料（24小時）-> 銷售趨勢
export const getDayDataes= (params: any) =>
  request({
    'url': `/report/hourCollect/${params.type}/${params.date}`,
    'method': 'get'
  })

// 支付類型資料彙總 -> 店內收款構成 - 當日
export const getDayPayType = (params: any) =>
  request({
    'url': `/report/payTypeCollect/${params.date}`,
    'method': 'get'
  })
// 取得當日各種優惠類型資料彙總 -> 優惠指標
export const getprivilege = (params: any) =>
  request({
    'url': `/report/privilegeCollect/${params.date}`,
    'method': 'get'
  })

// 取得商品分類銷售排行 - 商品分類占比 -當日
export const getSalesRanking = (params: any) =>
  request({
    'url': `/report/categoryCollect/${params.type}/${params.date}`,
    'method': 'get'
  })

// 取得當日商品銷售排行
export const getDayRanking = (params: any) =>
  request({
    'url': `/report/currentDishRank/${params.date}`,
    'method': 'get'
  })

// 取得一段日期內的銷售趨勢 - 銷售趨勢圖
export const getTimeQuantumDataes = (params: any) =>
  request({
    'url': `/report/dayAmountCollect/${params.type}/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的各種支付類型資料彙總 - 店內收款構成
export const getTimeQuantumReceivables = (params: any) =>
  request({
    'url': `/report/datePayTypeCollect/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的商品類別銷售彙總 - 商品分類占比
export const getTimeQuantumType = (params: any) =>
  request({
    'url': `/report/dateCategoryCollect/${params.type}/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的商品銷售排行 - 商品銷售排行
export const getTimeQuantumDishes = (params: any) =>
  request({
    'url': `/report/dishRankForDate/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的優惠指標彙總資料 - 頂部資訊
export const getTimeQuantumDiscount = (params: any) =>
  request({
    'url': `/report/privilegeByDate/${params.start}/${params.end}`,
    'method': 'get'
  })
