package com.robam.rper.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.view.ContextThemeWrapper;

/**
 * author : liuxiaohu
 * date   : 2019/9/6 13:51
 * desc   : 工具类
 * version: 1.0
 */
public class ContextUtil {
    private static final String TAG = "ContextUtil";

    /**
     * dp转pix
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 加载特定Theme的Context
     * @param context
     * @param theme
     * @return
     */
    @SuppressLint("RestrictedApi")
    public static Context getContextThemeWrapper(Context context, int theme) {
        return new ContextThemeWrapper(context, theme);
    }

    public static PackageInfo getPackageInfoByName(Context context, String packageName) {
        if (context == null) {
            return null;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e(TAG, "Catch PackageManager.NameNotFoundException: " + e.getMessage(), e);
        }
        return packageInfo;
    }
}
