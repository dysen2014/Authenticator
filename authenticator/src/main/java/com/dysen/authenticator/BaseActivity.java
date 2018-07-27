package com.dysen.authenticator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dysen.authenticator.utils.SharedPreUtils;

/**
 * @package com.dysen.gesturelock.activity
 * @email dy.sen@qq.com
 * created by dysen on 2018/7/23 - 下午2:07
 * @info
 */
public class BaseActivity extends AppCompatActivity {

    BaseActivity aty;
    /**
     * the tag for log messages
     */
    protected String TAG = getClass().getSimpleName();
    protected static final long VIBRATE_DURATION = 200l;
    protected static final int MIN_KEY_BYTES = 10;
    /**
     * frequency (milliseconds) with which totp countdown indicators are updated.
     */
    protected static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 100;
    protected static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;
    protected static final long HOTP_DISPLAY_TIMEOUT = 2 * 60 * 1000;
    protected static String U_USER = "user";
    protected static String U_SECRET = "secret";
    protected static String U_ORIGINALUSER = "originaluser";
    protected Context mContext;
    protected Handler mHandler;

    protected TextView tvBack;
    protected TextView tvTitle;
    protected TextView tvMenu;
    protected LinearLayout vContent;

    @Override
    protected void onCreate(@Nullable Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);

        setContentView(R.layout.activity_base);
        baseInit();
    }
    protected void baseInit() {
        if (mHandler == null)
            mHandler = new Handler();
        mContext = aty = this;
        tvBack = findViewById(R.id.tv_back);
        tvTitle = findViewById(R.id.tv_title);
        tvMenu = findViewById(R.id.tv_menu);
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
    /**
     * set screen view
     *
     * @param layoutResID
     */
    protected void baseSetContentView(int layoutResID) {

        vContent = findViewById(R.id.v_content); //v_content是在基类布局文件中预定义的layout区域
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout
                .LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        //通过LayoutInflater填充基类的layout区域
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(layoutResID, null);
        vContent.addView(v, layoutParams);

    }

    protected void gotoNext(Class cls, boolean... isfinish) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        if (isfinish.length > 0)
            if (isfinish[0])
                finish();
    }

}
