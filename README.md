# 在地鮮選 Local Fresh Platform

> 結合在地小農生鮮直送與揪團湊免運的 B2C 電商平台

![Java 17](https://img.shields.io/badge/Java-17-3A7D44?style=flat-square)
![Spring Boot 2.7](https://img.shields.io/badge/Spring%20Boot-2.7-6DB33F?style=flat-square)
![Vue 3](https://img.shields.io/badge/Vue-3-42B883?style=flat-square)
![License MIT](https://img.shields.io/badge/License-MIT-4E9F3D?style=flat-square)

**Demo URL**

[https://d3hqnux25iirgl.cloudfront.net](https://d3hqnux25iirgl.cloudfront.net)

> 用戶端 demo 開放使用,可透過開發模式快捷登入快速試玩,或使用 Google 帳號登入體驗完整 OAuth 流程。
> 後端 API 入口:`https://localfresh-demo.duckdns.org`
> 部署架構:Vue 3 用戶端託管於 AWS S3 + CloudFront(HTTPS),Spring Boot API 部署於 AWS EC2 (Nginx 反向代理 + Let's Encrypt)。

## Demo 流程截圖

實際操作流程,從首頁瀏覽到下單、發起揪團、查看訂單。

### 1. 首頁 — 在地小農直送

直接展示揪團湊免運核心訴求,商品依葉菜類 / 根莖類等 7 大分類陳列。

![首頁](docs/screenshots/01-home.png)

### 2. 商品列表 — 七大分類切換

點選分類即時切換商品,每個商品搭配產地描述與台幣定價。

![商品列表](docs/screenshots/02-product-list.png)

### 3. 商品詳情 — 雙路徑下單

可選「加入購物車」累積後一次結算,或「立即揪團」直接發起 3 人成團免運。

![商品詳情](docs/screenshots/03-product-detail.png)

### 4. 揪團詳情 — 即時進度與一人取消

倒數計時 + 進度顯示 + 分享連結;發起人在無他人加入時可取消揪團,預訂單同步取消。

![揪團詳情](docs/screenshots/04-group-buy.png)

### 5. 購物車 — 即時計算總額

商品數量調整即時更新總計,清空購物車一鍵歸零。

![購物車](docs/screenshots/05-cart.png)

### 6. 我的訂單 — 多狀態追蹤

列出各狀態訂單(已完成 / 派送中 / 已接單 / 待付款 / 已取消),含商品明細與取消備註。

![我的訂單](docs/screenshots/06-orders.png)

## 專案簡介

台灣生鮮電商常見兩個痛點：第一，運費門檻高，少量購買時消費者容易卻步；第二，平台多半只做商品陳列與配送，缺少能提升轉換率與社群擴散的購物機制。在地鮮選的設計目標，就是把「在地小農直送」與「揪團湊免運」結合成一個完整的 B2C 訂購流程。消費者可以瀏覽單品與直送箱、加入購物車、建立收貨地址並完成下單；若希望降低運費，也可以發起揪團，透過分享連結邀請其他會員加入，達到 3 人成團後即免運。平台後端同時提供商品、訂單、店鋪狀態與營運管理能力，前後端整體圍繞「基本功扎實、流程完整、可實際部署」作為實作目標。

## 技術架構

```text
┌───────────────────────────────┐
│        User Web (Vue 3)       │
│  商品瀏覽 / 購物車 / 揪團 / OAuth │
└──────────────┬────────────────┘
               │ HTTP / JWT
               ▼
┌──────────────────────────────────────────────┐
│         Spring Boot 2.7 Backend API         │
│  Member / Product / Cart / Order / GroupBuy │
│  Google OAuth / JWT / Cache / Scheduler     │
└───────┬──────────────────┬──────────────────┘
        │                  │
        ▼                  ▼
┌───────────────┐   ┌──────────────────┐
│   MySQL 8     │   │   Redis 7        │
│ 訂單 / 商品 /   │   │ 快取 / 店鋪狀態 /  │
│ 揪團 / 會員資料 │   │ 分散式鎖協作       │
└───────────────┘   └─────────┬────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │   Redisson       │
                     │ Group Buy Lock   │
                     └─────────┬────────┘
                               │
                               ▼
                     ┌──────────────────┐
                     │ 定時任務 / WS 通知 │
                     │ 過期失敗 / 成團通知 │
                     └──────────────────┘

外部服務：
- Google OAuth 2.0：會員登入
- Google Maps API：配送範圍計算
- AWS（規劃中）：EC2 / RDS / S3 / CloudFront
```

### 技術棧

| 區域 | 技術 |
|---|---|
| 後端 | Java 17、Spring Boot 2.7.3、MyBatis、PageHelper、JWT、Druid |
| 前端 | Vue 3、Vite 5、TypeScript、Pinia、Vue Router 4、Element Plus |
| 基礎設施 | MySQL 8、Redis 7、Redisson、Testcontainers、Docker |
| 第三方服務 | Google OAuth 2.0、Google Maps API、AWS EC2 / RDS / S3 / CloudFront（規劃中） |

## 核心功能

- 單品與直送箱瀏覽：依分類查看蔬果、肉品、海鮮等單品，以及主題直送箱。
- 購物車與下單流程：支援加入購物車、數量調整、地址選擇、備註填寫與歷史訂單查詢。
- 揪團湊免運：會員可建立揪團、分享連結邀請他人加入，3 人成團後轉為正式訂單。
- 雙軌登入機制：前端支援 Google OAuth 2.0，開發環境保留 mock login 方便測試與 demo。
- 訂單狀態流轉：涵蓋待付款、待接單、派送中、已完成、已取消，以及揪團中的預訂單狀態。
- 店鋪與營運管理：管理端可維護商品、分類、訂單與營業狀態。
- 快取與排程協作：以 Redis 快取熱門查詢、以排程處理過期揪團與退款模擬流程。

## 技術亮點

### 1. 揪團模組的併發控制

揪團的關鍵風險在於多人同時加入時，不能超過成團人數，也不能讓同一位會員重複加入。這個專案使用 Redisson 的 `RLock` 對每個 `groupNo` 建立細粒度鎖，採用 `tryLock(3, 5, TimeUnit.SECONDS)`，讓請求在 3 秒內嘗試取得鎖，並把鎖持有時間限制在 5 秒內。實作上將「檢查狀態、建立預訂單、寫入 participant、更新 currentCount、必要時觸發成團」包在同一段交易內，確保資料一致性；而 WebSocket 通知則刻意放在 transaction commit 之後，避免商家收到通知時資料尚未落庫。這組邏輯另外以 100-thread 併發測試驗證，確認最終人數不會超過 required count。

### 2. 預訂單與正式訂單分離的揪團建模

揪團期間的訂單並不是一般下單流程，而是先建立 `PENDING_GROUP` 狀態的預訂單。這樣做的好處是可以避免在尚未成團前就扣庫存、清購物車或進行完整配送檢查，讓一般下單與揪團下單的業務邊界保持清楚。成團時，系統再批次把所有 participant 對應訂單轉成 `TO_BE_CONFIRMED`；若過期失敗，則統一轉成 `CANCELLED` 並記錄 mock 退款 log。這種設計讓原有 `order` 模組邏輯大致維持穩定，只在狀態流轉層擴充揪團語意。

### 3. Testcontainers 驗證 Redis 鎖而非用 mock 帶過

揪團併發控制若只用 `MockBean RedissonClient` 驗證流程，說服力不足，因為真正的風險發生在多執行緒與真實 Redis 鎖行為。這個專案的整合測試採用 Testcontainers 啟動 Redis container，並以 `@DynamicPropertySource` 把 host / port 動態注入測試環境，讓 `GroupBuyRedisIntegrationTest` 真正對 Redisson 做 100-thread 並發驗證。這樣的取捨比 pure mock 更重，但能換來更可信的測試結論；對展示「我知道哪裡該用真實整合測試」這件事，比單純追求測試執行速度更有價值。

### 4. JWT 驗證與 ThreadLocal 請求隔離

前後端 API 使用 JWT 作為會員與管理端身份驗證，並透過攔截器在請求進入時解析 token，將當前使用者資訊放入 ThreadLocal，供後續 service / mapper 取得。這種作法的優點是 controller 不需要反覆傳遞 memberId，邏輯較乾淨；但同時也要求在請求結束時明確清理 ThreadLocal，否則在 servlet thread pool 重用情境下，容易出現跨請求資料污染。專案中已針對這個風險補上回歸測試，確保登入上下文不會殘留到下一個請求。

### 5. Google OAuth 2.0 採授權碼流程而非 Implicit Flow

會員登入採 Google OAuth 2.0 Authorization Code Flow，而不是已逐漸被淘汰的 Implicit Flow。前端只負責導向 Google 授權頁並接收 callback code，真正與 Google token endpoint 溝通、驗證 `id_token`、檢查 `aud / exp` 等工作放在後端進行，降低憑證暴露風險。服務層另外抽出 `GoogleOAuthClient` 作為外部依賴封裝，使測試可以直接 mock `GoogleProfile`，專注驗證 account merge、JWT 簽發與 mock login 開關，而不是把測試耦合到 Google SDK 細節。

### 6. Redisson 與 Spring Data Redis 職責分離

專案中 Redis 有兩種用途：一種是一般 KV / cache，例如商品列表、店鋪營業狀態；另一種是揪團需要的分散式鎖。如果所有 Redis 存取都混用同一套 client，實務上容易出現相容性與責任界線不清的問題。這個專案最後採取的策略是：`RedissonClient` 專責分散式鎖與協調，`RedisTemplate` 則回到 Spring Boot 2.7 預設的 Lettuce 路徑處理快取與一般資料存取。這個分離避免了 `Tuple` 類別相容性問題，也讓後續維護者更容易理解「哪種場景該用哪種 Redis API」。

### 7. 可部署導向的全流程設計

這個專案雖然是求職作品，但實作方式不是只做出 API 或畫面，而是完整串成「可啟動、可測試、可部署規劃」的系統。從 migration 版本化、環境變數管理、dev/test profile 分流、Google OAuth 設定隔離，到前端 Vite proxy 與後端 CORS 協作，都是以未來可實際部署到 AWS 為前提在設計。AWS 相關章節目前先保留為規劃，但資料庫、Redis、靜態前端與 API server 的切分方式已經能自然對應到 EC2、RDS、S3、CloudFront 的部署模型。

## 設計決策 Q&A

### Q1. 為什麼 JWT TTL 設為 2 小時？

2 小時 TTL 是在安全性與 demo / 一般操作體驗之間取的平衡。若時間太短，使用者在瀏覽商品、加入購物車、下單或加入揪團的過程中容易頻繁失效；若時間太長，token 外洩後的風險窗口會變大。這個專案的使用情境不是長時間編輯型系統，而是以購買流程為主，因此 2 小時足夠涵蓋一次完整購買與分享揪團的操作，同時保留合理的風險邊界。

### Q2. 為什麼揪團鎖選 Redisson，不自己用 `SETNX` 實作？

自己用 `SETNX` 實作分散式鎖不是做不到，但要正確處理過期、可重入、釋放鎖時的擁有者判斷與例外情境，代價不低，而且這個專案不是要展示「我能手寫完整鎖框架」。Redisson 已經把這些通用細節處理好，讓程式可以把重點放回業務邏輯：怎麼判斷成團、怎麼做交易邊界、怎麼避免超賣與重複加入。對這個專案來說，選擇成熟元件比重造輪子更合理。

### Q3. 為什麼揪團過期用定時任務，不用 Redis TTL 事件？

Redis key 過期事件看起來很直覺，但在真實系統裡，若採用這條路徑，需啟用 `notify-keyspace-events` 配置，且 Redis 過期事件本身屬非強保證投遞，在資料庫狀態流轉與退款記錄等需要強一致的場景可靠性不足。這個專案用每分鐘掃描一次的定時任務處理過期揪團，雖然不是毫秒級即時，但對「24 小時揪團」這種業務來說完全足夠，而且程式結構更容易理解與測試。這是一個刻意選擇簡單、可維護方案的例子。

### Q4. 為什麼要用 Testcontainers，而不是全都用 mock？

不是所有功能都需要 Testcontainers，但像揪團併發控制這種問題，如果只 mock 掉 Redis 鎖，本質上就跳過了最重要的風險區域。Testcontainers 的價值在於，它讓測試可以在本地與 CI 以接近真實環境的方式啟動 Redis，驗證 Redisson 的實際鎖行為與多執行緒競爭結果。對比 mock，它執行成本較高，但換來的是更強的可信度；這對面試展示來說是值得的。

### Q5. 為什麼保留管理端 Vue 2，卻新建用戶端 Vue 3？

管理端原本已有一套 Vue 2 + vue-cli 的初版實作，若整包重寫成 Vue 3，改動量大、風險高，而且對這個專案的加分有限。相反地，用戶端是這次作品的主要展示面，直接用 Vue 3 + Vite + Pinia + Element Plus 重建，能更直接反映目前主流前端工程實務。這個決策本質上是在控制重構成本，把時間投資在真正會被看到、也更能展示判斷力的部分。

### Q6. 為什麼 repo 內同時保留 mock login 與 Google OAuth？

開發與 demo 階段若完全依賴 Google OAuth，會讓本地測試高度綁定第三方憑證與人工授權流程，降低開發效率；但如果只做 mock login，又會讓整個登入設計顯得過於簡化。這個專案採雙軌策略：正式流程走 Google OAuth 2.0，開發環境則透過 `mock-login-enabled` 開關保留 mock login，前端也只在 dev 模式顯示快捷登入區塊。這樣既保留真實世界的登入設計，也兼顧開發效率。

## 系統需求

- Java 17+
- Node.js 18+ 與 pnpm
- MySQL 8.x
- Redis 7.x（可使用 Docker 啟動）
- Google OAuth Client（前端與後端需自行申請本地開發憑證）

## 快速開始

### 1. Clone 專案

```bash
git clone https://github.com/kevinlin000/local-fresh-platform.git
cd local-fresh-platform
```

### 2. 準備後端 `application-dev.yml`

請在以下路徑建立本地開發設定：

```text
backend-environment/sky-take-out/sky-server/src/main/resources/application-dev.yml
```

至少需要補齊以下設定：

```yaml
sky:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: sky_take_out
    username: your_db_user
    password: your_db_password

  redis:
    host: localhost
    port: 6379
    database: 0

  jwt:
    admin-secret-key: your-admin-secret
    user-secret-key: your-user-secret

  aws:
    s3:
      region: your-aws-region
      access-key-id: your-access-key
      secret-access-key: your-secret-key
      bucket-name: your-bucket

  google:
    api-key: your-google-maps-api-key

  oauth:
    google:
      client-id: your-google-oauth-client-id
      client-secret: your-google-oauth-client-secret
      redirect-uri: http://127.0.0.1:5173/oauth/callback

  auth:
    mock-login-enabled: true

  shop:
    address: 台北市信義區市府路1號

springfox:
  documentation:
    enabled: false

knife4j:
  enable: false
```

### 3. 執行資料庫 migration

依序執行：

```text
backend-environment/sky-take-out/sky-server/src/main/resources/db/migration/
```

順序如下：

1. `V2__rename_to_grocery.sql`
2. `V2_1__align_naming.sql`
3. `V3__add_groupbuy_tables.sql`
4. `V4__add_oauth_columns.sql`

### 4. 啟動後端

```bash
cd backend-environment/sky-take-out
mvn -pl sky-server spring-boot:run
```

### 5. 準備前端 `.env.local`

請建立：

```text
frontend-environment/sky-user-vue3/.env.local
```

內容範例：

```env
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

### 6. 啟動前端

```bash
cd frontend-environment/sky-user-vue3
pnpm install
pnpm dev
```

### 7. Google OAuth Client 申請步驟

完整的前端 OAuth 設定說明請參考：

- [frontend-environment/sky-user-vue3/README.md](frontend-environment/sky-user-vue3/README.md)

## 部署

目前規劃的部署架構如下：

- **EC2**：部署 Spring Boot API
- **RDS (MySQL)**：儲存商品、會員、訂單與揪團資料
- **S3**：承載前端靜態檔案
- **CloudFront**：對外提供 CDN 與 HTTPS 存取

**Demo URL**  
部署中，即將公開

> 部署步驟與架構圖會在部署完成後補齊。

## 已知限制

- 支付流程仍為 mock，未串接真實金流
- 管理端以前期初版實作為基礎，目前部署與展示重點放在用戶端
- 用戶端目前僅提供桌面版體驗，未做 RWD
- 測試環境與生產環境的部分 schema 約束仍存在差異

更多細節請參考：
- [docs/known-issues.md](docs/known-issues.md)

## 相關文件

- [docs/known-issues.md](docs/known-issues.md)
- [frontend-environment/sky-user-vue3/README.md](frontend-environment/sky-user-vue3/README.md)
- `docs/architecture.md`（規劃中）

## License

This project is licensed under the MIT License. You may use, modify, and distribute this project with proper attribution. See the repository license section or future `LICENSE` file updates for details.
