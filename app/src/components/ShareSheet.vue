<template>
  <!-- 遮罩 -->
  <view v-if="show" class="share-mask" :style="{ zIndex: overlayZIndex }" @click="close"></view>

  <!-- 分享海报弹层：Canvas 绘制海报（引用原文 + 感悟 + 链接二维码），支持保存/微信分享/复制链接 -->
  <view v-if="show" class="share-sheet" :style="{ zIndex: zIndex }">
    <!-- 离屏画布（仅小程序/App）：绘制完成后导出为图片预览；H5 直接用原生离屏 canvas，绕开 uni-h5 的导出裁剪 -->
    <!-- #ifndef H5 -->
    <canvas type="2d" id="poster-canvas" canvas-id="poster-canvas" class="poster-canvas"></canvas>
    <!-- #endif -->

    <view class="share-header">
      <text class="share-title">分享海报</text>
      <view class="share-close" @click="close">
        <text class="material-symbols-outlined">close</text>
      </view>
    </view>

    <scroll-view scroll-y class="share-body">
      <!-- 海报预览：canvas 离屏绘制后转图片展示 -->
      <view class="poster-wrap">
        <image
          v-if="posterPath"
          :src="posterPath"
          mode="widthFix"
          class="poster-image"
          @click="previewPoster"
        />
        <view v-else class="poster-loading">
          <text>{{ failed ? '海报生成失败' : '海报生成中...' }}</text>
        </view>
      </view>
      <view class="share-body-spacer"></view>
    </scroll-view>

    <view class="share-footer">
      <!-- 微信分享：小程序端用原生转发按钮（触发页面 onShareAppMessage）；H5/App 端引导转发或复制链接 -->
      <!-- #ifdef MP-WEIXIN -->
      <button class="share-btn wx-btn" open-type="share">
        <text class="material-symbols-outlined share-btn-icon">chat</text>
        <text>微信分享</text>
      </button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <view class="share-btn wx-btn" @click="doWechatShare">
        <text class="material-symbols-outlined share-btn-icon">chat</text>
        <text>微信分享</text>
      </view>
      <!-- #endif -->
      <view class="share-btn save-btn" :class="{ disabled: !posterPath }" @click="savePoster">
        <text class="material-symbols-outlined share-btn-icon">download</text>
        <text>保存图片</text>
      </view>
      <view class="share-btn link-btn" @click="copyLink">
        <text class="material-symbols-outlined share-btn-icon">link</text>
        <text>复制链接</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch, getCurrentInstance } from 'vue'
import qrcode from 'qrcode-generator'
import { shareUrlOf } from '@/utils/share'

const props = defineProps({
  show: { type: Boolean, default: false },
  // 要分享的感悟（feed 卡片数据）
  item: { type: Object, default: null },
  zIndex: { type: Number, default: 10001 },
  overlayZIndex: { type: Number, default: 10000 }
})

const emit = defineEmits(['update:show'])

const instance = getCurrentInstance()

// 海报画布逻辑尺寸（导出时放大一倍保证清晰度）
const POSTER_W = 600
const POSTER_H = 960

const posterPath = ref('')
const failed = ref(false)

const close = () => emit('update:show', false)

// ===== 文本换行：按字测量（中文逐字断行）；超出行数时末行截断加省略号 =====
const wrapText = (ctx, text, maxWidth, maxLines) => {
  const all = []
  let line = ''
  for (const ch of String(text || '')) {
    if (ch === '\n') {
      all.push(line)
      line = ''
      continue
    }
    if (ctx.measureText(line + ch).width > maxWidth && line) {
      all.push(line)
      line = ch
    } else {
      line += ch
    }
  }
  if (line) all.push(line)
  // 行尾孤立的省略号并回上一行（允许 24px 容差），避免海报上「……」独占一行
  if (all.length > 1 && /^…+$/.test(all[all.length - 1])) {
    const prev = all[all.length - 2]
    if (ctx.measureText(prev + all[all.length - 1]).width <= maxWidth + 24) {
      all.splice(all.length - 2, 2, prev + all[all.length - 1])
    }
  }
  if (maxLines && all.length > maxLines) {
    const kept = all.slice(0, maxLines)
    let last = kept[maxLines - 1]
    while (ctx.measureText(last + '…').width > maxWidth && last.length > 1) {
      last = last.slice(0, -1)
    }
    kept[maxLines - 1] = last + '…'
    // 省略号独占一行时并入上一行
    if (kept[kept.length - 1] === '…' && kept.length > 1) {
      kept.pop()
      kept[kept.length - 1] += '…'
    }
    return kept
  }
  return all
}

