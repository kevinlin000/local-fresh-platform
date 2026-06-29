# Portfolio Completion Roadmap

這份文件用來追蹤 `菜籃日 Cailan Day` 的工程完整度與後續投入順序。

評估標準不是「production SaaS 100%」，而是需求、架構、交易流程、測試證據、部署邊界與後續判斷是否清楚可驗證。如果把標準拉到真正營收系統，還會需要正式金流合約、資安稽核、監控告警、備援、客服流程與資料治理；那些不應該全部塞進這個專案。

## Current Assessment

| 面向 | 目前狀態 | 評估 |
|---|---|---|
| 產品定位 | 已從課程外賣專案換成台灣在地生鮮平台，README、截圖與工程證據已對齊。 | 作品層級完整 |
| 會員端流程 | 商品瀏覽、購物車、下單、付款、訂單查詢、揪團頁已可展示，並使用真實食物圖片。 | 作品層級完整 |
| 管理端流程 | Dashboard、商品、訂單、付款事件、操作紀錄已能支撐營運 demo。 | 作品層級完整 |
| 訂單生命週期 | 狀態轉移集中在 `OrderStatusTransitionPolicy`，付款、取消、婉拒、配送、完成都有 service 測試。 | 強 |
| 付款邊界 | Demo gateway、ECPay CheckMacValue parser、callback endpoint、付款事件表、前端 POST form 導轉、provider-switch readiness test、真 ECPay gateway/controller contract test、公開 EC2 callback preflight、SSM sandbox provider switch、真瀏覽器 stage checkout、sandbox OTP 成功回流、`CALLBACK_SUCCEEDED`、ECPay duplicate callback 測試、pending-request reconciliation 查詢、ECPay 查詢結果 parser、provider-query reconciliation job、pending candidate gauge、Grafana dashboard JSON、本機 Prometheus alert rules、Prometheus target `UP` 截圖與本機業務事件 metric 驗證已完成。 | 強，但仍缺外部查詢排程的長時間運行與雲端監控留痕 |
| 揪團併發 | Redisson lock、transaction boundary、唯一鍵、Testcontainers Redis、JMeter 證據已具備。 | 強 |
| 庫存一致性 | 一般訂單、取消還庫存、商品管理邊界與重複取消防線已有測試。 | 強 |
| 測試證據 | 後端 service/integration/Redis 測試、JaCoCo、前端 build、手動 Playwright 截圖證據已整理。 | 強 |
| 系統設計證據 | 已整理後端深挖筆記，涵蓋 correctness、idempotency、concurrency、payment、inventory、observability 與 residual risk。 | 強 |
| UI/UX | 已完成產品級 polish；不像最初的小 demo，但仍不是設計系統等級產品。 | 展示層級足夠 |
| 部署 | 已有 AWS EC2 + Nginx + Docker MySQL/Redis + S3 + CloudFront + DuckDNS 展示級部署敘事。 | 展示級部署完整 |
| 可觀測性 | 已有 Actuator health/info/metrics/prometheus 與少量業務 metrics，涵蓋付款 callback、付款 reconciliation、latest pending reconciliation candidates、取消防重與揪團狀態轉換；已整理 Prometheus scrape 範例、Grafana dashboard JSON、本機 Prometheus / Grafana / Alertmanager compose、告警規則、first checks、Prometheus target `UP` 截圖證據與可重跑的業務事件 metric 驗證。尚未補長時間雲端監控、告警接收器與 trace。 | 強 |
| 自動化交付 | 有 GitHub Actions checks；backend 現在可透過 `/actuator/info` 暴露部署 commit/branch；CI 會上傳 backend release package artifact，內含 jar、release metadata、SHA256 checksums、deploy commands、systemd/Nginx/env 範本；尚未做 image build / ECR / EC2 自動部署。 | 後期再做 |

## Recommended Priority

### P0 - Keep Stable

這些已經是作品核心，不應該大改：

- 揪團模型與 Redisson lock 流程。
- 訂單狀態機與付款 callback 邊界。
- README 截圖證據與工程深挖筆記。
- 現有 Vue 會員端/管理端資訊架構。

如果修改這些區域，必須先有明確問題或測試缺口。

### P1 - Best Next Slices

1. **Observability 長時間運行與雲端監控證據**
   - 目的：把已完成的本機 Prometheus / Grafana / Alertmanager 接線推進到更像營運現場的證據，而不只證明 target 能 scrape。
   - 範圍：Prometheus target `UP`、Grafana dashboard 載入、alert rules 可見與本機 business-event metric 驗證已完成；下一步補較長時間的 payment/reconciliation 外部查詢排程運行證據，若要往 EC2 推進，再補雲端監控留痕。
   - 風險：不要把作品部署複雜度拉太高；保留單機 demo 可穩定重現。

2. **Log / trace correlation**
   - 目的：回答「單一付款或訂單異常時，怎麼從 request 找到 callback、service log 與 DB event？」。
   - 範圍：先整理 correlation id / order number / provider trade number 的 log 規則，不急著導入分散式 tracing。
   - 風險：這是加分項，不應該早於 dashboard 證據與核心流程穩定。

3. **UI smoke test 或 screenshot checklist 自動化**
   - 目的：降低每次 polish 後靠人工截圖驗證的成本。
   - 範圍：現有 local/browser smoke 已可擋基本壞路徑，下一步可補 screenshot checklist 或輕量截圖差異檢查；不做完整視覺回歸。
   - 風險：前端測試框架目前不完整，應避免一次導入太多工具。

## Defer Until Last

### Full Cloud Automation

上雲與 CD 應該留在專案後段，不應該早於核心流程穩定化。

原因：

- 雲端部署會放大任何 seed data、環境變數、callback URL、CORS、HTTPS、migration 問題。
- 現在更有價值的是保證本機與作品證據可重跑。
- 專案審查通常先看程式碼、測試、流程與 README，再進一步檢查部署細節。

建議順序：

1. 本機核心流程與測試穩定。
2. 文件與截圖證據穩定。
3. ECPay sandbox callback / checkout 邊界完成。
4. Live observability 本機業務事件證據穩定後，再補長時間運行與雲端監控留痕。
5. 再做 AWS 部署更新與 CD 自動化。

## Current Next Step Recommendation

目前作品已具備完整 demo 閉環：會員端採買、揪團、訂單、管理端營運、付款事件、唯讀展示帳號、ECPay sandbox 證據、README 截圖、local/browser smoke、Prometheus target `UP` 與可重跑的 business-event metric evidence。

下一步建議：**開始錄 demo 與面試演練**。工程面再往下做，優先順序才是長時間 observability 證據、trace/log correlation、CD 自動化。

目前可安全收尾的理由：

- README 截圖已重跑並對齊最新 UI。
- EC2 backend `/actuator/info` 目前回報 `f93f6c41a373`，public readiness 通過。
- 公開管理端使用 `demo_viewer` 唯讀角色，不會破壞展示資料。
- 後端深度已能回答狀態機、callback 冪等、揪團併發、庫存防重、audit log、observability 與部署邊界。

後續只建議做加分項，不建議再大改主流程：

- 長時間 provider-query reconciliation 運行證據。
- 雲端監控留痕與告警接收器。
- structured JSON log / Trace ID。
- Docker image build + ECR + EC2 rollout。

## How To Use This Roadmap

每一輪完成後，更新三件事：

- `Current Assessment`：只在完整度明顯改變時更新。
- `Recommended Priority`：若下一步順位改變，說明原因。
- `Current Next Step Recommendation`：保持只有一個明確下一步。
