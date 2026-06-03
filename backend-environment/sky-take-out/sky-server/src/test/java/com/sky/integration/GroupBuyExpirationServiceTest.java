package com.sky.integration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sky.context.BaseContext;
import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.JoinGroupBuyDTO;
import com.sky.entity.GroupBuy;
import com.sky.entity.GroupBuyParticipant;
import com.sky.entity.Member;
import com.sky.entity.Orders;
import com.sky.entity.Product;
import com.sky.entity.ShippingAddress;
import com.sky.integration.support.RedisContainerTestBase;
import com.sky.mapper.GroupBuyMapper;
import com.sky.mapper.GroupBuyParticipantMapper;
import com.sky.mapper.MemberMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.service.GroupBuyService;
import com.sky.service.impl.GroupBuyServiceImpl;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.GroupBuyVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class GroupBuyExpirationServiceTest extends RedisContainerTestBase {

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyParticipantMapper groupBuyParticipantMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedissonClient redissonClient;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    @MockitoBean
    private WeChatPayUtil weChatPayUtil;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void handleExpiredGroupBuys_marksFailedCancelsOrders_andLogsRefund() {
        Fixture fixture = createExpiredGroupBuyFixture();

        Logger logger = (Logger) LoggerFactory.getLogger(GroupBuyServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            groupBuyService.handleExpiredGroupBuys();
        } finally {
            logger.detachAppender(appender);
        }

        GroupBuy expiredGroupBuy = groupBuyMapper.getByGroupNo(fixture.groupNo);
        assertEquals(3, expiredGroupBuy.getStatus());

        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(expiredGroupBuy.getId());
        for (GroupBuyParticipant participant : participants) {
            Orders order = orderMapper.getById(participant.getPreOrderId());
            assertEquals(Orders.CANCELLED, order.getStatus());
        }

        List<String> logMessages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertTrue(logMessages.stream().anyMatch(message ->
                message.contains("揪團失敗退款: groupNo=" + fixture.groupNo)
                        && message.contains("memberId=" + fixture.initiatorId)
                        && message.contains("amount=120.00")));
        assertTrue(logMessages.stream().anyMatch(message ->
                message.contains("揪團失敗退款: groupNo=" + fixture.groupNo)
                        && message.contains("memberId=" + fixture.joinerId)
                        && message.contains("amount=120.00")));
    }

    @Test
    void handleExpiredGroupBuys_skipsWhenLockHeld() throws Exception {
        Fixture fixture = createExpiredGroupBuyFixture();
        RLock lock = redissonClient.getLock("lock:groupbuy:" + fixture.groupNo);
        Logger logger = (Logger) LoggerFactory.getLogger(GroupBuyServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            lock.lock();
            try {
                locked.countDown();
                release.await(5, TimeUnit.SECONDS);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            return null;
        });

        assertTrue(locked.await(5, TimeUnit.SECONDS));
        try {
            groupBuyService.handleExpiredGroupBuys();
        } finally {
            release.countDown();
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            logger.detachAppender(appender);
        }

        GroupBuy expiredGroupBuy = groupBuyMapper.getByGroupNo(fixture.groupNo);
        assertEquals(1, expiredGroupBuy.getStatus());

        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(expiredGroupBuy.getId());
        for (GroupBuyParticipant participant : participants) {
            Orders order = orderMapper.getById(participant.getPreOrderId());
            assertEquals(Orders.PENDING_GROUP, order.getStatus());
        }
        List<String> logMessages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertTrue(logMessages.stream().anyMatch(message -> message.contains("略過過期揪團")));
    }

    private Fixture createExpiredGroupBuyFixture() {
        Member initiator = createMember("expired-initiator", "過期發起人");
        Long initiatorAddressId = createAddress(initiator.getId(), "過期發起人");

        Product product = Product.builder()
                .productName("有機青菜")
                .categoryId(1L)
                .price(new BigDecimal("120.00"))
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        productMapper.insert(product);

        BaseContext.setCurrentId(initiator.getId());
        InitiateGroupBuyDTO initiateGroupBuyDTO = new InitiateGroupBuyDTO();
        initiateGroupBuyDTO.setProductId(product.getId());
        initiateGroupBuyDTO.setQuantity(1);
        initiateGroupBuyDTO.setAddressId(initiatorAddressId);
        initiateGroupBuyDTO.setRequiredCount(3);
        GroupBuyVO initiated = groupBuyService.initiate(initiateGroupBuyDTO);

        Member joiner = createMember("expired-joiner", "過期參與人");
        Long joinerAddressId = createAddress(joiner.getId(), "過期參與人");
        BaseContext.setCurrentId(joiner.getId());
        JoinGroupBuyDTO joinGroupBuyDTO = new JoinGroupBuyDTO();
        joinGroupBuyDTO.setGroupNo(initiated.getGroupNo());
        joinGroupBuyDTO.setProductId(product.getId());
        joinGroupBuyDTO.setQuantity(1);
        joinGroupBuyDTO.setAddressId(joinerAddressId);
        groupBuyService.joinGroupBuy(joinGroupBuyDTO);

        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        groupBuy.setExpireAt(LocalDateTime.now().minusMinutes(1));
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);
        BaseContext.removeCurrentId();

        return new Fixture(groupBuy.getGroupNo(), initiator.getId(), joiner.getId());
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

    private Long createAddress(Long memberId, String consignee) {
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .memberId(memberId)
                .consignee(consignee)
                .phone("0912345678")
                .cityName("台南市")
                .districtName("中西區")
                .detail("民生路一段1號")
                .isDefault(1)
                .build();
        shippingAddressMapper.insert(shippingAddress);
        return shippingAddress.getId();
    }

    private static final class Fixture {
        private final String groupNo;
        private final Long initiatorId;
        private final Long joinerId;

        private Fixture(String groupNo, Long initiatorId, Long joinerId) {
            this.groupNo = groupNo;
            this.initiatorId = initiatorId;
            this.joinerId = joinerId;
        }
    }
}
