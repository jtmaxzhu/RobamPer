package com.robam.rper.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
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
import com.robam.rper.util.AppUtil;
import com.robam.rper.util.ContextUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.StringUtil;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.Surface.ROTATION_0;

public class PerFloatService extends BaseService {


    private static final int UPDATE_RECORD_TIME = 103;
    private static final String TAG = "PerFloatService";

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
        LogUtil.d(TAG, "进入setAppName");
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

    @Subscriber(@Param(MyApplication.SCREEN_ORIENTATION))
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

    @Subscriber(value = @Param(value = MyApplication.SHOW_LOADING_DIALOG, sticky = false), thread = RunningThread.MAIN_THREAD )
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
     * 恢复悬浮窗
     */
    private void restoreFloatWin(){
        record.setImageResource(R.drawable.start);
        cardView.setVisibility(View.VISIBLE);
        backgroundIcon.setVisibility(View.GONE);
        backgroundIcon.setAlpha(1.0f);
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
        titlePanal = view.findViewById(R.id.float_title);

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
        wmParams.alpha = 1.0F;

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
                        if (Math.abs(x-StartX)<10 && Math.abs(y-StartY)<10 && backgroundIcon.getVisibility() == View.VISIBLE){
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
                        }else if (result == PLAY_ICON){
                            recordCount = 0;
                            isCountTime = false;
                            recordTime.setVisibility(View.INVISIBLE);
                        }
                    }else {
                        if (isCountTime){
                            recordTime.setVisibility(View.INVISIBLE);
                            isCountTime = false;
                        }
                    }
                }
            }
        });
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        wakeLock.setReferenceCounted(false);
    }

    /**
     * 初始化数据
     */
    private void initData(String name){
        if (appText != null){
            appText.setText(appName);
        }
    }

    @Override
    public void onCreate() {
        LogUtil.d(TAG, "onCreate");
        super.onCreate();
        handler = new TimeProcessHandler(this);
        mInjectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
        mInjectorService.register(this);
        if (provider == null){
            provider = new AppInfoProvider();
            mInjectorService.register(provider);
        }
        createView();
        initData(appName);

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
        return new PerFloatBinder(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.d(TAG,"进入onUnbind");
        runListener = null;
        floatListener = null;
        stopListener = null;
        stopForeground(true);
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // 清理定时任务
        mInjectorService.unregister(this.provider);
        this.provider = null;

        LogUtil.w(TAG, "FloatWin onDestroy");
        writeFileData(fileName, "destroy recording:" + new Date());
        div = 0;
        //
        wm.removeView(view);

        //InjectorService.getInstance().stopInjection();

        SharedPreferences sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("state", "stop");
        editor.apply();
        // 取消注册广播
        super.onDestroy();
    }

    public void writeFileData(String fileName, String message) {
        try {
            FileOutputStream fout = openFileOutput(fileName, MODE_APPEND);

            byte[] bytes = message.getBytes();
            fout.write(bytes);
            bytes = "\n".getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "Catch Exception: " + e.getMessage(), e);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification=null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("channel_001", "RobamPer", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId("channel_001")
                    .setContentTitle("通知")
                    .setContentText("RobamPer悬浮窗正在运行")
                    .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true).build();
        }else{
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("通知")
                    .setContentText("RobamPer悬浮窗正在运行")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setAutoCancel(true).build();
        }
        startForeground(1, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    public static class PerFloatBinder extends Binder{
        private static final String TAG = "PerFloatBinder";
        private WeakReference<PerFloatService> perFloatServiceRefer;

        public PerFloatBinder(PerFloatService perFloatServiceRefer) {
            this.perFloatServiceRefer = new WeakReference<>(perFloatServiceRefer);
        }

        public Context loadServiceContext(){
            return perFloatServiceRefer.get();
        }

        /**
         * 提供主窗体
         *
         * @param baseView
         * @param params
         */
        public void provideDisplayView(final View baseView, final LinearLayout.LayoutParams params){
            if (perFloatServiceRefer.get() == null){
                return;
            }
            final PerFloatService service = perFloatServiceRefer.get();
            service.handler.post(new Runnable() {
                @Override
                public void run() {
                    View floatWrapper = service.floatDisplayWrapper;
                    LinearLayout floatDisplay = service.floatDisplay;
                    floatDisplay.removeAllViews();
                    if (baseView != null){
                        if (params == null){
                            floatDisplay.addView(baseView);
                        }else {
                            floatDisplay.addView(baseView, params);
                        }
                        floatWrapper.setVisibility(View.VISIBLE);
                    }else {
                        floatWrapper.setVisibility(View.GONE);
                    }

                }
            });
        }

        /**
         * 扩展窗体
         * @param expendView
         * @param params
         */
        public void provideExpendView(final View expendView, final WindowManager.LayoutParams params){
            if (perFloatServiceRefer.get() == null){
                return;
            }
            final PerFloatService service = perFloatServiceRefer.get();
            service.handler.post(new Runnable() {
                @Override
                public void run() {
                    LinearLayout expend = service.extraView;
                    LinearLayout expendButton = service.extraButton;
                    expend.removeAllViews();
                    if (expendView != null){
                        if (params == null){
                            expend.addView(expendView);
                        }else{
                            expend.addView(expendView, params);
                        }
                    }
                }
            });
        }

        /**
         * 添加view
         * @param v
         * @param params
         */
        public void addView(View v, WindowManager.LayoutParams params){
            PerFloatService service = perFloatServiceRefer.get();
            service.wm.addView(v, params);
            service.displayedViews.add(new WeakReference<View>(v));
        }

        /**
         * 清理特定View
         * @param v
         */

        public void removeView(View v){
            PerFloatService service = perFloatServiceRefer.get();
            service.wm.removeView(v);
            Iterator<WeakReference<View>> refIter = service.displayedViews.iterator();
            while(refIter.hasNext()){
                WeakReference<View> ref = refIter.next();
                if (ref.get() == null){
                    refIter.remove();
                }
                // 清理目标View
                if (ref.get() == v) {
                    refIter.remove();
                    break;
                }
            }
        }

        /**
         * 清理全部View
         */

        public void removeAllViews(){
            PerFloatService service = perFloatServiceRefer.get();
            Iterator<WeakReference<View>> refIter = service.displayedViews.iterator();
            while(refIter.hasNext()){
                WeakReference<View> ref = refIter.next();
                if (ref.get() != null){
                    View v = ref.get();
                    service.wm.removeView(v);
                }
                refIter.remove();
            }
        }

        public void registerRunClickListener(OnRunListener listener) {
            PerFloatService service = perFloatServiceRefer.get();
            service.runListener = listener;
        }

        public void registerFloatClickListener(OnFloatListener listener) {
            PerFloatService service = perFloatServiceRefer.get();
            service.floatListener = listener;
        }

        public void registerStopClickListener(OnStopListener listener) {
            PerFloatService service = perFloatServiceRefer.get();
            service.stopListener = listener;
        }

        /**
         * 隐藏悬浮窗
         */
        public void hideFloat(){
            final PerFloatService service = perFloatServiceRefer.get();
            service.handler.post(new Runnable() {
                @Override
                public void run() {
                    service.hideFloatWin();
                }
            });
        }

        /**
         * 恢复悬浮窗
         */
        public void restoreFloat() {
            final PerFloatService service = perFloatServiceRefer.get();
            service.handler.post(new Runnable() {
                @Override
                public void run() {
                    service.restoreFloatWin();
                }
            });
        }

        /**
         * 停止悬浮窗
         */
        public void stopFloat(){
            final PerFloatService service = perFloatServiceRefer.get();
            service.handler.post(new Runnable() {
                @Override
                public void run() {
                    LogUtil.d(TAG,"停止服务");
                    service.stopSelf();
                }
            });
        }

        /**
         * 更新悬浮窗小图标
         */
        public void updateFloatIcon(int res){
            final PerFloatService service = perFloatServiceRefer.get();
            service.backgroundIcon.setImageResource(res);
            service.cardIcon.setImageResource(res);
        }

        /**
         * 检查用户点击位置是否再悬浮窗内
         */
        public boolean checkInFloat(Point point){
            if (point == null){
                return false;
            }
            PerFloatService service = perFloatServiceRefer.get();
            Rect rect = new Rect();

            service.view.getDrawingRect(rect);

            // 通过当前LayoutParam进行判断
            Rect r = new Rect();
            service.view.getWindowVisibleDisplayFrame(r);
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) service.view.getLayoutParams();

            int x = r.left + params.x;
            int y = r.top + params.y;

            // 对于超过边界的情况
            if (x > r.right - rect.width()) {
                x = r.right - rect.width();
            }

            if (y > r.bottom - rect.height()) {
                y = r.bottom - rect.height();
            }

            LogUtil.d("FloatWinService", "悬浮窗坐标包含：%s, 目标x: %f, 目标y: %f", rect, point.x - service.x + rect.right, point.y - service.y - service.statusBarHeight);
            return rect.contains(point.x - x, point.y - y);
        }
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
