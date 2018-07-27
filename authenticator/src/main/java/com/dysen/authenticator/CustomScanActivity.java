package com.dysen.authenticator;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * xiezuofei
 * 2016-07-09 16:10
 * 793169940@qq.com
 *扫描rwei二维码
 */
public class CustomScanActivity extends BaseActivity implements DecoratedBarcodeView.TorchListener{
    private Activity context;
    private TextView tv_head_title;
    private TextView tv_head_back;
    private DecoratedBarcodeView mDBV;
    private CaptureManager captureManager;
    private boolean isLightOn = false;

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        captureManager.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scan);
        context=this;
        initView(savedInstanceState);
        initData();
    }


    private void initView(Bundle savedInstanceState) {
        mDBV=(DecoratedBarcodeView) findViewById(R.id.dbv_custom);
        mDBV.setTorchListener(this);
        //重要代码，初始化捕获
        captureManager = new CaptureManager(this,mDBV);
        captureManager.initializeFromIntent(getIntent(),savedInstanceState);
        captureManager.decode();
    }

    private void initData() {

    }
    // torch 手电筒
    @Override
    public void onTorchOn() {

        isLightOn = true;
    }

    @Override
    public void onTorchOff() {

        isLightOn = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mDBV.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }
}
