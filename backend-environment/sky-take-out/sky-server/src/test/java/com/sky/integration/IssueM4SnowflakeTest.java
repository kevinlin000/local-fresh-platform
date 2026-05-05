package com.sky.integration;

import cn.hutool.core.util.IdUtil;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssueM4SnowflakeTest {

    @Test
    void snowflake_100Threads_allIdsUnique() throws Exception {
        int threadCount = 100;
        int idsPerThread = 100;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(IdUtil.getSnowflakeNextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        int expected = threadCount * idsPerThread;
        assertEquals(expected, ids.size(),
                "Snowflake 產生了重複 ID：預期 " + expected + " 個，實際唯一 " + ids.size() + " 個");
    }
}
