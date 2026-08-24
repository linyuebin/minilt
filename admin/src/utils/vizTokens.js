// 图表配色令牌 —— 取自 dataviz 参考调色板，已用 validate_palette.js 校验：
//   node scripts/validate_palette.js "#2a78d6,#eb6834" --mode light --surface "#ffffff"
//   → 明度带 / 彩度下限 / CVD 分离(ΔE 24.7) / 常视觉下限(ΔE 33.6) / 对比度 全部 PASS
// 金色仅用于站点 chrome（侧边栏、品牌标记），不承担数据编码，避免用未校验的色值上图。
export const VIZ = {
  surface: '#ffffff',
  // 分类槽位：按固定顺序取用，不可跳位、不可循环
  series1: '#2a78d6', // 新增用户
  series2: '#eb6834', // 新增留言
  textPrimary: '#0b0b0b',
  textSecondary: '#52514e',
  textMuted: '#898781',
  gridline: '#e1e0d9',
  axis: '#c3c2b7',
}

export const AXIS_LABEL = { color: VIZ.textMuted, fontSize: 12 }
export const FONT_FAMILY =
  'system-ui, -apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif'
