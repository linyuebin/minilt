// 诵读音频全局状态：播放器不随页面销毁，离开阅读页后继续后台播放
import { defineStore } from 'pinia'
import { resolveMediaUrl } from '@/utils/request'
import { getChapters } from '@/api'

// 音频上下文与当前源不放进 state（不可序列化，避免被持久化/调试工具干扰）
let ctx = null
let currentSrc = ''

export const useAudioStore = defineStore('audio', {
  state: () => ({
    playing: false,
    chapterId: 0, // 正在播放的章节（0=未播放）
    chapterTitle: '',
    // 音色与播放方式的持久化与 ThemePanel 共用同一组 storage key
    voiceKey: uni.getStorageSync('audioVoice') || 'xiaoxiao',
    playMode: uni.getStorageSync('audioPlayMode') || 'continuous', // continuous 连播 | loop 循环本章
    chapters: [] // 章节标题表（懒加载，供连播/悬浮条显示）
  }),

  getters: {
    // 悬浮条可见性：有播放会话即显示（暂停时保留，关闭后 chapterId 归零隐藏）
    barVisible: (s) => s.chapterId > 0
  },

  actions: {
    ensureCtx() {
      if (ctx) return ctx
      ctx = uni.createInnerAudioContext()
      ctx.onPlay(() => { this.playing = true })
      ctx.onPause(() => { this.playing = false })
      ctx.onEnded(() => this.handleEnded())
      ctx.onError((err) => {
        this.playing = false
        console.error('音频播放失败:', err)
        uni.showToast({ title: '音频加载失败', icon: 'none' })
      })
      return ctx
    },

    srcOf(chapterId) {
      return resolveMediaUrl(`/api/audio/${this.voiceKey}/${chapterId}`)
    },

    // 章节标题表懒加载（失败静默，标题兜底为空）
    async ensureChapters() {
      if (this.chapters.length) return
      try {
        this.chapters = (await getChapters()) || []
      } catch (e) {
        console.error('加载章节列表失败:', e)
      }
    },

    titleOf(chapterId) {
      const c = this.chapters.find(x => x.id === chapterId)
      return c ? c.title : ''
    },

    // 播放指定章节（换章/换音色/循环重播统一从零开始）
    play(chapterId, title = '') {
      const c = this.ensureCtx()
      c.stop()
      this.chapterId = chapterId
      this.chapterTitle = title || this.titleOf(chapterId)
      this.ensureChapters().then(() => {
        // 章节标题表就绪后回填（连播自动进入下一章时标题可能延迟到达）
        if (this.chapterId === chapterId) this.chapterTitle = this.titleOf(chapterId)
      })
      currentSrc = this.srcOf(chapterId)
      c.src = currentSrc
      c.play()
      this.playing = true
    },

    pause() {
      if (ctx) ctx.pause()
      this.playing = false
    },

    resume() {
      if (!ctx) return
      ctx.play()
      this.playing = true
    },

    // 阅读页工具栏入口：同章在播则暂停、暂停中则续播，异章换源播放
    toggle(chapterId, title = '') {
      if (this.chapterId === chapterId) {
        if (this.playing) this.pause()
        else this.resume()
        return
      }
      this.play(chapterId, title)
    },

    // 关闭（悬浮条 ✕）：停止播放并收起悬浮条
    stop() {
      if (ctx) ctx.stop()
      this.playing = false
      this.chapterId = 0
      this.chapterTitle = ''
    },

    // 播放结束：循环本章重播；连播进入下一章，第 81 章播完收尾
    // 注意不能以 playing 判断短路——H5 端自然播完会先触发 onPause 再触发 onEnded，
    // playing 已被同步为 false（此前该守卫导致连播/循环全部失效）
    handleEnded() {
      if (this.playMode === 'loop') {
        // 循环本章：回零重播（不重设 src，同源重赋值在 H5 上可能不触发重载）
        if (ctx) {
          ctx.seek(0)
          ctx.play()
        }
        return
      }
      if (this.chapterId < 81) {
        this.play(this.chapterId + 1)
      } else {
        // 最后一章播完收尾（保留章号供悬浮条展示，可手动 ✕ 关闭）
        this.playing = false
      }
    },

    // 换音色：正在播放时无缝换源重播当前章
    setVoice(key) {
      if (key === this.voiceKey) return
      this.voiceKey = key
      uni.setStorageSync('audioVoice', key)
      if (this.playing) this.play(this.chapterId, this.chapterTitle)
    },

    setPlayMode(mode) {
      if (mode === this.playMode) return
      this.playMode = mode
      uni.setStorageSync('audioPlayMode', mode)
    }
  }
})
