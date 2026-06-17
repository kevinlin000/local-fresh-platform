package com.localfresh.mapper;

import com.localfresh.entity.GroupBuyParticipant;
import com.localfresh.vo.GroupBuyParticipantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupBuyParticipantMapper {

    void insert(GroupBuyParticipant participant);

    Integer countByGroupBuyIdAndMemberId(@Param("groupBuyId") Long groupBuyId, @Param("memberId") Long memberId);

    List<GroupBuyParticipant> listByGroupBuyId(Long groupBuyId);

    List<GroupBuyParticipantVO> listParticipantVOByGroupBuyId(Long groupBuyId);
}
