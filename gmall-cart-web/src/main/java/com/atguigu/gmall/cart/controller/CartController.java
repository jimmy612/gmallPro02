package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jimmy Hao
 * 2018-08-12 10:59
 */

@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;

    @RequestMapping("checkCart")
    public String checkCart(HttpServletRequest request, HttpServletResponse response, CartInfo cartInfo, ModelMap map) {
        // 修改购物车的选中状态

        List<CartInfo> cartInfos = new ArrayList<>();
        String userId = (String) request.getAttribute("userId");
        if (StringUtils.isBlank(userId)) {
            // 取cookie中的数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
                for (CartInfo info : cartInfos) {
                    if (info.getSkuId().equals(cartInfo.getSkuId())) {
                        info.setIsChecked(cartInfo.getIsChecked());
                    }
                }
            }
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(cartInfos), 60 * 60 * 2, true);

        } else {
            // 取缓存数据
            cartInfo.setUserId(userId);
            cartService.updateCartChecked(cartInfo);
            cartInfos = cartService.getCartCache(userId);

        }

        map.put("cartList", cartInfos);
        map.put("totalPrice", getTotalPrice(cartInfos));

        return "cartListInner";

    }


    @LoginRequire(ifNeedSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, ModelMap map) {
        List<CartInfo> cartInfos = new ArrayList<>();
        String userId = (String) request.getAttribute("userId");
        if (StringUtils.isBlank(userId)) {
            // 取cookie中的数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
            }

        } else {
            // 去缓存数据
            cartInfos = cartService.getCartCache(userId);
        }

        map.put("cartList", cartInfos);
        map.put("totalPrice", getTotalPrice(cartInfos));
        return "cartList";
    }


    private BigDecimal getTotalPrice(List<CartInfo> cartInfos) {
        BigDecimal b = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfos) {

            if (cartInfo.getIsChecked().equals("1")) {
                b = b.add(cartInfo.getCartPrice());
            }
        }
        return b;
    }

    @LoginRequire(ifNeedSuccess = false)
    @RequestMapping("addToCart")
    public String addToCart(HttpServletRequest request, HttpServletResponse response, CartInfo cartInfo) {
        //根据skuId查询skuInfo
        SkuInfo skuInfo = skuService.getSkuById(cartInfo.getSkuId());
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setIsChecked("1");
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setCartPrice(skuInfo.getPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));

        List<CartInfo> cartInfos = new ArrayList<>();
        String userId = (String) request.getAttribute("userId");
        //用户未登录
        if (StringUtils.isBlank(userId)) {
            String cartCookieStr = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartCookieStr)) {
                cartInfos.add(cartInfo);
            } else {
                List<CartInfo> cartInfoList = JSON.parseArray(cartCookieStr, CartInfo.class);
                boolean b = isNewCart(cartInfo, cartInfoList);
                if (b) {
                    cartInfoList.add(cartInfo);
                } else {
                    for (CartInfo info : cartInfoList) {
                        if (info.getSkuId().equals(cartInfo.getSkuId())) {
                            info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
                            info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        }
                    }
                }
                cartInfos = cartInfoList;
            }
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(cartInfos), 60 * 60 * 24 * 7, true);
        } else {
            //用户已登录
            cartInfo.setUserId(userId);
            List<CartInfo> cartInfosFromDb = cartService.getCartInfosFromDbByUserId(userId);
            boolean b = isNewCart(cartInfo, cartInfosFromDb);
            if (b) {
                cartService.saveCart(cartInfo);
            } else {
                for (CartInfo info : cartInfosFromDb) {
                    if (info.getSkuId().equals(cartInfo.getSkuId())) {
                        info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
                        info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        cartService.update(info);
                    }
                }
            }
            //同步缓存
            cartService.syncCache(userId);//cart:userId:info
        }

        return "redirect:/cartSuccess";
    }

    private boolean isNewCart(CartInfo cartInfo, List<CartInfo> cartInfoList) {
        boolean b = true;
        for (CartInfo info : cartInfoList) {
            String skuId = info.getSkuId();
            if (skuId.equals(cartInfo.getSkuId())) {
                b = false;
            }
        }
        return b;
    }


    @RequestMapping("cartSuccess")
    public String cartSuccess() {

        return "success";
    }
}
