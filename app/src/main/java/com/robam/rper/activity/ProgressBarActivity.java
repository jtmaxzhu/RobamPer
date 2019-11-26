package com.robam.rper.activity;


import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;

import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.util.LogUtil;

import java.lang.ref.WeakReference;


@EntryActivity(icon = R.drawable.xn, name = "进度条", index = 4)
public class ProgressBarActivity extends BaseActivity  {

    public static final int UPDATE_TEXT = 1;
    private ProgressBar progressBar;
    private int mProgress;


    private TextView textView;
    private Handler mhandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progressbar);

        progressBar = findViewById(R.id.loadProgressBar);

        mhandler = new TimeProcessBarHandler(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    mProgress = doWork();
                    Message ms = new Message();

                    LogUtil.d("liuxh11", "------" + mProgress);
                    LogUtil.d("liuxh11", "ms.what：" + ms.what);

                    if (mProgress < 100) {
                        ms.what = 0x1;
                        mhandler.sendMessage(ms);
                    } else {
                        ms.what = 0x0;
                        mhandler.sendMessage(ms);
                        break;
                    }
                }
            }
        }).start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_progressbar,null);
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        LogUtil.d("ProgressBarActivity","rect.width()"+rect.width());
        LogUtil.d("ProgressBarActivity","rect.height()"+rect.height());

    }

    private int doWork(){
        mProgress += Math.random()*10;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mProgress;
    }

    private static final class TimeProcessBarHandler extends Handler{
        private WeakReference<ProgressBarActivity> activityRef;

        public TimeProcessBarHandler(ProgressBarActivity activityRef) {
            this.activityRef = new WeakReference<>(activityRef);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ProgressBarActivity activity = activityRef.get();
            if (activity == null){
                return;
            }
            switch (msg.what){
                case 0x1:
                    activity.progressBar.setProgress(activity.mProgress);
                    LogUtil.d("liuxh11","activity.mProgress："+activity.mProgress);
                    break;
                case 0x0:
                    activity.progressBar.setVisibility(View.GONE);
                    break;
            }

        }
    }

}
