## GroupBuy Join Load Test

這份壓測針對會員端 `POST /user/groupBuy/join`，目標是驗證在本地環境下，`100` 個併發加入同一團時，Redisson 分散式鎖能穩定保護資料一致性，且 API 延遲與吞吐量可量測。

### 環境

- 後端：`mvn spring-boot:run -pl local-fresh-server -Dspring-boot.run.profiles=dev`
- MySQL：本機 `mysqld`，資料已套用 `V5__taiwan_localization.sql` 與 `V6__add_product_images.sql`
- Redis：`docker compose up -d redis`
- JMeter：`/usr/local/bin/jmeter`

### 測試資料

- 揪團單號：`loadtest_1778755924`
- `productId`：`1`
- `required_count`：`200`
- `current_count` 初始值：`1`
- 使用者清單：`loadtest-users.csv`
- 使用者數量：`100`

### 執行命令

```bash
cd docs/perf
JVM_ARGS="-Xms1g -Xmx2g" \
jmeter -n \
  -t groupbuy-join-load-test.jmx \
  -JGROUP_NO=loadtest_1778755924 \
  -JPRODUCT_ID=1 \
  -JCSV_FILE=/Users/kevinlintingwei/local-fresh-platform/docs/perf/loadtest-users.csv \
  -l results.jtl \
  -e -o html-report
```

### JMeter 設定摘要

- Thread Group：`100` threads
- Ramp-up：`1` 秒
- Loop Count：`1`
- 流程：
  1. `POST /user/member/login` 取得 JWT
  2. `POST /user/groupBuy/join` 使用 `authentication` header 打同一團

### 結果

#### Join API

- Samples：`100`
- Error rate：`0.00%`
- Throughput：`33.26 req/s`
- Mean：`1984.06 ms`
- P50：`1971.0 ms`
- P95：`2847.65 ms`
- P99：`2952.75 ms`
- Min / Max：`1021.0 ms / 2953.0 ms`

#### 全流程（login + join）

- Samples：`200`
- Error rate：`0.00%`
- Throughput：`47.10 req/s`
- Mean：`1449.91 ms`
- P50：`1158.5 ms`
- P95：`2780.05 ms`
- P99：`2927.76 ms`
- Min / Max：`492.0 ms / 2953.0 ms`

### 一致性驗證

壓測完成後，資料庫驗證如下：

- `group_buy.current_count = 101`
- `group_buy_participant` 新增 `100` 筆

說明：

- 團主原本已算 `1` 位參與者
- 本次 `100` 個壓測使用者全部成功加入
- 沒有出現 `500`、timeout、或 participant 數量與 `current_count` 不一致

### 產物

- JMeter 計畫：[groupbuy-join-load-test.jmx](/Users/kevinlintingwei/local-fresh-platform/docs/perf/groupbuy-join-load-test.jmx)
- 使用者對應表：[loadtest-users.csv](/Users/kevinlintingwei/local-fresh-platform/docs/perf/loadtest-users.csv)
- 原始結果：[results.jtl](/Users/kevinlintingwei/local-fresh-platform/docs/perf/results.jtl)
- HTML Dashboard：可透過上述 JMeter 指令重新產生至 `docs/perf/html-report/`；該目錄屬產物，不提交版本庫。

### 結論

在這次本地 `100` 併發加入同一團的壓測下，`joinGroupBuy()` 沒有出現超賣、重複 participant、HTTP 錯誤或 timeout，顯示目前 `Redisson lock + transaction` 的並發保護在這個負載級別下是有效的。
