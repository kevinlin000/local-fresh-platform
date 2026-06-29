# 菜籃日 User Web

## 專案介紹

`local-fresh-user` 是「菜籃日」的會員端前端，定位是生鮮購物網站。
核心流程包含：

- 會員登入（Email / 密碼、Google OAuth、dev 假登入）
- 商品 / 直送箱瀏覽
- 購物車與下單
- 揪團湊免運（3 人成團）

目前會員端已串起完整 B2C demo 流程，並提供可用的桌面與手機版購物體驗。

## 技術棧

- Vue 3
- Vite 5
- TypeScript
- Element Plus
- Pinia
- Vue Router 4
- axios

## 啟動指令

```bash
pnpm install
pnpm dev
pnpm build
```

預設本地網址：

```text
http://127.0.0.1:5173
```

## 環境變數

本專案不提交實際 OAuth 憑證，請在 `.env.local` 建立本地設定：

```env
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

`.env.local` 已在 `.gitignore` 中，不會進版本庫。

如果你有多組本地憑證，也可以使用：

```text
.env.local
.env.development.local
```

## Google OAuth 設定步驟

1. 到 Google Cloud Console。
2. 進入 `APIs & Services > Credentials`。
3. 建立 `OAuth 2.0 Client ID`。
4. Application type 選 `Web application`。
5. `Authorized JavaScript origins` 加入：

```text
http://localhost:5173
http://127.0.0.1:5173
```

6. `Authorized redirect URIs` 加入：

```text
http://localhost:5173/oauth/callback
http://127.0.0.1:5173/oauth/callback
```

7. 將 `client_id` 寫入前端 `.env.local`。
8. 將 `client_id / client_secret / redirect_uri` 寫入後端 `application-dev.yml`。

## Dev / Prod 模式差異

### Dev 模式

- 登入頁會顯示假登入區塊
- 可使用：
  - `試用會員 A`
  - `試用會員 B`
  - `試用會員 C`
  - 任意自訂測試 code
- 前端判斷條件為 `import.meta.env.DEV`
- 後端仍會再做一次保護，受 `localfresh.auth.mock-login-enabled` 控制

### Prod 模式

- 前端不顯示假登入按鈕與手動輸入區塊
- 後端若設定 `localfresh.auth.mock-login-enabled=false`
  - `/user/member/login` 會直接拒絕
- 正式環境保留 Email / 密碼與 Google OAuth；dev 假登入只作為本地測試入口

## 後端 Dev 注意事項

後端 `application-dev.yml` 需要關閉 Springfox / Knife4j：

```yaml
springfox:
  documentation:
    enabled: false

knife4j:
  enable: false
```

原因：

- 後端文件工具在本地開發不是會員端串接必要條件
- 關閉文件功能後，不影響本地 API 開發與前端串接

## 本地串接需求

完整前後端 dev 流程至少需要：

1. MySQL dev schema 已套用 `local-fresh-server/src/main/resources/db/migration/` 內的 Flyway migration
2. Redis 本地可用

```bash
docker run -d --name redis-dev -p 6379:6379 redis:7.2-alpine
```

3. 後端以 `dev` profile 啟動
4. 前端以 `pnpm dev` 啟動

## 已知限制

- 本地開發預設可使用 demo payment gateway；部署環境已具備 ECPay sandbox checkout / callback 驗證路徑
- 沒有會員中心、收藏、評論
- Google OAuth 的完整人工授權流程需依賴本地合法 `client_id / client_secret`
- 揪團頁採輪詢更新，不做 WebSocket 前端推播
