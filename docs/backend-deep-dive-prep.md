# Backend Deep-Dive Prep Notes

這份文件是 Kevin 自己準備 Java 後端 / 全端面試時用的深挖筆記，不是寫給面試官看的作品介紹。

使用方式：

- 面試前用它快速複習「哪些點可以講深」。
- 被問到 correctness、idempotency、concurrency、observability、scalability 時，用這份文件整理回答順序。
- 不要逐字照念，也不要在 README 主頁刻意推銷「大廠標準」。公開作品頁保持自然，這份文件只作為你的答辯準備。

## 你目前最強的後端敘事

這個專案最適合定位成：

> Java backend-first fullstack portfolio。核心價值是完整交易流程、訂單狀態、付款 callback、庫存一致性、揪團併發、測試證據、最小可觀測性與 AWS 作品級部署。

不要說這是 production 100% 電商系統。比較成熟的說法是：

> 目前是單體 Spring Boot 系統，已把高風險交易邊界做出可重跑證據；ECPay sandbox provider 已可在 EC2 透過 SSM 切換，下一階段會優先補真瀏覽器 checkout、監控告警與更正式的容量分析。

## 面試時圖怎麼用

不要主動把所有圖一次打開。比較自然的用法：

1. 先用 README 截圖講會員購物、揪團、管理端履約的完整流程。
2. 被問「資料怎麼設計」時，打開 `docs/architecture.md` 的揪團核心 ER，說明 `group_buy`、`group_buy_participant`、`orders`、`order_detail` 的責任分工。
3. 被問「這會不會只是 CRUD」時，打開訂單狀態機，說明合法狀態轉移集中在 `OrderStatusTransitionPolicy` 和 service tests。
4. 被問「付款重複 callback 怎麼辦」時，打開付款 callback sequence，說明 guarded update、`payment_event` 與 duplicate/rejected event。
5. 被問「出事怎麼追」時，再補付款、庫存與管理留痕模型，講 `payment_event`、`product_inventory_log`、`admin_operation_log`。

重點是把圖當作輔助證據，不是讓面試變成逐表背誦。

## 面試深挖對照表

| 面試官追問 | 你要回答的方向 | 可展示證據 |
|---|---|---|
| ER model 怎麼設計？ | 交易主線用 `orders/order_detail`，揪團用 `group_buy/group_buy_participant` 連到每人的 pre-order；付款、庫存、管理操作用 append-style evidence table 留痕。 | `docs/architecture.md` ER diagrams、Flyway migrations |
| 為什麼揪團不直接共用一張訂單？ | 每位 participant 有自己的 pre-order、地址、金額與狀態，成團後批次轉正式履約。 | `group_buy_participant.pre_order_id`、`GroupBuyServiceImpl` |
| callback 重複或 race 怎麼辦？ | 付款成功用 conditional update，重複 callback 記錄為 ignored。 | `OrderPaymentServiceImpl`、`OrderPaymentServiceImplTest` |
| 100 人同時 join 會不會超賣？ | Redisson lock + transaction + DB unique constraint，並有 Redis/Testcontainers 和 JMeter 證據。 | `GroupBuyRedisIntegrationTest`、`docs/perf/README.md` |
| 取消重試會不會重複退款或還庫存？ | 先用 order status 與 `ORDER_CANCEL_RESTORE` inventory log 做 service precheck，再用 `product_inventory_log.idempotency_key` unique constraint 當 DB 最後防線。 | `OrderCancellationServiceImpl`、`ProductInventoryOrderTest` |
| 系統跑起來後怎麼看異常？ | Actuator + business counters，可看付款 callback、取消防重、揪團狀態轉換。 | `docs/observability.md` |
| 這是不是只是 CRUD？ | 不是只做 CRUD，重點在狀態機、交易邊界、分散式鎖、callback idempotency、庫存與 audit log。 | `docs/testing.md`、service tests |

## 技術證據地圖

這張表是給你面試前快速定位用的。不要把它整張貼給面試官；它的用途是讓你被追問時，能把「問題、設計、程式碼、測試、限制」連成一條清楚的回答。

