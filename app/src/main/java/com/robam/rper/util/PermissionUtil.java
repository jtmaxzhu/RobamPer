package com.robam.rper.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.robam.rper.activity.MyApplication;
import com.robam.rper.activity.PermissionDialogActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : liuxiaohu
 * date   : 2019/9/5 16:41
 * desc   :
 * version: 1.0
 */
public class PermissionUtil {
    private static final String TAG = "PermissionUtil";

    private static Map<Integer, OnPermissionCallback> _callbackMap = new ConcurrentHashMap<>();

    private static long lastActionTime = 0;
    private static AtomicInteger callbackCount = new AtomicInteger(0);


    /**
     * 开始请求权限
     *
     * @param permissions
     * @param activity
     * @param callback
     */
    public static void requestPermissions(@NonNull final List<String> permissions, final Activity activity, @NonNull final OnPermissionCallback callback) {
        // 可能悬浮窗Dialog还没关闭，延后一下权限申请任务
        MyApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /*if (PermissionDialogActivity.runningStatus) {
                    LogUtil.w(TAG, "有其他任务正在申请权限");
                    callback.onPermissionResult(false, "正在申请其他权限");
                    return;
                }

                if (permissions.size() == 0) {
                    LogUtil.w(TAG, "请求权限为空");
                    callback.onPermissionResult(true, null);
                    return;
                }*/

                final Intent intent = new Intent(activity, PermissionDialogActivity.class);

                // 转化为ArrayList
                if (permissions instanceof ArrayList) {
                    intent.putStringArrayListExtra(PermissionDialogActivity.PERMISSIONS_KEY, (ArrayList<String>) permissions);
                } else {
                    intent.putStringArrayListExtra(PermissionDialogActivity.PERMISSIONS_KEY, new ArrayList<>(permissions));
                }

                // 设置回调idz
                int currentIdx = callbackCount.getAndIncrement();
                Log.d(TAG, "currentIdx: "+currentIdx);
                intent.putExtra(PermissionDialogActivity.PERMISSION_IDX_KEY, currentIdx);

                // 起了intent再设置callback
                activity.startActivity(intent);
                _callbackMap.put(currentIdx, callback);
            }
        });
    }

    /**
     * 处理权限
     * @param result
     * @param reason
     */
    public static void onPermissionResult(int idx, boolean result, String reason) {
        if (_callbackMap.isEmpty() || _callbackMap.get(idx) == null) {
            LogUtil.e(TAG, "callback引用消失");
            return;
        }

        OnPermissionCallback _callback = _callbackMap.remove(idx);
        _callback.onPermissionResult(result, reason);
    }


    /**
     * 检查未被授权的权限，并进行申请
     * @param activity
     * @param neededPermissions
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static List<String> checkUngrantedPermission(Activity activity, String[] neededPermissions){
        List<String> notGrantedPermissions = new ArrayList<>();

        int index = 0;
        for (String permission : neededPermissions){
            if(permission != null && ContextCompat.checkSelfPermission(activity,permission) != PackageManager.PERMISSION_GRANTED){
                notGrantedPermissions.add(permission);
            }
        }
        return notGrantedPermissions;
    }

    public interface OnPermissionCallback {
        void onPermissionResult(boolean result, String reason);
    }
}
