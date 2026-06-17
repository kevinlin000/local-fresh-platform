import request from '@/utils/request'
/**
 *
 * 直送箱管理
 *
 **/
//直送箱分頁查詢
export const getSetmealPage = (params: any) => {
    return request({
        url: '/giftbox/page',
        method: 'GET',
        params: params
    })
}

//直送箱啟售停售
export const enableOrDisableSetmeal = (params: any) => {
    return request({
        url: `/giftbox/status/${params.status}`,
        method: 'POST',
        params: {id: params.id}
    })
}

//刪除直送箱
export const deleteSetmeal = (ids: string) => {//1,2,3
    return request({
        url: '/giftbox',
        method: 'DELETE',
        params: {ids: ids}
    })
}


  
// 修改資料介面
export const editSetmeal = (params: any) => {
    return request({
        url: '/giftbox',
        method: 'put',
        data: { ...params }
    })
}

// 新增資料介面
export const addSetmeal = (params: any) => {
    return request({
        url: '/giftbox',
        method: 'post',
        data: { ...params }
    })
}

// 查詢詳情介面
export const querySetmealById = (id: string | (string | null)[]) => {
    return request({
        url: `/giftbox/${id}`,
        method: 'get'
    })
}
