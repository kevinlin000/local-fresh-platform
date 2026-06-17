package com.localfresh.controller.user;

import com.localfresh.dto.OrdersPaymentDTO;
import com.localfresh.dto.OrdersSubmitDTO;
import com.localfresh.result.PageResult;
import com.localfresh.result.Result;
import com.localfresh.service.OrderService;
import com.localfresh.vo.OrderPaymentVO;
import com.localfresh.vo.OrderSubmitVO;
import com.localfresh.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Tag(name = "會員端訂單相關介面")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;
    /**
     * 會員下單
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @Operation(summary = "會員下單")
    public Result<OrderSubmitVO> submit(@Valid @RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("會員下單, 參數為: {}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 訂單支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @Operation(summary = "訂單支付")
    public Result<OrderPaymentVO> payment(@Valid @RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("訂單支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 歷史訂單查詢
     *
     * @param page
     * @param pageSize
     * @param status   訂單狀態 1待付款 2待接單 3已接單 4派送中 5已完成 6已取消
     * @return
     */
    @GetMapping("/historyOrders")
    @Operation(summary = "歷史訂單查詢")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查詢訂單詳情
     *
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @Operation(summary = "查詢訂單詳情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.userDetails(id);
        return Result.success(orderVO);
    }

    /**
     * 會員取消訂單
     *
     * @return
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消訂單")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再來一單
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @Operation(summary = "再來一單")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 顧客催單
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @Operation(summary = "顧客催單")
    public Result reminder( @PathVariable("id") Long id){
        orderService.reminder(id);
        return Result.success();
    }
}
