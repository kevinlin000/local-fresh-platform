import request from '@/utils/request'
// 營業额資料
// export const getTurnoverDataes = (data) =>
//   request({
//     'url': `/report/turnoverStatistics`,
//     'method': 'get',
//     data
//   })
// 首頁資料
// // 今日資料
// export const getTodayDataes = () =>
//   request({
//     'url': `/workspace/todaydate`,
//     'method': 'get'
//   })
// 訂單管理
  export const getOrderData = () =>
  request({
    'url': `/workspace/overviewOrders`,
    'method': 'get'
  })
// 單品總覽
export const getOverviewDishes = () =>
request({
  'url': `/workspace/overviewDishes`,
  'method': 'get'
})
// 直送箱總覽
export const getSetMealStatistics = () =>
request({
  'url': `/workspace/overviewSetmeals`,
  'method': 'get'
})
// 營業資料
export const getBusinessData= () =>
request({
  'url': `/workspace/businessData`,
  'method': 'get'
})
/**
 *
 * 報表資料
 *
 **/
// 统计
// 取得當日銷售資料 -> 頂部資料
// export const getDataes = (params: any) =>
//   request({
//     'url': `/report/amountCollect/${params.date}`,
//     'method': 'get'
//   })


// 營業额统计
export const getTurnoverStatistics= (params: any) =>
  request({
    'url': `/report/turnoverStatistics`,
    'method': 'get',
    params
  })

// 會員统计
export const getUserStatistics= (params: any) =>
  request({
    'url': `/report/userStatistics`,
    'method': 'get',
    params
  })
  // 訂單统计
export const getOrderStatistics= (params: any) =>
request({
  'url': `/report/ordersStatistics`,
  'method': 'get',
  params
})
  // 销量排名TOP10
  export const getTop= (params: any) =>
  request({
    'url': `/report/top10`,
    'method': 'get',
    params
  })
  // 資料概览
  export const getDataOverView= (params: any) =>
  request({
    'url': `/report/dataOverView`,
    'method': 'get',
    params
  })
  // 匯出
  export function exportInfor() {
    return request({
      url: '/report/export',
      method: 'get',
      responseType: "blob"
    })
  }
