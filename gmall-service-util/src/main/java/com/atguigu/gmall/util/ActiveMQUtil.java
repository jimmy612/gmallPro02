package com.atguigu.gmall.util;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.ServerSession;

/**
 * @param
 * @return
 */
public class ActiveMQUtil {

    PooledConnectionFactory pooledConnectionFactory=null;


    public  void init(String brokerUrl){
        ActiveMQConnectionFactory activeMQConnectionFactory=new ActiveMQConnectionFactory(brokerUrl);
        pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);
        pooledConnectionFactory.setExpiryTimeout(2000);
        pooledConnectionFactory.setMaximumActiveSessionPerConnection(10);
        pooledConnectionFactory.setMaxConnections(30);
        pooledConnectionFactory.setReconnectOnException(true);
        System.out.println("初始化mq连接池");
    }

    public Connection getConnection(){

        Connection connection = null;
        try {
            connection = pooledConnectionFactory.createConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return connection;
    }




}
