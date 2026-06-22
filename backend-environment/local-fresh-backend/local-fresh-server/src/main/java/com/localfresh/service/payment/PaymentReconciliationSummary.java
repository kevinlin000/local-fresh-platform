package com.localfresh.service.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentReconciliationSummary {

    private int candidates;
    private int applied;
    private int rejected;
    private int stillPending;
    private int unknown;
    private int queryErrors;
    private int unsupported;
}
