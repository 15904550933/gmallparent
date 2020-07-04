package com.atguigu.gmall.product.test;

/**
 * @author zm
 * @create 2020-06-11 22:13
 */
public class test {
    public static void main(String[] args) {
//        One one = One.get();
//        One one1 = One.get();
//        System.out.println(one==one1);

        Two two2 = Two.getTwo();
        Two two1 = Two.getTwo();
        System.out.println(two1 == two2);
    }
}
