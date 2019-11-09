package com.robam.rper.permission;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.robam.rper.util.rom.HuaweiUtils;
import com.robam.rper.util.rom.MeizuUtils;
import com.robam.rper.util.rom.MiuiUtils;
import com.robam.rper.util.rom.OppoUtils;
import com.robam.rper.util.rom.QikuUtils;
import com.robam.rper.util.rom.RomUtils;
import com.robam.rper.util.rom.VivoUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * author : liuxiaohu
 * date   : 2019/9/12 8:29
 * desc   :
 * version: 1.0
 */
public class FloatWindowManager {
    private static final String TAG = "FloatWindowManager";

    private static volatile FloatWindowManager instance;
    private Dialog dialog;

    public static FloatWindowManager getInstance(){
        if (instance == null){
            synchronized (FloatWindowManager.class){
                if (instance == null){
                    instance = new FloatWindowManager();
                }
            }
        }
        return instance;
    }

    public boolean checkFloatPermission(Context context) {
        return true;
    }

    public boolean checkPermission(Context context) {
        if(Build.VERSION.SDK_INT < 23){
            if (RomUtils.checkIsMiuiRom()) {
                return miuiPermissionCheck(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                return meizuPermissionCheck(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                return huaweiPermissionCheck(context);
            } else if (RomUtils.checkIs360Rom()) {
                return qikuPermissionCheck(context);
            } else if (RomUtils.isOppoSystem()) {
                return oppoROMPermissionCheck(context);
            } else if (RomUtils.isVivoSystem()) {
                return vivoPermissionCheck(context);
            }
        }
        return commonROMPermissionCheck(context);
    }

    private boolean commonROMPermissionCheck(Context context) {
        if (RomUtils.checkIsMeizuRom()) {
            return meizuPermissionCheck(context);
        } else if (RomUtils.isVivoSystem()) {
            return vivoPermissionCheck(context);
        }else {
            Boolean result = true;
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class clazz = Settings.class;
                    Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
                    result = (Boolean) canDrawOverlays.invoke(null, context);
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            return result;
        }
    }

    /**
     * 直接去申请权限
     * @param context
     */
    public void applyPermissionDirect(Context context) {

        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                MiuiUtils.applyMiuiPermission(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                HuaweiUtils.applyPermission(context);
            } else if (RomUtils.checkIs360Rom()) {
                QikuUtils.applyPermission(context);
            } else if (RomUtils.isOppoSystem()) {
                OppoUtils.applyOppoPermission(context);
            } else if (RomUtils.isVivoSystem()) {
                VivoUtils.applyPermission(context);
            }
        } else {
            // 其他的再试一次
            if (RomUtils.checkIsMeizuRom()) {
                MeizuUtils.applyPermission(context);
            } else if (RomUtils.isVivoSystem()) {
                VivoUtils.applyPermission(context);
            } else {
                try {
                    commonROMPermissionApplyInternal(context);
                } catch (Exception e) {
                    Log.e(TAG, "Throw exception " + e.getMessage(), e);
                }
            }
        }
    }

    public static void commonROMPermissionApplyInternal(Context context) throws NoSuchFieldException, IllegalAccessException {
        Class clazz = Settings.class;
        Field field = clazz.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION");

        Intent intent = new Intent(field.get(null).toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }


    private boolean huaweiPermissionCheck(Context context) {
        return HuaweiUtils.checkFloatWindowPermission(context);
    }

    private boolean miuiPermissionCheck(Context context) {
        return MiuiUtils.checkFloatWindowPermission(context);
    }

    private boolean meizuPermissionCheck(Context context) {
        return MeizuUtils.checkFloatWindowPermission(context);
    }

    private boolean qikuPermissionCheck(Context context) {
        return QikuUtils.checkFloatWindowPermission(context);
    }

    private boolean oppoROMPermissionCheck(Context context) {
        return OppoUtils.checkFloatWindowPermission(context);
    }

    private boolean vivoPermissionCheck(Context context) {
        return VivoUtils.checkFloatWindowPermission(context);
    }






}
