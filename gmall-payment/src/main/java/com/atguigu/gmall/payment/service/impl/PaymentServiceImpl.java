package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;

/**
 * @author Jimmy Hao
 * 2018-08-18 21:06
 */

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePayment(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public void sendPaymentSuccessQueue(String tradeNo, String outTradeNo, String callbackContent) {
        //修改支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setAlipayTradeNo(tradeNo);
        paymentInfo.setCallbackContent(callbackContent);
        paymentInfo.setPaymentStatus("已支付");
        paymentInfo.setOutTradeNo(outTradeNo);//此处应该是不用再设置outTradeNo了
        updatePaymentInfo(paymentInfo);

        try {
            //1.建立连接
            Connection connection = activeMQUtil.getConnection();
            connection.start();

            //2.创建会话任务,参数1表示开启事务，参数2相当于数字0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //3.根据会话创建队列
            Queue testqueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");

            //4.通过会话任务将队列消息发送出去
            MessageProducer producer = session.createProducer(testqueue);

            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("trackingNo",tradeNo);
            mapMessage.setString("outTradeNo",outTradeNo);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(mapMessage);

            session.commit();
            connection.close();

        }catch (JMSException e){
            e.printStackTrace();
        }

    }

    private void updatePaymentInfo(PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    /**
     * 支付服务给支付宝发送检查消息的队列，未写完
     * @param outTradeNo
     * @param count
     */
    @Override
    public void sendPaymentCheckQueue(String outTradeNo, int count) {
        try {
            //1.建立连接
            Connection connection = activeMQUtil.getConnection();
            connection.start();

            //2.创建会话任务,参数1表示开启事务，参数2相当于数字0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //3.根据会话创建队列
            Queue testqueue = session.createQueue("PAYMENT_CHECK_QUEUE");

            //4.通过会话任务将队列消息发送出去
            MessageProducer producer = session.createProducer(testqueue);

            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setInt("count",count);
            mapMessage.setString("outTradeNo", outTradeNo);
            mapMessage.setProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000*60);

            producer.send(mapMessage);
            session.commit();
            connection.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("发送第"+(6-count)+"次的消息队列。。");
    }

    @Override
    public HashMap<String, String> checkPayment(String outTradeNo) {

        HashMap<String, String> stringStringHashMap = new HashMap<>();

        AlipayTradeQueryRequest alipayTradeQueryRequest = new AlipayTradeQueryRequest();
        HashMap<String,Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("out_trade_no", outTradeNo);
        String s = JSON.toJSONString(stringObjectHashMap);
        alipayTradeQueryRequest.setBizContent(s);

        AlipayTradeQueryResponse response = null;

        try {
           response = alipayClient.execute(alipayTradeQueryRequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(response.isSuccess()){
            String status = response.getTradeStatus();
            String callbackContent = response.getBody();
            String tradeNo = response.getTradeNo();

            stringStringHashMap.put("status",status);
            stringStringHashMap.put("callbackContent",callbackContent);
            stringStringHashMap.put("tradeNo",tradeNo);
        }else{
            System.out.println("用户未扫码ma");
        }

        return stringStringHashMap;
    }

    /**
     * 通过查询数据库支付结果判定是否支付成功
     * @param outTradeNo
     * @return
     */
    @Override
    public boolean checkPaied(String outTradeNo) {
        boolean b = false;
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo1 = paymentInfoMapper.selectOne(paymentInfo);
        if(paymentInfo1!=null&&paymentInfo1.getPaymentStatus().equals("已支付")){
            b = true;
        }
        return b;
    }
}
