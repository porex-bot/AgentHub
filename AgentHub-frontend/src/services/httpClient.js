import axios from 'axios'

export const API_BASE_URL = '/api'

const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      error.friendlyMessage = '连接失败，请确认后端服务已启动。'
    }
    return Promise.reject(error)
  }
)

export default httpClient
