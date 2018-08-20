package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jimmy Hao
 * 2018-08-17 21:24
 */

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    AlipayClient alipayClient;


    @RequestMapping(value = "alipay/callback/return")
    public String callbackReturn (HttpServletRequest request, String orderId, ModelMap map) {
        //用于接收支付宝回传的参数信息
        Map<String,String> paramsMap = null;
        //验签结果
        boolean signVerified = true;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap,AlipayConfig.alipay_public_key,AlipayConfig.charset,AlipayConfig.sign_type);
        } catch (Exception e) {
            System.out.println("此处支付宝验签通过");
        }
        if(signVerified){
            String tradeNo = request.getParameter("trade_no");
            String outTradeNo = request.getParameter("out_trade_no");
            String tradeStatus = request.getParameter("trade_status");

            String callbackContent =request.getQueryString();

            // 幂等性检查
            boolean b = paymentService.checkPaied(outTradeNo);
            // 发送支付成功的消息PAYMENT_SUCCESS_QUEUE
            if(!b){
                paymentService.sendPaymentSuccessQueue(tradeNo,outTradeNo,callbackContent);
            }

        }else{
            //返回失败页面
        }
        return "testPaySuccess";
    }


    @ResponseBody
    @RequestMapping("alipay/submit")
    public String alipay(){

        String userId = "2";

        String orderId = "117";

        //生成和保存支付信息
        OrderInfo order = orderService.getOrderById(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(order.getOutTradeNo());
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setOrderId(orderId);
        paymentInfo.setTotalAmount(order.getTotalAmount());
        paymentInfo.setSubject(order.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setCreateTime(new Date());

        paymentService.savePayment(paymentInfo);

        //重定向到支付宝平台

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        HashMap<String,Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("out_trade_no",order.getOutTradeNo());
        stringObjectHashMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        stringObjectHashMap.put("total_amount",0.01);//orderById.getTotalAmount()
        stringObjectHashMap.put("subject","测试硅谷手机phone");
        String json = JSON.toJSONString(stringObjectHashMap);
        alipayRequest.setBizContent(json);

        String form = "";

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        System.out.println("设置一个定时巡检订单"+paymentInfo.getOutTradeNo()+"的支付状态的延迟队列");
        paymentService.sendPaymentCheckQueue(paymentInfo.getOutTradeNo(),5);

        return form;
    }

    @RequestMapping("index")
    public String index(String orderId, HttpServletRequest request, ModelMap map){
        String nickName = (String)request.getAttribute("nickName");

        OrderInfo orderInfo = orderService.getOrderById(orderId);

        map.put("nickName",nickName);
        map.put("outTradeNo",orderInfo.getOutTradeNo());
        map.put("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

}
