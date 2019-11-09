package com.robam.rper.activity;

import android.os.Bundle;
import android.view.View;

import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.ui.HeadControlPanel;


@EntryActivity(icon = R.drawable.xn, name = "性能工具", permissions = {"adb", "float"}, index = 1)
public class PerformanceActivity extends BaseActivity {
    private HeadControlPanel mPanel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        mPanel = (HeadControlPanel) findViewById(R.id.head_layout);
        mPanel.setMiddleTitle("性能测试");
        mPanel.setBackIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
