package com.sky.service;

import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.vo.GroupBuyVO;

import java.util.List;

public interface GroupBuyService {

    GroupBuyVO initiate(InitiateGroupBuyDTO initiateGroupBuyDTO);

    GroupBuyVO getByGroupNo(String groupNo);

    List<GroupBuyVO> listMyGroupBuys();
}
