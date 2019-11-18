package com.robam.rper.service;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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
import com.robam.rper.util.ContextUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.Surface.ROTATION_0;

public class PerFloatService extends BaseService {

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
        updateViewPosiyion();
        backgroundIcon.setVisibility(View.VISIBLE);
        backgroundIcon.setAlpha(0.5F);
    }

    /**
     * 更新界面位置
     */
    private void updateViewPosiyion(){
        //更新浮动窗口位置
        wmParams.x = (int)(x - mTouchStartX);
        wmParams.y = (int)(y - mTouchStartY);
        wmParams.alpha = 1F;
        wm.updateViewLayout(view, wmParams);
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
}
