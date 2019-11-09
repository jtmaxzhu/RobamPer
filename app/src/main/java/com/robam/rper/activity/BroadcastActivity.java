package com.robam.rper.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;

//@EntryActivity(icon = R.drawable.xn, name = "广播演示", index = 3)
public class BroadcastActivity extends AppCompatActivity {

    private IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);
    }


    class NetworkChangeReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()){
                Toast.makeText(context,"网络开启", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(context,"网络关闭", Toast.LENGTH_SHORT).show();
            }

        }
    }
}
