package com.robam.rper.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.serviceTest.MyService;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        textView = findViewById(R.id.Mytext);
        Button changText = findViewById(R.id.change_text);
        Button startService = findViewById(R.id.start_service);
        Button stopService = findViewById(R.id.stop_service);
        changText.setOnClickListener(this);
        startService.setOnClickListener(this);
        stopService.setOnClickListener(this);

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
                Intent intent = new Intent(this, MyService.class);
            default:
                break;
        }

    }
}
