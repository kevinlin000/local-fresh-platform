package com.localfresh.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminOperationLogPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int page;
    private int pageSize;
    private String action;
    private String targetType;
    private Long targetId;
    private Long operatorId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
