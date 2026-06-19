package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent implements Serializable {

    public static final String PROVIDER_UNKNOWN = "UNKNOWN";

    public static final String EVENT_REQUEST_CREATED = "REQUEST_CREATED";
    public static final String EVENT_CALLBACK_SUCCEEDED = "CALLBACK_SUCCEEDED";
    public static final String EVENT_CALLBACK_DUPLICATE = "CALLBACK_DUPLICATE";
    public static final String EVENT_CALLBACK_REJECTED = "CALLBACK_REJECTED";

    public static final String RESULT_PENDING = "PENDING";
    public static final String RESULT_SUCCEEDED = "SUCCEEDED";
    public static final String RESULT_IGNORED = "IGNORED";
    public static final String RESULT_REJECTED = "REJECTED";

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String provider;
    private String eventType;
    private String providerReference;
    private BigDecimal amount;
    private String result;
    private String rawPayload;
    private LocalDateTime createdAt;
}
