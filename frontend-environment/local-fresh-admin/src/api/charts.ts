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

// 支付類型資料汇總 -> 店內收款構成 - 當日
export const getDayPayType = (params: any) =>
  request({
    'url': `/report/payTypeCollect/${params.date}`,
    'method': 'get'
  })
// 取得當日各种優惠類型資料汇總 -> 優惠指标
export const getprivilege = (params: any) =>
  request({
    'url': `/report/privilegeCollect/${params.date}`,
    'method': 'get'
  })

// 取得單品分類銷售排行 - 單品分類占比 -當日
export const getSalesRanking = (params: any) =>
  request({
    'url': `/report/categoryCollect/${params.type}/${params.date}`,
    'method': 'get'
  })

// 取得當日單品銷售排行
export const getDayRanking = (params: any) =>
  request({
    'url': `/report/currentDishRank/${params.date}`,
    'method': 'get'
  })

// 取得一定日期之内的銷售趨勢 - 銷售趨勢 图
export const getTimeQuantumDataes = (params: any) =>
  request({
    'url': `/report/dayAmountCollect/${params.type}/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的各种支付類型資料汇總 - 店內收款構成 - 時间段
export const getTimeQuantumReceivables = (params: any) =>
  request({
    'url': `/report/datePayTypeCollect/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的單品類別銷售汇總 -  單品分類占比 - 時间段
export const getTimeQuantumType = (params: any) =>
  request({
    'url': `/report/dateCategoryCollect/${params.type}/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的單品銷售排行 - 單品銷售排行
export const getTimeQuantumDishes = (params: any) =>
  request({
    'url': `/report/dishRankForDate/${params.start}/${params.end}`,
    'method': 'get'
  })

// 取得時間範圍內的優惠指标汇總資料 - 頂部資訊
export const getTimeQuantumDiscount = (params: any) =>
  request({
    'url': `/report/privilegeByDate/${params.start}/${params.end}`,
    'method': 'get'
  })
