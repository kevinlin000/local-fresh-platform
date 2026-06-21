# Portfolio Completion Roadmap

這份文件用來追蹤 `菜籃日 Cailan Day` 作為 Java 後端求職作品的完整度。

評估標準不是「production SaaS 100%」，而是「面試時能否清楚展示需求、架構、交易流程、測試證據、部署邊界與後續判斷」。如果把標準拉到真正營收系統，還會需要正式金流合約、資安稽核、監控告警、備援、客服流程與資料治理；那些不應該全部塞進這個作品。

## Current Assessment

| 面向 | 目前狀態 | 評估 |
|---|---|---|
| 產品定位 | 已從課程外賣專案換成台灣在地生鮮平台，README、截圖、面試稿已對齊。 | 作品層級完整 |
| 會員端流程 | 商品瀏覽、購物車、下單、付款、訂單查詢、揪團頁已可展示，並使用真實食物圖片。 | 作品層級完整 |
| 管理端流程 | Dashboard、商品、訂單、付款事件、操作紀錄已能支撐營運 demo。 | 作品層級完整 |
| 訂單生命週期 | 狀態轉移集中在 `OrderStatusTransitionPolicy`，付款、取消、婉拒、配送、完成都有 service 測試。 | 強 |
| 付款邊界 | Demo gateway、ECPay CheckMacValue parser、callback endpoint、付款事件表、前端 POST form 導轉骨架、provider-switch readiness test 與真 ECPay gateway/controller contract test 已完成；公開 EC2 preflight 目前顯示 `/payment/callback` 尚未部署。 | 強，但缺真 sandbox 端到端驗證 |
| 揪團併發 | Redisson lock、transaction boundary、唯一鍵、Testcontainers Redis、JMeter 證據已具備。 | 強 |
| 庫存一致性 | 一般訂單、取消還庫存、商品管理邊界與重複取消防線已有測試。 | 強 |
| 測試證據 | 後端 service/integration/Redis 測試、JaCoCo、前端 build、手動 Playwright 截圖證據已整理。 | 強 |
| 系統設計答辯 | 已整理自用後端深挖筆記，涵蓋 correctness、idempotency、concurrency、payment、inventory、observability 與 residual risk。 | 強 |
| UI/UX | 已完成產品級 polish；不像最初的小 demo，但仍不是設計系統等級產品。 | 足夠面試 |
| 部署 | 已有 AWS EC2 + Nginx + Docker MySQL/Redis + S3 + CloudFront + DuckDNS 作品級部署敘事。 | 足夠面試 |
| 可觀測性 | 已有 Actuator health/info/metrics 與少量業務 metrics，涵蓋付款 callback、取消防重與揪團狀態轉換；尚未接 Prometheus/Grafana 與 trace。 | 作品層級足夠 |
| 自動化交付 | 有 GitHub Actions checks，但尚未做 image build / ECR / EC2 自動部署。 | 後期再做 |

## Recommended Priority

### P0 - Keep Stable

這些已經是作品核心，不應該大改：

- 揪團模型與 Redisson lock 流程。
- 訂單狀態機與付款 callback 邊界。
- README 截圖證據與 interview guide。
- 現有 Vue 會員端/管理端資訊架構。

如果修改這些區域，必須先有明確問題或測試缺口。

### P1 - Best Next Slices

1. **ECPay sandbox 端到端驗證**
   - 目的：把「已建好骨架」推進到「真的跑過 sandbox」。
   - 範圍：部署環境切 `PAYMENT_PROVIDER=ecpay`、確認 ReturnURL 經 Nginx 進 Spring Boot、付款事件寫入 `ECPAY / CALLBACK_SUCCEEDED`。
   - 風險：需要可用雲端環境與公開 HTTPS callback；適合放在專案尾段。

2. **庫存異動 idempotency 設計**
   - 目的：回答「取消、退款、重複 callback、重複還庫存怎麼防？」。
   - 範圍：先做設計與一兩個 service 測試，不急著加大型 event sourcing。
   - 風險：過度設計會讓作品偏離基本功展示。

3. **可觀測性下一階段**
   - 目的：把現有 Actuator + 業務 counters 接到更完整的營運觀測。
   - 範圍：Prometheus registry、Grafana dashboard、告警門檻、trace/log correlation。
   - 風險：需要避免為了展示 Grafana 而把部署變複雜，建議晚於 sandbox 金流與雲端環境穩定化。

4. **UI smoke test 或 screenshot checklist 自動化**
   - 目的：降低每次 polish 後靠人工截圖驗證的成本。
   - 範圍：先做 login/home/orders/admin dashboard 的 smoke，不做完整視覺回歸。
   - 風險：前端測試框架目前不完整，應避免一次導入太多工具。

## Defer Until Last

### Full Cloud Automation

上雲與 CD 應該留在專案後段，不應該早於核心流程穩定化。

原因：

- 雲端部署會放大任何 seed data、環境變數、callback URL、CORS、HTTPS、migration 問題。
- 現在更有價值的是保證本機與作品證據可重跑。
- 面試官通常先看程式碼、測試、流程與 README，再問部署細節。

建議順序：

