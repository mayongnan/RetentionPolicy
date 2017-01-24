package com.example.retentionpolicy.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义View绑定注解
 * Created by YON on 2017/1/18.
 */

//声明注解范围为成员变量
@Target(ElementType.FIELD)
//声明注解为运行时注解
@Retention(RetentionPolicy.RUNTIME)
public @interface FindViewById {
    //定义一个int类型的元素
    int value();
}
