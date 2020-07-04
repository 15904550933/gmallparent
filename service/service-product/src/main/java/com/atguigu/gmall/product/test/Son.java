package com.atguigu.gmall.product.test;

/**
 * @author zm
 * @create 2020-06-11 23:05
 */
public class Son extends Father {
    private int i = test();
    private static int j = method();
    static {
        System.out.print("6\t");
    }
    Son(){
        System.out.println("7\t");
    }
    {
        System.out.print("8\t");
    }
    public int test(){
        System.out.print("9\t");
        return 1;
    }
    public static int method(){
        System.out.print("10\t");
        return 1;
    }

    public static void main(String[] args) {
        Son s1 = new Son();
        System.out.println();
        Son s2 = new Son();
    }
}
