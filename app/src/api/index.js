import { get, post, put, del, uploadFile } from '@/utils/request'

// ============= 用户认证相关 =============
// H5注册
export const register = (data) => post('/auth/register', data)

// H5登录
export const login = (data) => post('/auth/login', data)

// 微信小程序登录
export const wxLogin = (code) => post('/auth/wx-login', { code })

// 获取当前用户信息
export const getCurrentUser = () => get('/auth/me')

// ============= 章节相关 =============
// 获取章节列表
export const getChapters = () => get('/chapters')

// 获取章节详情（含原文、释义、点赞收藏状态）
export const getChapterDetail = (id) => get(`/chapters/${id}`)

// 获取章节评论（后端返回的是完整留言树数组，不分页）
export const getChapterComments = (chapterId) => {
  return get(`/chapters/${chapterId}/comments`)
}

// 社区广场：所有道友的感悟（filter: all/featured/dao/de，分页）
export const getCommunityFeed = (params) => get('/comments/feed', params)

// 更新阅读进度
export const updateProgress = (chapterId) => put('/users/me/progress', { chapterId })

// ============= 评论相关 =============
// 发布评论/回复
export const createComment = (data) => post('/comments', data)

// 上传留言配图（单张，返回 { url: '/api/upload/xx.jpg' }）
export const uploadCommentImage = (filePath) => uploadFile('/upload/image', filePath)

// 点赞/取消点赞评论
export const toggleCommentLike = (commentId) => post(`/comments/${commentId}/like`)

// ============= 收藏相关 =============
// 收藏章节
export const addFavorite = (chapterId) => post('/favorites', { chapterId })

// 取消收藏
export const removeFavorite = (chapterId) => del(`/favorites/${chapterId}`)

// 获取收藏列表
export const getFavorites = (params) => get('/favorites', params)

// ============= 个人中心相关 =============
// 我的评论
export const getMyComments = (params) => get('/users/me/comments', params)

// 我的回复
export const getMyReplies = (params) => get('/users/me/replies', params)

// 我的点赞
export const getMyLikes = (params) => get('/users/me/likes', params)

// ============= 诵读音频相关 =============
// 可用音色列表（[{ voiceKey, voiceName, count }]）
export const getAudioVoices = () => get('/audio/voices')

export default {
  // 认证
  register,
  login,
  wxLogin,
  getCurrentUser,
  // 章节
  getChapters,
  getChapterDetail,
  getChapterComments,
  getCommunityFeed,
  updateProgress,
  // 评论
  createComment,
  uploadCommentImage,
  toggleCommentLike,
  // 收藏
  addFavorite,
  removeFavorite,
  getFavorites,
  // 个人中心
  getMyComments,
  getMyReplies,
  getMyLikes,
  // 诵读音频
  getAudioVoices
}
