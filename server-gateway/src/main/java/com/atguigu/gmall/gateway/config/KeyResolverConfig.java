package com.atguigu.gmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class KeyResolverConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        System.out.println("IP限流");
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }

//    @Bean
//    public KeyResolver userKeyResolver() {
//        System.out.println("用户限流");
//        return exchange -> Mono.just(exchange.getRequest().getHeaders().get("token").get(0));
//    }
//
//
//    @Bean
//    public KeyResolver apiKeyResolver() {
//        System.out.println("接口限流");
//        return exchange -> Mono.just(exchange.getRequest().getPath().value());
//    }
}
