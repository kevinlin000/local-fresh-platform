package com.localfresh.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.OrdersSubmitDTO;
import com.localfresh.entity.Cart;
import com.localfresh.entity.GiftBoxProduct;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.exception.AddressBookBusinessException;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.CartMapper;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.ShippingAddressMapper;
import com.localfresh.service.InventoryService;
import com.localfresh.service.OrderSubmissionService;
import com.localfresh.utils.HttpClientUtil;
import com.localfresh.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderSubmissionServiceImpl implements OrderSubmissionService {

    private static final String INVENTORY_REASON_ORDER_RESERVE = "ORDER_RESERVE";
    private static final String INVENTORY_OPERATOR_MEMBER = "MEMBER";
    private static final int MAX_DELIVERY_DISTANCE_METERS = 5000;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GiftBoxProductMapper giftBoxProductMapper;

    @Autowired
    private InventoryService inventoryService;

    @Value("${localfresh.shop.address}")
    private String shopAddress;

    @Value("${localfresh.google.api-key}")
    private String apiKey;

    @Value("${localfresh.delivery.range-check-enabled:true}")
    private boolean deliveryRangeCheckEnabled;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        ShippingAddress addressBook = shippingAddressMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null || !userId.equals(addressBook.getMemberId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        String deliveryAddress = addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
        checkOutOfRange(deliveryAddress);

        Cart shoppingCart = new Cart();
        shoppingCart.setUserId(userId);
        List<Cart> shoppingCartList = cartMapper.list(shoppingCart);
        if (CollectionUtils.isEmpty(shoppingCartList)) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders, "packAmount", "tablewareNumber");
        orders.setPackAmount(defaultToZero(ordersSubmitDTO.getPackAmount()));
        orders.setTablewareNumber(defaultToZero(ordersSubmitDTO.getTablewareNumber()));
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(IdUtil.getSnowflakeNextId()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(deliveryAddress);
        orders.setUserId(userId);

        orderMapper.insert(orders);
        reserveProductStock(shoppingCartList, orders.getId(), userId);
        orderDetailMapper.insertBatch(buildOrderDetails(shoppingCartList, orders.getId()));
        cartMapper.deleteByUserId(userId);

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    private List<OrderDetail> buildOrderDetails(List<Cart> shoppingCartList, Long orderId) {
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (Cart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orderId);
            orderDetailList.add(orderDetail);
        }
        return orderDetailList;
    }

    private int defaultToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void reserveProductStock(List<Cart> shoppingCartList, Long orderId, Long userId) {
        for (Cart cart : shoppingCartList) {
            if (cart.getProductId() != null) {
                inventoryService.reserveProduct(cart.getProductId(), cart.getNumber(),
                        INVENTORY_REASON_ORDER_RESERVE, orderId, INVENTORY_OPERATOR_MEMBER, userId);
                continue;
            }
            if (cart.getGiftBoxId() != null) {
                reserveGiftBoxStock(cart.getGiftBoxId(), cart.getNumber(), orderId, userId);
            }
        }
    }

    private void reserveGiftBoxStock(Long giftBoxId, Integer giftBoxQuantity, Long orderId, Long userId) {
        List<GiftBoxProduct> giftBoxProducts = giftBoxProductMapper.getBySetmealId(giftBoxId);
        if (CollectionUtils.isEmpty(giftBoxProducts)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        for (GiftBoxProduct giftBoxProduct : giftBoxProducts) {
            int requiredQuantity = giftBoxQuantity * giftBoxProduct.getCopies();
            inventoryService.reserveProduct(giftBoxProduct.getProductId(), requiredQuantity,
                    INVENTORY_REASON_ORDER_RESERVE, orderId, INVENTORY_OPERATOR_MEMBER, userId);
        }
    }

    private void checkOutOfRange(String address) {
        if (!deliveryRangeCheckEnabled) {
            return;
        }

        String shopLngLat = getCoordinate(shopAddress);
        if (shopLngLat == null) {
            throw new OrderBusinessException("店家地址解析失敗");
        }

        String userLngLat = getCoordinate(address);
        if (userLngLat == null) {
            throw new OrderBusinessException("配送地址解析失敗");
        }

        Map<String, String> map = new HashMap<>();
        map.put("origins", shopLngLat);
        map.put("destinations", userLngLat);
        map.put("key", apiKey);

        String json = HttpClientUtil.doGet("https://maps.googleapis.com/maps/api/distancematrix/json", map);
        JSONObject jsonObject = JSON.parseObject(json);

        if (!"OK".equals(jsonObject.getString("status"))) {
            throw new OrderBusinessException("配送路線規劃失敗");
        }

        JSONArray rows = jsonObject.getJSONArray("rows");
        if (rows.isEmpty()) {
            throw new OrderBusinessException("無法計算配送距離");
        }

        JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
        JSONObject element = elements.getJSONObject(0);

        if (!"OK".equals(element.getString("status"))) {
            throw new OrderBusinessException("該地址無法送達（可能跨海或無道路）");
        }

        Integer distance = element.getJSONObject("distance").getInteger("value");
        if (distance > MAX_DELIVERY_DISTANCE_METERS) {
            throw new OrderBusinessException("超出配送範圍");
        }
    }

    private String getCoordinate(String address) {
        Map<String, String> map = new HashMap<>();
        map.put("address", address);
        map.put("key", apiKey);

        String json = HttpClientUtil.doGet("https://maps.googleapis.com/maps/api/geocode/json", map);
        JSONObject jsonObject = JSON.parseObject(json);

        if ("OK".equals(jsonObject.getString("status"))) {
            JSONObject location = jsonObject.getJSONArray("results")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONObject("location");
            String lat = location.getString("lat");
            String lng = location.getString("lng");
            return lat + "," + lng;
        }
        return null;
    }
}
