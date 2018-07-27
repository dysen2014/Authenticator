/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dysen.authenticator;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.dysen.authenticator.utils.AccountDb;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


/**
 * The page of the "Add account" flow that offers the user to add an account.
 * The page offers the user to scan a barcode or manually enter the account details.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class AddAccountActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseSetContentView(R.layout.add_other_account);
        initView();
    }

    private void initView() {
        findViewById(R.id.scan_barcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBarcode();
            }
        });
        findViewById(R.id.manually_add_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manuallyEnterAccountDetails();
            }
        });
    }

    private void manuallyEnterAccountDetails() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, EnterKeyActivity.class);
        startActivity(intent);
    }

    private void scanBarcode() {
        AppContext.customScan(this);
    }

    @Override
    // 通过 onActivityResult的方法获取 扫描回来的 值
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                //UIHelper.ToastMessage(context,getString(R.string.asset_nrwk));
            } else {
                String content = intentResult.getContents();
                backData(content);
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void backData(String content) {

        String mode, originalUser, user, secret;
        if (!TextUtils.isEmpty(content)) {
            //otpauth://totp/ZB:sendy1211@126.com?secret=DMHEEEEBXAZNOPWN
            content = content.replace("://", ",");
            mode = content.substring(content.lastIndexOf(",") + 1, content.indexOf("/"));
            originalUser = content.substring(content.indexOf("/") + 1, content.indexOf(":"));
            user = content.substring(content.indexOf(":") + 1, content.indexOf("?"));
            secret = content.substring(content.indexOf("=") + 1, content.length());
            //mode=totp	originalUser=ZB	user=sendy1211@126.com	secret=DMHEEEEBXAZNOPWN
//                Log.e("data", content + "\nmode=" + mode + "\toriginalUser=" + originalUser + "\tuser=" + user + "\tsecret=" + secret);
            secret = secret.replace('1', 'I').replace('0', 'O');
            final AccountDb.OtpType otpType = "totp".equals(mode) ? AccountDb.OtpType.TOTP : AccountDb.OtpType.HOTP;

            AuthenticatorActivity.newInstance().saveSecret(this, user, secret, originalUser, otpType,
                    AccountDb.DEFAULT_HOTP_COUNTER);

        } else
            Log.e("", "无扫描结果");
    }
}
