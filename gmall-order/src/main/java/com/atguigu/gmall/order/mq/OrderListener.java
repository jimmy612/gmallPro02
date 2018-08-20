package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.*;


@Component
public class OrderListener {

    @Autowired
    OrderService orderService;

    @JmsListener(containerFactory = "jmsQueueListener", destination = "PAYMENT_SUCCESS_QUEUE")
    public void consumePaymentSuccess(MapMessage mapMessage) throws JMSException {

        String outTradeNo = mapMessage.getString("outTradeNo");

        String trackingNo = mapMessage.getString("trackingNo");

       OrderInfo orderInfo =  new OrderInfo();

       orderInfo.setOutTradeNo(outTradeNo);
       orderInfo.setTrackingNo(trackingNo);
       orderInfo.setOrderStatus("已支付");

       orderService.updateOrderStatus(orderInfo);

       orderService.sendOrderResultQueue(outTradeNo);

    }
}
