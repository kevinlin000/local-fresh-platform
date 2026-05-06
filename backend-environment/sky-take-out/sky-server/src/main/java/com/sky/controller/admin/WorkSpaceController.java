package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.ProductOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.GiftBoxOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作台
 */
@RestController
@RequestMapping("/admin/workspace")
@Slf4j
@Api(tags = "工作台相關介面")
public class WorkSpaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 工作台今日資料查詢
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("工作台今日資料查詢")
    public Result<BusinessDataVO> businessData(){
        //获得当天的开始时间
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        //获得当天的结束时间
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);
        return Result.success(businessDataVO);
    }

    /**
     * 查詢訂單管理資料
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("查詢訂單管理資料")
    public Result<OrderOverViewVO> orderOverView(){
        return Result.success(workspaceService.getOrderOverView());
    }

    /**
     * 查詢單品總覽
     * @return
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("查詢單品總覽")
    public Result<ProductOverViewVO> dishOverView(){
        return Result.success(workspaceService.getDishOverView());
    }

    /**
     * 查詢直送箱總覽
     * @return
     */
    @GetMapping("/overviewSetmeals")
    @ApiOperation("查詢直送箱總覽")
    public Result<GiftBoxOverViewVO> setmealOverView(){
        return Result.success(workspaceService.getSetmealOverView());
    }
}
