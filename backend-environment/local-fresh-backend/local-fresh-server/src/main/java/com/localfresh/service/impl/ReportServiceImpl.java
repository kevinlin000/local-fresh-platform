package com.localfresh.service.impl;

import com.localfresh.dto.GoodsSalesDTO;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.MemberMapper;
import com.localfresh.service.ReportService;
import com.localfresh.service.WorkspaceService;
import com.localfresh.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final String BUSINESS_DATA_REPORT_TEMPLATE = "template/營運資料報表模板.xlsx";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 統計指定時間區間內的營業額數據
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //當前集合用於存放從begin到end範圍的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            //日期計算，計算指定日期的後一天對應的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的營業額
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查詢date日期所對應的營業額數據，營業額是指狀態為「已完成」的訂單金額合計
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0 : turnover;
            turnoverList.add(turnover);

        }

        //封裝返回結果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 統計指定時間區間內的用戶數據
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放begin 到end之間的每天對應的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的新增用戶數量 select count(id) from user where create_time < ? and create time > ?
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的總用戶數量 select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);

            // 總用戶數量
            Integer totalUser = memberMapper.countByMap(map);

            map.put("begin", beginTime);
            // 新增用戶數量
            Integer newUser = memberMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);

        }

        // 封裝結果數據
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 統計指定時間區間內的訂單數據
     * @param begin
     * @param end
     * @return
     */
     public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
         // 存放begin 到end之間的每天對應的日期
         List<LocalDate> dateList = new ArrayList<>();
         dateList.add(begin);
         while(!begin.equals(end)){
             begin = begin.plusDays(1);
             dateList.add(begin);
         }
         //存放每天的訂單總數
         List<Integer> orderCountList = new ArrayList<>();
         List<Integer> validOrderCountList = new ArrayList<>();

         // 遍歷dateList集合，查詢每天的有效訂單數和訂單總數
         for (LocalDate date : dateList) {
             //查詢每天訂單總數 select count(id) from orders where order_time > ? and order_time < ?
             LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
             LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
             Integer orderCount = getOrderCount(beginTime, endTime, null);


             //查詢每天有效訂單數 select count(id) from orders where order_time > ? and order_time < ? and status = 5
             Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

             orderCountList.add(orderCount);
             validOrderCountList.add(validOrderCount);

         }

         // 計算時間區間內的訂單總數
         Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();


         // 計算時間區間內的有效訂單數量
         Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();


         Double orderCompletionRate = 0.0;
         if(totalOrderCount !=0){
             // 計算訂單完成率
             orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
         }

         return OrderReportVO.builder()
                 .dateList(StringUtils.join(dateList,","))
                 .orderCountList(StringUtils.join(orderCountList,","))
                 .validOrderCountList(StringUtils.join(validOrderCountList,","))
                 .totalOrderCount(totalOrderCount)
                 .validOrderCount(validOrderCount)
                 .orderCompletionRate(orderCompletionRate)
                 .build();
     }

    /**
     * 根據條件統計訂單數量
     * @param begin
     * @param end
     * @param status
     * @return
     */
     private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
         Map map = new HashMap();
         map.put("begin", begin);
         map.put("end", end);
         map.put("status", status);

         return orderMapper.countByMap(map);
     }

    /**
     * 統計指定時間區間內的銷量排名前10的菜品數據
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        // 封裝返回結果數據
        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 導出運營數據報表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查詢數據，獲取營業資料 -- 查詢最近30天的數據
        LocalDate dateBegin =  LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查詢概覽數據
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2.通過POI將資料寫入EXCEL中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(BUSINESS_DATA_REPORT_TEMPLATE);


        try {
            //基於模板文件，創造一個新的EXCEL文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //獲取表格文字的標籤頁sheet
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充數據 - 時間
            sheet.getRow(1).getCell(1).setCellValue("時間： "+ dateBegin +"至" + dateEnd);

            //獲得第四列
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //獲得第五列
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());


            // 填充明細數據
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                // 查詢某一天的營業數據
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                // 獲得某一列
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());


            }
            //3.通過輸出流將EXCEL文件下載到客戶端
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //關閉資源
            out.close();
            excel.close();

        } catch (IOException e) {
            log.error("匯出營運報表失敗", e);
        }


    }
}