| 追問主題 | 風險 / 問題 | 目前設計 | 程式碼入口 | 測試 / 證據 | 可承認限制 |
|---|---|---|---|---|---|
| 訂單生命週期 | 非法狀態轉移、付款和履約狀態混在一起。 | 用 `OrderStatusTransitionPolicy` 集中合法轉移，付款狀態用 `pay_status` 與 payment event 補充，不把取消、退款、配送混成單一欄位。 | `OrderStatusTransitionPolicy`、`OrderServiceImpl`、`OrderFulfillmentServiceImpl`、`OrderCancellationServiceImpl`、`OrderPaymentServiceImpl` | `OrderStatusTransitionPolicyTest`、`OrderServiceImplTest`、`OrderFulfillmentServiceImplTest`、`OrderCancellationServiceImplTest`、`OrderPaymentServiceImplTest` | 目前是 service-level 狀態機；若未來有多服務或非 Java worker 寫 DB，關鍵轉移要升級成 DB conditional update / outbox / 更完整事件模型。 |
| 付款 callback 冪等 | provider 重複 callback、延遲 callback、失敗交易、偽造 payload 或不存在訂單。 | `PaymentGateway` 抽象 provider，callback boundary 先驗證 payload，再由 `OrderPaymentServiceImpl` 做 guarded update、寫 `payment_event`、記 metrics。 | `PaymentCallbackController`、`OrderPaymentServiceImpl`、`PaymentGateway`、`DemoPaymentGateway`、`EcpayPaymentGateway`、`PaymentEventMapper` | `PaymentCallbackControllerTest`、`PaymentCallbackControllerEcpayContractTest`、`OrderPaymentServiceImplTest`、`DemoPaymentGatewayTest`、`EcpayPaymentGatewayTest`、`PaymentEventApiTest`、`docs/screenshots/10-admin-payment-events.png` | 已有 sandbox 成功回流與重複 callback 測試；正式 production 還需要 callback rejected / duplicate 告警與更長時間運行證據。 |
| 付款待對帳 | 已建立付款請求，但 provider 沒有送 terminal callback。 | `payment_event` 保留 request / callback evidence，admin 可查 pending requests；provider-query reconciliation job 可查外部狀態並套用成功、失敗、未知與錯誤路徑。 | `PaymentReconciliationServiceImpl`、`PaymentReconciliationTask`、`EcpayPaymentGateway.queryPaymentStatus`、`PaymentEventController` | `PaymentReconciliationServiceImplTest`、`EcpayPaymentGatewayTest`、`BusinessMetricsServiceImplTest`、`docs/testing.md` | 排程預設關閉；目前重點是功能與測試證據，下一步才是 production alert threshold、dashboard 與長時間排程觀察。 |
| 揪團併發 | 同一團多人同時加入造成超賣、重複加入或 `current_count` 與 participant rows 不一致。 | Redisson lock 以 `groupNo` 做細粒度鎖，transaction 內完成檢查、建立預訂單、寫 participant、更新人數與成團判斷；DB unique key 當最後防線。 | `GroupBuyServiceImpl.joinGroupBuy`、`GroupBuyParticipantMapper`、`GroupBuyTask` | `GroupBuyRedisIntegrationTest`、`GroupBuyServiceTest`、`GroupBuyExpirationServiceTest`、`docs/perf/README.md` | 目前是單一熱團 100-user correctness + load evidence；還不是完整容量模型，下一步要補多團矩陣、lock wait time、slow query。 |
| 庫存與取消防重 | 下單扣庫存、取消還庫存、重試取消或直送箱組成商品回補可能不一致。 | 下單用 conditional decrease 防負庫存；取消用 order status precheck、`ORDER_CANCEL_RESTORE` log 與 `product_inventory_log.idempotency_key` unique key 防重。 | `InventoryServiceImpl`、`OrderSubmissionServiceImpl`、`OrderCancellationServiceImpl`、`ProductServiceImpl`、`ProductInventoryLogMapper` | `ProductInventoryOrderTest`、`OrderCancellationServiceImplTest`、`ProductServiceImplTest`、`WorkspaceLowStockTest` | 目前不是 event sourcing；若未來有 async retry 或多服務寫庫存，應把庫存異動統一成 command/event 並補 outbox。 |
| 可觀測性與 audit | 出事後只看 log，不容易知道 payment、取消、揪團或管理操作發生什麼。 | Actuator health / info / metrics 作為基本觀測；business counters 記高風險流程；`payment_event`、`product_inventory_log`、`admin_operation_log` 留可查證據。 | `BusinessMetricsServiceImpl`、`PaymentEventServiceImpl`、`AdminOperationLogServiceImpl`、`DeploymentInfoContributor` | `BusinessMetricsServiceImplTest`、`PaymentEventApiTest`、`AdminOperationLogServiceImplTest`、`AdminOperationLogApiTest`、`PrometheusEndpointTest`、`docs/observability.md` | 還不是完整 SRE stack；下一步是 Prometheus scrape、Grafana dashboard、alert rules、structured JSON log 與 trace id。 |
| Security boundaries | IDOR、ThreadLocal 污染、OAuth token 誤用、mock login 被誤開。 | member/admin JWT context 分離；ThreadLocal cleanup 有測試；跨會員訂單與地址存取有 IDOR 測試；Google OAuth 採 authorization code flow；mock login 有環境開關。 | JWT interceptors、`BaseContext`、`GoogleOAuthClientImpl`、member/admin controllers | `IssueS2IdorOrderTest`、`IssueS6IdorShippingAddressTest`、`IssueS4ThreadLocalTest`、`MemberOAuthLoginTest`、`GoogleOAuthClientImplTest` | 仍需 production CORS allowlist、Swagger 關閉策略、rate limiting、admin RBAC、secret rotation、IAM least privilege。 |

