package com.robam.rper.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.annotation.Param;
import com.robam.rper.service.MonkeyFloatService;
import com.robam.rper.tools.BackgroundExecutor;
import com.robam.rper.tools.CmdTools;
import com.robam.rper.ui.HeadControlPanel;
import com.robam.rper.util.FileUtils;
import com.robam.rper.util.GlideUtil;
import com.robam.rper.util.LogUtil;
import com.robam.rper.util.StringUtil;


import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;


@EntryActivity(icon = R.drawable.monkey, name = "压力测试", permissions = {"adb", "float"}, index = 2)
public class MonkeyActivity extends BaseActivity {

    public static String cmd = "wm size";
    private static final String TAG = MonkeyActivity.class.getSimpleName();
    public static final String NEED_REFRESH_PAGE = "NEED_REFRESH_PAGE";

    public static final String NEED_REFRESH_CASES_LIST = "NEED_REFRESH_CASES_LIST";

    private DrawerLayout mDrawerLayout;

    private View mAppListContainer;
    private ListView mAppListView;
    private AppAdapter mAdapter;
    private View mSwitchApp;
    private EditText monkeyParam;
    private Button jb;
    private Button ml;
    private Button startmonkey;
    private String app;
    private String pkName="appName";
    private volatile boolean FLAG = true;




    private List<ApplicationInfo> mListPack;
    private ApplicationInfo mCurrentApp;

    private HeadControlPanel mPanel;

    public static Timer timer;

