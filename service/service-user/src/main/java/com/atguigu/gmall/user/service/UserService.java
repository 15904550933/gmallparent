package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.user.service
 * @create 2020-06-22 14:44
 * @Description:
 */
public interface UserService {

    /**
     * 登陆
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);
}
