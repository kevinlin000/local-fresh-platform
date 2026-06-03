package com.sky.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.support.OrderStatusTransitionPolicy;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sky.service.support.OrderStatusTransitionPolicy.Transition;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShippingAddressMapper  shippingAddressMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private GiftBoxProductMapper giftBoxProductMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用戶下單
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        //1. 處理各種業務異常（地址簿為空，購物車數據為空）
        ShippingAddress addressBook = shippingAddressMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null || !userId.equals(addressBook.getMemberId())) {
            //拋出業務異常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 檢查使用者的收貨地址是否超出配送範圍
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        Cart shoppingCart = new Cart();
        shoppingCart.setUserId(userId);
        List<Cart> shoppingCartlist = cartMapper.list(shoppingCart);
        if (shoppingCartlist == null || shoppingCartlist.size() == 0) {
            //拋出業務異常
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        reserveProductStock(shoppingCartlist);

        //2. 向訂單表插入1條數據
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(IdUtil.getSnowflakeNextId()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3. 向訂單明細插入n條數據
        for (Cart cart : shoppingCartlist) {
            OrderDetail orderDetail = new OrderDetail(); //訂單明細
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); //設置當前訂單明細關聯的訂單id
            orderDetailList.add(orderDetail);

        }

        orderDetailMapper.insertBatch(orderDetailList);
        
        //4. 清空當前用戶的購物車數據
        cartMapper.deleteByUserId(userId);

        //5. 封裝VO並且返回結果

        OrderSubmitVO orderSubmitVO= OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null || !userId.equals(ordersDB.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Orders.UN_PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.PAY);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改
        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        //发现没有将支付时间 check_out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();
        //获取订单号码
        String orderNumber = ordersDB.getNumber();
        log.info("调用updateStatus，用于替换微信支付更新数据库状态的问题");
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderNumber);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.PAY);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通過websocket向客戶端推送訊息
        Map map = new HashMap();
        map.put("type",1);  // 1表示來單提醒，2表示客戶催單
        map.put("orderId", ordersDB.getId());
        map.put("content","訂單號：" + outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);


    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情（管理端，不驗證所有權）
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 查詢訂單詳情（用戶端，驗證訂單所有權）
     *
     * @param id
     * @return
     */
    public OrderVO userDetails(Long id) {
        Orders orders = orderMapper.getById(id);

        if (orders == null || !orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.USER_CANCEL);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    ordersDB.getAmount(),//退款金额，单位 元
                    ordersDB.getAmount());//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
        restoreProductStock(ordersDB.getId());
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        // 驗證訂單所有權
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getUserId().equals(userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<Cart> shoppingCartList = orderDetailList.stream().map(x -> {
            Cart shoppingCart = new Cart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        cartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }


    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_CONFIRM);

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
        restoreProductStock(ordersDB.getId());
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_REJECT);

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_CANCEL);

        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
        restoreProductStock(ordersDB.getId());
    }

    private void reserveProductStock(List<Cart> shoppingCartList) {
        for (Cart cart : shoppingCartList) {
            if (cart.getProductId() != null) {
                decreaseProductStock(cart.getProductId(), cart.getNumber());
                continue;
            }
            if (cart.getGiftBoxId() != null) {
                reserveGiftBoxStock(cart.getGiftBoxId(), cart.getNumber());
            }
        }
    }

    private void restoreProductStock(Long orderId) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
        for (OrderDetail orderDetail : orderDetailList) {
            if (orderDetail.getProductId() != null && orderDetail.getNumber() != null && orderDetail.getNumber() > 0) {
                productMapper.increaseStock(orderDetail.getProductId(), orderDetail.getNumber());
            }
            if (orderDetail.getGiftBoxId() != null && orderDetail.getNumber() != null && orderDetail.getNumber() > 0) {
                restoreGiftBoxStock(orderDetail.getGiftBoxId(), orderDetail.getNumber());
            }
        }
    }

    private void reserveGiftBoxStock(Long giftBoxId, Integer giftBoxQuantity) {
        List<GiftBoxProduct> giftBoxProducts = giftBoxProductMapper.getBySetmealId(giftBoxId);
        if (CollectionUtils.isEmpty(giftBoxProducts)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        for (GiftBoxProduct giftBoxProduct : giftBoxProducts) {
            int requiredQuantity = giftBoxQuantity * giftBoxProduct.getCopies();
            decreaseProductStock(giftBoxProduct.getProductId(), requiredQuantity);
        }
    }

    private void restoreGiftBoxStock(Long giftBoxId, Integer giftBoxQuantity) {
        List<GiftBoxProduct> giftBoxProducts = giftBoxProductMapper.getBySetmealId(giftBoxId);
        for (GiftBoxProduct giftBoxProduct : giftBoxProducts) {
            int restoredQuantity = giftBoxQuantity * giftBoxProduct.getCopies();
            productMapper.increaseStock(giftBoxProduct.getProductId(), restoredQuantity);
        }
    }

    private void decreaseProductStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }
        int updatedRows = productMapper.decreaseStock(productId, quantity);
        if (updatedRows == 0) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.START_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.COMPLETE_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客戶催單
     * @param id
     */
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);

        // 驗證訂單存在且屬於當前用戶
        if (ordersDB == null || !ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 通過websocket向客戶端推送催單訊息
        Map map = new HashMap();
        map.put("type",2);  // 1表示來單提醒，2表示客戶催單
        map.put("orderId", id);
        map.put("content","訂單號：" + ordersDB.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.google.api-key}")
    private String apiKey;

    @Value("${sky.delivery.range-check-enabled:true}")
    private boolean deliveryRangeCheckEnabled;

    /**
     * 檢查客戶的收貨地址是否超出配送範圍 (使用 Google Maps API)
     * @param address 客戶收貨地址
     */
    private void checkOutOfRange(String address) {
        if (!deliveryRangeCheckEnabled) {
            return;
        }

        // 1. 取得店家的經緯度 (Geocoding API)
        String shopLngLat = getCoordinate(shopAddress);
        if (shopLngLat == null) {
            throw new OrderBusinessException("店家地址解析失敗");
        }

        // 2. 取得用戶收貨地址的經緯度 (Geocoding API)
        String userLngLat = getCoordinate(address);
        if (userLngLat == null) {
            throw new OrderBusinessException("收貨地址解析失敗");
        }

        // 3. 路線規劃與距離計算 (Distance Matrix API)
        Map<String, String> map = new HashMap<>();
        map.put("origins", shopLngLat);
        map.put("destinations", userLngLat);
        map.put("key", apiKey);

        // 台灣外送通常算機車或開車距離
        String json = HttpClientUtil.doGet("https://maps.googleapis.com/maps/api/distancematrix/json", map);
        JSONObject jsonObject = JSON.parseObject(json);

        // 檢查 Google API 狀態
        if (!"OK".equals(jsonObject.getString("status"))) {
            throw new OrderBusinessException("配送路線規劃失敗");
        }

        // 數據解析
        JSONArray rows = jsonObject.getJSONArray("rows");
        if (rows.isEmpty()) {
            throw new OrderBusinessException("無法計算配送距離");
        }

        JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
        JSONObject element = elements.getJSONObject(0);

        if (!"OK".equals(element.getString("status"))) {
            throw new OrderBusinessException("該地址無法送達（可能跨海或無道路）");
        }

        // 取得距離 (單位：公尺)
        Integer distance = element.getJSONObject("distance").getInteger("value");

        // 判斷是否超過 5000 公尺
        if(distance > 5000){
            throw new OrderBusinessException("超出配送範圍");
        }
    }

    /**
     * 輔助方法：呼叫 Google Geocoding API 將地址轉為 "lat,lng" 格式字串
     */
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
