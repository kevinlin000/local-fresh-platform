package com.localfresh.service.impl;

import com.localfresh.constant.PasswordConstant;
import com.localfresh.constant.StatusConstant;
import com.localfresh.dto.EmployeeDTO;
import com.localfresh.dto.EmployeeLoginDTO;
import com.localfresh.entity.Employee;
import com.localfresh.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void loginShouldAcceptBcryptPasswordWithoutRehashing() {
        Employee employee = activeEmployee(passwordEncoder.encode("123456"));
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);

        EmployeeLoginDTO dto = loginDTO("admin", "123456");
        Employee result = employeeService.login(dto);

        assertEquals(employee, result);
        verify(employeeMapper, never()).update(any(Employee.class));
    }

    @Test
    void loginShouldUpgradeLegacyMd5PasswordToBcrypt() {
        String legacyHash = DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8));
        Employee employee = activeEmployee(legacyHash);
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);

        employeeService.login(loginDTO("admin", "123456"));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).update(captor.capture());
        Employee updated = captor.getValue();
        assertEquals(employee.getId(), updated.getId());
        assertNotEquals(legacyHash, updated.getPassword());
        assertTrue(passwordEncoder.matches("123456", updated.getPassword()));
    }

    @Test
    void saveShouldStoreDefaultPasswordWithBcrypt() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setUsername("operator");
        dto.setName("Operator");

        employeeService.save(dto);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(captor.capture());
        Employee saved = captor.getValue();
        assertTrue(passwordEncoder.matches(PasswordConstant.DEFAULT_PASSWORD, saved.getPassword()));
    }

    private Employee activeEmployee(String password) {
        return Employee.builder()
                .id(1L)
                .username("admin")
                .name("Admin")
                .password(password)
                .status(StatusConstant.ENABLE)
                .build();
    }

    private EmployeeLoginDTO loginDTO(String username, String password) {
        EmployeeLoginDTO dto = new EmployeeLoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }
}
