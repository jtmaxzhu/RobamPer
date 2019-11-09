package com.robam.rper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * author : liuxiaohu
 * date   : 2019/8/710:08
 * desc   :
 * version: 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalService {
    boolean lazy() default true;
    String name() default "";
    int level() default 1;
}
