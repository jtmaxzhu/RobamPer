package com.robam.rper.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.service.base.BaseService;
import com.robam.rper.tools.AppInfoProvider;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.tools.CmdTools;
import com.robam.rper.util.LogUtil;

import java.lang.ref.WeakReference;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.view.Surface.ROTATION_0;

public class MonkeyFloatService extends BaseService {

    private Handler handler;
    private static final String TAG = "MonkeyFloatService";
    private static final int UPDATE_RECORD_TIME = 103;
    private InjectorService mInjectorService;

    private AppInfoProvider provider = null;

    // Views
    List<WeakReference<View>> displayedViews = null;

    WindowManager wm = null;
    WindowManager.LayoutParams wmParams = null;

    /**
     * 显示
     */
    View floatDisplayWrapper;
    LinearLayout floatDisplay;

    LinearLayout extraButton;
    LinearLayout extraView;

    /**
     * 悬浮卡图标
     */
    private ImageView cardIcon;

    /**
     * 缩小状态图标
     */
    private ImageView backgroundIcon;

    /**
     * 悬浮卡
     */
    private LinearLayout cardView;

    private OnRunListener runListener = null;

    private OnFloatListener floatListener = null;

    private OnStopListener stopListener = null;

    private int recordCount = 0;
    private boolean isCountTime = false;

    // Draw
    private float mTouchStartX;
    private float mTouchStartY;
    private float x;
    private float y;
    int state, lastState;
    private float StartX;
    private float StartY;
    int div = 0;
    private int statusBarHeight = 0;
    // IO
    String fileName = "current.log";


    /**
     * 默认屏幕方向为垂直
     */
    private int currentOrientation = ROTATION_0;

    private String appPackage = "";
    private String appName = "";

