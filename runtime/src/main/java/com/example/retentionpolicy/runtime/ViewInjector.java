package com.example.retentionpolicy.runtime;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 绑定器（根据@FindViewById注解为成员View变量赋值，根据@SetOnClickById注解，为响应Id的View设置ClickListener）
 * Created by YON on 2017/1/18.
 */

public class ViewInjector {

    public static void inject(final Activity target) {
        //收集Field信息
        Class targetClass = target.getClass();
        List<Field> fieldList = new ArrayList<>();
        collectFields(fieldList, targetClass);
        for (Field field : fieldList) {
            //获取@FindViewById注解字段
            FindViewById annotation = field.getAnnotation(FindViewById.class);
            if (annotation != null) {
                try {
                    field.setAccessible(true);
                    field.set(target, target.findViewById(annotation.value()));
                } catch (Exception e) {
                    throw new RuntimeException("can't find view by id :" + annotation.value());
                }
            }
        }
        List<Method> methodList = new ArrayList<>();
        collectMethods(methodList, targetClass);
        for (final Method method : methodList) {
            //获取@FindViewById注解字段
            final SetOnClickById annotation = method.getAnnotation(SetOnClickById.class);
            if (annotation != null) {
                target.findViewById(annotation.value()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            method.setAccessible(true);
                            method.invoke(target);
                        } catch (Exception e) {
                            throw new RuntimeException("can't set click listener by id :" + annotation.value());
                        }
                    }
                });
            }
        }
    }

    private static void collectFields(List<Field> fieldList, Class targetClass) {
        while (targetClass != null) {
            Field[] fields = targetClass.getDeclaredFields();
            Collections.addAll(fieldList, fields);
            targetClass = targetClass.getSuperclass();
        }
    }

    private static void collectMethods(List<Method> methodList, Class targetClass) {
        while (targetClass != null) {
            Method[] methods = targetClass.getDeclaredMethods();
            Collections.addAll(methodList, methods);
            targetClass = targetClass.getSuperclass();
        }
    }
}
