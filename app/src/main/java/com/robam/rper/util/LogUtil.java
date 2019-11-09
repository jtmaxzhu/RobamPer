package com.robam.rper.util;

import com.orhanobut.logger.Logger;

/**
 * author : liuxiaohu
 * date   : 2019/8/5 14:54
 * desc   :
 * version: 1.0
 */
public class LogUtil {
    public static void d(String tag, String message, Object... args) {
        Logger.t(tag).d(message, args);
    }

    public static void i(String tag, String message, Object... args) {
        Logger.t(tag).i(message, args);
    }

    public static void w(String tag, String message, Object... args) {
        Logger.t(tag).w(message, args);
    }

    public static void e(String tag, String message, Object... args) {
        Logger.t(tag).e(message + "\n" + MiscUtil.getCurrentStrackTraceString(), args);
    }

    public static void e(String tag, Throwable throwable, String message, Object... args) {
        Logger.t(tag).e(throwable, message, args);
    }

    public static void i(String tag, String message, Throwable t) {
        Logger.log(Logger.INFO, tag, message, t);
    }

    public static void w(String tag, String message, Throwable t) {
        Logger.log(Logger.WARN, tag, message, t);
    }

    public static void d(String tag, String message, Throwable t) {
        Logger.log(Logger.DEBUG, tag, message, t);
    }

    public static void e(String tag, String message, Throwable t) {
        e(tag, t, message);
    }

    public static void v(String tag, String message, Object... args) {
        Logger.t(tag).v(message, args);
    }

    public static void t(String tag, String message, Object... args) {
        Logger.t(tag).wtf(message, args);
    }
}
