# 在地鮮選 User Web

## 啟動指令

```bash
pnpm install
pnpm dev
pnpm build
```

預設開發網址：

```text
http://127.0.0.1:5173
```

## 環境變數

本專案不提交實際 OAuth 憑證，請在 `.env.local` 建立本地設定：

```env
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

`.env.local` 已在 `.gitignore` 中，不會進版本庫。

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

## Dev 模式假登入

開發模式下，登入頁會顯示假登入區塊：

- `測試會員 A`
- `測試會員 B`
- `測試會員 C`
- 自由輸入任意測試 code

前端判斷條件是 `import.meta.env.DEV`，因此：

- `pnpm dev`：顯示假登入區塊
- `pnpm build` 的 production bundle：不顯示假登入區塊

後端仍會再做一次保護，受 `sky.auth.mock-login-enabled` 控制。

## Prod 模式差異

- 前端不顯示假登入按鈕與手動輸入區塊
- 後端若將 `sky.auth.mock-login-enabled=false`，`/user/member/login` 會直接拒絕
- 正式環境應只保留 Google OAuth 流程

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

- 目前專案使用 `Spring Boot 2.7.3 + Knife4j 3.0.2`
- dev 啟動時會在 `documentationPluginsBootstrapper` 觸發 Springfox NPE
- 關閉文件功能後，不影響本地 API 開發與前端串接

## 本地串接需求

完整前後端 dev 流程至少需要：

1. MySQL dev schema 已套用 `V2 / V2_1 / V3 / V4` migration
2. Redis 本地可用（例如 `docker run -d --name redis-dev -p 6379:6379 redis:7.2-alpine`）
3. 後端以 `dev` profile 啟動
4. 前端以 `pnpm dev` 啟動
