package com.dysen.authenticator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dysen.authenticator.testability.DependencyInjector;
import com.dysen.authenticator.utils.AccountDb;
import com.dysen.authenticator.utils.OtpProvider;
import com.dysen.authenticator.utils.OtpSource;
import com.dysen.authenticator.utils.OtpSourceException;
import com.dysen.authenticator.utils.SharedPreUtils;
import com.dysen.authenticator.utils.TotpClock;
import com.dysen.authenticator.utils.TotpCountdownTask;
import com.dysen.authenticator.utils.TotpCounter;
import com.dysen.authenticator.utils.Utilities;
import com.dysen.authenticator.views.CircleProgressView;
import com.dysen.authenticator.views.CountdownIndicator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

public class AuthenticatorActivity extends BaseActivity implements View.OnClickListener {


    Button btnAdd;
    TextView enterPinPrompt;
    ListView mUserList;
    LinearLayout contentAccountsPresent;
    private PinListAdapter mUserAdapter;
    private PinInfo[] mUsers = {};
    private double mTotpCountdownPhase;
    public static AuthenticatorActivity aty;

    /**
     * Counter used for generating TOTP verification codes.
     */
    private TotpCounter mTotpCounter;

    /**
     * Clock used for generating TOTP verification codes.
     */
    private TotpClock mTotpClock;

    /**
     * Task that periodically notifies this activity about the amount of time remaining until
     * the TOTP codes refresh. The task also notifies this activity when TOTP codes refresh.
     */
    private TotpCountdownTask mTotpCountdownTask;
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;
    private boolean isSave;

    public static AuthenticatorActivity newInstance() {

        if (aty == null)
            aty = new AuthenticatorActivity();
        return aty;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountDb = DependencyInjector.getAccountDb();
        mOtpProvider = DependencyInjector.getOtpProvider();
        mTotpCounter = mOtpProvider.getTotpCounter();
        mTotpClock = mOtpProvider.getTotpClock();

        baseSetContentView(R.layout.activity_authenticator);

        // restore state on screen rotation
        Object savedState = getLastNonConfigurationInstance();
        if (savedState != null) {
            mUsers = (PinInfo[]) savedState;
            // Re-enable the Get Code buttons on all HOTP accounts, otherwise they'll stay disabled.
            for (PinInfo account : mUsers) {
                if (account.isHotp) {
                    account.hotpCodeGenerationAllowed = true;
                }
            }
        }

        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUserList(true);
    }

    private void initData() {
        mUserAdapter = new PinListAdapter(this, R.layout.user_row, mUsers);

        mUserList.setVisibility(View.GONE);
        mUserList.setAdapter(mUserAdapter);

    }

    private void initView() {
        btnAdd = (Button) findViewById(R.id.btn_add);
        enterPinPrompt = (TextView) findViewById(R.id.enter_pin_prompt);
        mUserList = (ListView) findViewById(R.id.user_list);
        contentAccountsPresent = (LinearLayout) findViewById(R.id.content_accounts_present);


//        btnAdd.setVisibility((mUsers.length > 0) ? View.GONE : View.VISIBLE);
        contentAccountsPresent.setVisibility((mUsers.length > 0) ? View.VISIBLE : View.GONE);
        btnAdd.setOnClickListener(this);

    }

    public void saveSecretAndRefreshUserList() {
        if (isSave) {
            refreshUserList(true);
        }
    }

