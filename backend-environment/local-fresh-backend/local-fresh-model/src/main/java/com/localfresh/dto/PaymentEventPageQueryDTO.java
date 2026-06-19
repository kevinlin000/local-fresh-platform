package com.localfresh.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaymentEventPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int page;
    private int pageSize;
    private String orderNumber;
    private String provider;
    private String eventType;
    private String result;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
