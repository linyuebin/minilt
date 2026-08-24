import http from '../utils/request'

export const login = (username, password) => http.post('/auth/login', { username, password })

export const fetchOverview = () => http.get('/stats/overview')
export const fetchTrend = (days = 30) => http.get('/stats/trend', { params: { days } })
export const fetchHotChapters = (limit = 10) => http.get('/stats/chapters', { params: { limit } })

export const fetchComments = (params) => http.get('/comments', { params })
export const deleteComment = (id) => http.delete(`/comments/${id}`)

export const fetchUsers = (params) => http.get('/users', { params })