    /**
     * Display list of user emails and updated pin codes.
     *
     * @param isAccountModified if true, force full refresh
     */
    // @VisibleForTesting
    public void refreshUserList(boolean isAccountModified) {
        ArrayList<String> usernames = new ArrayList<String>();
        if (mAccountDb == null)
            mAccountDb = DependencyInjector.getAccountDb();
        mAccountDb.getNames(usernames);
        int userCount = usernames.size();

        if (userCount > 0) {
            boolean newListRequired = isAccountModified || mUsers.length != userCount;
            if (newListRequired) {
                mUsers = new PinInfo[userCount];
            }

            for (int i = 0; i < userCount; ++i) {
                String user = usernames.get(i);
                try {
                    computeAndDisplayPin(user, i, false);
                } catch (OtpSourceException e) {
                    e.printStackTrace();
                }
            }

            if (newListRequired) {
                // Make the list display the data from the newly created array of accounts
                // This forces the list to scroll to top.
                mUserAdapter = new PinListAdapter(this, R.layout.user_row, mUsers);
                mUserList.setAdapter(mUserAdapter);
            }

            mUserAdapter.notifyDataSetChanged();

            if (mUserList.getVisibility() != View.VISIBLE) {
                mUserList.setVisibility(View.VISIBLE);
                registerForContextMenu(mUserList);
            }
        } else {
            mUsers = new PinInfo[0]; // clear any existing user PIN state
            mUserList.setVisibility(View.GONE);
        }

        // Display the list of accounts if there are accounts, otherwise display a
        // different layout explaining the user how this app works and providing the user with an easy
        // way to add an account.
//        btnAdd.setVisibility((mUsers.length > 0) ? View.GONE : View.VISIBLE);
        contentAccountsPresent.setVisibility((mUsers.length > 0) ? View.VISIBLE : View.GONE);
    }

    /**
     * Computes the PIN and saves it in mUsers. This currently runs in the UI
     * thread so it should not take more than a second or so. If necessary, we can
     * move the computation to a background thread.
     *
     * @param user        the user email to display with the PIN
     * @param position    the index for the screen of this user and PIN
     * @param computeHotp true if we should increment counter and display new hotp
     */
    public void computeAndDisplayPin(String user, int position, boolean computeHotp) throws OtpSourceException {

        PinInfo currentPin;
        if (mUsers[position] != null) {
            currentPin = mUsers[position]; // existing PinInfo, so we'll update it
        } else {
            currentPin = new PinInfo();
            currentPin.pin = getString(R.string.empty_pin);
            currentPin.hotpCodeGenerationAllowed = true;
        }

        currentPin.user = user;

        if (!currentPin.isHotp || computeHotp) {
            // Always safe to recompute, because this code path is only
            // reached if the account is:
            // - Time-based, in which case getNextCode() does not change state.
            // - Counter-based (HOTP) and computeHotp is true.
            currentPin.pin = mOtpProvider.getNextCode(user);
            currentPin.hotpCodeGenerationAllowed = true;
        }

        mUsers[position] = currentPin;
    }

    /**
     * Saves the secret key to local storage on the phone.
     *
     * @param user         the user email address. When editing, the new user email.
     * @param secret       the secret key
     * @param originalUser If editing, the original user email, otherwise null.
     * @param type         hotp vs totp
     * @param counter      only important for the hotp type
     * @return {@code true} if the secret was saved, {@code false} otherwise.
     */
    public void saveSecret(Context context, String user, String secret,
                           String originalUser, AccountDb.OtpType type, Integer counter) {

        user = "".equals(originalUser) ? user : originalUser + "\n" + user;
        if (secret != null) {
            AccountDb accountDb = DependencyInjector.getAccountDb();
            accountDb.update(user, secret, user, type, counter);
            Toast.makeText(context, R.string.secret_saved, Toast.LENGTH_LONG).show();
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .vibrate(VIBRATE_DURATION);
            isSave = true;
        } else {
            Log.e(TAG, "Trying to save an empty secret key");
            Toast.makeText(context, R.string.error_empty_secret, Toast.LENGTH_LONG).show();
            isSave = false;
        }
    }

