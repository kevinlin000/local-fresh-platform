package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
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
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            // 新增用戶數量
            Integer newUser = userMapper.countByMap(map);

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
}
