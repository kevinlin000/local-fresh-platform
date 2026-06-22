# Known Issues / Follow-up Notes

這份文件記錄目前仍刻意保留或需要後續強化的限制。它的目的不是把專案包裝成 production SaaS，而是讓面試官看到哪些地方已修、哪些地方有清楚邊界。

## 目前刻意保留的限制

- 本機 demo 仍可回到 demo gateway；EC2 demo runtime 已可透過 SSM 切到 ECPay sandbox provider，並已通過 public `/actuator/info`、`/payment/callback` invalid-signature preflight、effective `PAYMENT_PROVIDER=ecpay`、Playwright 綠界 stage checkout、sandbox OTP 付款成功、ReturnURL HTTP 200、訂單轉已付款與 `payment_event` 寫入 `CALLBACK_SUCCEEDED` 驗證。後端已補重複 callback 測試、待對帳候選查詢、ECPay 查詢結果 parser 與 reconciliation job；尚未完成的是正式監控告警與外部查詢排程的長時間運行證據。
- 管理端與會員端已有產品級 demo polish，但目前沒有自動化視覺回歸測試；最新畫面證據以 Playwright 手動截圖保存在 `docs/screenshots/`。
- Demo 部署是求職作品級 AWS 架構：Vue 靜態站在 S3 + CloudFront，Spring Boot API 由 EC2 上的 Nginx HTTPS 反向代理轉到本機 Spring Boot，資料層為 MySQL / Redis；它不是多區高可用 production 架構。
- 開發環境預設關閉 Google Maps 配送範圍檢查，避免本地 demo 被第三方 API key 或地址資料阻塞；正式環境需以環境變數打開並設定有效 API key。

## 測試環境與生產環境 schema 約束不完全等價

上游 schema 中的 `orders` 表在 production MySQL 為 `NOT NULL DEFAULT 1`
的欄位（`pay_method`, `delivery_status`），但測試環境 `H2 schema-test.sql`
原本未強制這些約束。這導致 `buildPreOrder` 等方法在不設定這些欄位時，
測試環境通過（H2 接受 `NULL`）但 dev/prod MySQL 靠 default 靜默兜底。

本次修正（commit `b49a117` 與本次 `stage4-5` fix）：
- 在 `buildPreOrder` 顯式設定 `payMethod=1` / `deliveryStatus=1`
- `schema-test.sql` 對齊 `NOT NULL DEFAULT 1`
- 補 3 個 Issue 測試 fixture

後續注意：
- 變更 `orders` 欄位時，需同步檢查 production migration 與 `schema-test.sql`
- 新增訂單狀態或支付欄位時，需補對應的 Service 測試與 migration 驗證
