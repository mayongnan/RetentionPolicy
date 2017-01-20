package com.example.runtime_processor;

import com.example.runtime_annotation.BindClick;
import com.example.runtime_annotation.BindView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * 注解处理器所在Module需为java library
 * 注解处理器是javac的一个工具，它用来在编译时扫描和处理注解。
 * 你可以自定义注解，并注册到相应的注解处理器，由注解处理器来处理你的注解。
 * 一个注解的注解处理器，以Java代码（或者编译过的字节码）作为输入，生成文件（通常是.java文件）作为输出。
 * 这些生成的Java代码是在生成的.java文件中，所以你不能修改已经存在的Java类，例如向已有的类中添加方法。
 * 这些生成的Java文件，会同其他普通的手动编写的Java源代码一样被javac编译。
 *
 *
 * Element:表示程序元素（如包、类或方法）,每个元素代表静态的、语言级别的结构（而不是虚拟机的运行时结构）.
 * Element#getEnclosingElement 获取该元素最里面的元素，不严格地说，闭包的元素；如元素（Element） TextView text，EnclosingElement为最外层的Activity;
 * TypeElement:表示类或接口程序元素.提供关于类型及其成员的信息的访问.注意，enum 是类类型，annotation 是接口类型。
 *
 */
@AutoService(Processor.class)
public class BindAnnotationProcessor extends AbstractProcessor{

    /**
     * 用来处理Element的工具类
     */
    private Elements elementUtils;
    /**
     * 用来处理TypeMirror的工具类
     */
    private Types typeUtils;
    /**
     * 正如这个名字所示，使用Filer可以创建文件
     */
    private Filer filer;
    /**
     * View类型对应的TypeMirror
     * JAVA API:
     * Represents a type in the Java programming language.
     * Types include primitive types, declared types (class and interface types),
     * array types, type variables, and the null type.
     * Also represented are wildcard type arguments,
     * the signature and return types of executables,
     * and pseudo-types corresponding to packages and to the keyword void.
     */
    private TypeMirror viewType;
    private TypeMirror activityType;

