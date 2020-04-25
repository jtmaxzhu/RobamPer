package com.robam.rper.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.robam.rper.R;
import com.robam.rper.util.LogUtil;


/**
 * author : liuxiaohu
 * date   : 2020/1/8 16:22
 * desc   :
 * version: 1.0
 */
public class CustomView extends View {
    private static final String TAG = "CustomView";
    //文字内容
    private String customText;
    //文字的颜色
    private int customColor;
    //文字的字体大小
    private int fontSize;
    //画笔
    private TextPaint textPaint;

    public CustomView(Context context) {
        this(context, null);
    }

    public CustomView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCustomAttrs(context, attrs);
    }
    /**
     获取自定义属性
     */
    private void initCustomAttrs(Context context, AttributeSet attrs) {
        //获取自定义属性。
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CustomView);
        //获取字体大小,默认大小是16dp
        fontSize = (int) ta.getDimension(R.styleable.CustomView_Myfont, 16);
        //获取文字内容
        customText = ta.getString(R.styleable.CustomView_text);
        //获取文字颜色，默认颜色是BLUE
        customColor = ta.getColor(R.styleable.CustomView_color, Color.BLUE);
        ta.recycle();
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(customColor);
        textPaint.setTextSize(fontSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        LogUtil.e(TAG, "onDraw: custom text=" + customText);
        canvas.drawText(customText, 0, customText.length(), 100, 100, textPaint);
    }

    public void setCustomText(String customText) {
        this.customText = customText;
    }

    public void setCustomColor(int customColor) {
        this.customColor = customColor;
        textPaint.setColor(customColor);
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        textPaint.setTextSize(fontSize);
    }

}
