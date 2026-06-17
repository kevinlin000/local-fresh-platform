# 面試展示指南

這份文件用來把「菜籃日 Cailán Day」整理成可面試、可 demo、可被追問的作品敘事。重點不是背稿，而是讓每個技術選擇都能連回業務需求與工程取捨。

## 30 秒專案介紹

菜籃日是一個在地小農生鮮配送平台，包含會員端購物流程與管理端營運後台。會員可以瀏覽商品與直送箱、加入購物車、管理配送地址、下單、查詢訂單，也可以發起 3 人揪團免運。後端使用 Java 17、Spring Boot 3.5、MySQL、Redis、Redisson、Flyway 與 JWT，重點展示完整業務閉環、交易一致性、Redis 分散式鎖、測試證據與 AWS 部署能力。

## 5 分鐘 Demo 路線

1. 會員端首頁
   - 展示今日市場、分類、搜尋、商品卡與 3 人揪團免運入口。
   - 要講的點：前端不是 landing page，而是購物任務入口。

2. 商品詳情與購物車
   - 進入商品詳情，展示「加入購物車」與「立即揪團」兩條路徑。
   - 加入購物車後看金額摘要與數量調整。
   - 要講的點：一般下單與揪團下單是不同業務路徑。

3. 揪團詳情
   - 打開 seed 揪團 `GB-DEMO-ACTIVE`。
   - 展示倒數、目前人數、還差人數、參與者與分享連結。
   - 要講的點：每位參與者都有自己的預訂單，不共用同一張訂單。

4. 我的訂單
   - 展示一般訂單與揪團訂單的狀態追蹤。
   - 要講的點：訂單狀態覆蓋待付款、待確認、已確認、配送中、已完成、已取消與揪團中。

5. 管理端工作台
   - 展示營業額、訂單狀態、低庫存與待處理事項。
   - 要講的點：管理端是後端能力的驗證入口，不只是會員端 demo。

6. 管理端商品與訂單
   - 商品頁展示上架品質、低庫存、庫存調整與庫存紀錄。
   - 訂單頁展示確認、婉拒、取消、配送與完成。
   - 要講的點：履約操作會回到後端狀態流轉與資料一致性。

## 後端亮點講法

### 揪團併發控制

揪團最容易出問題的是多人同時加入同一團，可能造成超賣或重複加入。這個專案用 Redisson `RLock` 以 `groupNo` 做細粒度鎖，將檢查狀態、建立預訂單、寫入 participant、更新人數與成團判斷放在同一個交易邊界內。這不是只寫流程，而是有 Redis/Testcontainers 與 JMeter 壓測證據支撐。

### 預訂單模型

揪團不直接重用一般訂單流程，而是先建立 `PENDING_GROUP` 預訂單。成團後批次轉成 `TO_BE_CONFIRMED`，失敗則轉成 `CANCELLED`。這樣每個參與者都有自己的地址、金額與訂單快照，也讓一般 checkout 不被揪團邏輯污染。

### Flyway 與 seed story

資料庫 schema 與 demo data 由 Flyway migration 管理。全新資料庫啟動時會套用商品、會員、地址、訂單與揪團 seed，讓 reviewer 可以直接走完整 demo，不需要手動建資料。

### JWT 與 ThreadLocal

後端用 JWT 驗證會員與管理端身份，攔截器解析 token 後把使用者上下文放到 ThreadLocal。這讓 service 不用一直傳 memberId，但也有 servlet thread pool 重用的風險，所以專案有針對 ThreadLocal cleanup 補測試。

### Redis 職責分離

RedisTemplate 用在快取與一般 KV，RedissonClient 用在分散式鎖。這個分離讓程式更容易解釋，也避免不同 Redis client 職責混雜。

## 全端亮點講法

- 會員端與管理端都已是 Vue 3 + Vite + TypeScript 路線。
- 會員端不是純展示頁，而是實際購物、地址、下單、揪團、訂單查詢流程。
- 管理端不是空殼後台，能查商品、調庫存、看訂單、執行履約操作。
- Vite build 已做基本 bundle hygiene，避免整包 Element Plus 造成過大的單一入口。
- 最新 UI pass 已把首頁從模板式 hero 改成購物任務入口，管理端則改成更像營運工具的資訊密度。

## 面試常見追問

### Q: 這是不是課程專案改的？

可以誠實回答：原始基底來自課程專案，但我做了大量重構與重新定位，包括改成在地小農生鮮平台、移除舊平台名詞、部署從原課程環境改到 AWS、地圖改 Google Maps、登入改 Google OAuth + mock login、增加揪團模型、Redis 分散式鎖、Flyway migration、測試與壓測證據，以及 Vue 3 管理端。

### Q: 你最想讓我看哪段程式？

優先看揪團 service、group-buy migration、Redisson/Testcontainers 測試、訂單狀態流轉相關 service，以及 JWT interceptor/ThreadLocal cleanup 測試。這些比單純 CRUD 更能展示後端基本功。

### Q: 為什麼沒有真的接金流？

目前支付是 mock，因為這個作品的核心展示點是後端交易一致性、揪團流程、Redis lock、測試與部署。真實金流會是下一階段，應該包含 idempotency key、Webhook 簽章驗證、回呼重試與 reconciliation job，而不是只把 SDK 接進來。

### Q: 如果流量更大會怎麼改？

先不急著拆微服務。比較務實的下一步是：

- 將 group-buy join path 補更多觀測指標與慢查詢監控。
- 對商品列表與熱門直送箱做更明確的 cache invalidation。
- 把付款與庫存異動做 idempotency 防重。
- 管理端操作加 audit log。
- 只有在明確瓶頸出現後，再討論服務拆分。

## 可以主動承認的限制

- 支付仍是 mock，未接真實 TapPay / 信用卡流程。
- 管理端已有產品級基礎，但還沒有完整自動化視覺回歸。
- 部署是作品級單機 EC2 + Docker MySQL/Redis，不是高可用 production 架構。
- 前端 UI 已 polish，但主要價值仍是後端流程與工程證據。

## 建議展示順序

1. 先用 README 的截圖讓面試官快速理解系統。
2. 再開本地或雲端 demo 走一段會員流程。
3. 接著切到管理端，展示訂單/商品如何被營運。
4. 最後進程式碼，講揪團 service、Redisson lock、transaction boundary 和測試。

這個順序能避免一開始就陷入 UI 細節，也能讓 Java 後端面試官看到你真正想展示的工程能力。
