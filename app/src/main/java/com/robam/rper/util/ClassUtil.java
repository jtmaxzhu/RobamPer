package com.robam.rper.util;


import android.content.Context;


import com.robam.rper.activity.MyApplication;


import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;

/**
 * author : liuxiaohu
 * date   : 2019/8/316:48
 * desc   :
 * version: 1.0
 */
public class ClassUtil {
    private final static String TAG = ClassUtil.class.getSimpleName();

    /**
     * 区分外部类和内部类
     */
    private static String mFilter;

    private static boolean init = false;
    private static List<Class> classes = new ArrayList<>();

    private final static Map<String, List<Class>> mPatchClasses = new HashMap<>();

    /**
     * 根据类是否加载判断初始化状态
     * @return
     */
    public static boolean recordCLasses(){
        return classes.isEmpty();
    }

    public static <T> List<Class<? extends T>> findSubClass(Class<T> parent, Class<? extends Annotation> annotation){
        return findSubClass(parent, annotation, false);
    }

    public static <T> T constructClass(Class<T> targetClass, Object... arguments) {
        Class<?>[] classes;
        if (arguments == null || arguments.length == 0) {
            classes = null;
        } else {
            classes = new Class[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                classes[i] = arguments[i].getClass();
            }
        }

        return constructClass(targetClass, classes, arguments);
    }

    /**
     * 构造类
     *
     * @param targetClass 待构造类
     * @param arguments 构造函数
     * @return
     */
    public static <T> T constructClass(Class<T> targetClass, Class<?>[] classes, Object[] arguments) {
        // targetClass为空，无法构造
        if (targetClass == null) {
            return null;
        }

        try {
            // 通过参数类查找构造函数
            Constructor<T> constructor = targetClass.getDeclaredConstructor(classes);

            // 对于private的构造函数
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance(arguments);
        } catch (InstantiationException e) {
            LogUtil.e(TAG, "Catch java.lang.InstantiationException: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, "Catch java.lang.IllegalAccessException: " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, "Catch java.lang.reflect.InvocationTargetException: " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, "Catch java.lang.NoSuchMethodException: " + e.getMessage(), e);
        }

        return null;
    }


    /**
     * 查找子类
     * @param parent 父类
     * @param annotation 注解名称
     * @param <T>
     * @return 子类列表
     */
    public static <T> List<Class<? extends T>> findSubClass(Class<T> parent, Class<? extends Annotation> annotation, boolean isInterface){
        if(parent == null){
            return null;
        }
        List<Class<? extends T>> childrenClasses = new ArrayList<>();
        //遍历
        for (Class childClass : classes){
            if(childClass != null && parent.isAssignableFrom(childClass)){
                if(annotation != null){
                    Annotation targetAnnotation = childClass.getAnnotation(annotation);
                    if(targetAnnotation == null){
                        continue;
                    }
                }
                if(!isInterface){
                    if(childClass.isInterface()){
                        continue;
                    }
                }
                childrenClasses.add(childClass);

            }
        }

        synchronized (mPatchClasses){
            for (List<Class> patchClasses : mPatchClasses.values()){
                for (Class childClass : patchClasses){
                    if(childClass != null && parent.isAssignableFrom(childClass)){
                        if(annotation != null){
                            Annotation targetAnnotation = childClass.getAnnotation(annotation);
                            if(targetAnnotation == null){
                                continue;
                            }
                        }
                        childrenClasses.add(childClass);
                    }
                }
            }
        }

        return childrenClasses;
    }


    /**
     * 通过package名，查找package下面的指定类的类名
     * @param context
     * @param filter
     */
    public static void initClasses(Context context, String filter){
        LogUtil.d(TAG, "initClasses in: ");
        if(init){
            return;
        }
        init = true;
        if(StringUtil.isEmpty(filter)){
            mFilter = MyApplication.getInstance().getApplicationInfo().packageName;
            LogUtil.d(TAG, "initClasses: "+mFilter);
        }else{
            mFilter = filter;
        }

        //加载dex文件
        try {
            DexFile dex = new DexFile(context.getPackageCodePath());
            //枚举类，来遍历所有源代码
            Enumeration<String> entries = dex.entries();
            while(entries.hasMoreElements()){
                String className = entries.nextElement();
                if(StringUtil.isEmpty(mFilter) || StringUtil.contains(className, mFilter)){
                    try {
                        //LogUtil.d(TAG,"className:"+className);
                        Class scanClass = Class.forName(className);
                        classes.add(scanClass);
                    }catch (ClassNotFoundException e){
                        LogUtil.e(TAG, e, "Can't get class instance of %s", className);
                    }

                }
            }

        } catch (IOException e) {
            LogUtil.e(TAG, e, "Catch java.io.IOException: %s", e.getMessage());
        }
    }

    /**
     * 查找带有包含注解方法的类
     * @param annotation
     * @return
     */
    public static List<Class<?>> findClassWithMethodAnnotation(Class<? extends Annotation> annotation) {
        List<Class<?>> childrenClasses = new ArrayList<>();
        // 遍历查找子类
        for (Class childClass : classes) {
            if (childClass != null) {
                // 不找父类，因为父类也包含待注入方法
                for (Method method : childClass.getDeclaredMethods()) {
                    if (method.getAnnotation(annotation) != null) {
                        childrenClasses.add(childClass);
                        break;
                    }
                }
            }
        }

        // patch部分类
        synchronized (mPatchClasses) {
            for (List<Class> patchClasses : mPatchClasses.values()) {
                for (Class childClass : patchClasses) {
                    if (childClass != null) {
                        // 不找父类，因为父类也包含待注入方法
                        for (Method method : childClass.getDeclaredMethods()) {
                            if (method.getAnnotation(annotation) != null) {
                                childrenClasses.add(childClass);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return childrenClasses;
    }

    /**
     * 获取类包含的所有方法
     * @param clazz
     * @return
     */
    public static List<Method> getAllMethods(Class clazz) {
        return getAllMethods(clazz, null);
    }

    /**
     * 获取类包含包含特定注解的所有方法
     * @param clazz 类
     * @param annotation 注解
     * @return
     */
    public static List<Method> getAllMethods(Class clazz, Class<Annotation> annotation) {
        if (clazz == null) {
            LogUtil.e(TAG, "无法获取空对象的方法");
        }

        List<Method> allMethods = new ArrayList<>();
        Class currentClass = clazz;
        while (currentClass != null) {
            Method[] currentLevelMethods = currentClass.getDeclaredMethods();
            for (Method method : currentLevelMethods) {
                if (annotation != null) {
                    // 查找目标注解
                    Annotation targetAnnotation = method.getAnnotation(annotation);
                    if (targetAnnotation == null) {
                        continue;
                    }
                }

                // 添加该方法
                allMethods.add(method);
            }

            currentClass = currentClass.getSuperclass();
        }

        return allMethods;
    }




}