## 1. 訂單生命週期

### 你要講的重點

電商訂單不是只有 CRUD，真正重要的是狀態轉移是否一致：

- 待付款只能透過付款成功進入待確認。
- 管理端只能確認待確認訂單。
- 已確認才能配送，配送中才能完成。
- 取消和退款不是混成同一個概念：訂單取消是 `status=CANCELLED`，退款用 `pay_status=REFUND` 和 payment/refund evidence 表示。

### 對應程式碼

- `OrderStatusTransitionPolicy`
- `OrderServiceImpl`
- `OrderFulfillmentServiceImpl`
- `OrderCancellationServiceImpl`
- `OrderPaymentServiceImpl`

### 對應測試

- `OrderStatusTransitionPolicyTest`
- `OrderServiceImplTest`
- `OrderFulfillmentServiceImplTest`
- `OrderCancellationServiceImplTest`
- `OrderPaymentServiceImplTest`

### 不要吹過頭

目前是 service-level 狀態機，不是 DB-level 狀態機。若未來有多個服務或非 Java worker 直接寫 DB，應該把關鍵轉移改成更嚴格的 DB conditional update、outbox 或更完整事件模型。

## 2. 付款 Callback 與 Idempotency

### 你要講的重點

後端不能相信前端付款成功畫面。金流 provider callback 可能：

- 重複送。
- 延遲送。
- 送失敗交易。
- 送不存在訂單。
- 與前端回跳不同步。

目前設計把付款切成：

- `PaymentGateway`：provider abstraction。
- `PaymentCallbackController`：provider callback HTTP boundary。
- `OrderPaymentServiceImpl`：狀態更新、payment event、metrics、WebSocket 通知。

成功 callback 不是直接 update order，而是透過 `markPaymentSucceededByNumber` 帶條件更新：訂單必須仍是待付款且未付款，才會轉待確認與已付款。

### 對應程式碼

- `PaymentCallbackController`
- `OrderPaymentServiceImpl`
- `PaymentGateway`
- `DemoPaymentGateway`
- `EcpayPaymentGateway`
- `PaymentEventServiceImpl`

### 對應測試

- `DemoPaymentGatewayTest`
- `EcpayCheckMacValueCalculatorTest`
- `EcpayPaymentGatewayTest`
- `PaymentCallbackControllerTest`
- `OrderPaymentServiceImplTest`
- `PaymentEventMapperTest`
- `PaymentEventApiTest`

