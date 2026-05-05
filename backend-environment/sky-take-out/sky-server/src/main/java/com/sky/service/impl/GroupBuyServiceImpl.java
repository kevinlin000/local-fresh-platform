package com.sky.service.impl;

import cn.hutool.core.util.IdUtil;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.entity.GroupBuy;
import com.sky.entity.GroupBuyParticipant;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.Product;
import com.sky.entity.ShippingAddress;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.GroupBuyMapper;
import com.sky.mapper.GroupBuyParticipantMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.service.GroupBuyService;
import com.sky.vo.GroupBuyParticipantVO;
import com.sky.vo.GroupBuyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupBuyServiceImpl implements GroupBuyService {

    private static final Integer GROUP_BUY_ACTIVE = 1;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyParticipantMapper groupBuyParticipantMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    @Value("${sky.group-buy.expire-hours}")
    private Long groupBuyExpireHours;

    @Override
    @Transactional
    public GroupBuyVO initiate(InitiateGroupBuyDTO initiateGroupBuyDTO) {
        Long memberId = BaseContext.getCurrentId();
        if (memberId == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        Product product = productMapper.getById(initiateGroupBuyDTO.getProductId());
        if (product == null) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
        }

        ShippingAddress shippingAddress = shippingAddressMapper.getById(initiateGroupBuyDTO.getAddressId());
        if (shippingAddress == null || !memberId.equals(shippingAddress.getMemberId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Orders preOrder = buildPreOrder(memberId, initiateGroupBuyDTO, shippingAddress, product);

        LocalDateTime now = LocalDateTime.now();
        GroupBuy groupBuy = GroupBuy.builder()
                .groupNo(String.valueOf(IdUtil.getSnowflakeNextId()))
                .initiatorId(memberId)
                .status(GROUP_BUY_ACTIVE)
                .requiredCount(initiateGroupBuyDTO.getRequiredCount())
                .currentCount(1)
                .expireAt(now.plusHours(groupBuyExpireHours))
                .createdAt(now)
                .updatedAt(now)
                .build();
        groupBuyMapper.insert(groupBuy);

        GroupBuyParticipant participant = GroupBuyParticipant.builder()
                .groupBuyId(groupBuy.getId())
                .memberId(memberId)
                .preOrderId(preOrder.getId())
                .joinedAt(now)
                .build();
        groupBuyParticipantMapper.insert(participant);

        return buildGroupBuyVO(groupBuy);
    }

    @Override
    public GroupBuyVO getByGroupNo(String groupNo) {
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(groupNo);
        if (groupBuy == null) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_NOT_FOUND);
        }
        return buildGroupBuyVO(groupBuy);
    }

    @Override
    public List<GroupBuyVO> listMyGroupBuys() {
        Long memberId = BaseContext.getCurrentId();
        if (memberId == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        return groupBuyMapper.listByMemberId(memberId)
                .stream()
                .map(this::buildGroupBuyVO)
                .collect(Collectors.toList());
    }

    private Orders buildPreOrder(Long memberId, InitiateGroupBuyDTO initiateGroupBuyDTO,
                                 ShippingAddress shippingAddress, Product product) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(initiateGroupBuyDTO.getQuantity()));

        Orders preOrder = Orders.builder()
                .number(String.valueOf(IdUtil.getSnowflakeNextId()))
                .status(Orders.PENDING_GROUP)
                .userId(memberId)
                .addressBookId(shippingAddress.getId())
                .orderTime(now)
                .payStatus(Orders.UN_PAID)
                .amount(amount)
                .phone(shippingAddress.getPhone())
                .address(buildFullAddress(shippingAddress))
                .consignee(shippingAddress.getConsignee())
                .packAmount(0)
                .tablewareNumber(0)
                .tablewareStatus(0)
                .build();
        orderMapper.insert(preOrder);

        OrderDetail orderDetail = OrderDetail.builder()
                .orderId(preOrder.getId())
                .name(product.getProductName())
                .image(product.getImage())
                .productId(product.getId())
                .number(initiateGroupBuyDTO.getQuantity())
                .amount(product.getPrice())
                .build();
        orderDetailMapper.insert(orderDetail);

        return preOrder;
    }

    private GroupBuyVO buildGroupBuyVO(GroupBuy groupBuy) {
        List<GroupBuyParticipantVO> participants = groupBuyParticipantMapper.listParticipantVOByGroupBuyId(groupBuy.getId());
        return GroupBuyVO.builder()
                .id(groupBuy.getId())
                .groupNo(groupBuy.getGroupNo())
                .initiatorId(groupBuy.getInitiatorId())
                .status(groupBuy.getStatus())
                .currentCount(groupBuy.getCurrentCount())
                .requiredCount(groupBuy.getRequiredCount())
                .expireAt(groupBuy.getExpireAt())
                .participants(participants)
                .build();
    }

    private String buildFullAddress(ShippingAddress shippingAddress) {
        return nullToEmpty(shippingAddress.getProvinceName())
                + nullToEmpty(shippingAddress.getCityName())
                + nullToEmpty(shippingAddress.getDistrictName())
                + nullToEmpty(shippingAddress.getDetail());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
