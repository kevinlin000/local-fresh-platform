package com.sky.integration;

import com.sky.context.BaseContext;
import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.JoinGroupBuyDTO;
import com.sky.entity.GroupBuy;
import com.sky.entity.GroupBuyParticipant;
import com.sky.entity.Member;
import com.sky.entity.Orders;
import com.sky.entity.Product;
import com.sky.entity.ShippingAddress;
import com.sky.exception.OrderBusinessException;
import com.sky.integration.support.RedisContainerTestBase;
import com.sky.mapper.GroupBuyMapper;
import com.sky.mapper.GroupBuyParticipantMapper;
import com.sky.mapper.MemberMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.service.GroupBuyService;
import com.sky.vo.GroupBuyVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupBuyRedisIntegrationTest extends RedisContainerTestBase {

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

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    private Long initiatorId;
    private Long productId;
    private Long initiatorAddressId;

    @BeforeEach
    void setUp() {
        Member initiator = Member.builder()
                .openid("redis-group-initiator")
                .name("發起會員")
                .createTime(LocalDateTime.now())
                .build();
        memberMapper.insert(initiator);
        initiatorId = initiator.getId();

        Product product = Product.builder()
                .productName("小農番茄")
                .categoryId(1L)
                .price(new BigDecimal("120.00"))
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        productMapper.insert(product);
        productId = product.getId();

        initiatorAddressId = createAddress(initiatorId, "發起會員");
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void joinGroupBuy_succeeds_forNewParticipant() {
        GroupBuyVO initiated = initiateGroupBuy(3);
        Member joiner = createMember("joiner-1", "加入會員一");
        Long joinerAddressId = createAddress(joiner.getId(), "加入會員一");

        BaseContext.setCurrentId(joiner.getId());
        GroupBuyVO joined = groupBuyService.joinGroupBuy(buildJoinDTO(initiated.getGroupNo(), joinerAddressId));

        assertEquals(2, joined.getCurrentCount());
        assertEquals(2, joined.getParticipants().size());
    }

    @Test
    void joinGroupBuy_rejects_duplicateMember() {
        GroupBuyVO initiated = initiateGroupBuy(3);
        Member joiner = createMember("joiner-dup", "加入會員二");
        Long joinerAddressId = createAddress(joiner.getId(), "加入會員二");

        BaseContext.setCurrentId(joiner.getId());
        groupBuyService.joinGroupBuy(buildJoinDTO(initiated.getGroupNo(), joinerAddressId));

        OrderBusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
                OrderBusinessException.class,
                () -> groupBuyService.joinGroupBuy(buildJoinDTO(initiated.getGroupNo(), joinerAddressId))
        );
        assertEquals("您已加入此揪團", exception.getMessage());
    }

    @Test
    void joinGroupBuy_rejects_expiredGroup() {
        GroupBuyVO initiated = initiateGroupBuy(3);
        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        groupBuy.setExpireAt(LocalDateTime.now().minusMinutes(1));
        groupBuy.setUpdatedAt(LocalDateTime.now());
        groupBuyMapper.update(groupBuy);

        Member joiner = createMember("joiner-expired", "加入會員三");
        Long joinerAddressId = createAddress(joiner.getId(), "加入會員三");

        BaseContext.setCurrentId(joiner.getId());
        OrderBusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
                OrderBusinessException.class,
                () -> groupBuyService.joinGroupBuy(buildJoinDTO(initiated.getGroupNo(), joinerAddressId))
        );
        assertEquals("揪團已過期", exception.getMessage());
    }

    @Test
    void joinGroupBuy_completesGroup_whenRequiredCountReached() {
        GroupBuyVO initiated = initiateGroupBuy(2);
        Member joiner = createMember("joiner-complete", "成團會員");
        Long joinerAddressId = createAddress(joiner.getId(), "成團會員");

        BaseContext.setCurrentId(joiner.getId());
        GroupBuyVO joined = groupBuyService.joinGroupBuy(buildJoinDTO(initiated.getGroupNo(), joinerAddressId));

        assertEquals(2, joined.getCurrentCount());
        assertEquals(2, joined.getStatus());

        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        assertEquals(2, groupBuy.getStatus());

        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId());
        assertEquals(2, participants.size());
        for (GroupBuyParticipant participant : participants) {
            Orders order = orderMapper.getById(participant.getPreOrderId());
            assertEquals(Orders.TO_BE_CONFIRMED, order.getStatus());
        }

        verify(webSocketServer, atLeastOnce()).sendToAllClient(anyString());
    }

    @Test
    void joinGroupBuy_concurrency_doesNotExceedRequiredCount() throws Exception {
        GroupBuyVO initiated = initiateGroupBuy(3);
        List<MemberJoinData> joiners = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Member member = createMember("concurrency-" + i, "並發會員" + i);
            joiners.add(new MemberJoinData(member.getId(), createAddress(member.getId(), "並發會員" + i)));
        }

        CountDownLatch ready = new CountDownLatch(joiners.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(joiners.size());
        ExecutorService executorService = Executors.newFixedThreadPool(24);
        AtomicInteger successCount = new AtomicInteger();
        List<String> errors = new CopyOnWriteArrayList<>();

        for (MemberJoinData joiner : joiners) {
            executorService.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    BaseContext.setCurrentId(joiner.memberId);
                    groupBuyService.joinGroupBuy(buildJoinDTO(initiated.getGroupNo(), joiner.addressId));
                    successCount.incrementAndGet();
                } catch (OrderBusinessException e) {
                    errors.add(e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add("interrupted");
                } finally {
                    BaseContext.removeCurrentId();
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(20, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        GroupBuy groupBuy = groupBuyMapper.getByGroupNo(initiated.getGroupNo());
        List<GroupBuyParticipant> participants = groupBuyParticipantMapper.listByGroupBuyId(groupBuy.getId());

        assertEquals(3, groupBuy.getCurrentCount());
        assertEquals(3, participants.size());
        assertEquals(2, groupBuy.getStatus());
        assertEquals(2, successCount.get());
        assertEquals(98, errors.size());
        assertTrue(errors.stream().allMatch(message ->
                "揪團人數已滿".equals(message)
                        || "揪團狀態錯誤".equals(message)
                        || "系統繁忙，請稍後再試".equals(message)));
    }

    private GroupBuyVO initiateGroupBuy(int requiredCount) {
        BaseContext.setCurrentId(initiatorId);
        InitiateGroupBuyDTO dto = new InitiateGroupBuyDTO();
        dto.setProductId(productId);
        dto.setQuantity(1);
        dto.setAddressId(initiatorAddressId);
        dto.setRequiredCount(requiredCount);
        return groupBuyService.initiate(dto);
    }

    private JoinGroupBuyDTO buildJoinDTO(String groupNo, Long addressId) {
        JoinGroupBuyDTO dto = new JoinGroupBuyDTO();
        dto.setGroupNo(groupNo);
        dto.setProductId(productId);
        dto.setQuantity(1);
        dto.setAddressId(addressId);
        return dto;
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
                .cityName("台中市")
                .districtName("西屯區")
                .detail("福星路100號")
                .isDefault(1)
                .build();
        shippingAddressMapper.insert(shippingAddress);
        return shippingAddress.getId();
    }

    private static final class MemberJoinData {
        private final Long memberId;
        private final Long addressId;

        private MemberJoinData(Long memberId, Long addressId) {
            this.memberId = memberId;
            this.addressId = addressId;
        }
    }
}
