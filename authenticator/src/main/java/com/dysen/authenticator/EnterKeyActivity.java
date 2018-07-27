package com.dysen.authenticator;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.dysen.authenticator.utils.AccountDb;
import com.dysen.authenticator.utils.AccountDb.OtpType;
import com.dysen.authenticator.utils.Base32String;

/**
 * @package com.dysen.authenticator
 * @email dy.sen@qq.com
 * created by dysen on 2018/7/23 - 下午2:10
 * @info
 */
public class EnterKeyActivity extends BaseActivity implements View.OnClickListener, TextWatcher {

    EditText accountName;
    EditText keyValue;
    Spinner typeChoice;
    Button btnAdd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseSetContentView(R.layout.enter_key);
        initView();
    }

    private void initView() {
        accountName = (EditText) findViewById(R.id.account_name);
        keyValue = (EditText) findViewById(R.id.key_value);
        typeChoice = (Spinner) findViewById(R.id.type_choice);
        btnAdd = (Button) findViewById(R.id.btn_add);

        btnAdd.setOnClickListener(this);

        ArrayAdapter<CharSequence> types = ArrayAdapter.createFromResource(this,
                R.array.type, android.R.layout.simple_spinner_item);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeChoice.setAdapter(types);

        // Set listeners
        keyValue.addTextChangedListener(this);

        btnAdd.setText(R.string.enter_key_page_add_button);
    }

    @Override
    public void onClick(View view) {
        // TODO(cemp): This depends on the OtpType enumeration to correspond
        // to array indices for the dropdown with different OTP modes.
        Log.e("log", accountName.getText().toString() + ":=====log====:" + getEnteredKey());
        OtpType mode = typeChoice.getSelectedItemPosition() == OtpType.TOTP.value ?
                OtpType.TOTP :
                OtpType.HOTP;
        if (validateKeyAndUpdateStatus(true)) {
            AuthenticatorActivity.newInstance().saveSecret(this,
                    accountName.getText().toString(),
                    getEnteredKey(), "", mode,
                    AccountDb.DEFAULT_HOTP_COUNTER);
            gotoNext(AuthenticatorActivity.class, true);
        }
    }


    /*
     * Return key entered by user, replacing visually similar characters 1 and 0.
     */
    private String getEnteredKey() {
        String enteredKey = keyValue.getText().toString();
        return enteredKey.replace('1', 'I').replace('0', 'O');
    }

    /*
     * Verify that the input field contains a valid base32 string,
     * and meets minimum key requirements.
     */
    private boolean validateKeyAndUpdateStatus(boolean submitting) {
        String userEnteredKey = getEnteredKey();
        try {
            byte[] decoded = Base32String.decode(userEnteredKey);
//            Log.e("decoded", decoded+"========="+userEnteredKey+"============"+decoded.length);
            if (decoded.length < MIN_KEY_BYTES) {
                // If the user is trying to submit a key that's too short, then
                // display a message saying it's too short.
                keyValue.setError(submitting ? getString(R.string.enter_key_too_short) : null);
                return false;
            } else {
                keyValue.setError(null);
                return true;
            }
        } catch (Base32String.DecodingException e) {
            keyValue.setError(getString(R.string.enter_key_illegal_char));
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable userEnteredValue) {
        validateKeyAndUpdateStatus(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Do nothing
    }
}
