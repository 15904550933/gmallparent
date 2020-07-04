package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.gateway.config
 * @create 2020-06-10 11:14
 * @Description:
 */
@Configuration //变成XML
public class CorsConfig {

    //创建一个Bean对象
    @Bean
    public CorsWebFilter corsWebFilter(){
        //1.创建CorsConfiguration
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //2.设置跨域属性
        //设置允许访问的网络是谁
        corsConfiguration.addAllowedOrigin("*");
        //表示是否从服务器中能够获取到cookie,true为允许
        corsConfiguration.setAllowCredentials(true);
        //表示允许所有的请求方法（GET,POST...）
        corsConfiguration.addAllowedMethod("*");
        //表示设置请求头信息任意参数
        corsConfiguration.addAllowedHeader("*");
        //3.创建CorsConfigurationsource
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        //返回CorsWebFilter
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
