package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.*;

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
        List<CartInfo> select = cartInfoMapper.select(cartInfo);

        if(select==null||select.size()==0){
            jedis.del("cart:"+userId+":info");
        }else{
            HashMap<String, String> stringStringHashMap = new HashMap<>();
            for (CartInfo info : select) {
                stringStringHashMap.put(info.getId(), JSON.toJSONString(info));
            }

            jedis.hmset("cart:"+userId+":info",stringStringHashMap);

        }

        jedis.close();
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
            if (info == null) {//表明不存在相同的商品
                // 插入
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            } else {//表明存在相同的商品
                info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
                info.setCartPrice(info.getCartPrice().multiply(new BigDecimal(info.getSkuNum())));
                cartInfoMapper.updateByPrimaryKeySelective(info);
            }

        }

    }

    @Override
    public List<CartInfo> getCartCacheByChecked(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();

        List<String> hvals = jedis.hvals("cart:" + userId + ":info");

        if (hvals != null && hvals.size() > 0) {
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                if (cartInfo.getIsChecked().equals("1")) {
                    cartInfos.add(cartInfo);
                }

            }
        }
        return cartInfos;
    }

    /**
     * 删除指定的购物车记录
     * @param cartInfos
     */
    @Override
    public void deleteCartById(List<CartInfo> cartInfos) {
        Set cartIds = new HashSet();
        for (CartInfo cartInfo : cartInfos) {
            cartIds.add(cartInfo.getId());
        }
        String ids = StringUtils.join(cartIds, ",");
        cartInfoMapper.deleteCartByIds(ids);
        //同步缓存
        syncCache(cartInfos.get(0).getUserId());
    }


    /**
     * 判断购物车中是否有指定的商品
     *
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
