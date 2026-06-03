# 測試執行說明

## 一般測試

後端一般測試不依賴本機 Redis 或 Docker，可直接執行：

```bash
cd backend-environment/sky-take-out
mvn test
```

`sky-server` 的 Maven Surefire 預設排除 JUnit tag `redis`，避免 reviewer
在沒有 Docker/Testcontainers 環境時被 Redis 整合測試阻塞。

## Redis 整合測試

揪團分散式鎖測試需要 Docker 與 Testcontainers：

```bash
cd backend-environment/sky-take-out
mvn -pl sky-server -DexcludedGroups= -Dgroups=redis test
```

這類測試會啟動 Redis container，驗證 Redisson lock 的真實併發行為。
