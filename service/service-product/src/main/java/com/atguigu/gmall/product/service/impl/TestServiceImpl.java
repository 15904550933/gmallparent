package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.RedissonCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author smy
 * @create 2020-06-15 15:17
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

//    @Override
//    public void testLock() {
//        /*
//        set k1 v1 px 10000 nx -- 原生命令。Jedis可以操作的命令
//        但是现在我们使用的是redisTemplate,没有直接的set命令
//         */
//        //setIfAbsent 相当于 nx 相当于 setnx() 当key不存在时才会生效
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu");
//
//        //相当于 set lock atguigu px 3000 nx 具有原子性，自动释放锁资源
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu", 3, TimeUnit.SECONDS);
//
//        //防止误删锁，给Value设置一个UUID
//        String uuid = UUID.randomUUID().toString();
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
//
//        //模拟用户访问商品详情，通过sku_id访问  item.gmall.com/31.html
//        String skuId = "31";
//        //根据自己的规则定义锁
//        String lockKey = "sku:" + skuId + ":lock";
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 3, TimeUnit.SECONDS);
//
//        //如果返回true，说明上述命令执行成功，加锁成功！操作资源
//        if (lock) {
//
//            //获取缓存中的key
//            String num = redisTemplate.opsForValue().get("num");
//            if (StringUtils.isEmpty(num)) {
//                return;
//            }
//
//            int number = Integer.parseInt(num);
//            redisTemplate.opsForValue().set("num", String.valueOf(++number));
//            //初始化num为0  set num 0
//
//            /*
//            操作资源完成，删除锁
//             */
////            redisTemplate.delete("lock");
//
////            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
////                redisTemplate.delete("lock");
////            }
//
//            //推荐使用lua脚本
//            String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//            //如何操作
//            //构建RedisScript
//            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//            //指定好返回的数据类型
//            //因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，那么返回字符串与0 会有发生错误。
//            redisScript.setResultType(Long.class);
//            //指定好lua脚本
//            redisScript.setScriptText(script);
//            //第一个要是script 脚本 ，第二个需要判断的key，第三个就是key所对应的值
//            redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
//        } else {
//            try {
//                //说明上锁没有成功，有人在操作资源，只能等待
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            testLock();
//        }
//
//        /*
//        使用ab工具测试
//        之前在redis中，玩过ab测试工具：httpd-tools（yum install -y httpd-tools）
//        ab  -n（一次发送的请求数）  -c（请求的并发数） 访问路径
//        测试如下：5000请求，100并发
//        ab  -n 5000 -c 100 http://192.168.200.1:8206/admin/product/test/testLock
//         */
//
//
//    }


    /**
     * 使用redisson
     */
//    @Override
//    public void testLock() {
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://192.168.200.128:6379");
//        RedissonClient redissonClient = Redisson.create(config);
//
//        RLock lock = redissonClient.getLock("lock");
//
//        lock.lock();
//        //执行的业务逻辑代码！
//        lock.unlock();
//    }

    /**
     * 使用redisson
     * 自动装配
     * 表示在spring容器中有一个对象RedissonClient
     * <bean id="redisson" ></bean>
     */
    @Override
    public void testLock() {
        String skuId = "30";
        String lockKey = "sku:"+skuId+":lock";

        RLock lock = redissonClient.getLock(lockKey);
        //加锁,十秒自动解锁
        lock.lock(10, TimeUnit.SECONDS);

        /*业务逻辑代码*/
        //获取缓存中的key
        String num = redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(num)){
            return;
        }
        int number = Integer.parseInt(num);
        redisTemplate.opsForValue().set("num",String.valueOf(++number));
        System.out.println("number = " + number);

        //解锁
        lock.unlock();

    }


    /**
     * 读锁
     * @return
     */
    @Override
    public String readLock() {
        //获取读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.readLock();
        //上锁
        rLock.lock(10, TimeUnit.SECONDS);
        //表示从缓存中获取数据
        String msg = redisTemplate.opsForValue().get("msg");
        return msg;
    }

    /**
     * 写锁
     * @return
     */
    @Override
    public String writeLock() {
        //向缓存中写入数据
        //获取读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.writeLock();
        //上锁
        rLock.lock(10,TimeUnit.SECONDS);
        //写数据
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());
        return "-----写入数据成功-----";
    }
}