### 不要吹過頭

目前已完成真瀏覽器 ECPay sandbox stage checkout 與 OTP 付款成功回流。你可以說：

> provider abstraction、callback endpoint、demo HMAC、ECPay CheckMacValue、payment_event、provider-switch readiness、public callback preflight、EC2 SSM sandbox provider switch、Playwright stage checkout、OTP 付款成功、ReturnURL HTTP 200、訂單轉已付款、`CALLBACK_SUCCEEDED`、ECPay duplicate callback 單元測試、pending-request reconciliation 查詢、ECPay 查詢結果 parser 與 provider-query reconciliation job 都已完成；但還需要補正式監控告警與外部查詢排程的長時間運行證據。

下一步：

- 補 callback failure / pending request 告警。
- 補外部查詢排程長時間運行證據。
- 考慮 payment_event idempotency key unique constraint。

## 3. 揪團併發

### 你要講的重點

揪團最怕資料不一致：

- 超過 required count。
- 同一會員重複加入。
- `current_count` 跟 participant row 不一致。
- 成團後部分 pre-order 沒有轉成待確認。

目前設計：

- Redisson lock key: `lock:groupbuy:{groupNo}`
- `tryLock(3, 5, TimeUnit.SECONDS)`
- transactionTemplate 包住 join mutation
- DB unique constraint 防同會員重複 participant
- 成團後批次把 pre-order 轉 `TO_BE_CONFIRMED`
- WebSocket notification 放在 transaction 成功後

這裡最成熟的講法是：

> Redisson lock 降低同一團的競爭，DB unique key 作為最後防線，transaction 保證單次 join 的資料一致性。

### 對應程式碼

- `GroupBuyServiceImpl.joinGroupBuy`
- `GroupBuyServiceImpl.handleExpiredGroupBuys`
- `GroupBuyParticipantMapper`
- `GroupBuyTask`

### 對應測試與壓測

- `GroupBuyRedisIntegrationTest`
- `GroupBuyServiceTest`
- `GroupBuyExpirationServiceTest`
- `docs/perf/README.md`

目前 JMeter 數字：

| 指標 | 結果 |
|---|---|
| Concurrent users | 100 |
| API | `POST /user/groupBuy/join` |
| Error rate | 0.00% |
| P95 | 2847.65 ms |
| P99 | 2952.75 ms |
| DB result | `current_count=101`, participant rows +100 |

### 不要吹過頭

這是作品級 correctness + load evidence，不是 Shopee/Binance production capacity proof。

下一步要補：

- 熱團 vs 多團分散 benchmark。
- lock wait time、transaction time、DB slow query。
- 成團通知失敗時的 retry/outbox。

## 4. 庫存一致性

### 你要講的重點

庫存不是只有加減數字。面試官可能問：

- 扣庫存失敗時訂單是否仍建立？
- 取消重試是否重複還庫存？
- 直送箱取消是否回補組成商品？
- 管理員手動調整是否留痕？

目前設計：

- `reserveProduct` 透過 mapper conditional decrease 防止負庫存。
- 一般下單在 transaction 內建立訂單與扣庫存。
- 取消在 transaction 內取消訂單、退款、還庫存。
- `product_inventory_log` 記錄 reason、reference、before/after stock、operator 與取消還庫存的 idempotency key。
- 重複取消先查 order status 與 `ORDER_CANCEL_RESTORE` log，真正寫入時再由 `uk_inventory_log_idempotency_key` 擋掉重複 key。

### 對應程式碼

- `InventoryServiceImpl`
- `OrderSubmissionServiceImpl`
- `OrderCancellationServiceImpl`
- `ProductServiceImpl`

### 對應測試

- `ProductInventoryOrderTest`
- `OrderCancellationServiceImplTest`
- `ProductServiceImplTest`
- `WorkspaceLowStockTest`

### 不要吹過頭

目前是 service precheck + DB unique idempotency key。這仍不是完整事件溯源；若未來有 async worker 或多服務寫庫存，下一步應該把庫存異動統一成 command/event 模型，並補 retry/outbox。

