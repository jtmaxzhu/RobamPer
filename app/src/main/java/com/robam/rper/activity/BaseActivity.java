package com.robam.rper.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;


import com.robam.rper.R;
import com.robam.rper.util.DeviceInfoUtil;
import com.robam.rper.util.LogUtil;

public class BaseActivity extends AppCompatActivity {

    private static boolean initializeScreenInfo = false;
    private boolean canShowDialog;
    private static Toast toast;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //等待MyApplication初始化完毕
        LogUtil.d("BaseActivity", "!(this instanceof LoadingActivity)"+(!(this instanceof LoadingActivity)));

        if(!(this instanceof LoadingActivity)){
            long startTime = System.currentTimeMillis();
            MyApplication.getInstance().waitInMain();
            LogUtil.d("BaseActivity", "Activity: %s, 等待Launcher初始化耗时: %dms", getClass().getSimpleName(), System.currentTimeMillis() - startTime);
        }
        //正常初始化
        super.onCreate(savedInstanceState);
        MyApplication.getInstance().notifyCreate(this);


        // 如果屏幕信息还未初始化，初始化下
        if (!initializeScreenInfo) {
            getScreenSizeInfo();
            initializeScreenInfo = true;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.getInstance().notifyDestroy(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        canShowDialog = false;
        MyApplication.getInstance().notifyPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        canShowDialog = true;
        MyApplication.getInstance().notifyResume(this);
    }

    protected boolean canShowDialog() {
        return canShowDialog;
    }

    /**
     * 展开软键盘
     */
    public void showInputMethod() {
        InputMethodManager imManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
    }

    //隐藏输入法
    public void hideSoftInputMethod() {
        View view = getWindow().peekDecorView();
        if (view != null && view.getWindowToken() != null) {
            InputMethodManager imManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * toast短时间提示
     *
     * @param msg
     */
    public void toastShort(final String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_SHORT);
                } else {
                    toast.setText(msg);
                }
                toast.show();
            }
        });
    }

    public void toastShort(String msg, Object... args) {
        String formatMsg = String.format(msg, args);
        toastShort(formatMsg);
    }

    /**
     * toast长时间提示
     *
     * @param msg
     */
    public void toastLong(final String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_LONG);
                } else {
                    toast.setText(msg);
                }
                toast.show();
            }
        });
    }

    public void toastLong(String msg, Object... args) {
        String formatMsg = String.format(msg, args);
        toastLong(formatMsg);
    }

    public void showProgressDialog(final String str) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(BaseActivity.this, R.style.SimpleDialogTheme);
                    progressDialog.setMessage(str);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.show();
                } else if (progressDialog.isShowing()) {
                    progressDialog.setMessage(str);
                } else {
                    progressDialog.setMessage(str);
                    progressDialog.show();
                }
            }
        });
    }

    public void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    public void updateProgressDialog(final int progress, final int totalProgress, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog == null || !progressDialog.isShowing()) {
                    return;
                }

                // 更新progressDialog的状态
                progressDialog.setProgress(progress);
                progressDialog.setMax(totalProgress);
                progressDialog.setMessage(message);
            }
        });
    }


    private void getScreenSizeInfo() {
        getWindowManager().getDefaultDisplay().getRealSize(DeviceInfoUtil.realScreenSize);
        getWindowManager().getDefaultDisplay().getSize(DeviceInfoUtil.curScreenSize);
        getWindowManager().getDefaultDisplay().getMetrics(DeviceInfoUtil.metrics);
    }
}
