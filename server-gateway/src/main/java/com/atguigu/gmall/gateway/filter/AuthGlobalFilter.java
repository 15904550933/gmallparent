package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import netscape.security.UserTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.xml.ws.Action;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.gateway.filter
 * @create 2020-06-23 9:24
 * @Description: GlobalFilter --> 全局过滤器
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {


    //判断匹配路径的工具类
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")
    //trade.html,myOrder.html,list.html
    private String authUrlsUrl;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 过滤器
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取用户在浏览器输入的访问路径Url
        //获取到请求对象
        ServerHttpRequest request = exchange.getRequest();
        //通过请求对象来获取URL
        String path = request.getURI().getPath();
        //判断用户发起的请求中是否有inner,如果有,禁止浏览器直接访问。
        //路径匹配
        if (antPathMatcher.match("/**/inner/**", path)) {
            //给提示信息，没有访问权限！
            //获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            //out 方法提示信息
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //获取用户登录信息，用户登录成功以后，缓存中有用户userId
        //如果获取到了userId，则说明用户已经登陆 key = user:login:token
        //在登陆过程中，token放入可cookie和header
        String userId = getUserId(request);
        String userTempId = getUserTempId(request);

        //判断 防止token被盗用
        if ("-1".equals(userId)){
            //获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            //out 方法提示信息
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //用户登录认证
        //路径匹配,api接口，异步请求，校验用户必须登录
        if (antPathMatcher.match("/api/**/auth/**", path)){
            //如果用户访问的url中包含此路径，则用户必须登录
            if (StringUtils.isEmpty(userId)){
                //获取响应对象
                ServerHttpResponse response = exchange.getResponse();
                //out 方法提示信息
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }


        //验证用户访问web-all时，是否带有黑名单中的控制器
        //authUrlsUrl : trade.html,myOrder.html,list.html
        String[] split = authUrlsUrl.split(",");
        for (String authUrl : split) {
            //用户访问的路径中是否包含了上述的内容
            if (path.indexOf(authUrl) != -1 && StringUtils.isEmpty(userId)){
                //用户访问的路径中 包含 了上述的内容,并且用户 没有 登陆
                //获取响应对象
                ServerHttpResponse response = exchange.getResponse();
                //返回一个响应状态码303，重定向获取请求资源
                response.setStatusCode(HttpStatus.SEE_OTHER);
                //重定向到登录页面
                response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());
                //设置返回
                return response.setComplete();
            }
        }
        //用户再访问任何一个微服务的过程中，必须先走网关。
        //既然在万观众获取到了userId,那么就可以将Id传递给页面
        //传递用户Id,临时用户ID 到各个微服务
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)){
            if (!StringUtils.isEmpty(userId)){
                //将用户Id 储存在请求头
                request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)){
                //将临时用户Id 储存在请求头
                request.mutate().header("userTempId", userTempId).build();
            }
            //固定写法，
            return chain.filter(exchange.mutate().request(request).build());
        }
        return chain.filter(exchange);
    }

    /**
     * 获取用户Id
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        String token = "";
        //从header中获取
        List<String> tokenList = request.getHeaders().get("token");
        //如果不为空 直接获取到  如果为空 从cookie中获取
        if(null  != tokenList) {
            token = tokenList.get(0);
        } else {
            //得到所有cookie
            MultiValueMap<String, HttpCookie> cookies =  request.getCookies();
            //获取到token的cookie
            HttpCookie cookie = cookies.getFirst("token");
            if(null != cookie){
                //获取cookie中的token  URLDecoder.decode()
                token = URLDecoder.decode(cookie.getValue());
            }
        }
        //如果cookie不为空
        if(!StringUtils.isEmpty(token)) {
            //根据token获取redis中的用户信息
            String userStr = (String)redisTemplate.opsForValue().get("user:login:" + token);
            //将用户信息转换为JSON字符串
            JSONObject userJson = JSONObject.parseObject(userStr);
            String ip = userJson.getString("ip");
            String curIp = IpUtil.getGatwayIpAddress(request);
            //校验token是否被盗用
            if(ip.equals(curIp)) {
                //返回用户ID
                return userJson.getString("userId");
            } else {
                //ip不一致
                return "-1";
            }
        }
        return "";
    }

    /**
     * 获取临时用户Id,添加购物车时，临时Id已经存在于cookie中,也可能存在于header中
     */
     private String getUserTempId(ServerHttpRequest request){
         String userTempId = "";
         //从header中获取
         List<String> tokenList = request.getHeaders().get("userTempId");
         //如果不为空 直接获取到  如果为空 从cookie中获取
         if(null  != tokenList) {
             userTempId = tokenList.get(0);
         } else {
             //得到所有cookie
             MultiValueMap<String, HttpCookie> cookies =  request.getCookies();
             //获取到userTempId的cookie
             HttpCookie cookie = cookies.getFirst("userTempId");
             if(null != cookie){
                 //获取cookie中的userTempId  URLDecoder.decode()
                 userTempId = URLDecoder.decode(cookie.getValue());
             }
         }
         return userTempId;
     }

    /**
     * 提示信息方法
     * @param response
     * @param permission
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {
        //返回用户权限的通知提示
        Result<Object> result = Result.build(null, permission);

        //设置字符集, result对象变成一个字节数组
        byte[] bytes = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer wrap = response.bufferFactory().wrap(bytes);

        //给用户提示，显示到页面上
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");

        //输出到页面  Publisher ---> CorePublisher ---> Mono
        return response.writeWith(Mono.just(wrap));
    }

}