1. 本機核心流程與測試穩定。
2. 文件與截圖證據穩定。
3. ECPay sandbox 或 mock callback 邊界完成。
4. 再做 AWS 部署更新與 CD 自動化。

## Current Next Step Recommendation

剛完成的本地切面是 **可觀測性最小切面**。

理由：

- 付款 callback、訂單狀態機與取消還庫存防重已經補強。
- 面試官若繼續追 production thinking，下一個自然問題是「你怎麼知道系統現在健康？怎麼看付款 callback、揪團與訂單狀態流轉是否異常？」。
- 這能補上營運可見度，又不需要立刻進入完整雲端 CD 或多服務架構。

已完成做法：

- Actuator exposure 納入 `health,info,metrics`，並可用 `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` 覆蓋。
- 補少量業務 metrics：付款 callback 成功/拒絕/重複、訂單取消防重命中、揪團成功/失敗/取消。
- 新增 `docs/observability.md` 說明本機查詢方式與目前邊界。
- 不導入完整 Prometheus/Grafana；先把應用層 metrics 定義清楚。

剛完成的本地切面是取消訂單防重：若訂單已取消，或該訂單已存在 `ORDER_CANCEL_RESTORE` 庫存回補紀錄，取消流程會直接跳過；真正寫入庫存流水時，`product_inventory_log.idempotency_key` 也有 unique constraint 作為 DB 最後防線，避免重複退款與重複還庫存。

剛完成的本地切面是 **ECPay sandbox readiness gate，不急著正式上線**。

理由：

- 付款事件、callback parser、前端 POST form 與 observability 已具備。
- 真 sandbox 驗證能把「我有寫金流骨架」推進到「我知道 provider callback、HTTPS、Nginx 與資料落點怎麼串」。
- 這仍可先用 runbook/preflight 做，不需要現在就做完整 CD 或高可用雲端架構。

目前結論：

- 本機 provider-switch readiness 已用 `PaymentGatewayProviderSelectionTest` 固定住。
- 本機 ECPay callback contract 已用 `PaymentCallbackControllerEcpayContractTest` 固定住：有效簽章進 service，無效簽章回 `0|FAIL`。
- 2026-06-21 公開 preflight 顯示 health 與 Nginx 正常，但 `/payment/callback` 回 HTTP `404`，代表 EC2 backend 尚未部署到含 callback endpoint 的版本。
- 在這個 blocker 解掉以前，不應切 `PAYMENT_PROVIDER=ecpay`。

剛完成的本地切面是 **UI smoke precheck + browser smoke + member/admin commerce polish**：

- 新增 `scripts/check-ui-smoke-local.mjs`，不引入 Playwright 或其他 npm 依賴。
- 檢查 backend health、會員端/管理端 Vue app shell、會員 mock login、管理端 login、商品列表、會員訂單、管理端 business data 與訂單查詢。
- 這不是視覺回歸；它用來在手動截圖或 live demo 前快速發現服務沒開、proxy/auth 壞掉、demo data 空掉。
- 另新增 Playwright Chromium smoke，覆蓋會員端 mock login、home、orders，以及管理端 login、dashboard、orders、products。
- 會員端首頁商品卡已從展示型卡片升級為 commerce card：直接加入購物車、查看詳情、配送訊號、用途提示與揪團免運訊號；商品詳情也補上配送、保存、揪團信任資訊。
- 管理端訂單頁已從 CRUD table 升級為履約工作台：待確認、已確認、配送中可作為隊列入口，列表列出履約判斷與下一步動作。
- 管理端商品頁已從商品清單升級為商品健檢工作台：上架、低庫存、需補資料、下架都可作為操作入口，列表列出庫存水位、門檻與圖文完整度。

剛完成的文件切面是 **backend deep-dive prep notes**：

- 新增 [docs/backend-deep-dive-prep.md](backend-deep-dive-prep.md)，作為 Kevin 面試前自用的後端深挖筆記。
- 筆記將訂單狀態機、付款 callback、揪團併發、庫存一致性、observability、security boundary、capacity position 分開整理。
- 每個區塊都列出要怎麼講、對應程式碼/測試、不要吹過頭的地方，避免把作品包裝成 production 100%。

下一個建議切面是 **截圖證據重跑，然後再決定是否進 ECPay/雲端同步**：

- 若要整理作品證據：用已新增的 local/browser smoke 當前置檢查，重跑 9 張 README 截圖與 demo acceptance，確認真實食物圖片、會員端、管理端畫面都維持最新狀態。
- 若要繼續衝全端觀感：下一刀可做管理端 dashboard 的更細緻優先級排序，但目前 orders/products 的操作面已足夠支撐面試 demo。
- 若要往 ECPay sandbox 推進：先做 EC2 backend 部署同步，讓 `/payment/callback` preflight 變成 HTTP `200` + `0|FAIL`，再正式跑 sandbox 付款。

## How To Use This Roadmap

每一輪完成後，更新三件事：

- `Current Assessment`：只在完整度明顯改變時更新。
- `Recommended Priority`：若下一步順位改變，說明原因。
- `Current Next Step Recommendation`：保持只有一個明確下一步。
