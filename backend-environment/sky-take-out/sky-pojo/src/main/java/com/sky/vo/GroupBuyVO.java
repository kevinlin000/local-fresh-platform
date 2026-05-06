package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyVO implements Serializable {

    private Long id;
    private String groupNo;
    private Long initiatorId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Integer status;
    private Integer currentCount;
    private Integer requiredCount;
    private LocalDateTime expireAt;
    private String shareUrl;
    private List<GroupBuyParticipantVO> participants;
}
