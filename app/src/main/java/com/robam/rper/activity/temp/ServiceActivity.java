package com.robam.rper.activity.temp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.activity.BaseActivity;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.serviceTest.DownloadService;
import com.robam.rper.serviceTest.MyService;

import static com.liulishuo.filedownloader.util.DownloadServiceNotConnectedHelper.startForeground;

@EntryActivity(icon = R.drawable.xn, name = "服务工具", index = 3)
public class ServiceActivity extends BaseActivity implements View.OnClickListener {

    public static final int UPDATE_TEXT = 1;

    private TextView textView;
    private TextView textView2;
    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_TEXT:
                    textView.setText("111");
                default:
                    break;
            }
        }
    };

//    private MyService.DownloadBinder1 downloadBinder1;
    private DownloadService.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
         /* downloadBinder1 = (MyService.DownloadBinder1)iBinder;
            downloadBinder1.startDownload();
            downloadBinder1.getProgress();*/
            downloadBinder = (DownloadService.DownloadBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        Bundle bundle = getIntent().getExtras();
        String name = bundle.getString("name");
        String phone = bundle.getString("phone");

        textView = findViewById(R.id.Mytext);
        textView2 = findViewById(R.id.Mytext2);

        textView.setText(name);
        textView2.setText(phone);



        Button changText = findViewById(R.id.change_text);
        Button startService = findViewById(R.id.start_service);
        Button stopService = findViewById(R.id.stop_service);
        Button bind_service = findViewById(R.id.bind_service);
        Button unbind_service = findViewById(R.id.unbind_service);
        Button start_download = findViewById(R.id.start_download);
        Button pause_download = findViewById(R.id.pause_download);
        Button cancel_download = findViewById(R.id.cancel_download);
        changText.setOnClickListener(this);
        startService.setOnClickListener(this);
        stopService.setOnClickListener(this);
        unbind_service.setOnClickListener(this);
        bind_service.setOnClickListener(this);
        start_download.setOnClickListener(this);
        pause_download.setOnClickListener(this);
        cancel_download.setOnClickListener(this);
        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent,connection,BIND_AUTO_CREATE);



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.change_text:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = UPDATE_TEXT;
                        handler.sendMessage(message);
                    }
                }).start();
                break;
            case R.id.start_service:
                Intent startIntent = new Intent(this, MyService.class);
                startService(startIntent);
                break;
            case R.id.stop_service:
                Intent stopIntent = new Intent(this, MyService.class);
                stopService(stopIntent);
                break;
            case R.id.unbind_service:
                unbindService(connection);
                break;
            case R.id.bind_service:
                Intent bindIntent = new Intent(this, MyService.class);
                bindService(bindIntent, connection, BIND_AUTO_CREATE);//绑定服务
                break;
            case R.id.start_download:
                String url = "https://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
                downloadBinder.startDownload(url);
                break;
            case R.id.pause_download:
                downloadBinder.pauseDownload();
                break;
            case R.id.cancel_download:
                downloadBinder.cancelDownload();
                break;
            default:
                break;
        }

    }
}
