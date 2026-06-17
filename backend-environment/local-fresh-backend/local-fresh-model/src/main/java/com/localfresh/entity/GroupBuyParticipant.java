package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyParticipant implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupBuyId;
    private Long memberId;
    private Long preOrderId;
    private LocalDateTime joinedAt;
}
