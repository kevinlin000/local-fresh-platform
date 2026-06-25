package com.localfresh.integration.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class MockWebSocketMvcIntegrationTest {

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;
}
