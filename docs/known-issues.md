## 測試環境與生產環境 schema 約束不完全等價

蒼穹外賣原始的 `orders` 表在 production MySQL 為 `NOT NULL DEFAULT 1`
的欄位（`pay_method`, `delivery_status`），但測試環境 `H2 schema-test.sql`
原本未強制這些約束。這導致 `buildPreOrder` 等方法在不設定這些欄位時，
測試環境通過（H2 接受 `NULL`）但 dev/prod MySQL 靠 default 靜默兜底。

本次修正（commit `b49a117` 與本次 `stage4-5` fix）：
- 在 `buildPreOrder` 顯式設定 `payMethod=1` / `deliveryStatus=1`
- `schema-test.sql` 對齊 `NOT NULL DEFAULT 1`
- 補 3 個 Issue 測試 fixture

尚未處理：
- 其他可能存在類似問題的 `orders` 表欄位未完整審視
- `V1` migration 不存在（`orders` 等原始表的 DDL 未版本化管理）

後續處理時機：`Stage 5` 收尾或下一個專案版本
