package com.sky.service;

import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.JoinGroupBuyDTO;
import com.sky.vo.GroupBuyVO;

import java.util.List;

public interface GroupBuyService {

    GroupBuyVO initiate(InitiateGroupBuyDTO initiateGroupBuyDTO);

    GroupBuyVO joinGroupBuy(JoinGroupBuyDTO joinGroupBuyDTO);

    GroupBuyVO getByGroupNo(String groupNo);

    List<GroupBuyVO> listMyGroupBuys();

    void handleExpiredGroupBuys();
}
