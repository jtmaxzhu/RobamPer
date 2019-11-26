package com.robam.rper.serviceTest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.robam.rper.R;
import com.robam.rper.activity.ServiceActivity;
import com.robam.rper.annotation.Param;
import com.robam.rper.annotation.Provider;
import com.robam.rper.util.LogUtil;

import static com.liulishuo.filedownloader.util.DownloadServiceNotConnectedHelper.startForeground;


@Provider(@Param(value = "MyService"))
public class MyService extends Service {
    private static final String TAG = "MyService";
    private static Notification notification;
    private DownloadBinder1 mBinder = new DownloadBinder1();
    public class DownloadBinder1 extends Binder{
        public void startDownload(){
            LogUtil.d(TAG,"startDownload executed");
        }
        public int getProgress(){
            LogUtil.d(TAG,"getProgress executed");
            return 0;
        }
    }
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
       return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG,"onCreate executed");
        Intent intent = new Intent(this, ServiceActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0 ,intent,0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("channel_002", "name2", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId("channel_002")
                    .setContentTitle("通知")
                    .setContentText("服务以及启动")
                    .setContentIntent(pi)
                    .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true).build();
        }else{
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("这是通知标题")
                    .setContentText("这是通知内容")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setAutoCancel(true).build();
        }

        startForeground(1, notification);



    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG,"onDestroy executed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG,"onStartCommand executed");
        return super.onStartCommand(intent, flags, startId);
    }
}
