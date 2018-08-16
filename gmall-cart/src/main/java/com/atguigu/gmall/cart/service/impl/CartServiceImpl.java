package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jimmy Hao
 * 2018-08-12 16:07
 */

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<CartInfo> getCartInfosFromDbByUserId(String userId) {
        CartInfo cartInfo1 = new CartInfo();
        cartInfo1.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo1);
        return cartInfos;
    }

    @Override
    public void saveCart(CartInfo cartInfo) {

        cartInfoMapper.insertSelective(cartInfo);

    }

    @Override
    public void update(CartInfo info) {

        cartInfoMapper.updateByPrimaryKeySelective(info);
    }

    @Override
    public void syncCache(String userId) {

        Jedis jedis = redisUtil.getJedis();
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfoList = cartInfoMapper.select(cartInfo);
        Map<String, String> map = new HashMap<>();
        for (CartInfo info : cartInfoList) {
            map.put(info.getId(), JSON.toJSONString(info));
        }
        jedis.hmset("cart:" + userId + ":info", map);
    }

    @Override
    public List<CartInfo> getCartCache(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();

        List<String> hvals = jedis.hvals("cart:" + userId + ":info");

        if (hvals != null && hvals.size() > 0) {
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }
        return cartInfos;
    }

    @Override
    public void updateCartChecked(CartInfo cartInfo) {
        Example e = new Example(CartInfo.class);
        e.createCriteria().andEqualTo("userId", cartInfo.getUserId()).andEqualTo("skuId", cartInfo.getSkuId());
        cartInfoMapper.updateByExampleSelective(cartInfo, e);
        syncCache(cartInfo.getUserId());
    }

    @Override
    public void combineCartList(List<CartInfo> cartInfos, String userId) {


        for (CartInfo cartInfo : cartInfos) {
           CartInfo info = ifExistCart(cartInfo);
           if(info==null){//表明不存在相同的商品
               // 插入
               cartInfo.setUserId(userId);
               cartInfoMapper.insertSelective(cartInfo);
           }else{//表明存在相同的商品
               info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
               info.setCartPrice(info.getCartPrice().multiply(new BigDecimal(info.getSkuNum())));
               cartInfoMapper.updateByPrimaryKeySelective(info);
           }

        }

    }



    /**
     * 判断购物车中是否有指定的商品
     * @param cartInfo
     * @return
     */
    private CartInfo ifExistCart(CartInfo cartInfo) {

        CartInfo cartInfo1 = new CartInfo();
        cartInfo1.setUserId(cartInfo.getUserId());
        cartInfo1.setSkuId(cartInfo.getSkuId());
        CartInfo select = cartInfoMapper.selectOne(cartInfo1);

        return select;

    }
}