const roundRect = (ctx, x, y, w, h, r) => {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

// 二维码绘制到指定区域（白底黑码，留 quiet zone）
const drawQr = (ctx, url, x, y, size) => {
  const qr = qrcode(0, 'M')
  qr.addData(url)
  qr.make()
  const count = qr.getModuleCount()
  // 二维码四周留 2 模块的静区
  const quiet = 2
  const cell = size / (count + quiet * 2)
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(x, y, size, size)
  ctx.fillStyle = '#1a1815'
  for (let row = 0; row < count; row++) {
    for (let col = 0; col < count; col++) {
      if (qr.isDark(row, col)) {
        ctx.fillRect(
          x + (col + quiet) * cell,
          y + (row + quiet) * cell,
          Math.ceil(cell),
          Math.ceil(cell)
        )
      }
    }
  }
}

// ===== 海报绘制：引用原文 + 感悟 + 二维码 =====
// 只负责画：调用方先备好画布（按 scale 放大 buffer 后 ctx.scale），这里全部用 600×960 逻辑坐标
const drawPoster = (ctx, item) => {

  const INK_BG = '#17140f'
  const GOLD = '#d4af37'
  const TEXT_MAIN = '#f0ede6'
  const TEXT_SUB = 'rgba(248, 246, 242, 0.55)'
  const TEXT_DIM = 'rgba(248, 246, 242, 0.4)'
  const FONT = '"PingFang SC", "Microsoft YaHei", sans-serif'
  const MARGIN = 56
  const CONTENT_W = POSTER_W - MARGIN * 2

  ctx.fillStyle = INK_BG
  ctx.fillRect(0, 0, POSTER_W, POSTER_H)

  // 外框
  ctx.strokeStyle = 'rgba(212, 175, 55, 0.4)'
  ctx.lineWidth = 2
  ctx.strokeRect(24, 24, POSTER_W - 48, POSTER_H - 48)

  // 标题
  ctx.textAlign = 'center'
  ctx.fillStyle = GOLD
  ctx.font = `600 34px ${FONT}`
  ctx.fillText('道德经 · 道友感悟', POSTER_W / 2, 92)
  ctx.fillStyle = TEXT_DIM
  ctx.font = `18px ${FONT}`
  ctx.fillText('修身养性 · 悟道参禅', POSTER_W / 2, 124)

  // 作者行
  ctx.textAlign = 'left'
  const authorY = 176
  ctx.beginPath()
  ctx.arc(MARGIN + 24, authorY, 24, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(212, 175, 55, 0.18)'
  ctx.fill()
  ctx.strokeStyle = 'rgba(212, 175, 55, 0.5)'
  ctx.lineWidth = 1
  ctx.stroke()
  ctx.fillStyle = GOLD
  ctx.font = `600 22px ${FONT}`
  ctx.textAlign = 'center'
  ctx.fillText(item.avatarChar || (item.nickname || '道').charAt(0), MARGIN + 24, authorY + 8)
  ctx.textAlign = 'left'
  ctx.fillStyle = TEXT_MAIN
  ctx.font = `500 24px ${FONT}`
  ctx.fillText(item.nickname || '道友', MARGIN + 62, authorY - 2)
  ctx.fillStyle = TEXT_SUB
  ctx.font = `18px ${FONT}`
  const source = item.chapterTitle ? `《${item.chapterTitle}》` : '随笔心得'
  ctx.fillText(source, MARGIN + 62, authorY + 24)

  // 引用原文块（数据里的「……」是列表截断产物，海报上收敛为单个省略号）
  let cursorY = authorY + 48
  if (item.chapterExcerpt) {
    const rawQuote = String(item.chapterExcerpt).replace(/…+\s*$/, '')
    ctx.font = `22px ${FONT}`
    const quoteLines = wrapText(ctx, rawQuote, CONTENT_W - 44, 4)
    const last = quoteLines[quoteLines.length - 1] || ''
    if (rawQuote.length < String(item.chapterExcerpt).trim().length && !last.endsWith('…')) {
      quoteLines[quoteLines.length - 1] = last + '…'
    }
    const quoteH = quoteLines.length * 34 + 36
    roundRect(ctx, MARGIN, cursorY, CONTENT_W, quoteH, 12)
    ctx.fillStyle = 'rgba(255, 255, 255, 0.05)'
    ctx.fill()
    ctx.fillStyle = 'rgba(212, 175, 55, 0.6)'
    ctx.fillRect(MARGIN, cursorY + 8, 4, quoteH - 16)
    ctx.fillStyle = TEXT_SUB
    ctx.font = `22px ${FONT}`
    quoteLines.forEach((l, i) => {
      ctx.fillText(l, MARGIN + 22, cursorY + 42 + i * 34)
    })
    cursorY += quoteH + 32
  }

  // 感悟正文：底部给二维码区留 240px
  ctx.font = `26px ${FONT}`
  const contentMaxBottom = POSTER_H - 250
  const maxLines = Math.max(1, Math.floor((contentMaxBottom - cursorY - 20) / 42))
  const contentLines = wrapText(ctx, item.content, CONTENT_W, maxLines)
  ctx.fillStyle = TEXT_MAIN
  ctx.font = `26px ${FONT}`
  contentLines.forEach((l, i) => {
    ctx.fillText(l, MARGIN, cursorY + 30 + i * 42)
  })

  // 二维码区：白底码块 + 右侧引导文案
  const qrY = POSTER_H - 216
  drawQr(ctx, shareUrlOf(item), MARGIN, qrY, 152)
  ctx.textAlign = 'left'
  ctx.fillStyle = GOLD
  ctx.font = `500 22px ${FONT}`
  ctx.fillText('微信扫码 · 看这条感悟', MARGIN + 180, qrY + 46)
  ctx.fillStyle = TEXT_SUB
  ctx.font = `17px ${FONT}`
  const linkLines = wrapText(ctx, shareUrlOf(item), POSTER_W - MARGIN - (MARGIN + 180), 2)
  linkLines.forEach((l, i) => {
    ctx.fillText(l, MARGIN + 180, qrY + 82 + i * 26)
  })

  // 底部落款
  ctx.textAlign = 'center'
  ctx.fillStyle = TEXT_DIM
  ctx.font = `16px ${FONT}`
  ctx.fillText('— 道德经阅读社区 —', POSTER_W / 2, POSTER_H - 40)
}

// ===== 打开时绘制并导出海报 =====
// H5：uni-h5 的 canvasToTempFilePath 会把导出区域裁剪到画布的 CSS 布局尺寸（offsetWidth/offsetHeight），
// 且内部 canvas 缓冲可能被 ResizeSensor 异步重置 —— 布局尺寸小于缓冲时只能导出左上角。
// 故 H5 直接建原生离屏 canvas 绘制并 toDataURL，完全绕开这套逻辑。
// 小程序/App：仍用页面上的 canvas 2d 节点，显式按 2 倍设置缓冲，导出按缓冲像素尺寸取全图。
const RENDER_SCALE = 2

// #ifdef H5
const renderPoster = (item) => {
  const canvas = document.createElement('canvas')
  canvas.width = POSTER_W * RENDER_SCALE
  canvas.height = POSTER_H * RENDER_SCALE
  const ctx = canvas.getContext('2d')
  ctx.scale(RENDER_SCALE, RENDER_SCALE)
  drawPoster(ctx, item)
  return canvas.toDataURL('image/png')
}
// #endif

// #ifndef H5
const renderPoster = async (item) => {
  // 等弹层与 canvas 完成挂载
  await new Promise(r => setTimeout(r, 120))
  const node = await new Promise((resolve, reject) => {
    uni.createSelectorQuery().in(instance?.proxy)
      .select('#poster-canvas')
      .fields({ node: true, size: true })
      .exec((res) => (res && res[0] && res[0].node ? resolve(res[0].node) : reject(new Error('canvas 未就绪'))))
  })
  node.width = POSTER_W * RENDER_SCALE
  node.height = POSTER_H * RENDER_SCALE
  const ctx = node.getContext('2d')
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.scale(RENDER_SCALE, RENDER_SCALE)
  drawPoster(ctx, item)
  return await new Promise((resolve, reject) => {
    uni.canvasToTempFilePath({
      canvasId: 'poster-canvas',
      canvas: node,
      x: 0,
      y: 0,
      width: node.width,
      height: node.height,
      destWidth: node.width,
      destHeight: node.height,
      success: (res) => resolve(res.tempFilePath),
      fail: reject
    }, instance?.proxy)
  })
}
// #endif

watch(() => props.show, async (open) => {
  if (!open || !props.item) return
  posterPath.value = ''
  failed.value = false
  try {
    posterPath.value = await renderPoster(props.item)
  } catch (e) {
    console.error('海报生成失败:', e)
    failed.value = true
  }
})

// 保存图片：H5 走浏览器下载，小程序/App 存相册
const savePoster = () => {
  if (!posterPath.value) return
  // #ifdef H5
  const a = document.createElement('a')
  a.href = posterPath.value
  a.download = `daodejing-share-${props.item?.id || Date.now()}.png`
  document.body.appendChild(a)
  a.click()
  a.remove()
  uni.showToast({ title: '海报已保存', icon: 'none' })
  // #endif
  // #ifndef H5
  uni.saveImageToPhotosAlbum({
    filePath: posterPath.value,
    success: () => uni.showToast({ title: '已保存到相册', icon: 'none' }),
    fail: () => uni.showToast({ title: '保存失败，请检查相册权限', icon: 'none' })
  })
  // #endif
}

// 微信分享（H5/App 端）：微信内引导右上角转发，普通浏览器复制链接
const doWechatShare = () => {
  // #ifdef H5
  if (/MicroMessenger/i.test(navigator.userAgent)) {
    uni.showToast({ title: '点击右上角「···」转发给朋友', icon: 'none' })
  } else {
    uni.setClipboardData({
      data: shareUrlOf(props.item),
      success: () => uni.showToast({ title: '链接已复制，去微信粘贴给好友', icon: 'none' })
    })
  }
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: shareUrlOf(props.item),
    success: () => uni.showToast({ title: '链接已复制，去微信粘贴给好友', icon: 'none' })
  })
  // #endif
}

