package com.robam.rper.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
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
import com.robam.rper.activity.IndexActivity;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.annotation.Param;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.injector.param.RunningThread;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.service.base.BaseService;
import com.robam.rper.tools.AppInfoProvider;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.tools.CmdTools;
import com.robam.rper.util.AppUtil;
import com.robam.rper.util.ContextUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.Surface.ROTATION_0;

public class PerFloatService extends BaseService {


    private static final int UPDATE_RECORD_TIME = 103;
    private static final String TAG = "FloatWinService";

    public static final int RECORDING_ICON = R.drawable.recording;
    public static final int PLAY_ICON = R.drawable.start;
    //views
    List<WeakReference<View>> displayedViews = null;

    WindowManager wm = null;
    WindowManager.LayoutParams wmParams = null;

    /**
     * 悬浮窗根节点
     */
    View view;
    /**
     * 关闭按钮
     */
    ImageView close;

    /**
     * 录制按钮
     */
    ImageView record;

    /**
     * 显示
     */
    View floatDisplayWrapper;
    LinearLayout floatDisplay;
    LinearLayout extraButton;
    LinearLayout extraView;

    /**
     * 数据列表标题
     */
    LinearLayout titlePanal;

    /**
     * 录制时间文字
     */
    TextView recordTime;

    /**
     * 应用名称文字
     */
    private TextView appText;

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

    private float mTouchStartX;
    private float mTouchStartY;
    private float x;
    private float y;
    int state, lastState;
    private float StartX;
    private float StartY;
    int div = 0;
    private int statusBarHeight = 0;
    private AppInfoProvider provider = null;

    //Io
    String fileName = "current.log";
    private InjectorService mInjectorService;

    /**
     * 默认屏幕方向垂直
     */
    private int currentOrientation = ROTATION_0;
    private String appPackage = "";
    private String appName = "";

    @Subscriber(@Param(SubscribeParamEnum.APP))
    public void setAppPackage(String appPackage){
        this.appPackage = appPackage;
    }

    private Handler handler;

