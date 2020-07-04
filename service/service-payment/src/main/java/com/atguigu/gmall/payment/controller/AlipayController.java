package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;


    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable(value = "orderId") Long orderId, HttpServletResponse response) {
        String from = "";
        try {
            //直接调用返回html
            from = alipayService.createaliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return from;
    }

    /**
     * 支付宝同步回调
     * http://api.gmall.com/api/payment/alipay/callback/return
     *
     * @return
     */
    @RequestMapping("callback/return")
    public String callBack() {
        // 同步回调给用户展示信息
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 支付宝异步回调  必须使用内网穿透
     *
     * @param paramMap
     * @return
     */
    @RequestMapping("callback/notify")
    @ResponseBody
    public String callBackNotify(@RequestParam Map<String, String> paramMap) {
        System.out.println("来人了，开始接客了。。。。。。。");
        // https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        // 获取交易状态
        String trade_status = paramMap.get("trade_status");
        String out_trade_no = paramMap.get("out_trade_no");
        String app_id = paramMap.get("app_id");
        String total_amount = paramMap.get("total_amount");
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (signVerified) {
            // 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                // 还需要做一个判断，虽然你支付的状态判断完了，但是有没有这么一种可能，你的交易记录中的支付状态已经变成付款了，或者是关闭了，那么应该返回验签失败！
                // 查询交易记录对象 根据out_trade_no 查询
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) ||
                        paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
                    return "failure";
                }

                // 商户需要验证该通知数据中的 out_trade_no 是否为商户系统中创建的订单号；
                // 判断 total_amount 是否确实为该订单的实际金额（即商户订单创建时的金额）；
                // 验证 app_id 是否为该商户本身。
//                if (out_trade_no.equals(paymentInfo.getOutTradeNo()) && total_amount == paymentInfo.getTotalAmount() && app_id.equals(AlipayConfig.appId)){
//                    // 表示支付成功，此时才会更新交易记录的状态
//                    paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramMap);
//                    return "success";
//                }
                // 表示支付成功，此时才会更新交易记录的状态
                paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramMap);


                return "success";
            }
        } else {
            // 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    /**
     * 发起退款！http://localhost:8205/api/payment/alipay/refund/20
     *
     * @param orderId
     * @return
     */
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable(value = "orderId") Long orderId) {
        //根据orderId 调用退款接口
        boolean flag = alipayService.refund(orderId);
        //返回结果
        return Result.ok(flag);
    }


    /**
     * 关闭支付宝的交易
     *
     * @param orderId
     * @return
     */
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId) {
        Boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

    /**
     * 根据订单查询是否支付成功！
     *
     * @param orderId
     * @return
     */
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {
        // 调用退款接口
        boolean flag = alipayService.checkPayment(orderId);
        return flag;
    }

    /**
     * 添加数据接口
     *
     * @param outTradeNo
     * @return
     */
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo) {
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null != paymentInfo) {
            return paymentInfo;
        }
        return null;
    }


}
