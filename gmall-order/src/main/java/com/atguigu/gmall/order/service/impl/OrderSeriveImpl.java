package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

/**
 * @author Jimmy Hao
 * 2018-08-15 21:18
 */

@Service
public class OrderSeriveImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String genTradeCode(String userId) {

        //生成随机的code
        String k = "user:" + userId + ":tradeCode";
        String v = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(k, 60 * 30, v);
        jedis.close();

        return v;
    }

    /**
     * 比较交易码，相等后删除交易码
     *
     * @param tradeCode
     * @param userId
     * @return
     */
    @Override
    public boolean checkTradeCode(String tradeCode, String userId) {
        boolean b = false;
        Jedis jedis = redisUtil.getJedis();
        String cacheCode = jedis.get("user:" + userId + ":tradeCode");
        if (tradeCode.equals(cacheCode)) {
            b = true;
            jedis.del(cacheCode);
        }
        return b;
    }

    @Override
    public String saveOrder(OrderInfo orderInfo) {

        orderInfoMapper.insertSelective(orderInfo);

        String orderId = orderInfo.getId();

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        for (OrderDetail orderDetail : orderDetailList) {

            orderDetail.setOrderId(orderId);

            orderDetailMapper.insertSelective(orderDetail);

        }

        return orderId;
    }

    @Override
    public OrderInfo getOrderById(String orderId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        OrderInfo orderInfo1 = orderInfoMapper.selectOne(orderInfo);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        orderInfo1.setOrderDetailList(orderDetails);
        return orderInfo1;
    }

    @Override
    public void updateOrderStatus(OrderInfo orderInfo) {

        Example e = new Example(OrderInfo.class);

        e.createCriteria().andEqualTo("outTradeNo", orderInfo.getOutTradeNo());

        orderInfoMapper.updateByExampleSelective(orderInfo,e);

    }

    @Override
    public void sendOrderResultQueue(String outTradeNo) {

        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(queue);

            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOutTradeNo(outTradeNo);
            OrderInfo info = orderInfoMapper.selectOne(orderInfo);
            String orderInfoMessage = JSON.toJSONString(info);

            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderInfoMessage", orderInfoMessage);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(activeMQMapMessage);
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
