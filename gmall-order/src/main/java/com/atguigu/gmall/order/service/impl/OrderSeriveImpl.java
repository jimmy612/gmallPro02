package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.service.OrderService
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.UUID;

/**
 * @author Jimmy Hao
 * 2018-08-15 21:18
 */

@Service
public class OrderSeriveImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Override
    public String genTradeCode(String userId) {

        //生成随机的code
        String k = "user:" + userId + ":tradeCode";
        String v = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(k,60*30,v);
        jedis.close();

        return v;
    }




}
