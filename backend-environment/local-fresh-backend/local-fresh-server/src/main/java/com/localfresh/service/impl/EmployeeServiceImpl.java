package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.constant.MessageConstant;
import com.localfresh.constant.PasswordConstant;
import com.localfresh.constant.StatusConstant;
import com.localfresh.dto.EmployeeDTO;
import com.localfresh.dto.EmployeeLoginDTO;
import com.localfresh.dto.EmployeePageQueryDTO;
import com.localfresh.entity.Employee;
import com.localfresh.exception.AccountLockedException;
import com.localfresh.exception.AccountNotFoundException;
import com.localfresh.exception.PasswordErrorException;
import com.localfresh.mapper.EmployeeMapper;
import com.localfresh.result.PageResult;
import com.localfresh.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 员工登入
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根據會員名查詢資料库中的資料
        Employee employee = employeeMapper.getByUsername(username);

        //2、處理各種例外情况（會員名不存在、密碼不对、帳號被鎖定）
        if (employee == null) {
            //帳號不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!passwordMatches(password, employee)) {
            // 密碼錯誤
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //帳號被鎖定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、回傳实体物件
        return employee;
    }

    /**
     * 新增員工
     * @param employeeDTO
     */
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        BeanUtils.copyProperties(employeeDTO, employee);

        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(passwordEncoder.encode(PasswordConstant.DEFAULT_PASSWORD));

        employeeMapper.insert(employee);


    }

    /**
     * 分頁查詢
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // select * from employee limit 0, 10
        //開始分頁查詢
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();
        List<Employee> records = page.getResult();
        records.forEach(employee -> employee.setPassword("****"));
        return new PageResult(total, records);
    }
    /**
     * 啟用或停用员工帳號
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = Employee.builder()
                .id(id)
                .status(status)
                .build();

        employeeMapper.update(employee);
    }
    /**
     * 根據id查詢員工資訊
     * @param id
     * @return
     */
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("****");
        return employee;
    }

    /**
     * 編輯員工資訊
     * @param employeeDTO
     */
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        employeeMapper.update(employee);
    }

    private boolean passwordMatches(String rawPassword, Employee employee) {
        String storedPassword = employee.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isBcryptHash(storedPassword) && passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }
        if (legacyMd5Matches(rawPassword, storedPassword)) {
            upgradePasswordHash(employee.getId(), rawPassword);
            return true;
        }
        return false;
    }

    private boolean legacyMd5Matches(String rawPassword, String storedPassword) {
        return DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8)).equals(storedPassword);
    }

    private boolean isBcryptHash(String storedPassword) {
        return storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$");
    }

    private void upgradePasswordHash(Long employeeId, String rawPassword) {
        Employee employee = Employee.builder()
                .id(employeeId)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        employeeMapper.update(employee);
    }
}
