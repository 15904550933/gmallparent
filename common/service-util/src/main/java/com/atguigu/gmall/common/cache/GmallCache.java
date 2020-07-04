package com.atguigu.gmall.common.cache;


import java.lang.annotation.*;

//注解在方法上使用
@Target(ElementType.METHOD)
//注解的生命周期
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    //表示一个前缀
    String prefix() default "cache";
}