    /**
     * A tuple of user, OTP value, and type, that represents a particular user.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private static class PinInfo {
        private String pin; // calculated OTP, or a placeholder if not calculated
        private String user;
        private boolean isHotp = false; // used to see if button needs to be displayed

        /**
         * HOTP only: Whether code generation is allowed for this account.
         */
        private boolean hotpCodeGenerationAllowed;
    }

    /**
     * Scale to use for the text displaying the PIN numbers.
     */
    private static final float PIN_TEXT_SCALEX_NORMAL = 1.0f;
    /**
     * Underscores are shown slightly smaller.
     */
    private static final float PIN_TEXT_SCALEX_UNDERSCORE = 0.87f;

    /**
     * Displays the list of users and the current OTP values.
     *
     * @author adhintz@google.com (Drew Hintz)
     */
    private class PinListAdapter extends ArrayAdapter<PinInfo> {

        public PinListAdapter(Context context, int userRowId, PinInfo[] items) {
            super(context, userRowId, items);
        }

        /**
         * Displays the user and OTP for the specified position. For HOTP, displays
         * the button for generating the next OTP value; for TOTP, displays the countdown indicator.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            PinInfo currentPin = getItem(position);

            View row;
            if (convertView != null) {
                // Reuse an existing view
                row = convertView;
            } else {
                // Create a new view
                row = inflater.inflate(R.layout.user_row, null);
            }
            TextView pinView = (TextView) row.findViewById(R.id.pin_value);
            TextView userView = (TextView) row.findViewById(R.id.current_user);
            CountdownIndicator countdownIndicator =
                    (CountdownIndicator) row.findViewById(R.id.countdown_icon);
            CircleProgressView circleProgressView = row.findViewById(R.id.cpv);

            if (currentPin.isHotp) {
                ((ViewGroup) row).setDescendantFocusability(
                        ViewGroup.FOCUS_BLOCK_DESCENDANTS); // makes long press work

                countdownIndicator.setVisibility(View.GONE);
            } else { // TOTP, so no button needed
                row.setTag(null);

//                countdownIndicator.setVisibility(View.VISIBLE);
                countdownIndicator.setPhase(mTotpCountdownPhase);
                if (circleProgressView != null) {
                    circleProgressView.setTextType("S");
                    circleProgressView.setMaxProgress(OtpProvider.DEFAULT_INTERVAL, 1000);
                    circleProgressView.setProgress(mTotpCountdownPhase, OtpProvider.DEFAULT_INTERVAL, 1000);
                }
            }

            if (getString(R.string.empty_pin).equals(currentPin.pin)) {
                pinView.setTextScaleX(PIN_TEXT_SCALEX_UNDERSCORE); // smaller gap between underscores
            } else {
                pinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);
            }
            StringBuilder sb = new StringBuilder(currentPin.pin);//构造一个StringBuilder对象
            sb.insert(3, "\t\t");//在指定的位置，(offset 表示在第几个元素后) 插入指定的字符串
            String pin = sb.toString();
            pinView.setText(pin);
            userView.setText(currentPin.user);

            return row;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateCodesAndStartTotpCountdownTask();
    }

    private void stopTotpCountdownTask() {
        if (mTotpCountdownTask != null) {
            mTotpCountdownTask.stop();
            mTotpCountdownTask = null;
        }
    }

    private void updateCodesAndStartTotpCountdownTask() {
        stopTotpCountdownTask();

        mTotpCountdownTask =
                new TotpCountdownTask(mTotpCounter, mTotpClock, TOTP_COUNTDOWN_REFRESH_PERIOD);
        mTotpCountdownTask.setListener(new TotpCountdownTask.Listener() {
            @Override
            public void onTotpCountdown(long millisRemaining) {
                if (isFinishing()) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return;
                }
                setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining);
            }

            @Override
            public void onTotpCounterValueChanged() {
                if (isFinishing()) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return;
                }
                refreshVerificationCodes();
            }
        });

        mTotpCountdownTask.startAndNotifyListener();
    }

    private void setTotpCountdownPhaseFromTimeTillNextValue(long millisRemaining) {
        setTotpCountdownPhase(
                ((double) millisRemaining) / Utilities.secondsToMillis(mTotpCounter.getTimeStep()));
    }

    private void setTotpCountdownPhase(double phase) {
        mTotpCountdownPhase = phase;
        updateCountdownIndicators();
    }


    private void updateCountdownIndicators() {
        for (int i = 0, len = mUserList.getChildCount(); i < len; i++) {
            View listEntry = mUserList.getChildAt(i);
            CountdownIndicator indicator =
                    (CountdownIndicator) listEntry.findViewById(R.id.countdown_icon);
            if (indicator != null) {
                indicator.setPhase(mTotpCountdownPhase);
            }
            CircleProgressView circleProgressView = listEntry.findViewById(R.id.cpv);
            if (circleProgressView != null) {
                circleProgressView.setTextType("S");
                circleProgressView.setMaxProgress(OtpProvider.DEFAULT_INTERVAL, 1000);
                circleProgressView.setProgress(mTotpCountdownPhase, OtpProvider.DEFAULT_INTERVAL, 1000);
            }
        }
    }

    private void refreshVerificationCodes() {
        refreshUserList(false);
        setTotpCountdownPhase(1.0);
    }

    /**
     * Converts user list ordinal id to user email
     */
    private String idToEmail(long id) {
        return mUsers[(int) id].user;
    }

    // @VisibleForTesting
    public static final int CHECK_KEY_VALUE_ID = 0;
    // @VisibleForTesting
    public static final int RENAME_ID = 1;
    // @VisibleForTesting
    public static final int REMOVE_ID = 2;
    // @VisibleForTesting
    static final int COPY_TO_CLIPBOARD_ID = 3;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        String user = idToEmail(info.id);
        AccountDb.OtpType type = mAccountDb.getType(user);
        menu.setHeaderTitle(user);
        menu.add(0, COPY_TO_CLIPBOARD_ID, 0, R.string.copy_to_clipboard);
        // Option to display the check-code is only available for HOTP accounts.
        if (type == AccountDb.OtpType.HOTP) {
            menu.add(0, CHECK_KEY_VALUE_ID, 0, R.string.check_code_menu_item);
        }
        menu.add(0, RENAME_ID, 0, R.string.rename);
        menu.add(0, REMOVE_ID, 0, R.string.context_menu_remove_account);
    }

    /**
     * 列表item 长按事件处理
     *
     * @param item
     * @return
     */
    @SuppressLint("StringFormatInvalid")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Intent intent;
        final String user = idToEmail(info.id); // final so listener can see value
        switch (item.getItemId()) {
            case COPY_TO_CLIPBOARD_ID:
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(mUsers[(int) info.id].pin);
                return true;
            case CHECK_KEY_VALUE_ID:
//                intent = new Intent(Intent.ACTION_VIEW);
//                intent.setClass(this, CheckCodeActivity.class);
//                intent.putExtra("user", user);
//                startActivity(intent);
                return true;
            case RENAME_ID:
                final Context context = this; // final so listener can see value
                final View frame = getLayoutInflater().inflate(R.layout.rename,
                        (ViewGroup) findViewById(R.id.rename_root));
                final EditText nameEdit = (EditText) frame.findViewById(R.id.rename_edittext);
                nameEdit.setText(user);
                new AlertDialog.Builder(this)
                        .setTitle(String.format(getString(R.string.rename_message), user))
                        .setView(frame)
                        .setPositiveButton(R.string.submit,
                                this.getRenameClickListener(context, user, nameEdit))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case REMOVE_ID:
                // Use a WebView to display the prompt because it contains non-trivial markup, such as list
                View promptContentView =
                        getLayoutInflater().inflate(R.layout.remove_account_prompt, null, false);
                WebView webView = (WebView) promptContentView.findViewById(R.id.web_view);
                webView.setBackgroundColor(Color.TRANSPARENT);
                // Make the WebView use the same font size as for the mEnterPinPrompt field
                double pixelsPerDip =
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()) / 10d;
                webView.getSettings().setDefaultFontSize((int) (enterPinPrompt.getTextSize() / pixelsPerDip));
                Utilities.setWebViewHtml(
                        webView,
                        "<html><body style=\"background-color: transparent;\" text=\"black\">"
                                + getString(
                                mAccountDb.isGoogleAccount(user)
                                        ? R.string.remove_google_account_dialog_message
                                        : R.string.remove_account_dialog_message)
                                + "</body></html>");

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.remove_account_dialog_title, user))
                        .setView(promptContentView)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.remove_account_dialog_button_remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mAccountDb.delete(user);
                                        refreshUserList(true);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * 重命名
     *
     * @param context
     * @param user
     * @param nameEdit
     * @return
     */
    private DialogInterface.OnClickListener getRenameClickListener(final Context context,
                                                                   final String user, final EditText nameEdit) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String newName = nameEdit.getText().toString();
                if (newName != user) {
                    if (mAccountDb.nameExists(newName)) {
                        Toast.makeText(context, R.string.error_exists, Toast.LENGTH_LONG).show();
                    } else {
                        mAccountDb.update(newName,
                                mAccountDb.getSecret(user), user, mAccountDb.getType(user),
                                mAccountDb.getCounter(user));
                        refreshUserList(true);
//                        saveSecret(AuthenticatorActivity.this, newName,
//                                mAccountDb.getSecret(user), user, mAccountDb.getType(user),
//                                mAccountDb.getCounter(user));
                    }
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        gotoNext(AddAccountActivity.class);
    }
}
