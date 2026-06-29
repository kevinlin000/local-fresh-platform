package com.localfresh.integration;

import com.localfresh.constant.EmployeeRoleConstant;
import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.integration.support.MockWebSocketMvcIntegrationTest;
import com.localfresh.properties.JwtProperties;
import com.localfresh.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AdminReadOnlyRoleApiTest extends MockWebSocketMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into employee
                    (username, name, password, status, role, create_time, update_time, create_user, update_user)
                values
                    ('demo_viewer', '面試展示帳號', 'b48d9c5f3bc873d3213500c1a0c5eadd', 1, 'VIEWER', now(), now(), 1, 1)
                """);
    }

    @Test
    void demoViewerLoginReturnsViewerRole() throws Exception {
        mockMvc.perform(post("/admin/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo_viewer","password":"viewonly"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.userName").value("demo_viewer"))
                .andExpect(jsonPath("$.data.role").value(EmployeeRoleConstant.VIEWER));
    }

    @Test
    void viewerCanReadAdminPages() throws Exception {
        mockMvc.perform(get("/admin/employee/page")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("token", token(EmployeeRoleConstant.VIEWER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].password").value("****"));
    }

    @Test
    void viewerCannotWriteAdminResources() throws Exception {
        mockMvc.perform(post("/admin/employee/status/1")
                        .param("id", "1")
                        .header("token", token(EmployeeRoleConstant.VIEWER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("展示帳號僅供查看，不能修改後台資料"));
    }

    @Test
    void adminCanWriteAdminResources() throws Exception {
        mockMvc.perform(post("/admin/employee/status/1")
                        .param("id", "1")
                        .header("token", token(EmployeeRoleConstant.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    private String token(String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 7L);
        claims.put(JwtClaimsConstant.ROLE, role);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
    }
}
