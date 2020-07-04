package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.all.controller
 * @create 2020-06-22 16:25
 * @Description:
 */
@Controller
public class PassportController {



    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        //http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);

        return "login";
    }



}