## 5. 可觀測性與營運留痕

### 你要講的重點

如果被問「出問題你怎麼知道」，不要只說看 log。你可以說目前有三層：

1. Actuator health/info/metrics。
2. business counters。
3. admin operation log / payment event。

目前 metrics：

- `localfresh.payment.callback.total`
- `localfresh.order.cancellation.total`
- `localfresh.group_buy.transition.total`

### 對應程式碼與文件

- `BusinessMetricsServiceImpl`
- `AdminOperationLogServiceImpl`
- `PaymentEventServiceImpl`
- `docs/observability.md`

### 對應測試

- `BusinessMetricsServiceImplTest`
- `AdminOperationLogServiceImplTest`
- `AdminOperationLogApiTest`
- `PaymentEventApiTest`

### 不要吹過頭

Actuator counters 只是起點，不是完整 SRE stack。

下一步：

- Prometheus scrape。
- Grafana dashboard。
- callback rejected rate / duplicate rate alert。
- structured JSON log + trace id。

## 6. Security Boundaries

### 你要講的重點

目前可講的 security 不是「我做了完整資安」，而是：

- member/admin JWT context 分離。
- ThreadLocal 有 cleanup 測試。
- IDOR 測試覆蓋跨會員訂單與地址。
- Google OAuth 用 authorization code flow。
- mock login 有環境開關。

### 對應測試

- `IssueS2IdorOrderTest`
- `IssueS6IdorShippingAddressTest`
- `IssueS4ThreadLocalTest`
- `MemberOAuthLoginTest`
- `GoogleOAuthClientImplTest`

### 不要吹過頭

大廠還會繼續問：

- production CORS allowlist。
- Swagger/SpringDoc production 關閉。
- rate limiting。
- admin RBAC。
- secret rotation。
- IAM least privilege。

## 7. 如果被問「怎麼擴展？」

不要直接回答拆微服務。比較好的回答：

> 我會先量測，不會先拆。熱點目前可能在 group-buy join、product list cache、payment callback、order fulfillment。我會先補 slow query、lock wait time、cache hit/miss、callback metrics，再根據瓶頸決定是否需要 read replica、queue、outbox 或服務拆分。

優先順序：

| 優先級 | 行動 | 原因 |
|---|---|---|
| P0 | 補付款 callback / pending request 告警。 | 把已完成的 OTP 成功回流與 reconciliation job 推進到可觀測、可營運的金流證據。 |
| P0 | 重跑 README screenshots。 | 作品第一印象要跟最新 UI 一致。 |
| P1 | Prometheus + Grafana + alert thresholds。 | 補 production operations story。 |
| P1 | Group-buy benchmark matrix。 | 從單一 100-user case 升級成容量分析。 |
| P1 | Inventory restore retry/outbox design。 | 取消還庫存已補 DB 冪等鍵，下一步才需要處理非同步重試與跨服務寫入。 |
| P2 | CI/CD image build + EC2 rollout。 | 改善部署可靠性，但不應早於金流端到端證據穩定。 |

## 面試時的 5 分鐘後端深挖順序

1. 先開 `docs/architecture.md` 的揪團核心 ER：證明資料模型不是把多人塞進同一張訂單。
2. 再開 `OrderStatusTransitionPolicy`：證明狀態流轉集中管理。
3. 再開 `OrderPaymentServiceImpl` 與付款 callback sequence：講 guarded update、duplicate callback、payment_event。
4. 再開 `GroupBuyServiceImpl.joinGroupBuy`：講 Redisson lock + transaction。
5. 再開 `OrderCancellationServiceImpl`：講取消防重與還庫存。
6. 最後開 `docs/testing.md` / `docs/perf/README.md`：證明不是只靠口頭說。

## 最後提醒

這份文件是你的準備筆記。面試時不要說「我照 big-tech standard 做了一份 review」。你要自然地講：

> 我把這個作品的高風險邊界整理過：訂單狀態、付款 callback、揪團併發、庫存防重和可觀測性。這些地方都有對應測試或壓測證據，也有我知道還沒完成的下一步。
