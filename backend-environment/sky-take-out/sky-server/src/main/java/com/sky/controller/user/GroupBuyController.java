package com.sky.controller.user;

import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.JoinGroupBuyDTO;
import com.sky.result.Result;
import com.sky.service.GroupBuyService;
import com.sky.vo.GroupBuyVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/groupBuy")
@Api(tags = "會員端揪團介面")
@Slf4j
public class GroupBuyController {

    @Autowired
    private GroupBuyService groupBuyService;

    @PostMapping("/initiate")
    @ApiOperation("發起揪團")
    public Result<GroupBuyVO> initiate(@RequestBody InitiateGroupBuyDTO initiateGroupBuyDTO) {
        log.info("發起揪團: {}", initiateGroupBuyDTO);
        return Result.success(groupBuyService.initiate(initiateGroupBuyDTO));
    }

    @PostMapping("/join")
    @ApiOperation("加入揪團")
    public Result<GroupBuyVO> join(@RequestBody JoinGroupBuyDTO joinGroupBuyDTO) {
        log.info("加入揪團: {}", joinGroupBuyDTO);
        return Result.success(groupBuyService.joinGroupBuy(joinGroupBuyDTO));
    }

    @GetMapping("/{groupNo}")
    @ApiOperation("查詢揪團狀態")
    public Result<GroupBuyVO> getByGroupNo(@PathVariable String groupNo) {
        return Result.success(groupBuyService.getByGroupNo(groupNo));
    }

    @GetMapping("/my")
    @ApiOperation("查詢我參與的揪團列表")
    public Result<List<GroupBuyVO>> listMyGroupBuys() {
        return Result.success(groupBuyService.listMyGroupBuys());
    }
}