    final SimpleDateFormat myFmt1 = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monkey);



        initDrawerLayout();
        initAppList();
        initHeadPanel();
        final Intent intent = new Intent(MonkeyActivity.this, MonkeyFloatService.class);

        startmonkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLAG=true;
                if (monkeyParam.getText().toString().equals("")){
                    MyApplication.getInstance().showToast("请先填充参数");
                }else{
                    BackgroundExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            String Monkey="", appName="";
                            long now = System.currentTimeMillis();
                            if (MyApplication.getInstance().hashMap.get("MyAppName") != null){
                                //Monkey = "monkey"+" "+monkeyParam.getText().toString()+" 1> /sdcard/Monkeylog/monkey"+myFmt1.format(now)+".txt 2> /sdcard/Monkeylog/error"+myFmt1.format(now)+".txt";
                                appName = (String) MyApplication.getInstance().hashMap.get("MyAppName");
                            }else{
                               //Monkey = "monkey -p "+mCurrentApp.packageName+" "+monkeyParam.getText().toString()+" 1> /sdcard/Monkeylog/monkey"+myFmt1.format(now)+".txt 2> /sdcard/Monkeylog/error"+myFmt1.format(now)+".txt";
                                appName = mCurrentApp.packageName;
                            }

                            if (monkeyParam.getText().toString().contains("-f")){
                                Monkey = "monkey"+" "+monkeyParam.getText().toString()+" 1> /sdcard/Monkeylog/monkey"+myFmt1.format(now)+".txt 2> /sdcard/Monkeylog/error"+myFmt1.format(now)+".txt";
                                LogUtil.d(TAG, Monkey);
                                if (FileUtils.isExists(monkeyParam.getText().toString().substring(monkeyParam.getText().toString().lastIndexOf("/")+1,monkeyParam.getText().toString().lastIndexOf("."))+".txt")){
                                    MyApplication.getInstance().showToast("进入脚本模式......");
                                    //CmdTools.execAdbCmd("am start "+appName, 5000);

                                    CmdTools.execAdbCmd(Monkey, 5000);
                                    //timer.schedule(CLEAR_FILES_TASK, 5*1000, 5 * 1000);
                                    LogUtil.d(TAG,"服务启动");
                                    startService(intent);
                                    BackgroundExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            while (FLAG){
                                                try {
                                                    TimeUnit.SECONDS.sleep(5);
                                                    LogUtil.d(TAG, "定时查询--");
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                                        if (CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey ", 5000).equals("")){
                                                            getNotification();
                                                            FLAG=false;
                                                            LogUtil.d(TAG, "定时关闭--");
                                                        }
                                                    }else {
                                                        if (CmdTools.execAdbCmd("ps | grep com.android.commands.monkey ", 5000).equals("")){
                                                            getNotification();
                                                            FLAG=false;
                                                            LogUtil.d(TAG, "定时关闭--");
                                                        }
                                                    }

                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                        }
                                    });
                                }else {
                                    MyApplication.getInstance().showToast("脚本不存在");
                                }

                            }else{
                                Monkey = "monkey -p "+appName+" "+monkeyParam.getText().toString()+" 1> /sdcard/Monkeylog/monkey"+myFmt1.format(now)+".txt 2> /sdcard/Monkeylog/error"+myFmt1.format(now)+".txt";
                                MyApplication.getInstance().showToast("进入命令行模式......");
                                //CmdTools.execAdbCmd("am start "+appName, 5000);
                                CmdTools.execAdbCmd("input keyevent 3", 5000);
                                LogUtil.d(TAG, Monkey);
                                MyApplication.getInstance().showToast("Monkey启动......");
                                CmdTools.execAdbCmd(Monkey, 5000);
                                LogUtil.d(TAG,"服务启动");
                                startService(intent);
                                BackgroundExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (FLAG){
                                            try {
                                                TimeUnit.SECONDS.sleep(5);
                                                LogUtil.d(TAG, "定时查询--");
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                                    if (CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey ", 5000).equals("")){
                                                        getNotification();
                                                        FLAG=false;
                                                        LogUtil.d(TAG, "定时关闭--");
                                                    }
                                                }else {
                                                    if (CmdTools.execAdbCmd("ps | grep com.android.commands.monkey ", 5000).equals("")){
                                                        getNotification();
                                                        FLAG=false;
                                                        LogUtil.d(TAG, "定时关闭--");
                                                    }
                                                }

                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        jb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monkeyParam.setText("-f /sdcard/monkey.txt --ignore-crashes --ignore-timeouts -v -v -v 10");

               // stopService(intent);

            }
        });

        ml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monkeyParam.setText("-s 200 --pct-touch 40 --pct-motion 25 --pct-appswitch 10 --pct-rotation 10 --throttle 300 --ignore-crashes --ignore-timeouts -v -v -v 1000");

            }
        });
    }

    /**
     * 定时任务
     */
   /* private  TimerTask CLEAR_FILES_TASK = new TimerTask() {

        @Override
        public void run() {
            LogUtil.d(TAG, "定时器--");
            if (CmdTools.execAdbCmd("ps -A | grep com.android.commands.monkey ", 5000).equals("")){
                getNotification();
                timer.cancel();
                LogUtil.d(TAG, "定时器关闭");
            }
        }
    };*/

    private void getNotification() {
        String id = "channel_001";
        String name = "name";
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        Intent intent = new Intent(this, MonkeyActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0, intent,0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId(id)
                    .setContentTitle("通知")
                    .setContentText("测试完成,请在/sdcard/Monkeylog查看运行日志")
                    .setContentIntent(pi)
                    .setSmallIcon(R.drawable.monkey).build();
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("通知")
                    .setContentText("测试完成,请在/sdcard/Monkeylog查看运行日志")
                    .setContentIntent(pi)
                    .setSmallIcon(R.drawable.monkey)
                    .setOngoing(true)
                    .setChannelId(id);//无效
            notification = notificationBuilder.build();
        }
        notificationManager.notify(1, notification);
    }

    @Subscriber(@Param(SubscribeParamEnum.APP))
    public void setApp(String app) {
        this.app = app;
    }

    private void initDrawerLayout(){
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);//关闭手势滑动
        mAppListContainer = findViewById(R.id.app_list_container);
        startmonkey = findViewById(R.id.start_monkey);
        jb = findViewById(R.id.set_monkey_jb);
        ml = findViewById(R.id.set_monkey_ml);
        monkeyParam = findViewById(R.id.monkey_input);

    }


    private void initHeadPanel(){
        mPanel = (HeadControlPanel) findViewById(R.id.head_layout);
        mPanel.setMiddleTitle("压力测试");
        mPanel.setBackIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public static String Monkey="", appName="";

    private void initAppList(){
        mAppListView = findViewById(R.id.app_list);
        mSwitchApp = findViewById(R.id.switch_app);
        mListPack = MyApplication.getInstance().loadAppList();

        int position = 0;
        if(!StringUtil.isEmpty(app)){
            for (int i = 0; i < mListPack.size(); i++) {
                if (StringUtil.equals(mListPack.get(i).packageName, app)) {
                    position = i;
                    break;
                }
            }
        }
        mAdapter = new AppAdapter();
        mAppListView.setAdapter(mAdapter);
        mCurrentApp = mListPack.get(position);
        updateHeadView();

        mSwitchApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(mAppListContainer);
            }
        });




        mAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerLayout.closeDrawer(mAppListContainer);
                mCurrentApp = (ApplicationInfo) mAdapter.getItem(position);
                MyApplication.getInstance().hashMap.put("AppLabel",mCurrentApp.loadLabel(getPackageManager()));
                MyApplication.getInstance().hashMap.put("AppIcon",mCurrentApp.loadIcon(getPackageManager()));
                MyApplication.getInstance().hashMap.put("MyAppName",mCurrentApp.packageName);
                //((MyApplication)getApplication()).updateAppAndName(mCurrentApp.packageName, mCurrentApp.loadLabel(getPackageManager()).toString());
                updateHeadView();


            }
        });
    }

    private ImageView mAppIcon;
    private TextView mAppLabel;
    private TextView mAppPkgName;

    private void updateHeadView() {
        mAppIcon = (ImageView) findViewById(R.id.test_app_icon);
        mAppLabel = (TextView) findViewById(R.id.test_app_label);
        mAppPkgName = (TextView) findViewById(R.id.test_app_pkg_name);
        if (MyApplication.getInstance().hashMap.get("MyAppName") != null){
            mAppIcon.setImageDrawable((Drawable) MyApplication.getInstance().hashMap.get("AppIcon"));
            mAppLabel.setText((CharSequence) MyApplication.getInstance().hashMap.get("AppLabel"));
            mAppPkgName.setText((String)MyApplication.getInstance().hashMap.get("MyAppName"));
        }else{
            mAppIcon.setImageDrawable(mCurrentApp.loadIcon(getPackageManager()));
            mAppLabel.setText(mCurrentApp.loadLabel(getPackageManager()));
            mAppPkgName.setText(mCurrentApp.packageName);
        }


    }

    private class AppAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mListPack.size();
        }

        @Override
        public Object getItem(int position) {
            return mListPack.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null){
                convertView = LayoutInflater.from(MonkeyActivity.this).inflate(R.layout.item_app_list, parent, false);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.app_icon);
                holder.name = convertView.findViewById(R.id.app_name);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            ApplicationInfo info = (ApplicationInfo) getItem(position);
            GlideUtil.loadIcon(MonkeyActivity.this, info.packageName, holder.icon);
            holder.name.setText(info.loadLabel(getPackageManager()));
            return convertView;
        }


        class ViewHolder {
            ImageView icon;
            TextView name;
        }
    }


}
