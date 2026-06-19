package com.localfresh.vo;

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
public class AdminOperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String action;
    private String targetType;
    private Long targetId;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private String operatorType;
    private Long operatorId;
    private LocalDateTime createdAt;
}
