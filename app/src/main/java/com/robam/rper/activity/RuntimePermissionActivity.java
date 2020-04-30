package com.robam.rper.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;

import java.util.ArrayList;
import java.util.List;

//@EntryActivity(icon = R.drawable.xn, name = "动态权限", index = 5)
public class RuntimePermissionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runtime_permission);
        Button makecall = findViewById(R.id.make_call);
        Button startmonkey = findViewById(R.id.start_monkey);
        makecall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(RuntimePermissionActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(RuntimePermissionActivity.this,
                            new String[]{Manifest.permission.CALL_PHONE},1);
                }else{
                    call();
                }

            }
        });
    }

    private void call(){
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:10086"));
            startActivity(intent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    call();
                }else {
                    Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
                }
                break;
             default:
        }
    }
}
