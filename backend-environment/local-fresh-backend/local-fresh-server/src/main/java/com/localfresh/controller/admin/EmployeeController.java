package com.localfresh.controller.admin;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.dto.EmployeeDTO;
import com.localfresh.dto.EmployeeLoginDTO;
import com.localfresh.dto.EmployeePageQueryDTO;
import com.localfresh.entity.Employee;
import com.localfresh.properties.JwtProperties;
import com.localfresh.result.PageResult;
import com.localfresh.result.Result;
import com.localfresh.service.EmployeeService;
import com.localfresh.utils.JwtUtil;
import com.localfresh.vo.EmployeeLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 員工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Tag(name = "員工管理相關介面")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登入
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "員工登入")
    public Result<EmployeeLoginVO> login(@Valid @RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("員工登入：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登入成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @Operation(summary = "員工登出")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     *新增員工
     *
     * @return
     */
    @PostMapping
    @Operation(summary = "新增員工")
    public Result save(@Valid @RequestBody EmployeeDTO employeeDTO) {
        log.info("新增員工：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }

    /**
     * 員工分耶查詢
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "員工分頁查詢")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("員工分頁查詢：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 啟用或停用員工帳號
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "啟用或停用員工帳號")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("啟用停用員工帳號：{},{}", status, id);
        employeeService.startOrStop(status, id);
        return Result.success();
    }
    /**
     * 根據 ID 查詢員工資訊
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根據 ID 查詢員工資訊")
    public Result<Employee> getById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    /**
     * 編輯員工資訊
     * @param employeeDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "編輯員工資訊")
    public Result update(@Valid @RequestBody EmployeeDTO employeeDTO){
        log.info("編輯員工資訊：{}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }


}