// 复制链接
const copyLink = () => {
  uni.setClipboardData({
    data: shareUrlOf(props.item),
    success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
  })
}

// 预览海报大图
const previewPoster = () => {
  if (!posterPath.value) return
  uni.previewImage({ urls: [posterPath.value] })
}
</script>

<style lang="scss" scoped>
.share-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
}

.share-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-height: 85vh;
  border-radius: 24rpx 24rpx 0 0;
  background: var(--panel-bg-solid, var(--surface-1));
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-top: 1px solid var(--accent-15);
  display: flex;
  flex-direction: column;
  animation: sheet-up 0.3s ease-out;
}

@keyframes sheet-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.share-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 30rpx 10rpx;
  flex-shrink: 0;
}

.share-title {
  font-size: 32rpx;
  font-weight: 500;
  color: var(--accent);
  letter-spacing: 4rpx;
}

.share-close {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: var(--surface-2);
  border: 1px solid var(--accent-15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-2);

  .material-symbols-outlined {
    font-size: 36rpx;
  }
}

.share-body {
  flex: 1;
  min-height: 0;
  padding: 10rpx 30rpx 0;
  box-sizing: border-box;
}

.poster-wrap {
  border-radius: 20rpx;
  overflow: hidden;
  border: 1px solid var(--accent-15);
}

.poster-image {
  display: block;
  width: 100%;
}

.poster-loading {
  height: 560rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  font-size: 26rpx;
}

.share-body-spacer {
  height: 30rpx;
}

// 离屏 canvas：不参与布局但要可查询（display:none 会导致部分端无法取 node）
.poster-canvas {
  position: fixed;
  left: -9999px;
  top: 0;
  width: 600px;
  height: 960px;
}

// 底部操作条
.share-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx 30rpx calc(40rpx + env(safe-area-inset-bottom));
  border-top: 1px solid var(--accent-15);
  flex-shrink: 0;
}

.share-btn {
  flex: 1;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border-radius: 40rpx;
  font-size: 24rpx;
  margin: 0;
  padding: 0;
  white-space: nowrap;

  &::after {
    border: none;
  }

  .share-btn-icon {
    font-size: 32rpx;
  }
}

.wx-btn {
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: #16130c;
  font-weight: 600;
}

.save-btn {
  background: var(--surface-2);
  border: 1px solid var(--accent-30);
  color: var(--accent);

  &.disabled {
    opacity: 0.5;
  }
}

.link-btn {
  background: var(--surface-2);
  border: 1px solid var(--accent-15);
  color: var(--text-2);
}
</style>
