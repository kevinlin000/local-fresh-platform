package com.sky.entity;

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
public class ProductInventoryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private Integer changeQuantity;
    private Integer stockBefore;
    private Integer stockAfter;
    private String reason;
    private String referenceType;
    private Long referenceId;
    private String operatorType;
    private Long operatorId;
    private LocalDateTime createdAt;
}
