package com.robam.rper.injector.param;

/**
 * author : liuxiaohu
 * date   : 2019/8/9 9:14
 * desc   :
 * version: 1.0
 */
public class SubscribeParamEnum {
    public static final String APP = "app";

    /** 应用名 */
    public static final String APP_NAME = "appName";

    /** 屏幕顶层应用包名 */
    public static final String PACKAGE = "package";

    /** 应用所有子进程包名 */
    public static final String PACKAGE_CHILDREN = "packageChildren";

    /** 屏幕顶层活动名 */
    public static final String TOP_ACTIVITY = "topActivity";

    /** AccessibilityService */
    public static final String ACCESSIBILITY_SERVICE = "accessibilityService";

    /** 目标进程pid */
    public static final String PID = "pid";

    /** 应用所有子进程pid */
    public static final String PID_CHILDREN = "pidChildren";

    /** ps获取的uid */
    public static final String PUID = "puid";

    /** 应用UID */
    public static final String UID = "uid";

    /** 是否显示额外信息 */
    public static final String EXTRA = "extra";


}
