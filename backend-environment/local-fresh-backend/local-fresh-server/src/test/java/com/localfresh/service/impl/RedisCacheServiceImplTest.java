package com.localfresh.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void evictByPattern_scansAndDeletesMatchedKeys() {
        RedisCacheServiceImpl cacheService = new RedisCacheServiceImpl();
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        Cursor<String> cursor = mock(Cursor.class);
        ReflectionTestUtils.setField(cacheService, "appRedisTemplate", redisTemplate);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(0);
            consumer.accept("product_1");
            consumer.accept("product_2");
            return null;
        }).when(cursor).forEachRemaining(any());

        cacheService.evictByPattern("product_*");

        ArgumentCaptor<Collection<String>> keysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(redisTemplate).delete(keysCaptor.capture());
        assertThat(keysCaptor.getValue()).containsExactlyInAnyOrder("product_1", "product_2");
    }
}
