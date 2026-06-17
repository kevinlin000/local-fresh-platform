package com.localfresh.service;

import com.localfresh.dto.EmployeeDTO;
import com.localfresh.dto.EmployeeLoginDTO;
import com.localfresh.dto.EmployeePageQueryDTO;
import com.localfresh.entity.Employee;
import com.localfresh.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登入
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增員工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分頁查詢
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
    /**
     * 啟用或停用员工账号
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根據id查詢員工資訊
     * @param id
     * @return
     */
    Employee getById(Long id);

    /**
     * 編輯員工資訊
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);
}
