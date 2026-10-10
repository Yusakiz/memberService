let csrf = null

export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function parseResponse(response) {
  if (response.status === 204) return null
  const type = response.headers.get('content-type') || ''
  return type.includes('application/json') ? response.json() : null
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    credentials: 'same-origin',
    ...options,
  })
  const body = await parseResponse(response)
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) csrf = null
    throw new ApiError(body?.message || '系統發生錯誤，請稍後再試', response.status, body?.fieldErrors)
  }
  return body
}

export async function refreshCsrf() {
  csrf = await request('/api/auth/csrf')
  return csrf
}

async function writeRequest(path, options = {}) {
  if (!csrf) await refreshCsrf()
  return request(path, {
    ...options,
    headers: {
      ...options.headers,
      [csrf.headerName]: csrf.token,
    },
  })
}

export function getMe() {
  return request('/api/members/me')
}

export function register(data) {
  return writeRequest('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export async function login(username, password) {
  const form = new URLSearchParams({ username, password })
  const member = await writeRequest('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: form,
  })
  csrf = null
  await refreshCsrf()
  return member
}

export async function logout() {
  await writeRequest('/api/auth/logout', { method: 'POST' })
  csrf = null
  await refreshCsrf()
}

export function searchMembers(field, keyword, page) {
  const query = new URLSearchParams({ field, keyword, page: String(page) })
  return request(`/api/members?${query}`)
}

export async function updateProfile(data) {
  const result = await writeRequest('/api/members/me', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (result.reLoginRequired) csrf = null
  return result
}

export async function changePassword(data) {
  const result = await writeRequest('/api/members/me/password', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  csrf = null
  return result
}

export async function deleteAccount() {
  const result = await writeRequest('/api/members/me', { method: 'DELETE' })
  csrf = null
  return result
}
