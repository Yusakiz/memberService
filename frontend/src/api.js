let csrf = null

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

async function request(path, options = {}) {
  const response = await fetch(path, { credentials: 'same-origin', ...options })
  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) csrf = null
    throw new ApiError(body?.message || '操作失敗', response.status)
  }
  return body
}

async function getCsrf() {
  if (!csrf) csrf = await request('/api/auth/csrf')
  return csrf
}

async function write(path, options) {
  const token = await getCsrf()
  return request(path, {
    ...options,
    headers: { ...options.headers, [token.headerName]: token.token },
  })
}

export const getMe = () => request('/api/members/me')

export const register = (data) => write('/api/auth/register', {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
})

export async function login(username, password) {
  const body = new URLSearchParams({ username, password })
  await write('/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body,
  })
  csrf = null
}

export async function logout() {
  await write('/api/auth/logout', { method: 'POST' })
  csrf = null
}

export const searchMembers = (field, keyword) =>
  request(`/api/members?${new URLSearchParams({ field, keyword })}`)

export async function updateMember(data) {
  const result = await write('/api/members/me', {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  })
  if (result.reLoginRequired) csrf = null
  return result
}

export async function deleteMember() {
  await write('/api/members/me', { method: 'DELETE' })
  csrf = null
}
