package com.localfresh.controller.admin;

import com.localfresh.result.Result;
import com.localfresh.service.ReportService;
import com.localfresh.vo.OrderReportVO;
import com.localfresh.vo.SalesTop10ReportVO;
import com.localfresh.vo.TurnoverReportVO;
import com.localfresh.vo.UserReportVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 數據統計相關介面
 */
@RestController
@RequestMapping("/admin/report")
@Tag(name = "資料統計相關介面")
@Slf4j

public class ReportController {

    @Autowired
    private ReportService reportService;
    /**
     * 營業額統計
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @Operation(summary = "營業額統計")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("營業額統計，{}，{}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    /**
     * 會員統計
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @Operation(summary = "會員統計")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("會員統計，{}，{}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    /**
     * 訂單統計
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @Operation(summary = "訂單統計")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("訂單統計，{}，{}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }


    /**
     * 銷量排名 Top 10
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @Operation(summary = "銷量排名 Top 10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("銷量排名TOP10，{}，{}", begin, end);
        return Result.success(reportService.getSalesTop10(begin, end));
    }

    /**
     * 匯出營運資料報表
     * @param response
     */
    @GetMapping("/export")
    @Operation(summary = "匯出營運資料報表")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);

    }

}
