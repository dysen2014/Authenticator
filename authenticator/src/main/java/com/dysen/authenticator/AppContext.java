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

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.dysen.authenticator.testability.DependencyInjector;
import com.dysen.authenticator.utils.FileUtilities;
import com.google.zxing.integration.android.IntentIntegrator;


/**
 * Authenticator application which is one of the first things instantiated when our process starts.
 * At the moment the only reason for the existence of this class is to initialize
 * {@link DependencyInjector} with the application context so that the class can (later) instantiate
 * the various objects it owns.
 *
 * Also restrict UNIX file permissions on application's persistent data directory to owner
 * (this app's UID) only.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class AppContext extends Application {

  public static Context mContext;
  @Override
  public void onCreate() {
    super.onCreate();

    mContext = getApplicationContext();
    // Try to restrict data dir file permissions to owner (this app's UID) only. This mitigates the
    // security vulnerability where SQLite database transaction journals are world-readable.
    // NOTE: This also prevents all files in the data dir from being world-accessible, which is fine
    // because this application does not need world-accessible files.
    try {
      FileUtilities.restrictAccessToOwnerOnly(
          mContext.getApplicationInfo().dataDir);
    } catch (Throwable e) {
      // Ignore this exception and don't log anything to avoid attracting attention to this fix
    }

    // During test runs the injector may have been configured already. Thus we take care to avoid
    // overwriting any existing configuration here.
    DependencyInjector.configureForProductionIfNotConfigured(mContext);
  }

  @Override
  public void onTerminate() {
    DependencyInjector.close();

    super.onTerminate();
  }


  /**
   * scanQR
   * @param activity
   */
  public static void customScan(Activity activity) {
    new IntentIntegrator(activity)
            .setOrientationLocked(false)
            .setCaptureActivity(CustomScanActivity.class) // 设置自定义的activity是CustomActivity
            .initiateScan(); // 初始化扫描
  }
}
