package com.atguigu.gmall.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Feign拦截器
 */
@Component
public class FeignInterceptor implements RequestInterceptor {

    /**
     * 1，我们在web-all微服务中通过cartFeignClient.addToCart(skuId, skuNum)添加购物车，调用service-cart微服务
     * 2，如果不添加Feign拦截器，service-cart微服务addToCart获取不到用户信息
     * @param requestTemplate
     */
    @Override
    public void apply(RequestTemplate requestTemplate){
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

//            System.out.println(request.getHeader("userTempId"));
//            System.out.println(request.getHeader("userId"));
            requestTemplate.header("userTempId", request.getHeader("userTempId"));
            requestTemplate.header("userId", request.getHeader("userId"));

            //
    }

}
