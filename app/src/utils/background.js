// 自定义背景图工具：双端一份代码，按运行时能力分支（不用条件编译）。
// - 小程序端：chooseImage(compressed) -> saveFile 得到持久 savedFilePath
// - H5 端：chooseImage -> canvas 压缩（先填深色底防止透明 PNG 变白）-> dataURL 入 storage

const MAX_EDGE = 1280 // H5 压缩后的最长边
const JPEG_QUALITY = 0.82

const hasFileSystem = () => typeof uni.getFileSystemManager === 'function'

// 选择并处理一张背景图，返回可持久化的值（MP: savedFilePath / H5: dataURL）
// 取消或失败返回 null
export const pickCustomBg = () => {
  return new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        try {
          if (hasFileSystem()) {
            const saved = await saveFile(res.tempFilePaths[0])
            resolve(saved || null)
          } else {
            const dataUrl = await compressToDataUrl(res.tempFiles[0])
            resolve(dataUrl || null)
          }
        } catch (e) {
          console.log('处理背景图失败:', e)
          resolve(null)
        }
      },
      fail: () => resolve(null)
    })
  })
}

// 小程序端：临时文件转为持久文件
const saveFile = (tempFilePath) => {
  return new Promise((resolve) => {
    uni.saveFile({
      tempFilePath,
      success: (res) => resolve(res.savedFilePath),
      fail: () => {
        // 老基础库无 uni.saveFile 时兜底文件系统 API
        if (!hasFileSystem()) return resolve(null)
        uni.getFileSystemManager().saveFile({
          tempFilePath,
          success: (res) => resolve(res.savedFilePath),
          fail: () => resolve(null)
        })
      }
    })
  })
}

// H5 端：canvas 等比压缩为 JPEG dataURL
const compressToDataUrl = (file) => {
  return new Promise((resolve) => {
    if (!file) return resolve(null)
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      const scale = Math.min(1, MAX_EDGE / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.round(img.width * scale)
      canvas.height = Math.round(img.height * scale)
      const ctx = canvas.getContext('2d')
      // 先铺深色底，透明 PNG 才不会在深色界面里发白
      ctx.fillStyle = '#0a0a0a'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      resolve(canvas.toDataURL('image/jpeg', JPEG_QUALITY))
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(null)
    }
    img.src = url
  })
}

// 校验已存的背景值是否仍可用（小程序本地文件可能已失效）
export const validateBgPath = (value) => {
  if (!value) return Promise.resolve(false)
  if (!hasFileSystem()) {
    // H5：dataURL 无需校验文件
    return Promise.resolve(/^data:image\//.test(value))
  }
  return new Promise((resolve) => {
    uni.getFileSystemManager().access({
      path: value,
      success: () => resolve(true),
      fail: () => resolve(false)
    })
  })
}

// 释放旧的本地背景文件（尽力而为，任何错误都吞掉）
export const releaseBgFile = (value) => {
  if (!value || !hasFileSystem() || /^data:/.test(value)) return
  try {
    uni.getFileSystemManager().removeSavedFile({
      filePath: value,
      fail: () => {}
    })
  } catch (e) {
    // 静默
  }
}
