package com.localfresh.mapper;

import com.localfresh.entity.GroupBuy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GroupBuyMapper {

    void insert(GroupBuy groupBuy);

    GroupBuy getById(Long id);

    GroupBuy getByGroupNo(String groupNo);

    void update(GroupBuy groupBuy);

    List<GroupBuy> listExpiredActive(@Param("now") LocalDateTime now);

    List<GroupBuy> listByMemberId(Long memberId);
}
