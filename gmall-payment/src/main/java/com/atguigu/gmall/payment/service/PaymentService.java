package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.HashMap;

/**
 * @author Jimmy Hao
 * 2018-08-18 21:05
 */
public interface PaymentService {
    void savePayment(PaymentInfo paymentInfo);

    void sendPaymentSuccessQueue(String tradeNo, String outTradeNo, String callbackContent);

    boolean checkPaied(String outTradeNo);

    void sendPaymentCheckQueue(String outTradeNo, int i);

    HashMap<String,String> checkPayment(String outTradeNo);
}
