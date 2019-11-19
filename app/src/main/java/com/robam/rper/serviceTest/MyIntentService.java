package com.robam.rper.serviceTest;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.robam.rper.util.LogUtil;

/**
 * author : liuxiaohu
 * date   : 2019/11/19 9:21
 * desc   :
 * version: 1.0
 */
public class MyIntentService extends IntentService {
    private static final String TAG = "MyIntentService";
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogUtil.d(TAG,"Thread id is "+Thread.currentThread().getId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG,"onDestroy executed");

    }
}