    @Subscriber(@Param(SubscribeParamEnum.APP_NAME))
    public void setAppName(final String appName){
        if (!StringUtil.equals(this.appName, appName)){
            this.appName = appName;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    appText.setText(appName);
                }
            });
        }
    }

    public void setScreenOrientation(int orientation){
        if (orientation != currentOrientation){
            currentOrientation = orientation;
            //更新下位置坐标
            if (cardView.getVisibility() == View.GONE){
                hideFloatWin();
            }
        }
    }

    /**
     * 加载dialog
     */
    private AlertDialog loadingDialog;
    private TextView messageText;

    @Subscriber(value = @Param(value = MyApplication.SHOW_LOADING_DIALOG, sticky = false) )
    public void startDialog(String message){
        //调用了长时间加载
        if (loadingDialog == null){
            View v = LayoutInflater.from(ContextUtil.getContextThemeWrapper(this, R.style.AppDialogTheme))
                    .inflate(R.layout.dialog_loading, null);
            messageText = v.findViewById(R.id.loading_dialog_text);
            loadingDialog =  new AlertDialog.Builder(this, R.style.AppDialogTheme)
                    .setView(v)
                    .setNegativeButton("隐藏", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            // 设置dialog
            loadingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            loadingDialog.setCanceledOnTouchOutside(false);                                   //点击外面区域不会让dialog消失
            loadingDialog.setCancelable(false);
        }
        messageText.setText(message);
        loadingDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        // 显示加载提示窗
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }

    }

    @Subscriber(value = @Param(value = MyApplication.DISMISS_LOADING_DIALOG, sticky = false), thread = RunningThread.MAIN_THREAD)
    public void dismissDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }



    /**
     * 隐藏悬浮窗
     */
    private void hideFloatWin(){
        cardView.setVisibility(View.GONE);
        Display screenDisplay = ((WindowManager)PerFloatService.this.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        screenDisplay.getSize(size);
        x = size.x;
        y = (size.y / 2) - 4*statusBarHeight;
        updateViewPosition();
        backgroundIcon.setVisibility(View.VISIBLE);
        backgroundIcon.setAlpha(0.5F);
    }

    /**
     * 更新界面位置
     */
    private void updateViewPosition(){
        //更新浮动窗口位置
        wmParams.x = (int)(x - mTouchStartX);
        wmParams.y = (int)(y - mTouchStartY);
        wmParams.alpha = 1F;
        wm.updateViewLayout(view, wmParams);
    }

    private void createView(){
        view = LayoutInflater.from(this).inflate(R.layout.float_per, null);
        //关闭按钮
        close = view.findViewById(R.id.closeIcon);
        //数据采集按钮
        record = view.findViewById(R.id.recordIcon);
        //录制文字
        recordTime = view.findViewById(R.id.recordText);
        recordTime.setVisibility(View.GONE);
        appText =  view.findViewById(R.id.float_title_app);
        appText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PackageInfo pkgInfo = getPackageManager().getPackageInfo(appPackage,0);
                    if (pkgInfo == null){
                        return;
                    }
                    AppUtil.startApp(pkgInfo.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
        titlePanal = (LinearLayout) view.findViewById(R.id.float_title);

        //主窗体
        floatDisplayWrapper = view.findViewById(R.id.float_display_wrapper);
        floatDisplay = view.findViewById(R.id.float_display_view);
        floatDisplayWrapper.setVisibility(View.GONE);

        //额外窗体
        extraView = view.findViewById(R.id.float_extra_layout);
        extraButton = view.findViewById(R.id.float_expand_layout);
        extraView.setVisibility(View.GONE);
        extraButton.setVisibility(View.GONE);

        final ImageView expandButton = view.findViewById(R.id.float_expand_icon);
        extraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandButton.getRotation() % 360 == 0){
                    extraView.setVisibility(View.VISIBLE);
                }else{
                    extraView.setVisibility(View.GONE);
                }
                expandButton.setRotation(expandButton.getRotation() + 180);
            }
        });

        //悬浮卡图标
        cardIcon = view.findViewById(R.id.float_card_icon);
        backgroundIcon = view.findViewById(R.id.floatIcon);
        backgroundIcon.setVisibility(View.GONE);
        cardView = view.findViewById(R.id.float_card);

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
        //获得windowManager
        wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        wmParams = ((MyApplication)getApplication()).getFloatWinParams();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 注意TYPE_SYSTEM_ALERT从Android8.0开始被舍弃了
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            // 从Android8.0开始悬浮窗要使用TYPE_APPLICATION_OVERLAY
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        wmParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;//这个窗口不会得到键输入焦点用户不能发送键或其他按钮事件给它
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;//悬浮窗左上角
        //屏幕左上角为原点，设置初始值
        wmParams.x = 0;
        wmParams.y = 0;
        //设置悬浮窗长宽
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.format = 1;
        wmParams.alpha = 1F;

        displayedViews = new ArrayList<>();
        wm.addView(view, wmParams);
        view.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //获取相对屏幕坐标,以屏幕左上角为原点
                x = event.getRawX();
                y = event.getRawY() - statusBarHeight;
                LogUtil.i(TAG, "currX" + x + "====currY" + y);
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        state = MotionEvent.ACTION_DOWN;
                        StartX = x;
                        StartY = y;
                        //相对view的坐标，view左上角为原点
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
                        //缩小状态下点击移动小于10，10 恢复成原始状态
                        if (Math.abs(x-StartX)<10 && Math.abs(y-StartY)<10 && backgroundIcon.getVisibility() == View.GONE){
                            //如果注册悬浮窗监听器
                            if (floatListener != null){
                                floatListener.onFloatClick(false);
                            }else {
                                cardView.setVisibility(View.VISIBLE);
                                backgroundIcon.setVisibility(View.GONE);
                            }
                        }
                        mTouchStartY = mTouchStartX = 0;
                        lastState = state;
                        break;
                }
                return false;
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stopListener != null){
                    boolean result = stopListener.onStopClick();
                    if (!result){
                        return;
                    }
                }
                PerFloatService.this.stopSelf();
            }
        });

        ImageView homeButton = view.findViewById(R.id.homeIcon);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * 服务使用startActivity直接启动activity可能会比较慢，GooGle这样设计避免用户毫不知情的情况下突然中断用户的操作
                 * 这里使用PendingIntent启动
                 */
                Intent intent = new Intent(PerFloatService.this, IndexActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(PerFloatService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                try{
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    LogUtil.e(TAG, "PendingIntent canceled ", e);
                }
            }
        });

        //悬浮卡点击图标变缩小状态
        cardIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (floatListener != null){
                    floatListener.onFloatClick(true);
                }else {
                    cardView.setVisibility(View.GONE);
                    backgroundIcon.setVisibility(View.VISIBLE);
                }
            }
        });

        //录制
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runListener != null){
                    int result = runListener.onRunClick();
                    if (result != 0){
                        record.setImageResource(result);
                        if (result == RECORDING_ICON){
                            recordCount = 0;
                            isCountTime = true;
                            recordTime.setVisibility(View.VISIBLE);
                            handler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME,1000);
                        }
                    }

                }
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public interface OnRunListener{
        int onRunClick();
    }

    public interface OnFloatListener{
        void onFloatClick(boolean hide);
    }

    public interface OnStopListener{
        boolean onStopClick();
    }


    /**
     *
     * @param intent
     * @return
     */
    @Override

    public IBinder onBind(Intent intent) {
        return null;
    }


    private static final class TimeProcessHandler extends Handler{
        private WeakReference<PerFloatService> serviceRef;

        public TimeProcessHandler(PerFloatService service) {
            this.serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PerFloatService service = serviceRef.get();
            if (service == null){
                return;
            }
            switch (msg.what){
                case UPDATE_RECORD_TIME:
                    //每秒增加recordCount，作为已录制的时间
                    service.recordCount++;
                    service.recordTime.setText(timefyCount(service.recordCount));
                    if (service.isCountTime){
                        sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
                    }
                    break;
            }

            super.handleMessage(msg);
        }

        /**
         * 将秒数转化为xx:xx格式
         * @param count 秒数
         * @return 转化后的字符串
         */
        private static String timefyCount(int count) {
            return String.format(Locale.CHINA, "%02d:%02d", count / 60, count % 60);
        }
    }
}
