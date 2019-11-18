package com.robam.rper.activity;

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
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.serviceTest.MyService;

import static com.liulishuo.filedownloader.util.DownloadServiceNotConnectedHelper.startForeground;

@EntryActivity(icon = R.drawable.xn, name = "服务工具", index = 3)
public class ServiceActivity extends BaseActivity implements View.OnClickListener {

    public static final int UPDATE_TEXT = 1;

    private TextView textView;
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

    private MyService.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBinder = (MyService.DownloadBinder)iBinder;
            downloadBinder.startDownload();
            downloadBinder.getProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        textView = findViewById(R.id.Mytext);
        Button changText = findViewById(R.id.change_text);
        Button startService = findViewById(R.id.start_service);
        Button stopService = findViewById(R.id.stop_service);
        Button bind_service = findViewById(R.id.bind_service);
        Button unbind_service = findViewById(R.id.unbind_service);
        changText.setOnClickListener(this);
        startService.setOnClickListener(this);
        stopService.setOnClickListener(this);
        unbind_service.setOnClickListener(this);
        bind_service.setOnClickListener(this);


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
                Intent unbindIntent = new Intent(this, MyService.class);
                unbindService(connection);
                break;
            case R.id.bind_service:
                Intent bindIntent = new Intent(this, MyService.class);
                bindService(bindIntent, connection, BIND_AUTO_CREATE);//绑定服务
                break;
            default:
                break;
        }

    }
}
