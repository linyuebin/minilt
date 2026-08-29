# 道德经阅读社区

深色水墨风格的《道德经》阅读社区。游客可自由阅读全部 81 章，登录后可留言、回复、点赞、收藏并同步修行进度。

一套后端同时服务三个前端：H5 网页、微信小程序、PC 管理后台。

## 目录结构

```
minilt/
├── server/                 # Spring Boot 3 后端（:8080）
├── app/                    # uni-app 用户端，一套代码编译 H5 + 微信小程序（H5 :5173）
├── admin/                  # Vue3 + Element Plus 管理后台（:5174）
├── 道德经产品PRD.md         # 产品需求
├── 开发计划.md              # 任务拆分与进度（权威记录）
├── 开发日志.md              # 各批次开发过程与问题定位
└── daoism-forum.html       # 早期单文件原型（保留作视觉参考）
```

## 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | `server/pom.xml` 指定 `java.version=21` |
| Maven | 3.9+ | |
| MySQL | 8.x | 需运行在 3306，账号默认 root/root（可用环境变量覆盖） |
| Node.js | 22 | 前端两个工程都用 |
| 微信开发者工具 | 最新稳定版 | 仅小程序端需要 |
| HBuilderX | 5.x（与 uni-app 编译器版本匹配） | 仅 App（Android/iOS）与 HarmonyOS 端运行/打包需要 |
| DevEco Studio | 5.0+ | 仅 HarmonyOS 真机运行/打 hap 包需要 |

---

## 本地启动

三个服务相互独立，**先启后端**，前端才有数据。

### 1. 后端 server/（:8080）

数据库无需手工创建：连接串带 `createDatabaseIfNotExist=true`，`schema.sql` 与 `data.sql` 由 Spring 在启动时自动执行，且都是幂等的（`CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`），重复启动不会重复插数据。

```bash
cd server
mvn spring-boot:run
```

数据库账号密码与默认值不同时，用环境变量覆盖，不要改配置文件：

```bash
DB_USERNAME=你的账号 DB_PASSWORD=你的密码 mvn spring-boot:run
```

验证：

```bash
curl http://localhost:8080/api/chapters      # 应返回 81 章列表
```

**种子数据**：管理员账号 `admin` / `admin123`；81 章经文与释义随 `data.sql` 一并写入。

### 2. 用户端 app/ — H5（:5173）

```bash
cd app
npm install
npm run dev:h5
```

打开 http://localhost:5173 。vite 已配置把 `/api` 代理到 `:8080`，无需额外设置。

> 用桌面浏览器调试时**建议开设备模拟（手机视口）**。这是移动端布局，rpx 按 750px 设计稿换算，桌面全宽下留白会很大。

### 3. 用户端 app/ — 微信小程序

```bash
cd app
npm run dev:mp-weixin        # 产物在 app/dist/dev/mp-weixin，会持续监听改动
```

然后用**微信开发者工具**导入 `app/dist/dev/mp-weixin` 目录运行。

两点必须注意：

- **勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」**。小程序端没有 vite 代理，开发期直连 `http://localhost:8080`，不关这个校验请求会被拦掉。
- `app/src/manifest.json` 里的 `mp-weixin.appid` 目前是空的，用测试号运行即可；发布前需填入真实 AppID。

### 4. 管理后台 admin/（:5174）

```bash
cd admin
npm install
npm run dev
```

打开 http://localhost:5174 ，用 `admin` / `admin123` 登录。同样已配置 `/api` 代理到 `:8080`。

### 5. 用户端 app/ — App（Android / iOS）与 HarmonyOS

App 与鸿蒙端编译产物只是资源/应用包，**真机运行与出包需经 HBuilderX**（鸿蒙上真机还需 DevEco Studio 与签名证书）：

```bash
cd app
npm run dev:app             # Android/iOS App 资源（两端共用一套），监听改动 → app/dist/dev/app
npm run dev:app-harmony     # HarmonyOS 应用资源，监听改动 → app/dist/dev/app-harmony
```

用 HBuilderX 导入 `app/dist/dev/app`（或 `app-harmony`）目录，选择对应基座运行到设备；正式出包用 `build:*` 的产物走 HBuilderX 云打包/离线打包。

**接口地址注意**：`app/src/utils/request.js` 的 `#ifndef H5` 分支对 App/鸿蒙端生效，开发期写死 `http://localhost:8080/api`。真机上 `localhost` 指向设备自身，需改为电脑内网 IP；Android 模拟器用 `http://10.0.2.2:8080/api`（模拟器访问宿主机）。App 端没有跨域限制，无需代理。

---

## 构建产物

```bash
# 后端：可执行 jar → server/target/daodejing-server-1.0.0.jar
cd server && mvn clean package

# 用户端 H5 → app/dist/build/h5
cd app && npm run build:h5

# 用户端小程序 → app/dist/build/mp-weixin
cd app && npm run build:mp-weixin

# 用户端 App（Android/iOS 共用）→ app/dist/build/app
cd app && npm run build:app

# 用户端 HarmonyOS → app/dist/build/app-harmony
cd app && npm run build:app-harmony

# 管理后台 → admin/dist
cd admin && npm run build
```

---

## 部署

### 上线前必须修改的配置

这几项目前都是开发期的占位值，**逐项确认后再上线**：

