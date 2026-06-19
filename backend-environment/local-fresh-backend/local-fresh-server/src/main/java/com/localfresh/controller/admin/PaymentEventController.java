package com.localfresh.controller.admin;

import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.result.PageResult;
import com.localfresh.result.Result;
import com.localfresh.service.PaymentEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/paymentEvents")
@Tag(name = "管理端付款事件")
@Slf4j
public class PaymentEventController {

    @Autowired
    private PaymentEventService paymentEventService;

    @GetMapping("/page")
    @Operation(summary = "付款事件分頁查詢")
    public Result<PageResult> page(PaymentEventPageQueryDTO queryDTO) {
        log.info("付款事件分頁查詢：{}", queryDTO);
        PageResult pageResult = paymentEventService.pageQuery(queryDTO);
        return Result.success(pageResult);
    }
}