    Timer timer = new Timer("check monkey");
    /**
     * 定时任务
     */
    private TimerTask CLEAR_FILES_TASK = new TimerTask() {

        @Override
        public void run() {
            MyApplication.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateView();
                }
            });

        }
    };


    public MonkeyFloatService() {
    }

    private volatile boolean FLAG = true;
    private volatile int imageId = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createView();
        //timer.schedule(CLEAR_FILES_TASK, 0, 1 * 500);
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while(FLAG){
                    updateView();
                }

            }
        });




    }

    /**
     * 悬浮窗根节点
     */
    View view;
    /** 关闭按钮 */
    ImageView close;
    /** 启停按钮 */
    ImageView startAndstop;


    /**
     * 更新界面位置
     */
    private void updateViewPosition() {
        // 更新浮动窗口位置参数
        wmParams.x = (int) (x - mTouchStartX);
        wmParams.y = (int) (y - mTouchStartY);
        wmParams.alpha = 1F;
        wm.updateViewLayout(view, wmParams);
    }

    public static final int RECORDING_ICON = R.drawable.recording;
    public static final int PLAY_ICON = R.drawable.start;

    private void createView() {
        view = LayoutInflater.from(this).inflate(R.layout.float_monkey, null);
        // 关闭按钮
        close = (ImageView) view.findViewById(R.id.closeIcon);
        startAndstop = (ImageView) view.findViewById(R.id.recordIcon);

        // 悬浮卡图标与缩小图标
        cardIcon = (ImageView) view.findViewById(R.id.float_card_icon);
        backgroundIcon = (ImageView) view.findViewById(R.id.floatIcon);
        backgroundIcon.setVisibility(View.GONE);
        cardView = (LinearLayout) view.findViewById(R.id.float_card);

        // 获取WindowManager
        wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        // 设置LayoutParams(全局变量）相关参数
        wmParams = ((MyApplication) getApplication()).getFloatWinParams();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 注意TYPE_SYSTEM_ALERT从Android8.0开始被舍弃了
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            // 从Android8.0开始悬浮窗要使用TYPE_APPLICATION_OVERLAY
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        wmParams.flags |= 8;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP; // 调整悬浮窗口至左上角
        // 以屏幕左上角为原点，设置x、y初始值
        wmParams.x = 0;
        wmParams.y = 0;
        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.format = 1;
        wmParams.alpha = 1F;

        displayedViews = new ArrayList<>();

        wm.addView(view, wmParams);

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
                        lastState = state;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        state = MotionEvent.ACTION_MOVE;
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
                        break;
                }
                return false;
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stopListener != null) {
                    // 看看监听器是不是要停止
                    boolean result = stopListener.onStopClick();
                    if (!result) {
                        return;
                    }
                }

                MonkeyFloatService.this.stopSelf();
                view.setVisibility(View.GONE);
                FLAG = false;
                //timer.cancel();
                //updateView();
                LogUtil.i(TAG, "Stop self");
            }
        });


        // 悬浮卡点击图标变成缩小状态
        cardIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (floatListener != null) {
                    floatListener.onFloatClick(true);
                } else {
                    cardView.setVisibility(View.GONE);
                    backgroundIcon.setVisibility(View.VISIBLE);
                }
            }
        });

        // 录制按钮
        startAndstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey", 5000).equals("")){
                        MyApplication.getInstance().showToast("Monkey线程不存在");
                    }else {
                        String pid = CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey",5000).substring(13, 18);
                        LogUtil.d(TAG, "__________"+pid);
                        String kill = "kill " + pid;
                        CmdTools.execAdbCmd(kill, 5000);
                        MyApplication.getInstance().showToast("Monkey线程中止");
                    }
                }else {
                    if(CmdTools.execAdbCmd("ps | grep com.android.commands.monkey", 5000).equals("")){
                        MyApplication.getInstance().showToast("Monkey线程不存在");
                    }else {
                        String pid = CmdTools.execAdbCmd("ps | grep com.android.commands.monkey",5000).substring(10, 15);
                        LogUtil.d(TAG, "______________________"+pid);
                        String kill = "kill " + pid;
                        CmdTools.execAdbCmd(kill, 5000);
                        MyApplication.getInstance().showToast("Monkey线程中止");
                    }
                }

            }
        });


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

    }



    private void updateView() {
        LogUtil.d(TAG,"kaishi"+CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey", 5000));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey", 5000).equals("")){
                startAndstop.setImageResource(PLAY_ICON);

            }else {
                startAndstop.setImageResource(RECORDING_ICON);

            }
        }else {
            MyApplication.getInstance().showToast(""+CmdTools.execAdbCmd("ps | grep com.android.commands.monkey", 5000).equals(""));
            if(CmdTools.execAdbCmd("ps | grep com.android.commands.monkey", 5000).equals("")){
                startAndstop.setImageResource(PLAY_ICON);

            }else {
                startAndstop.setImageResource(RECORDING_ICON);

            }
        }


    }


    @Override
    public IBinder onBind(Intent intent) {
        return new FloatBinder(this);
    }



    public static class FloatBinder extends Binder {
        private static final String TAG = "FloatBinder";
        private WeakReference<MonkeyFloatService> floatWinServiceRef;

        private FloatBinder(MonkeyFloatService service) {
            this.floatWinServiceRef = new WeakReference<>(service);
        }

        public Context loadServiceContext() {
            return floatWinServiceRef.get();
        }

    }

    /**
     * 隐藏悬浮窗
     */
    private void hideFloatWin() {
        cardView.setVisibility(View.GONE);
        Display screenDisplay = ((WindowManager) MonkeyFloatService.this.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        screenDisplay.getSize(size);
        x = size.x;

        //y = (size.y - statusBarHeight) / 2;
        y = size.y / 2 - 4 * statusBarHeight;
        updateViewPosition();
        // handler.removeCallbacks(task);
        backgroundIcon.setVisibility(View.VISIBLE);
        backgroundIcon.setAlpha(0.5f);
    }

    private void restoreFloatWin() {
        startAndstop.setImageResource(R.drawable.start);
        cardView.setVisibility(View.VISIBLE);
        backgroundIcon.setVisibility(View.GONE);
        backgroundIcon.setAlpha(1.0f);
    }





    public interface OnRunListener {
        int onRunClick();
    }

    public interface OnFloatListener {
        void onFloatClick(boolean hide);
    }

    public interface OnStopListener {
        boolean onStopClick();
    }



}
