package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

/**
 * @author Jimmy Hao
 * 2018-08-15 21:05
 */
public interface OrderService {
    String genTradeCode(String userId);

    boolean checkTradeCode(String tradeCode, String userId);

    String saveOrder(OrderInfo orderInfo);

    OrderInfo getOrderById(String orderId);

    void updateOrderStatus(OrderInfo outTradeNo);

    void sendOrderResultQueue(String outTradeNo);
}
