package com.example.runtime_processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * 存储Activity中注解相关View信息
 * Created by YON on 2017/1/19.
 */

class ActivityAnnotatedInfo {
    /**
     * 生成的java文件后缀名
     */
    public static final String SUFFIX = "_InjectUtil";

    private final Map<Integer, IdAnnotatedInfo> viewIdMap = new LinkedHashMap<>();
    private final String classPackage;//类所在包的包名
    private final String className;//类的名称

    ActivityAnnotatedInfo(String classPackage, String className) {
        this.classPackage = classPackage;
        this.className = className;
    }

    void addBindField(int id, String name, String type) {
        getTargetIdAnnotatedInfo(id).field = new AnnotatedField(name, type);
    }

    void addBindMethod(int id, String name, String parameterType) {
        getTargetIdAnnotatedInfo(id).method = new AnnotatedMethod(name, parameterType);
    }

    private IdAnnotatedInfo getTargetIdAnnotatedInfo(int id) {
        IdAnnotatedInfo info = viewIdMap.get(id);
        if (info == null) {
            info = new IdAnnotatedInfo(id);
            viewIdMap.put(id, info);
        }
        return info;
    }

    String getActivityName() {
        return classPackage + "." + className;
    }

    /**
     * 使用JavaPoet创建Java文件
     */
    JavaFile createBinderClassFile() {
        final String acName = getActivityName();
        ClassName targetActivityName = ClassName.get(classPackage, className);
        //方法名（Bind）
        MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(targetActivityName, "activity",Modifier.FINAL);
        //遍历处理每个View
        for (Map.Entry<Integer, IdAnnotatedInfo> entry : viewIdMap.entrySet()) {
            IdAnnotatedInfo annotatedInfo = entry.getValue();
            AnnotatedField field = annotatedInfo.field;
            AnnotatedMethod method = annotatedInfo.method;
            if (field != null) {
                String findViewStatement = "activity." + field.name + " = (" + field.type + ")activity.findViewById(" + annotatedInfo.id + ")";
                injectMethodBuilder.addStatement(findViewStatement);
            }
            if (method != null) {
                String variable;
                if (field != null) {
                    variable = "activity."+field.name;
                } else {//只设置了Click
                    variable = "activity.findViewById(" + annotatedInfo.id + ")";
                }
                String paramStatement = "";
                if (method.parameterType != null) {
                    paramStatement = "(" + method.parameterType + ")view";
                }
                String setClickStatement =
                        variable + ".setOnClickListener(new android.view.View.OnClickListener(){\n" +
                                "   public void onClick(android.view.View view) {\n" +
                                "       activity."+method.name+"("+paramStatement+");\n" +
                                "   }})";
                injectMethodBuilder.addStatement(setClickStatement);
            }

        }
        MethodSpec injectMethod = injectMethodBuilder.build();
        //类名为：Activity名+$$InjectUtil.java
        TypeSpec binderClass = TypeSpec.classBuilder(className + SUFFIX)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(injectMethod)
                .build();
        return JavaFile.builder(classPackage, binderClass)
                .addFileComment("auto generate InjectUtil class response to : $S ", acName)
                .build();
    }

    /**
     * 一个R.id.xxx（View）相关的注解信息,一个id只能被注解到一个变量和一个方法中
     */
    private static class IdAnnotatedInfo {
        final int id;
        AnnotatedField field;
        AnnotatedMethod method;

        IdAnnotatedInfo(int id) {
            this.id = id;
        }
    }

    /**
     * 被注解的View成员变量信息
     */
    private static class AnnotatedField {

        /**
         * 变量名称，如 mPasswordTextView
         */
        final String name;

        /**
         * View类型，如 TextView
         */
        final String type;//变量

        AnnotatedField(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    /**
     * 被注解的Click方法信息
     */
    private static class AnnotatedMethod {
        /**
         * 方法名称
         */
        final String name;

        /**
         * 参数类型，必须为View
         */
        final String parameterType;

        AnnotatedMethod(String name, String parameterType) {
            this.name = name;
            this.parameterType = parameterType;
        }
    }
}
