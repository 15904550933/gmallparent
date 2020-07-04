package com.atguigu.gmall.user.comtroller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import com.sun.org.apache.regexp.internal.RE;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.acl.LastOwnerException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.user.comtroller
 * @create 2020-06-22 14:52
 * @Description:
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    //登陆的控制器url 是谁？以及提交方式是什么？login.html中login方法 的出登陆控制器

    /**
     * 登陆
     *
     * @param userInfo 接收数据
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request) {
        //login.login(this.user)
        UserInfo login = userService.login(userInfo);
        //判断查询出来的数据是否为空
        if (null != login) {
            //登陆成功之后，返回一个toker,token由一个uuid组成
            String token = UUID.randomUUID().toString();

            //页面中 auth.setToken(response.data.data.token) ---> 将token放入cookie
            //声明一个map
            HashMap<String, Object> map = new HashMap<>();
            map.put("token", token);

            //登陆成功之后，页面上方需要显示一个用户昵称
            map.put("nickName", login.getNickName());

            //将此时登陆的用户IP地址放入缓存
            //声明一个对象
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", login.getId().toString());
            //工具类
            jsonObject.put("ip", IpUtil.getIpAddress(request));

            //将数据放入缓存
            //定义Key
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(userKey, jsonObject.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            //将Map返回
            return Result.ok(map);
        } else {
            return Result.fail().message("用户名和密码不匹配！");
        }
    }

    /**
     * 登出
     *
     * @return
     */
    @GetMapping("logout")
    public Result logout(HttpServletRequest request) {
        //删除缓存中的数据
        //token跟用户缓存key有直接的关系，在登陆的时候，将token放入了cookie!
        //但是，在登陆的时候，token不止放入了cookie中，还放入了header中（考虑到这个登陆还可以扩展到移动端使用！）
        //      手机----浏览器是没用cookie 的，只有header
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));

        return Result.ok();
    }



}
