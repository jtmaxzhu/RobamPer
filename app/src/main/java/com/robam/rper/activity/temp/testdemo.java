package com.robam.rper.activity.temp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.robam.rper.R;
import com.robam.rper.activity.BaseActivity;
import com.robam.rper.annotation.EntryActivity;
import com.robam.rper.ui.CustomView;


/**
 * author : liuxiaohu
 * date   : 2019/11/22 10:54
 * desc   :
 * version: 1.0
 */
@EntryActivity(icon = R.drawable.xn, name = "自定义View", index = 4)
public class testdemo extends BaseActivity implements View.OnClickListener {
    private CustomView customView;
    private RelativeLayout rlCustomeView;

    public Button button1;
    public EditText Param1,Param2;

    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custome_view);
        rlCustomeView = (RelativeLayout) findViewById(R.id.rl_custome_view);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        //new 一个CustomView
        customView = new CustomView(this);
        customView.setCustomText("在代码中添加");
        customView.setCustomColor(Color.GREEN);
        customView.setFontSize(60);
        //添加view
        rlCustomeView.addView(customView, params);

        button1 = (Button) this.findViewById(R.id.save);
        button1.setOnClickListener(this);

        Param1 = findViewById(R.id.edit1);
        Param2 = findViewById(R.id.edit2);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.save:
                String site1 = Param1.getText().toString();
                String site2= Param2.getText().toString();
                Intent intent = new Intent(testdemo.this, ServiceActivity.class);
                Bundle bundle = new Bundle();
                bundle.putCharSequence("name",site1);
                bundle.putCharSequence("phone",site2);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
        }

    }
}
