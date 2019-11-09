package com.robam.shared.display.items.base;

/**
 * author : liuxiaohu
 * date   : 2019/11/8 13:03
 * desc   :
 * version: 1.0
 */
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
