package com.robam.rper.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.robam.rper.activity.MyApplication;
import com.robam.rper.bean.ProcessInfo;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author : liuxiaohu
 * date   : 2019/10/26 8:43
 * desc   :
 * version: 1.0
 */
public class AppInfoProvider {
    private static final String TAG = "AppInfoProvider";
    public static final String MAIN = "main";

    private String appName;

    @Subscriber(@Param(SubscribeParamEnum.APP))
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Provider(value = {@Param(SubscribeParamEnum.PACKAGE),
            @Param(SubscribeParamEnum.PACKAGE_CHILDREN),
            @Param(SubscribeParamEnum.PID), @Param(SubscribeParamEnum.PID_CHILDREN),
            @Param(SubscribeParamEnum.UID), @Param(SubscribeParamEnum.PUID),
            @Param(SubscribeParamEnum.TOP_ACTIVITY)}, updatePeriod = 4999)
    public Map<String, Object> provide() {
        Map<String, Object> result = new HashMap<>(8);
        LogUtil.d(TAG,"运行provide函数");

        ProcessInfo process = new ProcessInfo(0, MAIN);

        // 基础依赖
        result.put(SubscribeParamEnum.PACKAGE, appName);
        result.put(SubscribeParamEnum.UID, 0);
        result.put(SubscribeParamEnum.PID, process);
        result.put(SubscribeParamEnum.TOP_ACTIVITY, "");
        result.put(SubscribeParamEnum.PUID, "");

        Context context = MyApplication.getContext();
        // 全局选项，APP为空
        if (StringUtil.isEmpty(this.appName) || context == null) {
            return result;
        }

        // 根据包名查询UID
        try {
            @SuppressLint("WrongConstant")
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(appName, PackageManager.GET_ACTIVITIES);

            result.put(SubscribeParamEnum.UID, info.uid);
            LogUtil.d(TAG,"info.uid"+info.uid);

        } catch (Exception e) {
            // 当catch到Interrupt，属于onDestroy调用，直接结束
            if (e instanceof InterruptedException) {
                LogUtil.e(TAG, "onDestroy called, Params can't update invocation methods", e);
                return result;
            }

            result.put(SubscribeParamEnum.UID, 0);
        }

        String activity = CmdTools.execAdbCmd("dumpsys activity top | grep \"ACTIVITY " + appName + "\"", 1000);
        int filterPid = findTopPid(result, activity);
        LogUtil.d(TAG,"activity："+activity);
        LogUtil.d(TAG,"filterPid："+filterPid);

        // 查询PID，针对该应用所有进程
        String[] pids = CmdTools.ps(appName);
        List<ProcessInfo> childrenPid = new ArrayList<>(pids.length + 1);
        List<String> childrenPackage = new ArrayList<>(pids.length + 1);

        //手动查找目标pid
        for (String lineContent : pids) {
            processPsLine(filterPid, lineContent, childrenPackage, childrenPid, result);
        }

        result.put(SubscribeParamEnum.PID_CHILDREN, childrenPid);
        result.put(SubscribeParamEnum.PACKAGE_CHILDREN, childrenPackage);

        return result;
    }

    /**
     * 通过顶层ACTIVITY查找pid
     * @param result
     * @param activity
     * @return
     */
    private int findTopPid(Map<String, Object> result, String activity) {

        // 当顶层ACTIVITY存在时，以顶层PID过滤
        String trimmed;
        if (activity != null && !StringUtil.isEmpty((trimmed = activity.trim()))) {
            String[] pidContent = trimmed.split("\\s+");
            if (pidContent.length > 1 && pidContent[pidContent.length - 1].contains("pid=")) {
                String originActivityName = pidContent[1];
                String[] topActivity = originActivityName.split("/");

                LogUtil.i(TAG, "获取Top Activity：" + StringUtil.hide(topActivity));
                if (topActivity.length > 1) {
                    // 针对Activity是以"."开头的相对定位路径
                    String mActivity = topActivity[1];
                    if (StringUtil.startWith(mActivity, ".")) {
                        mActivity = topActivity[0] + mActivity;
                    }

                    // 拼接会完整名称
                    originActivityName = topActivity[0] + "/" + mActivity;
                }
                result.put(SubscribeParamEnum.TOP_ACTIVITY, originActivityName);

                // 记录过滤PID
                return Integer.parseInt(pidContent[pidContent.length - 1].substring(4));
            }
        }
        return -1;
    }

    /**
     * 处理单行ps
     * @param filterPid 过滤pid
     * @param lineContent ps行数据
     * @param childrenPackage 子进程package列表
     * @param childrenPid 子进程Pid列表
     * @param result 查找结果
     */
    private void processPsLine(int filterPid, String lineContent, List<String> childrenPackage, List<ProcessInfo> childrenPid, Map<String, Object> result) {
        String[] contents = lineContent.trim().split("\\s+");

        if (contents.length > 2) {
            try {
                int pid = Integer.valueOf(contents[1]);

                // 对于小程序而言，需要设置PACKAGE
                String packageName = contents[contents.length - 1];

                // 子进程名
                String target;
                if (StringUtil.startWith(packageName, appName)) {
                    target = packageName.substring(appName.length());
                    if (StringUtil.isEmpty(target)) {
                        target = MAIN;
                    } else {
                        target = target.substring(1);
                    }
                } else {
                    // 拿到grep 进程数据
                    return;
                }

                ProcessInfo processInfo = new ProcessInfo(pid, target);
                childrenPid.add(processInfo);
                childrenPackage.add(packageName);
                // 是否是目标进程
                boolean filterFlag;
                if (pid > -1) {
                    filterFlag = pid == filterPid;
                } else {
                    filterFlag = StringUtil.equals(target, MAIN);
                }

                // 如果是目标进程，保留PID，PACKAGE等信息
                if (filterFlag) {
                    result.put(SubscribeParamEnum.PID, processInfo);
                    result.put(SubscribeParamEnum.PACKAGE, packageName);

                    // PUID 指UID，与UID有不同，暂时无用
                    result.put(SubscribeParamEnum.PUID, contents[0].replace("_", ""));
                }

            } catch (Exception e) {
                LogUtil.e(TAG, "integer type of exception! contents: " + contents[1], e);
            }
        }
    }
}
