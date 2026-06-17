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
