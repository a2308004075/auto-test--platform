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
service.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/project'
    } else if (status === 403) {
      ElMessage.error('没有操作权限')
    } else if (status === 400) {
      ElMessage.error(data?.message || '请求参数错误')
    } else if (status >= 500) {
      ElMessage.error(data?.message || '服务器错误')
    } else if (!status) {
      ElMessage.error('网络连接失败')
    }

    return Promise.reject(error)
  },
)

export default service
