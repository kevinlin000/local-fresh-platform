import md5 from 'md5';

// 根據請求地址、方法與參數，統一計算目前請求的 md5 值作為 key
const getRequestKey = (config) => {
    if (!config) {
        // 如果沒有取得請求的相關配置資訊，根據時間戳產生
        return md5(+new Date());
    }

    const data = typeof config.data === 'string' ? config.data : JSON.stringify(config.data);
    // console.log(config,pending,config.url,md5(config.url + '&' + config.method + '&' + data),'config')
    return md5(config.url + '&' + config.method + '&' + data);
}

// 儲存 key 值
const pending = {};
// 检查key值
const checkPending = (key) => !!pending[key];
// 刪除key值
const removePending = (key) => {
    // console.log(key,'key')
    delete pending[key];
};

export {
    getRequestKey,
    pending,
    checkPending,
    removePending
}
