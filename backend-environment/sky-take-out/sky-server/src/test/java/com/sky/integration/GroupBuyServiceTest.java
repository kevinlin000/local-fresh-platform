package com.sky.integration;

import com.sky.context.BaseContext;
import com.sky.exception.ForbiddenOperationException;
import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.GroupBuy;
import com.sky.entity.GroupBuyParticipant;
import com.sky.entity.Member;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.Product;
import com.sky.entity.ProductInventoryLog;
import com.sky.entity.ShippingAddress;
import com.sky.mapper.GroupBuyMapper;
import com.sky.mapper.GroupBuyParticipantMapper;
import com.sky.mapper.MemberMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ProductInventoryLogMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.service.GroupBuyService;
import com.sky.vo.GroupBuyVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.Assertions;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupBuyServiceTest {

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductInventoryLogMapper productInventoryLogMapper;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyParticipantMapper groupBuyParticipantMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    private Long memberId;
    private Long productId;
    private Long addressId;
    private RLock groupBuyLock;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .openid("group-buy-member")
                .name("揪團測試會員")
                .createTime(LocalDateTime.now())
                .build();
        memberMapper.insert(member);
        memberId = member.getId();

        BaseContext.setCurrentId(memberId);

        Product product = Product.builder()
                .productName("高山高麗菜")
                .categoryId(1L)
                .price(new BigDecimal("88.00"))
                .status(1)
                .stock(5)
                .lowStockThreshold(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        productMapper.insert(product);
        productId = product.getId();

        ShippingAddress shippingAddress = ShippingAddress.builder()
                .memberId(memberId)
                .consignee("測試收件人")
                .phone("0912345678")
                .cityName("台北市")
                .districtName("信義區")
                .detail("市府路1號")
                .isDefault(1)
                .build();
        shippingAddressMapper.insert(shippingAddress);
        addressId = shippingAddress.getId();

        groupBuyLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(groupBuyLock);
        try {
            when(groupBuyLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        when(groupBuyLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void initiate_createsGroupBuyParticipantAndPendingOrder() {
        InitiateGroupBuyDTO dto = new InitiateGroupBuyDTO();
        dto.setProductId(productId);
        dto.setQuantity(2);
        dto.setAddressId(addressId);
        dto.setRequiredCount(3);

        GroupBuyVO result = groupBuyService.initiate(dto);

        assertNotNull(result.getGroupNo());
        assertEquals(1, result.getStatus());
        assertEquals(1, result.getCurrentCount());
        assertEquals(3, result.getRequiredCount());
        assertEquals(productId, result.getProductId());
        assertEquals("高山高麗菜", result.getProductName());
        assertEquals(2, result.getQuantity());
        assertEquals("http://localhost:5173/groupBuy/" + result.getGroupNo(), result.getShareUrl());
        assertEquals(1, result.getParticipants().size());
        assertEquals(memberId, result.getParticipants().get(0).getMemberId());
        assertEquals("揪團測試會員", result.getParticipants().get(0).getMemberName());
        long expireHours = Duration.between(LocalDateTime.now(), result.getExpireAt()).toHours();
        assertTrue(expireHours >= 23 && expireHours <= 24);

        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setUserId(memberId);
        List<Orders> orders = orderMapper.pageQuery(queryDTO);
        assertEquals(1, orders.size());
        Orders preOrder = orders.get(0);
        assertEquals(Orders.PENDING_GROUP, preOrder.getStatus());
        assertEquals(addressId, preOrder.getAddressBookId());
        assertEquals("測試收件人", preOrder.getConsignee());
        assertEquals(new BigDecimal("176.00"), preOrder.getAmount());

        List<OrderDetail> details = orderDetailMapper.getByOrderId(preOrder.getId());
        assertEquals(1, details.size());
        assertEquals(productId, details.get(0).getProductId());
        assertEquals(2, details.get(0).getNumber());
        assertEquals(new BigDecimal("88.00"), details.get(0).getAmount());

        assertEquals(3, productMapper.getById(productId).getStock());
        List<ProductInventoryLog> logs = productInventoryLogMapper.listByProductId(productId);
        assertEquals(1, logs.size());
        assertInventoryLog(logs.get(0), -2, 5, 3, "GROUP_BUY_RESERVE", preOrder.getId(), "MEMBER", memberId);
    }

    @Test
    void initiate_whenProductStockIsNotEnough_throwsBusinessExceptionAndDoesNotCreatePreOrder() {
        InitiateGroupBuyDTO dto = new InitiateGroupBuyDTO();
        dto.setProductId(productId);
        dto.setQuantity(6);
        dto.setAddressId(addressId);
        dto.setRequiredCount(3);

        com.sky.exception.OrderBusinessException exception = Assertions.assertThrows(
                com.sky.exception.OrderBusinessException.class,
                () -> groupBuyService.initiate(dto)
        );
        assertEquals("商品庫存不足", exception.getMessage());
        assertEquals(5, productMapper.getById(productId).getStock());

        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setUserId(memberId);
        assertEquals(0, orderMapper.pageQuery(queryDTO).size());
        assertEquals(0, productInventoryLogMapper.listByProductId(productId).size());
    }

    @Test
    void getByGroupNo_and_listMyGroupBuys_returnParticipants() {
        InitiateGroupBuyDTO dto = new InitiateGroupBuyDTO();
        dto.setProductId(productId);
        dto.setQuantity(1);
        dto.setAddressId(addressId);
        dto.setRequiredCount(3);

        GroupBuyVO initiated = groupBuyService.initiate(dto);

        GroupBuyVO detail = groupBuyService.getByGroupNo(initiated.getGroupNo());
        assertEquals(initiated.getGroupNo(), detail.getGroupNo());
        assertEquals(productId, detail.getProductId());
        assertEquals("高山高麗菜", detail.getProductName());
        assertEquals(1, detail.getQuantity());
        assertEquals("http://localhost:5173/groupBuy/" + initiated.getGroupNo(), detail.getShareUrl());
        assertFalse(detail.getParticipants().isEmpty());
        assertEquals("揪團測試會員", detail.getParticipants().get(0).getMemberName());

        List<GroupBuyVO> myGroupBuys = groupBuyService.listMyGroupBuys();
        assertEquals(1, myGroupBuys.size());
        assertEquals(initiated.getGroupNo(), myGroupBuys.get(0).getGroupNo());
        assertEquals(productId, myGroupBuys.get(0).getProductId());
        assertEquals("http://localhost:5173/groupBuy/" + initiated.getGroupNo(), myGroupBuys.get(0).getShareUrl());
    }

    @Test
    void cancelGroupBuy_marksGroupCanceled_andCancelsPendingOrder() {
        GroupBuyVO initiated = initiateGroupBuyFixture();

        GroupBuyVO canceled = groupBuyService.cancelGroupBuy(initiated.getGroupNo());

        assertEquals(4, canceled.getStatus());
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        assertEquals(4, groupBuy.getStatus());

        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId());
        assertEquals(1, participants.size());
        Orders preOrder = orderMapper.getById(participants.get(0).getPreOrderId());
        assertEquals(Orders.CANCELLED, preOrder.getStatus());
        assertEquals("發起人取消揪團", preOrder.getCancelReason());
        assertNotNull(preOrder.getCancelTime());
        assertEquals(5, productMapper.getById(productId).getStock());

        List<ProductInventoryLog> logs = productInventoryLogMapper.listByProductId(productId);
        assertEquals(2, logs.size());
        assertInventoryLog(logs.get(0), -1, 5, 4, "GROUP_BUY_RESERVE", preOrder.getId(), "MEMBER", memberId);
        assertInventoryLog(logs.get(1), 1, 4, 5, "GROUP_BUY_CANCEL_RESTORE", preOrder.getId(), "MEMBER", memberId);
    }

    @Test
    void cancelGroupBuy_whenNotInitiator_throwsForbidden() {
        GroupBuyVO initiated = initiateGroupBuyFixture();
        Member otherMember = createMember("group-buy-other", "其他會員");

        BaseContext.setCurrentId(otherMember.getId());
        ForbiddenOperationException exception = Assertions.assertThrows(
                ForbiddenOperationException.class,
                () -> groupBuyService.cancelGroupBuy(initiated.getGroupNo())
        );
        assertEquals("只有發起人可以取消揪團", exception.getMessage());
    }

    @Test
    void cancelGroupBuy_whenStatusNotActive_throwsBusinessException() {
        GroupBuyVO initiated = initiateGroupBuyFixture();
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        groupBuy.setStatus(2);
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        com.sky.exception.OrderBusinessException exception = Assertions.assertThrows(
                com.sky.exception.OrderBusinessException.class,
                () -> groupBuyService.cancelGroupBuy(initiated.getGroupNo())
        );
        assertEquals("揪團已結束,無法取消", exception.getMessage());
    }

    @Test
    void cancelGroupBuy_whenAnotherParticipantJoined_throwsBusinessException() {
        GroupBuyVO initiated = initiateGroupBuyFixture();
        Member joiner = createMember("group-buy-joiner", "加入會員");
        Long joinerAddressId = createAddress(joiner.getId(), "加入會員");

        BaseContext.setCurrentId(joiner.getId());
        com.sky.dto.JoinGroupBuyDTO joinGroupBuyDTO = new com.sky.dto.JoinGroupBuyDTO();
        joinGroupBuyDTO.setGroupNo(initiated.getGroupNo());
        joinGroupBuyDTO.setProductId(productId);
        joinGroupBuyDTO.setQuantity(1);
        joinGroupBuyDTO.setAddressId(joinerAddressId);
        groupBuyService.joinGroupBuy(joinGroupBuyDTO);

        BaseContext.setCurrentId(memberId);
        com.sky.exception.OrderBusinessException exception = Assertions.assertThrows(
                com.sky.exception.OrderBusinessException.class,
                () -> groupBuyService.cancelGroupBuy(initiated.getGroupNo())
        );
        assertEquals("已有其他成員加入,無法取消", exception.getMessage());
    }

    @Test
    void cancelGroupBuy_whenGroupNoNotFound_throwsBusinessException() {
        BaseContext.setCurrentId(memberId);
        com.sky.exception.OrderBusinessException exception = Assertions.assertThrows(
                com.sky.exception.OrderBusinessException.class,
                () -> groupBuyService.cancelGroupBuy("NOT_FOUND_GROUP")
        );
        assertEquals("揪團不存在", exception.getMessage());
    }

    @Test
    void handleExpiredGroupBuys_whenPreOrderAlreadyCanceled_doesNotRestoreStockAgain() {
        GroupBuyVO initiated = initiateGroupBuyFixture();
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        GroupBuyParticipant participant = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId()).get(0);

        Orders preOrder = new Orders();
        preOrder.setId(participant.getPreOrderId());
        preOrder.setStatus(Orders.CANCELLED);
        preOrder.setCancelReason("測試預先取消");
        preOrder.setCancelTime(LocalDateTime.now());
        orderMapper.update(preOrder);

        groupBuy.setExpireAt(LocalDateTime.now().minusMinutes(1));
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        groupBuyService.handleExpiredGroupBuys();

        GroupBuy expired = groupBuyMapper.getById(groupBuy.getId());
        assertEquals(3, expired.getStatus());
        assertEquals(4, productMapper.getById(productId).getStock());
        List<ProductInventoryLog> logs = productInventoryLogMapper.listByProductId(productId);
        assertEquals(1, logs.size());
        assertInventoryLog(logs.get(0), -1, 5, 4, "GROUP_BUY_RESERVE", participant.getPreOrderId(), "MEMBER", memberId);
    }

    @Test
    void handleExpiredGroupBuys_cancelsPendingOrdersAndRestoresStock() {
        GroupBuyVO initiated = initiateGroupBuyFixture();
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        GroupBuyParticipant participant = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId()).get(0);

        groupBuy.setExpireAt(LocalDateTime.now().minusMinutes(1));
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        groupBuyService.handleExpiredGroupBuys();

        GroupBuy expired = groupBuyMapper.getById(groupBuy.getId());
        assertEquals(3, expired.getStatus());
        Orders preOrder = orderMapper.getById(participant.getPreOrderId());
        assertEquals(Orders.CANCELLED, preOrder.getStatus());
        assertEquals("揪團逾期未成團", preOrder.getCancelReason());
        assertNotNull(preOrder.getCancelTime());
        assertEquals(5, productMapper.getById(productId).getStock());
        List<ProductInventoryLog> logs = productInventoryLogMapper.listByProductId(productId);
        assertEquals(2, logs.size());
        assertInventoryLog(logs.get(0), -1, 5, 4, "GROUP_BUY_RESERVE", participant.getPreOrderId(), "MEMBER", memberId);
        assertInventoryLog(logs.get(1), 1, 4, 5, "GROUP_BUY_CANCEL_RESTORE", participant.getPreOrderId(), "SYSTEM", null);
    }

    private GroupBuyVO initiateGroupBuyFixture() {
        InitiateGroupBuyDTO dto = new InitiateGroupBuyDTO();
        dto.setProductId(productId);
        dto.setQuantity(1);
        dto.setAddressId(addressId);
        dto.setRequiredCount(3);
        BaseContext.setCurrentId(memberId);
        return groupBuyService.initiate(dto);
    }

    private Member createMember(String openid, String name) {
        Member member = Member.builder()
                .openid(openid)
                .name(name)
                .createTime(LocalDateTime.now())
                .build();
        memberMapper.insert(member);
        return member;
    }

    private Long createAddress(Long ownerId, String consignee) {
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .memberId(ownerId)
                .consignee(consignee)
                .phone("0912345678")
                .cityName("台北市")
                .districtName("信義區")
                .detail("市府路1號")
                .isDefault(1)
                .build();
        shippingAddressMapper.insert(shippingAddress);
        return shippingAddress.getId();
    }

    private void assertInventoryLog(ProductInventoryLog log, int changeQuantity, int stockBefore, int stockAfter,
                                    String reason, Long orderId, String operatorType, Long operatorId) {
        assertEquals(changeQuantity, log.getChangeQuantity());
        assertEquals(stockBefore, log.getStockBefore());
        assertEquals(stockAfter, log.getStockAfter());
        assertEquals(reason, log.getReason());
        assertEquals("ORDER", log.getReferenceType());
        assertEquals(orderId, log.getReferenceId());
        assertEquals(operatorType, log.getOperatorType());
        assertEquals(operatorId, log.getOperatorId());
    }
}
