const storage = window.localStorage

// App
const sidebarStatusKey = 'sidebar_status'
export const getSidebarStatus = () => storage.getItem(sidebarStatusKey) || undefined
export const setSidebarStatus = (sidebarStatus: string) => storage.setItem(sidebarStatusKey, sidebarStatus)

// Store
const storeId = 'storeId'
export const getStoreId = () => storage.getItem(storeId) || undefined
export const setStoreId = (id: string) => storage.setItem(storeId, id)
export const removeStoreId = () => storage.removeItem(storeId)

// User
const tokenKey = 'token'
export const getToken = () => storage.getItem(tokenKey) || undefined
export const setToken = (token: string) => storage.setItem(tokenKey, token)
export const removeToken = () => storage.removeItem(tokenKey)

const userInfoKey = 'userInfo'
export const getUserInfo = () => storage.getItem(userInfoKey) || undefined
export const setUserInfo = (userInfo: object) => storage.setItem(userInfoKey, JSON.stringify(userInfo))
export const removeUserInfo = () => storage.removeItem(userInfoKey)

const printKey = 'print'
export const getPrint = () => storage.getItem(printKey) || undefined
export const setPrint = (printInfo: object) => storage.setItem(printKey, JSON.stringify(printInfo))
export const removePrint = () => storage.removeItem(printKey)

const newData = 'new'
export const getNewData = () => storage.getItem(newData) || undefined
export const setNewData = (val: object) => storage.setItem(newData, JSON.stringify(val))
