package com.localfresh.controller.user;

import com.localfresh.dto.InitiateGroupBuyDTO;
import com.localfresh.dto.JoinGroupBuyDTO;
import com.localfresh.result.Result;
import com.localfresh.service.GroupBuyService;
import com.localfresh.vo.GroupBuyVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/user/groupBuy")
@Tag(name = "會員端揪團介面")
@Slf4j
public class GroupBuyController {

    @Autowired
    private GroupBuyService groupBuyService;

    @PostMapping("/initiate")
    @Operation(summary = "發起揪團")
    public Result<GroupBuyVO> initiate(@Valid @RequestBody InitiateGroupBuyDTO initiateGroupBuyDTO) {
        log.info("發起揪團: {}", initiateGroupBuyDTO);
        return Result.success(groupBuyService.initiate(initiateGroupBuyDTO));
    }

    @PostMapping("/join")
    @Operation(summary = "加入揪團")
    public Result<GroupBuyVO> join(@Valid @RequestBody JoinGroupBuyDTO joinGroupBuyDTO) {
        log.info("加入揪團: {}", joinGroupBuyDTO);
        return Result.success(groupBuyService.joinGroupBuy(joinGroupBuyDTO));
    }

    @PostMapping("/{groupNo}/cancel")
    @Operation(summary = "取消揪團")
    public Result<GroupBuyVO> cancelGroupBuy(@PathVariable String groupNo) {
        log.info("取消揪團: {}", groupNo);
        return Result.success(groupBuyService.cancelGroupBuy(groupNo));
    }

    @GetMapping("/{groupNo}")
    @Operation(summary = "查詢揪團狀態")
    public Result<GroupBuyVO> getByGroupNo(@PathVariable String groupNo) {
        return Result.success(groupBuyService.getByGroupNo(groupNo));
    }

    @GetMapping("/my")
    @Operation(summary = "查詢我參與的揪團列表")
    public Result<List<GroupBuyVO>> listMyGroupBuys() {
        return Result.success(groupBuyService.listMyGroupBuys());
    }
}
