package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.user.service.impl
 * @create 2020-06-22 14:46
 * @Description:
 */
@Service
public class UserServiceImpl implements UserService {

    //注入Mapper
    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 登陆
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        //select * from user where username = ? and pwd = ?
        //密码要做MD5加密处理
        String newPwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());

        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", userInfo.getLoginName());
        wrapper.eq("passwd", newPwd);

        UserInfo info = userInfoMapper.selectOne(wrapper);

        if (null != info){
            return info;
        }

        return null;
    }
}
