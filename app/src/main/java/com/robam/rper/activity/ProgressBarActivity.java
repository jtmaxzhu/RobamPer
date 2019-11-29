package com.robam.rper.activity;


import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;

import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.util.LogUtil;

import java.lang.ref.WeakReference;


@EntryActivity(icon = R.drawable.xn, name = "进度条", index = 4)
public class ProgressBarActivity extends BaseActivity  {

    private static final String TAG = ProgressBarActivity.class.getSimpleName();
    public static final int UPDATE_TEXT = 1;
    private ProgressBar progressBar;
    private int mProgress;

    private float mTouchStartX;
    private float mTouchStartY;
    private float x;
    private float y;
    int state, lastState;
    private float StartX;
    private float StartY;
    private int statusBarHeight = 0;


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

        final View view = findViewById(R.id.ProgressLayout);

        //获取标题栏高度
        if(statusBarHeight == 0){
            try {
                Class<?> clazz = Class.forName("com.android.internal.R$dimen");
                Object object = clazz.newInstance();
                statusBarHeight = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
                statusBarHeight = getResources().getDimensionPixelSize(statusBarHeight);
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (statusBarHeight == 0){
                    statusBarHeight = 50;
                }
            }
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // 获取相对屏幕的坐标，即以屏幕左上角为原点
                x = event.getRawX();
                y = event.getRawY() - statusBarHeight; // 25是系统状态栏的高度
                LogUtil.i(TAG, "currX" + x + "====currY" + y);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        state = MotionEvent.ACTION_DOWN;
                        StartX = x;
                        StartY = y;
                        // 获取相对View的坐标，即以此View左上角为原点
                        mTouchStartX = event.getX();
                        mTouchStartY = event.getY();
                        LogUtil.i(TAG, "startX" + mTouchStartX + "====startY" + mTouchStartY);
                        LogUtil.d(TAG,"落点位置是否再view内："+inRangeOfView(v, event));
                    break;
/*                    case MotionEvent.ACTION_MOVE:
                        state = MotionEvent.ACTION_MOVE;y
                        updateViewPosition();
                        lastState = state;
                        break;

                    case MotionEvent.ACTION_UP:
                        state = MotionEvent.ACTION_UP;
                        updateViewPosition();
                        // 对于点击移动小于(10, 10) 且处于缩小状态下，恢复成原始状态
                        if (Math.abs(x - StartX) < 10 && Math.abs(y - StartY) < 10 && backgroundIcon.getVisibility() == View.VISIBLE) {
                            // 有注册悬浮窗监听器的话
                            if (floatListener != null) {
                                floatListener.onFloatClick(false);
                            } else {
                                cardView.setVisibility(View.VISIBLE);
                                // handler.postDelayed(task, period);
                                backgroundIcon.setVisibility(View.GONE);
                            }
                        }
                        mTouchStartX = mTouchStartY = 0;
                        lastState = state;
                        break;*/
                }
                return false;
            }
        });

    }

    private boolean inRangeOfView(View view, MotionEvent ev){
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int x = location[0]; // view距离window 左边的距离（即x轴方向）
        int y = location[1]; // view距离window 顶边的距离（即y轴方向）

        Rect r = new Rect();
        view.getWindowVisibleDisplayFrame(r);

        LogUtil.d("ProgressBarActivity","触点相对于VIEW的坐标x--"+ev.getX());
        LogUtil.d("ProgressBarActivity","触点相对于VIEW的坐标--"+ev.getY());
        LogUtil.d("ProgressBarActivity","view的宽度--"+view.getWidth());
        LogUtil.d("ProgressBarActivity","view的高度--"+view.getHeight());
        LogUtil.d("ProgressBarActivity","触点相对于window的坐标x--"+ev.getRawX());
        LogUtil.d("ProgressBarActivity","触点相对于window的坐标y--"+ev.getRawY());
        return  r.contains((int)ev.getX(), (int)ev.getX());
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //View view = LayoutInflater.from(this).inflate(R.layout.activity_progressbar,null);
        View view = findViewById(R.id.ProgressLayout);
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int x = location[0]; // view距离window 左边的距离（即x轴方向）
        int y = location[1]; // view距离window 顶边的距离（即y轴方向）

        LogUtil.d("ProgressBarActivity","x:"+x);
        LogUtil.d("ProgressBarActivity","y:"+y);

        LogUtil.d("ProgressBarActivity","rect.width()"+rect.width());
        LogUtil.d("ProgressBarActivity","rect.height()"+rect.height());
        LogUtil.d("ProgressBarActivity","view.getLeft()"+ view.getLeft());
        LogUtil.d("ProgressBarActivity","view.getTop()"+view.getTop());
        LogUtil.d("ProgressBarActivity","view.getRight()"+view.getRight());
        LogUtil.d("ProgressBarActivity","view.getBottom()"+view.getBottom());


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
                    //activity.progressBar.setVisibility(View.GONE);
                    break;
            }

        }
    }

}
