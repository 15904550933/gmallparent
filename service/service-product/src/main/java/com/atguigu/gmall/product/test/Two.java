package com.atguigu.gmall.product.test;

/**
 * 饿汉式
 * 使用静态内部类的方式实现单例模式
 * @author zm
 * @create 2020-06-11 22:50
 */
public class Two {

    private Two(){}

    private static class Inner{
        private static final Two TWO = new Two();
    }

    public static Two getTwo(){
        return Inner.TWO;
    }
}
