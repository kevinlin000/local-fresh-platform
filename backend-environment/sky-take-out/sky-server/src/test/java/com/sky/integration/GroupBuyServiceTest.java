package com.sky.integration;

import com.sky.context.BaseContext;
import com.sky.dto.InitiateGroupBuyDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Member;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.Product;
import com.sky.entity.ShippingAddress;
import com.sky.mapper.MemberMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.service.GroupBuyService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.GroupBuyVO;
import com.sky.websocket.WebSocketServer;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private ShippingAddressMapper shippingAddressMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @MockBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockBean
    private WebSocketServer webSocketServer;

    @MockBean
    private WeChatPayUtil weChatPayUtil;

    private Long memberId;
    private Long productId;
    private Long addressId;

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
}
