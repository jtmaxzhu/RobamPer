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
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.robam.rper.R;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.activity.temp.ServiceActivity;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadTask;
    private String downloadUrl;
    private DownloadBinder mBinder = new DownloadBinder();

    private DownloadListener listener = new DownloadListener(){

        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("正在下载...", progress));

        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            //下载成功将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载成功",-1));
            MyApplication.getInstance().showToast("下载成功");

        }

        @Override
        public void onFailed() {
            downloadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载失败",-1));
            MyApplication.getInstance().showToast("下载失败");
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            MyApplication.getInstance().showToast("下载暂停");
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            MyApplication.getInstance().showToast("退出下载");
        }
    };

    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DownloadBinder extends Binder{

        public void startDownload(String url){
            if (downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("开始下载....",0));
                MyApplication.getInstance().showToast("Downloading...");
            }
        }

        public void pauseDownload(){
            if (downloadTask != null){
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if (downloadTask != null){
                downloadTask.cancelDownload();
            }else {
                if (downloadUrl != null){
                    //取消下载要将文件删除
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory+fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    MyApplication.getInstance().showToast("Canceled");
                }
            }
        }

    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress){
        Intent intent = new Intent(this, ServiceActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("channel_003", "name3", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);
            builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
            builder.setContentIntent(pi);
            builder.setContentTitle(title);
            builder.setChannelId("channel_003");
            builder.setAutoCancel(true);


        }else{
            builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
            builder.setContentIntent(pi);
            builder.setContentTitle(title);
        }
        if (progress > 0){
            builder.setContentText(progress + "%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();


    }


}
