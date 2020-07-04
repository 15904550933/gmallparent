package com.atguigu.gmall.activity.service.impl;

import com.alibaba.nacos.client.naming.utils.StringUtils;
import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {


    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;


    /**
     * 查询全部
     * 因为秒杀商品会在凌晨将数据加载到缓存中，所以此处查询缓存即可
     */
    @Override
    public List<SeckillGoods> findAll() {
        //商品保存到缓存redis-Hash
        List<SeckillGoods> seckillGoodsList = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return seckillGoodsList;
    }

    /**
     * 根据ID获取实体类秒杀商品详情
     *
     * @param id
     * @return
     */
    @Override
    public SeckillGoods getSeckillGoodsBySkuId(Long id) {
        //根据skuId 查询秒杀对象信息
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(id.toString());

    }

    /***
     * 创建订单
     * @param skuId
     * @param userId
     */
    @Override
    public void seckillOrder(Long skuId, String userId) {
        //产品状态位， 1：可以秒杀 0：秒杀结束
        String state = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(state)) {
            //已售罄
            return;
        }

        //判断用户是否下单
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!isExist) {
            //用户已经存在 --> 第二次下单
            return;
        }

        //获取队列中的商品，如果能够获取，则商品存在，可以下单
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(goodsId)) {
            //商品售罄，更新状态位
            redisTemplate.convertAndSend("seckillpush", skuId + ":0");
            //已售罄
            return;
        }

        //订单记录
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(this.getSeckillGoodsBySkuId(skuId));
        orderRecode.setNum(1);
        //生成下单码
        orderRecode.setOrderStr(MD5.encrypt(userId + skuId));

        //订单数据存入Reids
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(), orderRecode);
        //更新库存
        this.updateStockCount(orderRecode.getSeckillGoods().getSkuId());
    }

    /**
     * 更新库存
     *
     * @param skuId
     */
    private void updateStockCount(Long skuId) {
        //更新库存，批量更新，用于页面显示，以实际扣减库存为准
        Long stockCount = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //避免频繁更新数据库
        if (stockCount % 2 == 0) {
            //商品卖完,同步数据库
            SeckillGoods seckillGoods = this.getSeckillGoodsBySkuId(skuId);
            seckillGoods.setStockCount(stockCount.intValue());
            seckillGoodsMapper.updateById(seckillGoods);

            //更新缓存 缓存中秒杀商品对象中的库存数，需要更新
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
        }
    }

    /***
     * 根据用户ID查看订单信息
     * @param userId
     * @return
     */
    @Override
    public Result checkOrder(Long skuId, String userId) {
        // 用户在缓存中存在，有机会秒杀到商品
        boolean isExist =redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (isExist) {
            //判断用户是否正在排队
            //判断用户是否预下单
            boolean isHasKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (isHasKey) {
                //抢单成功
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                // 秒杀成功！ 返回对应的code码
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }

        //判断是否下单
        boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if(isExistOrder) {
            String orderId = (String)redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        String state = (String) CacheHelper.get(skuId.toString());
        if("0".equals(state)) {
            //已售罄 抢单失败
            return Result.build(null, ResultCodeEnum.SECKILL_FAIL);
        }

        //正在排队中
        return Result.build(null, ResultCodeEnum.SECKILL_RUN);
    }




}
