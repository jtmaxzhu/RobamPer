package com.robam.rper.util;

import android.os.Process;

import java.util.ArrayList;
import java.util.List;

/**
 * author : liuxiaohu
 * date   : 2019/8/12 9:36
 * desc   :
 * version: 1.0
 */
public class RperCrashHandler implements Thread.UncaughtExceptionHandler {
    private final static String TAG = ClassUtil.class.getSimpleName();

    private static volatile RperCrashHandler rperCrashHandler;

    private static volatile boolean sHasInited = false;

    public static RperCrashHandler getRperCrashHandler(){
        if (rperCrashHandler == null){
            synchronized (RperCrashHandler.class){
                if(rperCrashHandler == null){
                    rperCrashHandler = new RperCrashHandler();
                }
            }
        }
        return rperCrashHandler;
    }

    private List<CrashCallback> mCrashCallbacks = new ArrayList<>();

    public void init() {
        if (!sHasInited) {
            Thread.setDefaultUncaughtExceptionHandler(this);
            sHasInited = true;
        }
    }

    public void registerCrashCallback(CrashCallback cb) {
        synchronized (mCrashCallbacks) {
            mCrashCallbacks.add(cb);
        }
    }

    public void unregisterCrashCallback(CrashCallback cb) {
        synchronized (mCrashCallbacks) {
            mCrashCallbacks.remove(cb);
        }
    }

    private void runCallbacks(Thread t, Throwable reason) {
        List<CrashCallback> copyCrashCallbacks = new ArrayList<>();
        synchronized (mCrashCallbacks) {
            copyCrashCallbacks.addAll(mCrashCallbacks);
        }

        for (CrashCallback cb : mCrashCallbacks) {
            cb.onAppCrash(t, reason);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LogUtil.e(TAG, e.getMessage(), e);
        runCallbacks(t, e);
        CrashCallback.KILL_PROCESS_CALLBACK.onAppCrash(t, e);
    }

    public interface CrashCallback {
        void onAppCrash(Thread t, Throwable e);

        CrashCallback KILL_PROCESS_CALLBACK = new CrashCallback() {
            @Override
            public void onAppCrash(Thread t, Throwable e) {
                Process.killProcess(Process.myPid());
            }
        };
    }
}
