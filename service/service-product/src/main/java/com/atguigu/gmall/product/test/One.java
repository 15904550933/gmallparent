package com.atguigu.gmall.product.test;

/**
 * 饿汉式
 * @author zm
 * @create 2020-06-11 22:12
 */
public class One {

    private static final One ONE = new One();

    private One(){

    }

    public void hello(){
        System.out.println("hello");
    }

    public static One get(){
        return ONE;
    }
}
