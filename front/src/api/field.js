import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// interceptor for unified error handling
api.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  (error) => {
    return Promise.reject(error)
  }
)

export function getFields() {
  return api.get('/fields')
}

export function saveField(data) {
  return api.post('/fields', data)
}

export function deleteField(key) {
  return api.delete(`/fields/${key}`)
}

export function updateSort(keys) {
  return api.put('/fields/sort', keys)
}

export function simulateExpression(data) {
  return api.post('/fields/simulate', data)
}
