package com.localfresh.integration.support;

import com.localfresh.websocket.WebSocketServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class MockInfrastructureIntegrationTest {

    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;
}
