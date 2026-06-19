package com.localfresh.mapper;

import com.localfresh.entity.PaymentEvent;
import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentEventMapper {

    @Insert("insert into payment_event " +
            "(order_id, order_number, provider, event_type, provider_reference, amount, result, raw_payload, created_at) " +
            "values (#{orderId}, #{orderNumber}, #{provider}, #{eventType}, #{providerReference}, #{amount}, #{result}, #{rawPayload}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PaymentEvent paymentEvent);

    List<PaymentEvent> listByOrderNumber(@Param("orderNumber") String orderNumber);

    Page<PaymentEvent> pageQuery(PaymentEventPageQueryDTO queryDTO);
}
