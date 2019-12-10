package com.robam.rper.injector.param;

import com.robam.rper.annotation.Param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * author : liuxiaohu
 * date   : 2019/8/9 9:57
 * desc   :
 * version: 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Subscriber {
    /**
     * 参数名称，注入参数可以是 <br/>
     * {@link SubscribeParamEnum#APP} 应用名称，类型 {@link String} <br/>
     * {@link SubscribeParamEnum#UID} 应用UID，类型 {@link Integer} <br/>
     * {@link SubscribeParamEnum#PID} 应用PID，类型 {@link Integer} <br/>
     * {@link SubscribeParamEnum#EXTRA} 是否显示额外信息，类型 {@link Boolean} <br/>
     * {@link SubscribeParamEnum#PUID} ps获取的UID，类型 {@link String} <br/>
     * {@link SubscribeParamEnum#ACCESSIBILITY_SERVICE} Service上下文，类型 {@link android.content.Context}
     */
    Param[] value();

    RunningThread thread() default RunningThread.MESSAGE_THREAD;
}
