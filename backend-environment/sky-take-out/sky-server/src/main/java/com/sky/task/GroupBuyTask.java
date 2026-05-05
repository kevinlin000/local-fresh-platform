package com.sky.task;

import com.sky.service.GroupBuyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class GroupBuyTask {

    @Autowired
    private GroupBuyService groupBuyService;

    /**
     * 每分鐘掃描過期未成團的揪團，標記為失敗並退款
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processExpiredGroupBuy() {
        log.info("掃描過期揪團: {}", LocalDateTime.now());
        groupBuyService.handleExpiredGroupBuys();
    }
}
