package com.atguigu.gmall.item.config;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;

import java.util.concurrent.TimeUnit;

/**
 * @author smy
 * 自定义线程池
 * @date 2020/6/17 14:16
 */
@Configuration
public class ThreadPoolConfig {

    // 自定义一个线程池
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){

        return new ThreadPoolExecutor(
                50, // 核心线程池数
                200, // 最大线程池数据
                30, // 剩余空闲线程的存活时间
                TimeUnit.SECONDS, // 时间单位
                new ArrayBlockingQueue<>(50) // 阻塞队列
        );
    }
}
