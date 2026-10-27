import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  changePassword: (data) => request.post('/auth/password', data)
}

export const userApi = {
  managers: () => request.get('/users/managers'),
  options: () => request.get('/users/options'),
  create: (data) => request.post('/users', data),
  update: (id, data) => request.put(`/users/${id}`, data),
  resetPassword: (id, data) => request.put(`/users/${id}/password`, data),
  remove: (id) => request.delete(`/users/${id}`)
}

export const groupApi = {
  list: (keyword) => request.get('/groups', { params: { keyword } }),
  create: (data) => request.post('/groups', data),
  update: (id, data) => request.put(`/groups/${id}`, data),
  remove: (id) => request.delete(`/groups/${id}`)
}

export const planApi = {
  search: (params) => request.get('/plans', { params }),
  get: (id) => request.get(`/plans/${id}`),
  months: () => request.get('/plans/months'),
  create: (data) => request.post('/plans', data),
  update: (id, data) => request.put(`/plans/${id}`, data),
  remove: (id) => request.delete(`/plans/${id}`)
}

export const visitApi = {
  search: (params) => request.get('/visits', { params }),
  options: () => request.get('/visits/options'),
  create: (data) => request.post('/visits', data),
  update: (id, data) => request.put(`/visits/${id}`, data),
  remove: (id) => request.delete(`/visits/${id}`)
}

export const statApi = {
  home: (month) => request.get('/stats/home', { params: { month } }),
  overview: (params) => request.get('/stats/overview', { params }),
  byManager: (month) => request.get('/stats/by-manager', { params: { month } }),
  byMonth: (params) => request.get('/stats/by-month', { params }),
  byGroup: (params) => request.get('/stats/by-group', { params })
}

function ensureBlob(response) {
  const blob = response.data
  if (blob && blob.type && blob.type.includes('application/json')) {
    return blob.text().then((text) => {
      let message = '导出失败'
      try {
        message = JSON.parse(text).message || message
      } catch (e) {
        // ignore
      }
      throw new Error(message)
    })
  }
  return null
}

function saveBlob(blob, fallbackName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fallbackName
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 3000)
}

async function download(url, params, fallbackName) {
  const response = await request.get(url, { params, responseType: 'blob' })
  const pending = ensureBlob(response)
  if (pending) {
    await pending
    throw new Error('导出失败')
  }
  const disposition = response.headers['content-disposition'] || ''
  let filename = fallbackName
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match) {
    filename = decodeURIComponent(utf8Match[1])
  } else {
    const plain = /filename="?([^";]+)"?/i.exec(disposition)
    if (plain) filename = plain[1]
  }
  saveBlob(response.data, filename)
}

export const excelApi = {
  preview: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/excel/import/preview', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  apply: (data) => request.post('/excel/import/apply', data),
  template: () => download('/excel/template', {}, '拜访计划导入模板.xlsx'),
  exportVisits: (params) => download('/excel/export/visits', params, '拜访记录.xlsx'),
  exportFull: (params) => download('/excel/export/full', params, '拜访清单.xlsx')
}
