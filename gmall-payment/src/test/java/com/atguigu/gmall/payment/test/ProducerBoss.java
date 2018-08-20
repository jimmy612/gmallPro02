package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerBoss {

    public static void main(String[] args) {

        // 生成某一个地址下的连接池
        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try {
            //  建立mq的连接
            Connection connection = connect.createConnection();
            connection.start();

            // 通过连接创建一次于mq的回话任务
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue testqueue = session.createQueue("DAOSHUI");

            // 通过mq的回话任务将队列消息发送出去
            MessageProducer producer = session.createProducer(testqueue);
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("boss渴了，要喝水。。。");
            MapMessage mapMessage = new ActiveMQMapMessage();
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(textMessage);

            // 提交任务
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
