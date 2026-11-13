import http from './http'

export const login = payload => http.post('/auth/login', payload)
export const logout = () => http.post('/auth/logout')
export const getMe = () => http.get('/auth/me')
export const getOverview = () => http.get('/users/overview')
export const getUsers = params => http.get('/users', { params })
export const setUserStatus = (id, status) => http.put(`/users/${id}/status`, { status })
export const getFeed = params => http.get('/feed', { params })
