package com.example.rententionpolicy.source;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义SOURCE级注解，定义登录状态
 * <p>
 * Java.lang中最基本的Annotation有 @Deprecated, @Override, @SuppressWarnings三种；
 * 其中Deprecated表示过时或者抛弃不用的element，因为有更好的可以替代或者是原element的使用存在一定危险；
 * Override 声明了一个方法打算重写父类的方法；利用@Override很多时候可以检测手动重写的方法是否正确；
 * SuppressWarnings 指示在注释元素以及包含在该注释元素中的所有程序元素中取消显示指定的编译器警告。
 * <p>
 * java.lang.annotation 元注解
 * <p>
 * android.support.annotation通常用于辅助代码上的静态检查
 * <p>
 * Created by YON on 2017/1/17.
 */

@Target(ElementType.PARAMETER)//定义Annotation所修饰的对象范围
@IntDef({SignInProgress.START, SignInProgress.SUCCESS, SignInProgress.FAILURE})
@Retention(RetentionPolicy.SOURCE)//定义该Annotation被保留的时间长短
public @interface SignInState {
}
