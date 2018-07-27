/*
 * Copyright 2010 Google Inc. All Rights Reserved.
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

package com.dysen.authenticator.utils;

/**
 * Abstraction for collection of OTP tokens.
 *
 * @author cemp@google.com (Cem Paya)
 */
public interface OtpSource {

  /**
   * Return the next OTP code for specified username.
   * Invoking this function may change internal state of the OTP generator,
   * for example advancing the counter.
   *
   * @param accountName Username, email address or other unique identifier for the account.
   * @return OTP as string code.
   */
  String getNextCode(String accountName) throws OtpSourceException;

  /**
   * Gets the counter for generating or verifying TOTP codes.
   */
  TotpCounter getTotpCounter();

  /**
   * Gets the clock for generating or verifying TOTP codes.
   */
  TotpClock getTotpClock();
}
