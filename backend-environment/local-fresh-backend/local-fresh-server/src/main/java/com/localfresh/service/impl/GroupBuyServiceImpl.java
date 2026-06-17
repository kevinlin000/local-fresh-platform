package com.localfresh.service.impl;

import cn.hutool.core.util.IdUtil;
import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.InitiateGroupBuyDTO;
import com.localfresh.dto.JoinGroupBuyDTO;
import com.localfresh.entity.GroupBuy;
import com.localfresh.entity.GroupBuyParticipant;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.entity.Product;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.exception.AddressBookBusinessException;
import com.localfresh.exception.ForbiddenOperationException;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.GroupBuyMapper;
import com.localfresh.mapper.GroupBuyParticipantMapper;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.ShippingAddressMapper;
import com.localfresh.service.GroupBuyService;
import com.localfresh.service.InventoryService;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.GroupBuyParticipantVO;
import com.localfresh.vo.GroupBuyVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.localfresh.websocket.WebSocketServer;

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
    private static final Integer GROUP_BUY_CANCELED = 4;
    private static final String INVENTORY_REASON_GROUP_BUY_RESERVE = "GROUP_BUY_RESERVE";
    private static final String INVENTORY_REASON_GROUP_BUY_CANCEL_RESTORE = "GROUP_BUY_CANCEL_RESTORE";
    private static final String INVENTORY_OPERATOR_MEMBER = "MEMBER";
    private static final String INVENTORY_OPERATOR_SYSTEM = "SYSTEM";
    private static final String CANCEL_REASON_INITIATOR = "發起人取消揪團";
    private static final String CANCEL_REASON_EXPIRED = "揪團逾期未成團";

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

    @Autowired
    private InventoryService inventoryService;

    @Value("${localfresh.group-buy.expire-hours}")
    private Long groupBuyExpireHours;

    @Value("${localfresh.group-buy.share-base-url}")
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

    @Override
    public GroupBuyVO cancelGroupBuy(String groupNo) {
        Long memberId = BaseContext.getCurrentId();
        if (memberId == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        String lockKey = "lock:groupbuy:" + groupNo;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new OrderBusinessException(MessageConstant.GROUP_BUY_BUSY);
            }
            GroupBuyVO result = transactionTemplate.execute(status -> doCancelGroupBuy(memberId, groupNo));
            if (result == null) {
                throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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

    private GroupBuyVO doCancelGroupBuy(Long memberId, String groupNo) {
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(groupNo);
        if (groupBuy == null) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_NOT_FOUND);
        }
        if (!memberId.equals(groupBuy.getInitiatorId())) {
            throw new ForbiddenOperationException(MessageConstant.GROUP_BUY_CANCEL_FORBIDDEN);
        }
        if (!GROUP_BUY_ACTIVE.equals(groupBuy.getStatus())) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_CANNOT_CANCEL_FINISHED);
        }

        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId());
        if (participants.size() > 1) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_CANNOT_CANCEL_WITH_PARTICIPANTS);
        }
        if (participants.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        groupBuy.setStatus(GROUP_BUY_CANCELED);
        groupBuy.setUpdatedAt(now);
        groupBuyMapper.update(groupBuy);

        GroupBuyParticipant initiatorParticipant = participants.get(0);
        if (cancelPendingGroupOrder(initiatorParticipant.getPreOrderId(), CANCEL_REASON_INITIATOR, now)) {
            restoreGroupBuyStock(initiatorParticipant.getPreOrderId(), INVENTORY_OPERATOR_MEMBER, memberId);
        }

        GroupBuy updatedGroupBuy = groupBuyMapper.getById(groupBuy.getId());
        return buildGroupBuyVO(updatedGroupBuy);
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

        LocalDateTime now = LocalDateTime.now();
        groupBuy.setStatus(GROUP_BUY_FAILED);
        groupBuy.setUpdatedAt(now);
        groupBuyMapper.update(groupBuy);

        for (GroupBuyParticipant participant : participants) {
            if (cancelPendingGroupOrder(participant.getPreOrderId(), CANCEL_REASON_EXPIRED, now)) {
                restoreGroupBuyStock(participant.getPreOrderId(), INVENTORY_OPERATOR_SYSTEM, null);
                Orders order = orderMapper.getById(participant.getPreOrderId());
                if (order != null) {
                    log.info("揪團失敗退款: groupNo={}, memberId={}, orderId={}, amount={}",
                            groupBuy.getGroupNo(), participant.getMemberId(), order.getId(), order.getAmount());
                }
            }
        }
    }

    private boolean cancelPendingGroupOrder(Long orderId, String cancelReason, LocalDateTime cancelTime) {
        return orderMapper.cancelPendingGroupOrder(orderId, Orders.PENDING_GROUP, Orders.CANCELLED,
                cancelReason, cancelTime) > 0;
    }

    private void sendGroupBuyCompletedNotification(String groupNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", 1);
        payload.put("content", "揪團已成團，請店家確認");
        payload.put("groupNo", groupNo);
        webSocketServer.sendToAllClient(JsonUtil.toJson(payload));
    }

    private Orders buildPreOrder(Long memberId, Long productId, Integer quantity, Long addressId) {
        Product product = productMapper.getById(productId);
        if (product == null) {
            throw new OrderBusinessException(MessageConstant.GROUP_BUY_FAILED);
        }
        if (!Integer.valueOf(1).equals(product.getStatus())
                || quantity == null
                || quantity <= 0
                || product.getStock() == null
                || product.getStock() < quantity) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
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
        inventoryService.reserveProduct(product.getId(), quantity,
                INVENTORY_REASON_GROUP_BUY_RESERVE, preOrder.getId(), INVENTORY_OPERATOR_MEMBER, memberId);

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

    private void restoreGroupBuyStock(Long orderId, String operatorType, Long operatorId) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
        for (OrderDetail orderDetail : orderDetails) {
            if (orderDetail.getProductId() == null || orderDetail.getNumber() == null || orderDetail.getNumber() <= 0) {
                continue;
            }
            inventoryService.restoreProduct(orderDetail.getProductId(), orderDetail.getNumber(),
                    INVENTORY_REASON_GROUP_BUY_CANCEL_RESTORE, orderId, operatorType, operatorId);
        }
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
