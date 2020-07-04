package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author mqx
 * @date 2020/6/23 14:41
 */
public interface CartService {
    // 添加购物车的接口
    void addToCart(Long skuId, String userId, Integer skuNum);

    // 查询购物车列表，查询登录，未登录
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据用户Id 查询购物车列表
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户Id查询购物车最新数据并放入缓存
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);

}
