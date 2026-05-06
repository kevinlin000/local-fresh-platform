package com.sky.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.JoinGroupBuyDTO;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.sky.websocket.WebSocketServer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupBuyServiceImpl implements GroupBuyService {

    private static final Integer GROUP_BUY_ACTIVE = 1;
    private static final Integer GROUP_BUY_COMPLETED = 2;
    private static final Integer GROUP_BUY_FAILED = 3;

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

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.group-buy.expire-hours}")
    private Long groupBuyExpireHours;

    @Value("${sky.group-buy.share-base-url}")
    private String groupBuyShareBaseUrl;

    @Override
    @Transactional
    public GroupBuyVO initiate(InitiateGroupBuyDTO initiateGroupBuyDTO) {
        Long memberId = BaseContext.getCurrentId();
        if (memberId == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        Orders preOrder = buildPreOrder(memberId, initiateGroupBuyDTO.getProductId(),
                initiateGroupBuyDTO.getQuantity(), initiateGroupBuyDTO.getAddressId());

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
    public GroupBuyVO joinGroupBuy(JoinGroupBuyDTO joinGroupBuyDTO) {
        Long memberId = BaseContext.getCurrentId();
        if (memberId == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        String lockKey = "lock:groupbuy:" + joinGroupBuyDTO.getGroupNo();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new OrderBusinessException(MessageConstant.GROUP_BUY_BUSY);
            }

            JoinGroupBuyResult result = transactionTemplate.execute(status -> doJoinGroupBuy(memberId, joinGroupBuyDTO));
            if (result == null) {
                throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
            }
            if (result.groupBuyCompleted()) {
                sendGroupBuyCompletedNotification(result.groupNo());
            }
            return result.groupBuyVO();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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

    // 已知 N+1：每筆過期揪團 + 每個 participant 各一次 getById
    // 評估：揪團 required_count 通常為個位數、定時任務非熱路徑，
    // 暫不優化為 batch query。如未來揪團規模或頻率提升，可改用
    // orderMapper.getByIdsIn(orderIds) 一次撈完。
    @Override
    public void handleExpiredGroupBuys() {
        List<GroupBuy> expiredGroupBuys = groupBuyMapper.listExpiredActive(LocalDateTime.now());
        for (GroupBuy groupBuy : expiredGroupBuys) {
            String lockKey = "lock:groupbuy:" + groupBuy.getGroupNo();
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = false;
            try {
                locked = lock.tryLock(0, 5, TimeUnit.SECONDS);
                if (!locked) {
                    log.info("略過過期揪團，鎖已被佔用: groupNo={}", groupBuy.getGroupNo());
                    continue;
                }
                transactionTemplate.executeWithoutResult(status -> handleExpiredGroupBuyInTransaction(groupBuy.getGroupNo()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("處理過期揪團時被中斷: groupNo={}", groupBuy.getGroupNo(), e);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private JoinGroupBuyResult doJoinGroupBuy(Long memberId, JoinGroupBuyDTO joinGroupBuyDTO) {
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(joinGroupBuyDTO.getGroupNo());
        if (groupBuy == null) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_NOT_FOUND);
        }
        if (!GROUP_BUY_ACTIVE.equals(groupBuy.getStatus())) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_INVALID_STATUS);
        }
        if (groupBuy.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_EXPIRED);
        }
        if (groupBuy.getCurrentCount() >= groupBuy.getRequiredCount()) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_FULL);
        }
        if (groupBuyParticipantMapper.countByGroupBuyIdAndMemberId(groupBuy.getId(), memberId) > 0) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_ALREADY_JOINED);
        }

        Orders preOrder = buildPreOrder(memberId, joinGroupBuyDTO.getProductId(),
                joinGroupBuyDTO.getQuantity(), joinGroupBuyDTO.getAddressId());

        try {
            groupBuyParticipantMapper.insert(GroupBuyParticipant.builder()
                    .groupBuyId(groupBuy.getId())
                    .memberId(memberId)
                    .preOrderId(preOrder.getId())
                    .joinedAt(LocalDateTime.now())
                    .build());
        } catch (DuplicateKeyException e) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_ALREADY_JOINED);
        }

        groupBuy.setCurrentCount(groupBuy.getCurrentCount() + 1);
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        boolean groupBuyCompleted = false;
        if (groupBuy.getCurrentCount().equals(groupBuy.getRequiredCount())) {
            doCompleteGroupBuy(groupBuy);
            groupBuyCompleted = true;
        }

        GroupBuy updatedGroupBuy = groupBuyMapper.getById(groupBuy.getId());
        return new JoinGroupBuyResult(buildGroupBuyVO(updatedGroupBuy), groupBuyCompleted, updatedGroupBuy.getGroupNo());
    }

    private void doCompleteGroupBuy(GroupBuy groupBuy) {
        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId());
        List<Long> preOrderIds = participants.stream()
                .map(GroupBuyParticipant::getPreOrderId)
                .collect(Collectors.toList());

        groupBuy.setStatus(GROUP_BUY_COMPLETED);
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        if (!preOrderIds.isEmpty()) {
            orderMapper.updateStatusBatch(preOrderIds, Orders.PENDING_GROUP, Orders.TO_BE_CONFIRMED);
        }
    }

    private void handleExpiredGroupBuyInTransaction(String groupNo) {
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(groupNo);
        if (groupBuy == null) {
            return;
        }
        if (!GROUP_BUY_ACTIVE.equals(groupBuy.getStatus()) || !groupBuy.getExpireAt().isBefore(LocalDateTime.now())) {
            return;
        }

        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId());
        List<Long> preOrderIds = participants.stream()
                .map(GroupBuyParticipant::getPreOrderId)
                .collect(Collectors.toList());

        groupBuy.setStatus(GROUP_BUY_FAILED);
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        if (!preOrderIds.isEmpty()) {
            orderMapper.updateStatusBatch(preOrderIds, Orders.PENDING_GROUP, Orders.CANCELLED);
        }

        for (GroupBuyParticipant participant : participants) {
            Orders order = orderMapper.getById(participant.getPreOrderId());
            if (order != null) {
                log.info("揪團失敗退款: groupNo={}, memberId={}, orderId={}, amount={}",
                        groupBuy.getGroupNo(), participant.getMemberId(), order.getId(), order.getAmount());
            }
        }
    }

    private void sendGroupBuyCompletedNotification(String groupNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", 1);
        payload.put("content", "揪團已成團，請商家接單");
        payload.put("groupNo", groupNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(payload));
    }

    private Orders buildPreOrder(Long memberId, Long productId, Integer quantity, Long addressId) {
        Product product = productMapper.getById(productId);
        if (product == null) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
        }

        ShippingAddress shippingAddress = shippingAddressMapper.getById(addressId);
        if (shippingAddress == null || !memberId.equals(shippingAddress.getMemberId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        Orders preOrder = Orders.builder()
                .number(String.valueOf(IdUtil.getSnowflakeNextId()))
                .status(Orders.PENDING_GROUP)
                .userId(memberId)
                .addressBookId(shippingAddress.getId())
                .orderTime(now)
                .payMethod(1)
                .payStatus(Orders.UN_PAID)
                .amount(amount)
                .phone(shippingAddress.getPhone())
                .address(buildFullAddress(shippingAddress))
                .consignee(shippingAddress.getConsignee())
                .deliveryStatus(1)
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
                .number(quantity)
                .amount(product.getPrice())
                .build();
        orderDetailMapper.insert(orderDetail);

        return preOrder;
    }

    private GroupBuyVO buildGroupBuyVO(GroupBuy groupBuy) {
        List<GroupBuyParticipantVO> participants = groupBuyParticipantMapper.listParticipantVOByGroupBuyId(groupBuy.getId());
        GroupBuyParticipant initiatorParticipant = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId()).stream()
                .filter(participant -> groupBuy.getInitiatorId().equals(participant.getMemberId()))
                .findFirst()
                .orElse(null);
        OrderDetail initiatorOrderDetail = null;
        if (initiatorParticipant != null && initiatorParticipant.getPreOrderId() != null) {
            initiatorOrderDetail = orderDetailMapper.getByOrderId(initiatorParticipant.getPreOrderId()).stream()
                    .findFirst()
                    .orElse(null);
        }

        return GroupBuyVO.builder()
                .id(groupBuy.getId())
                .groupNo(groupBuy.getGroupNo())
                .initiatorId(groupBuy.getInitiatorId())
                .productId(initiatorOrderDetail == null ? null : initiatorOrderDetail.getProductId())
                .productName(initiatorOrderDetail == null ? null : initiatorOrderDetail.getName())
                .productImage(initiatorOrderDetail == null ? null : initiatorOrderDetail.getImage())
                .quantity(initiatorOrderDetail == null ? null : initiatorOrderDetail.getNumber())
                .status(groupBuy.getStatus())
                .currentCount(groupBuy.getCurrentCount())
                .requiredCount(groupBuy.getRequiredCount())
                .expireAt(groupBuy.getExpireAt())
                .shareUrl(groupBuyShareBaseUrl + "/" + groupBuy.getGroupNo())
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

    private static final class JoinGroupBuyResult {
        private final GroupBuyVO groupBuyVO;
        private final boolean groupBuyCompleted;
        private final String groupNo;

        private JoinGroupBuyResult(GroupBuyVO groupBuyVO, boolean groupBuyCompleted, String groupNo) {
            this.groupBuyVO = groupBuyVO;
            this.groupBuyCompleted = groupBuyCompleted;
            this.groupNo = groupNo;
        }

        private GroupBuyVO groupBuyVO() {
            return groupBuyVO;
        }

        private boolean groupBuyCompleted() {
            return groupBuyCompleted;
        }

        private String groupNo() {
            return groupNo;
        }
    }
}
