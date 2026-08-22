/**
 * @author HXN
 * @date 2026-08-18 17:31
 * @description HTTP 请求封装
 */
import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 实例与拦截器
 * 统一处理请求头、Token 注入、错误响应等
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器
let isRedirecting = false // 防止重复跳转

service.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data
    const url = error.config?.url || ''

    // 登录接口由调用方自行处理错误提示，避免重复弹窗
    const skipGlobalMessage = url.includes('/v1/auth/login')

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      if (!skipGlobalMessage) {
        ElMessage.error('登录已过期，请重新登录')
      }
      window.location.href = '/home'
    } else if (status === 403) {
      ElMessage.error('没有操作权限')
    } else if (status === 400) {
      if (!skipGlobalMessage) {
        ElMessage.error(data?.message || '请求参数错误')
      }
    } else if (status === 502 || status === 503 || !status) {
      // 后端服务未启动或网络不可达
      const token = localStorage.getItem('token')
      if (token && !isRedirecting) {
        // 已登录用户：自动退出登录
        isRedirecting = true
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        ElMessage.error('服务连接失败，请稍后重试')
        window.location.href = '/home'
      } else if (!token) {
        // 未登录用户：仅提示网络错误
        ElMessage.error('网络连接失败')
      }
    } else if (status >= 500) {
      ElMessage.error(data?.message || '服务器错误')
    }

    return Promise.reject(error)
  },
)

export default service
