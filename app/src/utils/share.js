// 分享工具：感悟分享链接与海报文案
// 链接统一指向 H5 路由（扫码/转发落地页）：H5 端取当前 origin（开发、生产自适应），
// 小程序/App 端没有 location，用上线域名（上线前确认已备案并配置在下方）
const H5_SITE = 'https://daodejing.com'

export const siteBase = () => {
  // #ifdef H5
  return location.origin
  // #endif
  // #ifndef H5
  return H5_SITE
  // #endif
}

// 感悟分享链接：引用了正文的落到对应章节阅读页，随笔落到道友广场
export const shareUrlOf = (item) => {
  const base = siteBase()
  return item.chapterId
    ? `${base}/#/pages/reading/index?chapterId=${item.chapterId}`
    : `${base}/#/pages/community/index`
}
