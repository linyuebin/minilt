// 重新生成 Material Symbols 图标字体子集（base64 内嵌到 src/styles/iconfont.css）
//
// 用法：新增/修改页面里的图标名后，同步维护下面的 ICONS 列表，然后执行：
//   cd app && node tools/build-iconfont.mjs
//
// 原理：调用 Google Fonts CSS2 API 按图标名单裁剪出子集 woff2（保留 rlig 连字，
// 静态实例 FILL 0 / wght 400 / GRAD 0 / opsz 24），转 base64 写入 iconfont.css。
// 仅重新生成时需要外网，运行时不依赖 Google。

import { writeFileSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))

// 与 icon-names.txt 保持一致；动态绑定的图标（如三元表达式）也要列进来
const ICONS = [
  'add', 'arrow_back', 'arrow_forward', 'arrow_forward_ios', 'auto_awesome',
  'auto_stories', 'chat_bubble', 'chevron_right', 'close', 'error_outline',
  'expand_more', 'favorite', 'forum', 'group', 'help',
  'history', 'hourglass_empty', 'image',
  'keyboard_double_arrow_up', 'lock', 'logout', 'menu', 'menu_book', 'person',
  'remove', 'reply', 'restart_alt', 'rotate_90_degrees_ccw', 'send',
  'settings', 'spa', 'thumb_up', 'thumb_up_off', 'thumb_up_off_alt',
  'visibility'
]

const cssUrl =
  'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined' +
  `&icon_names=${ICONS.join(',')}&display=block`
const UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'

const css = await (await fetch(cssUrl, { headers: { 'User-Agent': UA } })).text()
const fontUrl = css.match(/src:\s*url\((https:[^)]+)\)/)?.[1]
if (!fontUrl) throw new Error('未能从 Google Fonts 响应中解析出字体地址:\n' + css)

const buf = Buffer.from(await (await fetch(fontUrl, { headers: { 'User-Agent': UA } })).arrayBuffer())
if (buf.length < 1000) throw new Error('字体下载异常，仅 ' + buf.length + ' 字节')
// woff2 魔数校验：请求被限流时 Google 会返回 HTML 错误页，仅凭大小判断会误内嵌
if (buf.slice(0, 4).toString('ascii') !== 'wOF2') {
  throw new Error('下载内容不是 woff2 字体（收到: ' + buf.slice(0, 20).toString('utf8') + '），可能被限流，请稍后重试')
}

// 同步留存字体与名单，便于排查与离线复用
writeFileSync(join(__dirname, 'material-symbols.subset.woff2'), buf)
writeFileSync(join(__dirname, 'icon-names.txt'), ICONS.join('\n') + '\n')

const out = `/* Material Symbols Outlined 图标字体子集（base64 内嵌，H5 与小程序共用，不依赖外网）
 * 仅含项目用到的图标字形，由 tools/build-iconfont.mjs 生成。
 * 新增/更换图标后请重新生成：node tools/build-iconfont.mjs
 * 字体已是静态实例（FILL 0 / wght 400 / GRAD 0 / opsz 24），无需 font-variation-settings。*/
@font-face {
  font-family: 'Material Symbols Outlined';
  font-style: normal;
  font-weight: 400;
  font-display: block;
  src: url(data:font/woff2;base64,${buf.toString('base64')}) format('woff2');
}
`
const outPath = join(__dirname, '../src/styles/iconfont.css')
writeFileSync(outPath, out)
console.log(`已生成 ${outPath}（字体 ${(buf.length / 1024).toFixed(1)} KB，${ICONS.length} 个图标）`)
