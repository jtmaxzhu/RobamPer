package com.robam.rper.display.items.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * author : liuxiaohu
 * date   : 2019/11/8 13:03
 * desc   :
 * version: 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DisplayItem {
    /**
     * 显示名称
     * @return
     */
    String name();

    /**
     * 需动态申请权限
     * @return
     */
    String[] permissions() default {};

    String tip() default "";

    /**
     * 显示图标
     * @return
     */
    int icon() default 0;

    String trigger() default "";

    int level() default 1;
}