| 位置 | 项 | 当前值 | 上线要求 |
|---|---|---|---|
| 环境变量 | `JWT_SECRET` | 内置开发密钥 | 换成足够长的随机串 |
| 环境变量 | `DB_USERNAME` / `DB_PASSWORD` | root / root | 换成生产库账号 |
| 环境变量 | `WX_MOCK` | `true` | 改为 `false`，否则任何 code 都能当 openid 登录 |
| 环境变量 | `WX_APPID` / `WX_SECRET` | 占位 | 填真实小程序凭据 |
| `app/src/utils/request.js` | `PROD_API` | `https://api.daodejing.com/api` | 改成真实接口域名 |
| `app/src/manifest.json` | `mp-weixin.appid` | 空 | 填真实 AppID |
| 数据库 | admin 账号密码 | admin / admin123 | 首次登录后立即改密 |
| `server/.../WebConfig.java` | CORS `allowedOriginPatterns("*")` | 全放开 | 由 Nginx/网关收敛到具体域名 |

> `WX_MOCK=true` 是开发期为了无 AppID 也能联调而设的：`wx-login` 会直接把传入的 code 当作 openid。**生产环境开着等同于任何人可冒充任意用户**，务必关闭。

### 1. 后端

```bash
# 生产环境变量按需覆盖，不要把密钥写进 application.yml
export DB_USERNAME=... DB_PASSWORD=...
export JWT_SECRET=...
export WX_MOCK=false WX_APPID=... WX_SECRET=...

java -jar daodejing-server-1.0.0.jar
```

首次启动会自动建库建表并写入 81 章种子数据。建议用 systemd 或容器托管，日志接到文件或日志系统。

### 2. H5 与管理后台

两者都是纯静态产物，交给 Nginx 托管，并由 Nginx 把 `/api` 反代到后端。

管理后台的 axios `baseURL` 是相对路径 `/api/admin`，**依赖同域反代**；H5 走 `PROD_API` 的绝对地址。两者部署在同一域名下最省事。

参考配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 用户端 H5
    location / {
        root /var/www/daodejing/h5;
        try_files $uri $uri/ /index.html;   # 前端路由需要回退到 index.html
    }

    # 管理后台（子路径部署时前端需相应配置 base）
    location /admin/ {
        alias /var/www/daodejing/admin/;
        try_files $uri $uri/ /admin/index.html;
    }

    # 接口反代
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> 管理后台用的是 `createWebHistory`，**必须配 `try_files` 回退**，否则刷新非根路径会 404。
> 若把管理后台部署到 `/admin/` 子路径，还需在 `admin/vite.config.js` 里设置 `base: '/admin/'` 后重新构建。

### 3. 小程序

用微信开发者工具打开 `app/dist/build/mp-weixin`，上传代码并在微信公众平台提交审核。

上传前确认：AppID 已填、`WX_MOCK` 已关、接口域名已在小程序后台配置为**合法 request 域名**（必须是备案过的 HTTPS 域名）。

---

## 接口约定（对接时注意）

统一返回体，**HTTP 状态恒为 200，业务状态在 body 里**：

```json
{ "code": 0, "msg": "ok", "data": {} }
```

- `code = 0` 成功，`data` 为业务数据
- `code = 401` 未登录/登录过期 —— 注意这不是 HTTP 401，前端拦截器要按 `body.code` 分流，按状态码判断会拿到一个「成功」的空响应
- 其余 `code` 为业务错误，`msg` 可直接提示

鉴权：登录后返回 JWT，后续请求带 `Authorization: Bearer <token>`，有效期 30 天。

## 常见问题

**后端起不来，报数据库连接失败**
确认 MySQL 已运行在 3306，并用 `DB_USERNAME` / `DB_PASSWORD` 覆盖账号密码。连接串已带 `createDatabaseIfNotExist=true`，不需要手工建库。

**小程序端所有请求都失败**
微信开发者工具里勾选「不校验合法域名…」。开发期小程序直连 `http://localhost:8080`，不关校验会被拦截。

**H5 页面正常但小程序样式不对**
两端有条件编译差异（如 H5 的 tabBar 占页面视口、小程序是原生的）。改样式时留意 `#ifdef H5` / `#ifndef H5` 分支，改完两端都要重新构建验证。

**Windows 下用 curl 测接口，中文请求返回 400**
命令行内联中文会以 GBK 发送。把 JSON 写入 UTF-8 文件后用 `--data-binary @file` 发送：

```bash
curl -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json;charset=UTF-8" \
  -H "Authorization: Bearer $TOKEN" \
  --data-binary @body.json
```

**管理后台刷新页面 404**
Nginx 缺 `try_files ... /index.html` 回退配置，见上方参考配置。

**手机/其他设备用内网 IP 访问不了 H5 与管理后台**
两个工程的 vite 配置已设 `server.host: true`（监听所有网卡），直接用 `http://<电脑内网IP>:5173` / `:5174` 访问。若仍不通，通常是 Windows 防火墙拦截（WLAN 被归为「公用网络」时必拦），放行开发端口后重试：

```bash
netsh advfirewall firewall add rule name="minilt-dev-ports" dir=in action=allow protocol=TCP localport=5173,5174,8080
```

小程序真机调试还需把 `app/src/utils/request.js` 开发环境地址 `localhost` 改为电脑内网 IP（Android 模拟器访问宿主机用 `10.0.2.2`）。
