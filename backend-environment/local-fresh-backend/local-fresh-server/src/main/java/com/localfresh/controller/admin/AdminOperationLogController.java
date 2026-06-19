package com.localfresh.controller.admin;

import com.localfresh.dto.AdminOperationLogPageQueryDTO;
import com.localfresh.result.PageResult;
import com.localfresh.result.Result;
import com.localfresh.service.AdminOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/operationLogs")
@Tag(name = "管理端操作紀錄")
@Slf4j
public class AdminOperationLogController {

    @Autowired
    private AdminOperationLogService adminOperationLogService;

    @GetMapping("/page")
    @Operation(summary = "管理端操作紀錄分頁查詢")
    public Result<PageResult> page(AdminOperationLogPageQueryDTO queryDTO) {
        log.info("管理端操作紀錄分頁查詢：{}", queryDTO);
        PageResult pageResult = adminOperationLogService.pageQuery(queryDTO);
        return Result.success(pageResult);
    }
}
