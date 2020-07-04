package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.security.provider.Sun;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String WARE_URL;

    @Autowired
    private RabbitService rabbitService;


    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        orderInfo.setPaymentWay("ONLINE");
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 定义为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        // 获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer tradeBody = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName() + " ");
        }
        if (tradeBody.toString().length() > 100) {
            orderInfo.setTradeBody(tradeBody.toString().substring(0, 100));
        } else {
            orderInfo.setTradeBody(tradeBody.toString());
        }

        orderInfoMapper.insert(orderInfo);

        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }

        //发送延迟队列，如果定时未支付，取消订单
        /*
        1.在下订单的时候，发送一个延迟队列消息。延迟时间根据业务而定
        2.做配置延迟消息配置类 OrderCanelMqConfig
            配置队列
            配置交换机
            配置绑定关系
        3.接收处理消息
         */
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);

        return orderInfo.getId();
    }

    /**
     * 生成流水号
     *
     * @param userId 充当redis中key
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        //将流水号放入缓存
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    /**
     * 比较流水号
     *
     * @param userId      获取缓存中的流水号
     * @param tradeCodeNo 页面传递过来的流水号
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeCodeNo.equals(redisTradeNo);
    }

    /**
     * 删除流水号
     *
     * @param userId
     */
    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);
    }

    /**
     * 验证库存
     *
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    /**
     * 处理过期订单
     *
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        // orderInfo
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        // paymentInfo
        //paymentFeignClient.closePayment(orderId);
        //取消交易
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }


    /**
     * 根据订单Id 修改订单的状态
     *
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        //订单的状态，可以通过进度状态来获取
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);

        //查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }

    /**
     * 发送消息，通知减库存
     * 需要参考库存管理文档 根据管理手册
     * 发送的数据是orderInfo 中的部分属性数据，并非全部属性数据
     *
     * @param orderId
     */
    @Override
    public void sendOrderStatus(Long orderId) {
        //更改订单的状态，变成通知仓库准备发货
        this.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);

        //获取发送的字符串
        String wareJson = initWareOrder(orderId);

        //准备发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);

    }

    /**
     * Map转换为Json
     *
     * @param orderId
     * @return
     */
    public String initWareOrder(Long orderId) {
        //查询到orederInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        //将orderInfo中的部分属性放入一个map中
        Map map = initWareOrder(orderInfo);
        //返回Json字符串
        return JSON.toJSONString(map);
    }

    /**
     * 将orderInfo部分数据放入Map中
     *
     * @param orderInfo
     * @return
     */
    @Override
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
         /*
            details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
                     {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        ArrayList<Map> mapArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId", orderDetail.getSkuId());
            orderDetailMap.put("skuNum", orderDetail.getSkuNum());
            orderDetailMap.put("skuName", orderDetail.getSkuName());
            mapArrayList.add(orderDetailMap);
        }
//        map.put("details", JSON.toJSONString(mapArrayList));
        map.put("details", mapArrayList);
        //返回构成好的map集合
        return map;

    }

    /**
     * 拆单方法
     * 1. 先获取原始订单
     * 2. wareSkuMap(JSON) --> 对象
     * 3. 创建新的子订单
     * 4. 给子订单进行赋值
     * 5. 保存子订单（订单以及订单明细）
     * 6. 更新原始订单状态
     * 7. test... 商品需要在不同仓库中
     *
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public List<OrderInfo> orderSplit(long orderId, String wareSkuMap) {

        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        //1. 获取原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //2. wareSkuMap(JSON) --> List<Map>
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //3. 创建新的子订单
        for (Map map : mapList) {
            //获取map中的仓库Id
            String wareId = (String) map.get("wareId");
            //获取仓库Id对应的商品Id
            List<String> skuIds = (List<String>) map.get("skuIds");

            //4. 给子订单进行赋值  创建新的子订单
            OrderInfo subOrderInfo = new OrderInfo();
            //属性拷贝 --> 原始订单的基础数据
            BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
            //id不能拷贝
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            //赋值一个仓库Id
            subOrderInfo.setWareId(wareId);
            //计算总金额 --> 在订单实体类中有sumTotalAmount()方法
            //声明一个子订单明细集合
            List<OrderDetail> orderDetails = new ArrayList<>();
            //将子订单的订单明细准备好
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (CollectionUtils.isEmpty(orderDetailList)){
                //遍历原始订单明细
                for (OrderDetail orderDetail : orderDetailList) {
                    //遍历仓库Id对应的商品Id
                    for (String skuId : skuIds) {
                        //比较两个商品skuId，如果相同，则这个商品就是子订单明细需要的商品
                        if (Long.parseLong(skuId) == orderDetail.getSkuId()){
                            orderDetails.add(orderDetail);
                        }
                    }
                }
            }
            //需要将子订单的名单明细准备好。添加到子订单中
            subOrderInfo.setOrderDetailList(orderDetails);
            //获取到总金额
            subOrderInfo.sumTotalAmount();

            //5. 保存子订单
            saveOrderInfo(subOrderInfo);
            //将新的子订单放入集合中
            subOrderInfoList.add(subOrderInfo);
        }

        //6. 更新原始订单状态
        updateOrderStatus(orderId, ProcessStatus.SPLIT);

        return subOrderInfoList;
    }


    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //更新状态
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        if ("2".equals(flag)){
            //发送信息关闭支付宝交易
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
        }

    }
}
