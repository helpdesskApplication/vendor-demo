package cometchat.inscripts.com.cometchatui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatui.helper.LaunchHelper;
import cometchat.inscripts.com.cometchatui.keys.SharedPrefrenceKeys;


public class CCGuestLoginActivity extends AppCompatActivity implements View.OnClickListener  {

    private static final java.lang.String TAG = CCGuestLoginActivity.class.getSimpleName();
    private Button btnLogin;
    private EditText edtGuestName;
    private TextInputLayout tllGuestName;
    private int primaryColor;
    private ImageView imgBackArrow,imgLogoLogin;
    private CometChat cometChat;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_login);
        cometChat = CometChat.getInstance(this);
        setupFields();
        setupThemeColor();
        setupClickListners();
    }

    private void setupFields(){
        btnLogin = (Button) findViewById(R.id.buttonGuestLogin);
        edtGuestName = (EditText) findViewById(R.id.editTextGuestName);
        tllGuestName = (TextInputLayout) findViewById(R.id.input_layout_guest_name);
        imgBackArrow = (ImageView) findViewById(R.id.imageViewBottomBack);
        progressBar = (ProgressBar) findViewById(R.id.progress_weel);
        imgLogoLogin = (ImageView) findViewById(R.id.imageViewCometchatLogo);
    }

    private void setupThemeColor(){
        primaryColor = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        btnLogin.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
        imgBackArrow.setColorFilter(primaryColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progressBar.getIndeterminateDrawable().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
//            progressBar.setProgressTintList(ColorStateList.valueOf(primaryColor));
        }
        if(!(LocalConfig.isWhiteLabelled())){
            imgLogoLogin.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void setupClickListners(){
        btnLogin.setOnClickListener(this);
        imgBackArrow.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageViewBottomBack:
                onBackPressed();
                break;

            case R.id.buttonGuestLogin:

                String guestName = edtGuestName.getText().toString().trim();
                tllGuestName.setError(null);

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(btnLogin.getApplicationWindowToken(), 0);

                if(TextUtils.isEmpty(guestName)){
                    tllGuestName.setError("Guest Name cannot be empty");
                }else{
                    progressBar.setVisibility(View.VISIBLE);
                    cometChat.guestLogin(guestName, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject responce) {
                            Logger.error(TAG,"GuestLogin success responce = "+responce);
                            PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.LOGGED_IN, "1");
                            PreferenceHelper.save(PreferenceKeys.LoginKeys.REMEMBER_ME, CometChatKeys.LoginKeys.USER_REMEMBER);
                            LaunchHelper.launchCometChat(CCGuestLoginActivity.this);
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void failCallback(JSONObject responce) {
                            Logger.error(TAG,"GuestLogin fail responce = "+responce);
                            try {
                                if (responce.has("message")) {
                                    tllGuestName.setError(responce.getString("message"));
                                }
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
                break;
        }
    }
}