    /**
     * 每一个注解处理器类都必须有一个空的构造函数。
     * 然而，这里有一个特殊的init()方法，它会被注解处理工具调用，并输入ProcessingEnviroment参数。
     * @param processingEnvironment ProcessingEnvironment提供很多有用的工具类Elements, Types和Filer。
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        filer = processingEnvironment.getFiler();

        viewType = elementUtils.getTypeElement("android.view.View").asType();
        activityType = elementUtils.getTypeElement("android.app.Activity").asType();
    }

    /**
     * 这相当于每个处理器的主函数main()。
     * 你在这里写你的扫描、评估和处理注解的代码，以及生成Java文件。
     * 输入参数RoundEnvironment，可以让你查询出包含特定注解的被注解元素。
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Map<TypeElement, ActivityAnnotatedInfo> targetClassMap = findAndParseTargets(roundEnvironment);

        for (Map.Entry<TypeElement, ActivityAnnotatedInfo> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            ActivityAnnotatedInfo classBindInfo = entry.getValue();

            // 生成java文件
            try {
                JavaFile jfo = classBindInfo.createBinderClassFile();
                jfo.writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Unable to generate InjectUtil for type %s: %s", typeElement, e.getMessage());
            }
        }

        return true;
    }

    private Map<TypeElement, ActivityAnnotatedInfo> findAndParseTargets(RoundEnvironment env) {
        Map<TypeElement, ActivityAnnotatedInfo> targetClassMap = new LinkedHashMap<>();
        //存放类型擦除（泛型擦除：虚拟机中没有泛型，所有泛型类的类型参数在编译时都会被擦除）之后的类信息
        //Set<TypeMirror> erasedTargetTypes = new LinkedHashSet<TypeMirror>();
        for (Element element : env.getElementsAnnotatedWith(BindView.class)) {
            //获取BindView注解元素所在类（Activity）
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

            // 验证注解元素是否继承View
            if (!typeUtils.isSubtype(element.asType(), viewType)) {
                error(element, "@BindView fields must extend from View (%s.%s).",
                        enclosingElement.getQualifiedName(), element);
                continue;
            }

            // 验证修饰符
            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
                error(element, "@BindView fields must not be private or static (%s.%s).",
                        enclosingElement.getQualifiedName(), element);
                continue;
            }

            // 验证注解元素所在类类型为CLASS，而非INTERFACE、ENUM...
            if (enclosingElement.getKind() != CLASS) {
                error(element, "@BindView field annotations may only be specified in classes (%s).",
                        enclosingElement);
                continue;
            }
            // 验证注解元素所在类是否继承Activity
            if (!typeUtils.isSubtype(enclosingElement.asType(), activityType)) {
                error(element, "@BindView field annotations may only be specified in classes (%s).",
                        enclosingElement);
                continue;
            }
            // 验证注解元素所在类访问修饰符
            if (enclosingElement.getModifiers().contains(PRIVATE)) {
                error(element, "@BindView fields may not be on private classes (%s).", enclosingElement);
                continue;
            }

            // 收集注解相关信息
            // 注解成员变量名称
            String name = element.getSimpleName().toString();
            //BindView注解值（id）
            int id = element.getAnnotation(BindView.class).value();
            // 注解成员变量元素类型
            String type = element.asType().toString();

            ActivityAnnotatedInfo activityInfo = getOrCreateActivityInfo(targetClassMap, enclosingElement);
            activityInfo.addBindField(id, name, type);

            // 记录有注解信息的类型擦除后的类信息
            //TypeMirror erasedTargetType = typeUtils.erasure(enclosingElement.asType());
            //erasedTargetTypes.add(erasedTargetType);
        }
        
        for (Element element : env.getElementsAnnotatedWith(BindClick.class)) {
            //验证BindClick是否注解在成员方法上
            if (!(element instanceof ExecutableElement)) {
                error(element, "@BindClick annotation must be on a method.");
                continue;
            }

            ExecutableElement executableElement = (ExecutableElement) element;
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

            //验证注解的成员方法的访问修饰符
            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
                error(element, "@BindClick methods must not be private or static (%s.%s).",
                        enclosingElement.getQualifiedName(), element);
                continue;
            }

            //验证注解的成员方法所在类的访问修饰符
            if (enclosingElement.getKind() != CLASS) {
                error(element, "@BindClick method annotations may only be specified in classes (%s).",
                        enclosingElement);
                continue;
            }
            // 验证注解的方法所在类是否继承Activity
            if (!typeUtils.isSubtype(enclosingElement.asType(), activityType)) {
                error(element, "@BindView method annotations may only be specified in classes (%s).",
                        enclosingElement);
                continue;
            }
            // 验证注解的方法所在类访问修饰符
            if (enclosingElement.getModifiers().contains(PRIVATE)) {
                error(element, "@BindClick methods may not be on private classes (%s).", enclosingElement);
                continue;
            }

            // 验证注解方法返回类型是否为VOID
            if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
                error(element, "@BindClick methods must have a 'void' return type (%s.%s).",
                        enclosingElement.getQualifiedName(), element);
                continue;
            }

            String type = null;
            List<? extends VariableElement> parameters = executableElement.getParameters();
            if (!parameters.isEmpty()) {
                // 验证是否注解的方法只有一个参数
                if (parameters.size() != 1) {
                    error(element,
                            "@BindClick methods may only have one parameter which is View (or subclass) (%s.%s).",
                            enclosingElement.getQualifiedName(), element);
                    continue;
                }
                //验证是否注解的方法参数是否继承View.
                VariableElement variableElement = parameters.get(0);
                if (!typeUtils.isSubtype(variableElement.asType(), viewType)) {
                    error(element, "@BindClick method parameter must extend from View (%s.%s).",
                            enclosingElement.getQualifiedName(), element);
                    continue;
                }

                type = variableElement.asType().toString();
            }

            // 收集注解相关信息
            String name = executableElement.getSimpleName().toString();
            int id = element.getAnnotation(BindClick.class).value();

            ActivityAnnotatedInfo activityInfo = getOrCreateActivityInfo(targetClassMap, enclosingElement);
            activityInfo.addBindMethod(id,name,type);
            // 记录有注解信息的类型擦除后的类信息
            //TypeMirror erasedTargetType = typeUtils.erasure(enclosingElement.asType());
            //erasedTargetTypes.add(erasedTargetType);
        }

        //......
        //！！！这里省略Activity类之间继承相关处理（用到erasedTargetTypes），因为一个Activity对应一个xxxActivity$$Binder.java文件，
        // 如果A extends B,那么在A$$Binder.java类的inject()方法中要B.inject()先调用父类的inject()方法（$$Binder）

        return targetClassMap;
    }

    private ActivityAnnotatedInfo getOrCreateActivityInfo(Map<TypeElement, ActivityAnnotatedInfo> targetClassMap,
                                                          TypeElement enclosingElement) {
        ActivityAnnotatedInfo targetClass = targetClassMap.get(enclosingElement);
        if (targetClass == null) {
            String classPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, classPackage);

            targetClass = new ActivityAnnotatedInfo(classPackage, className);
            targetClassMap.put(enclosingElement, targetClass);
        }
        return targetClass;
    }
    /**
     * 这里你必须指定，这个注解处理器是注册给哪个注解的。
     * 注意，它的返回值是一个字符串的集合，包含本处理器想要处理的注解类型的合法全称。
     * 换句话说，你在这里定义你的注解处理器注册到哪些注解上。
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        //只处理指定注解
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(BindView.class.getCanonicalName());
        annotations.add(BindClick.class.getCanonicalName());
        return annotations;
    }

    /**
     * 用来指定你使用的Java版本。通常这里返回SourceVersion.latestSupported()。
     * 在Java 7中，你也可以使用注解来代替getSupportedAnnotationTypes()和getSupportedSourceVersion()
     * <br/>@SupportedSourceVersion(SourceVersion.latestSupported())
     * <br/>@SupportedAnnotationTypes({
     * <br/>   //合法注解全名的集合
     * <br/>})
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return super.getSupportedSourceVersion();
    }

    protected void error(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(ERROR, String.format(message, args), element);
    }

    protected String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }
    protected static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }
}
