package com.example.retentionpolicy.clazz;

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 注入处理器
 * Created by YON on 2017/1/19.
 */

public class BindInjector {
    /**
     * BindAnnotationProcessor生成的java文件后缀名
     */
    private static final String SUFFIX = "_InjectUtil";

    private static final String TAG = "BindInjector";
    private static final Map<Class<?>, Method> INJECT_UTILS = new LinkedHashMap<Class<?>, Method>();
    private static final Method NO_OP = null;

    /**
     * 为xxActivity找到并调用xxActivity_InjectUtil工具类的inject方法进行注入
     */
    public static void inject(Activity activity) {
        Class<?> targetClass = activity.getClass();
        try {
            Log.d(TAG, "Looking up view injector for " + targetClass.getName());
            Method inject = findInjectUtilMethodForClass(targetClass);
            if (inject != NO_OP) {
                inject.invoke(null, activity);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new UnableToInjectException("Unable to inject views for " + activity, e);
        }
    }

    /**
     * 查找cls对应的工具类方法并进行缓存
     */
    private static Method findInjectUtilMethodForClass(Class<?> cls) throws NoSuchMethodException {
        Method inject = INJECT_UTILS.get(cls);
        if (inject != null) {
            Log.d(TAG, "HIT: Cached in injector map.");
            return inject;
        }
        String clsName = cls.getName();
        if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
            Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return NO_OP;
        }
        try {
            Class<?> injector = Class.forName(clsName + SUFFIX);
            inject = injector.getMethod("inject", cls);
            Log.d(TAG, "HIT: Class loaded injection class.");
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            inject = findInjectUtilMethodForClass(cls.getSuperclass());
        }
        INJECT_UTILS.put(cls, inject);
        return inject;
    }

    private static class UnableToInjectException extends RuntimeException {
        UnableToInjectException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
