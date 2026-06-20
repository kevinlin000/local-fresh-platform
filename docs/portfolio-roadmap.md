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
| 付款邊界 | Demo gateway、ECPay CheckMacValue parser、callback endpoint、付款事件表、前端 POST form 導轉骨架已完成。 | 強，但缺真 sandbox 端到端驗證 |
| 揪團併發 | Redisson lock、transaction boundary、唯一鍵、Testcontainers Redis、JMeter 證據已具備。 | 強 |
| 庫存一致性 | 一般訂單、取消還庫存、商品管理邊界與重複取消防線已有測試。 | 強 |
| 測試證據 | 後端 service/integration/Redis 測試、JaCoCo、前端 build、手動 Playwright 截圖證據已整理。 | 強 |
| UI/UX | 已完成產品級 polish；不像最初的小 demo，但仍不是設計系統等級產品。 | 足夠面試 |
| 部署 | 已有 AWS EC2 + Nginx + Docker MySQL/Redis + S3 + CloudFront + DuckDNS 作品級部署敘事。 | 足夠面試 |
| 可觀測性 | 有 Actuator 基礎，但沒有 Prometheus/Grafana、業務 metrics、結構化 trace。 | 下一階段可補 |
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

3. **可觀測性最小切面**
   - 目的：補 production thinking，但不改架構。
   - 範圍：Actuator health/info、少量業務 metrics，例如付款 callback 結果、揪團成功/失敗計數、訂單狀態轉移計數。
   - 風險：需要避免為了展示 Grafana 而把部署變複雜。

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

下一個最值得做的切面是 **可觀測性最小切面**。

理由：

- 付款 callback、訂單狀態機與取消還庫存防重已經補強。
- 面試官若繼續追 production thinking，下一個自然問題是「你怎麼知道系統現在健康？怎麼看付款 callback、揪團與訂單狀態流轉是否異常？」。
- 這能補上營運可見度，又不需要立刻進入完整雲端 CD 或多服務架構。

建議做法：

- 先盤點現有 Actuator 設定，不改部署架構。
- 補少量業務 metrics，例如付款 callback 成功/拒絕/重複、訂單取消防重命中、揪團成功/失敗。
- 在 `docs/testing.md` 或 architecture docs 補上如何本機查看這些 metrics。
- 不急著引入完整 Prometheus/Grafana；先把應用層 metrics 定義清楚。

剛完成的本地切面是取消訂單的 service-level 防重：若訂單已取消，或該訂單已存在 `ORDER_CANCEL_RESTORE` 庫存回補紀錄，取消流程會直接跳過，避免重複退款與重複還庫存。這不是取代資料庫唯一鍵的最終方案，但符合本作品目前的務實範圍。

## How To Use This Roadmap

每一輪完成後，更新三件事：

- `Current Assessment`：只在完整度明顯改變時更新。
- `Recommended Priority`：若下一步順位改變，說明原因。
- `Current Next Step Recommendation`：保持只有一個明確下一步。
