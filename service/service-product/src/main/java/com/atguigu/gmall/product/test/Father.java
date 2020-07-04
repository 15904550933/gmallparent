package com.atguigu.gmall.product.test;

/**
 * @author zm
 * @create 2020-06-11 23:03
 */
public class Father {
    private int i = test();
    static {
        System.out.print("1\t");
    }

    private static int j = method();
    Father(){
        System.out.print("2\t");
    }
    {
        System.out.print("3\t");
    }
    public int test(){
        System.out.print("4\t");
        return 1;
    }
    public static int method(){
        System.out.print("5\t");
        return 1;
    }
}
