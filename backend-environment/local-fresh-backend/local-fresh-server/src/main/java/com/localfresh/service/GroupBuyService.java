package com.localfresh.service;

import com.localfresh.dto.InitiateGroupBuyDTO;
import com.localfresh.dto.JoinGroupBuyDTO;
import com.localfresh.vo.GroupBuyVO;

import java.util.List;

public interface GroupBuyService {

    GroupBuyVO initiate(InitiateGroupBuyDTO initiateGroupBuyDTO);

    GroupBuyVO joinGroupBuy(JoinGroupBuyDTO joinGroupBuyDTO);

    GroupBuyVO getByGroupNo(String groupNo);

    List<GroupBuyVO> listMyGroupBuys();

    GroupBuyVO cancelGroupBuy(String groupNo);

    void handleExpiredGroupBuys();
}
