package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import springfox.documentation.spring.web.json.Json;

import java.lang.reflect.Method;
import java.security.Key;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.common.cache
 * @create 2020-06-16 16:19
 * @Description:
 */
@Component
//面向切面的工作
@Aspect
public class GmallCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 编写一个环绕通知
     * @param point
     * @return Object通用
     */
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point){

        Object result = null;

        //获取到传递的参数 方法上的参数
        Object[] args = point.getArgs();

        //获取方法上的注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);

        //获取到传递的参数 注解中的参数
        String prefix = gmallCache.prefix();

        //定义一个key key: sku[30]
        String key = prefix + Arrays.asList(args).toString();

        //需要将数据存储到缓存中
        //key = prefix + Arrays.asList(args).toString();
        //value = 方法执行之后的返回值数据
        /*
        1. 先判断缓存中是否有数据
        2. 缓存中有从缓存中获取
        3. 缓存中没有从数据库中获取，并放入缓存
         */
        //表示根据 key 来获取缓存中返回的数据
        result = cacheHit(signature, key);

        //判断缓存中是否获取到了数据
        if (null != result){
            return result;
        }
        //如果获取到的数据是空，那么就应该走数据库，并放入缓存
        RLock lock = redissonClient.getLock(key + ":lock");

        try {
            boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
            //说明上锁成功
            try {
                if (res){
                    //查询数据库中数据 相当于执行 加注解的方法体，并可以得到返回值
                    result = point.proceed(point.getArgs());
                    //判断result 返回的数据是否为空
                    if (result == null){
                        //说明在数据库中没有这个数据 防止缓存击穿
                        Object o = new Object();
                        //将空对象放入缓存
                        redisTemplate.opsForValue().set(key, JSON.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        //返回数据
                        return o;
                    }
                    //查询出来的数据不是空，将对象放入缓存
                    redisTemplate.opsForValue().set(key,JSON.toJSONString(result),RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    //返回数据
                    return result;
                }else {
                    //其他线程睡眠
                    Thread.sleep(1000);
                    //继续获取数据
                    return cacheHit(signature, key);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                lock.unlock();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;

    }

    /**
     * 表示获取缓存中的数据
     * @param signature
     * @param key
     * @return
     */
    private Object cacheHit(MethodSignature signature, String key) {
        //根据key获取缓存中的数据,返回方法执行的返回值
        //放入缓存中的为 字符串，取出来也是字符串
        String object = (String)redisTemplate.opsForValue().get(key);
        //此时获取返回值应该明确
        if (!StringUtils.isEmpty(object)){
            //表示缓存中有数据，并获取返回值数据类型
            Class returnType = signature.getReturnType();
            //返回数据
            return JSON.parseObject(object, returnType);
        }
        return null;
    }
}
