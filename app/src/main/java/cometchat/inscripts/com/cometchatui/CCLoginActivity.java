package cometchat.inscripts.com.cometchatui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.inscripts.activities.CCWebViewActivity;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.LaunchCallbacks;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.IntentExtraKeys;
import com.inscripts.orm.SugarContext;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatui.helper.LaunchHelper;

import cometchat.inscripts.com.cometchatui.keys.SharedPrefrenceKeys;
import cometchat.inscripts.com.readyui.CCReadyUI;
import rolebase.RolebaseFeatures;
import utils.NetworkUtil;

public class CCLoginActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,TextView.OnEditorActionListener {
    private static final java.lang.String TAG = CCLoginActivity.class.getSimpleName();
    private EditText edtUserName, edtPassword;
    private TextInputLayout tllUserName, tllPassword;
    private TextView tvGuestLogin,tvRegister;
    private Button btnLogin;
    private CometChat cometChat;
    private RelativeLayout rtlSingleContainer;
    private int primaryColor;
    private ProgressBar wheel;
    private RelativeLayout containerSingle;
    private boolean isGuestLoginEnabled,isRegistrationEnabled;
    private RelativeLayout containerBoth;
    private View registerDivider;
    private TextView tvTryDemoBoth;
    private TextView tvRegisterBoth;
    private TextView tvSocialLoginBoth;
    private TextView tvRemeberMe;
    private TextView tvDontHaveAccount;
    private View centerDividerSocial;
    private SwitchCompat rememberSwitch;
    private TextView tvDontHaveAccount1;
    private ImageView imgLogoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SugarContext.init(this);

        cometChat = CometChat.getInstance(this);
        setupFields();
        setupThemeColor();
        setupClickListners();
        setAutoFilledData();
        setLanguage();
    }

    private void setLanguage() {
        tvTryDemoBoth.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGIN_AS_GUEST)));
        tvSocialLoginBoth.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SOCIAL_LOGIN)));
        tvRegisterBoth.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REGISTER_TITLE)));
        tvDontHaveAccount1.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_DONT_HAVE_AN_ACCOUNT)));
        tvRemeberMe.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REMEMBER_ME)));
        tvGuestLogin.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGIN_AS_GUEST)));
        btnLogin.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGIN)));
        tvRegister.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REGISTER_TITLE)));
        tllUserName.setHint((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_USERNAME)));
        tllPassword.setHint((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_PASSWORD)));
    }

    private void setAutoFilledData() {
        if(PreferenceHelper.contains(PreferenceKeys.LoginKeys.REMEMBER_ME) && PreferenceHelper.get(PreferenceKeys.LoginKeys.REMEMBER_ME).equals("1") && PreferenceHelper.contains(SharedPrefrenceKeys.LoginKeys.USERNAME) && PreferenceHelper.contains(SharedPrefrenceKeys.LoginKeys.PASSWORD)){
            edtUserName.setText(PreferenceHelper.get(SharedPrefrenceKeys.LoginKeys.USERNAME));
            edtPassword.setText(PreferenceHelper.get(SharedPrefrenceKeys.LoginKeys.PASSWORD));
        }
    }


    private void setupFields() {
        edtUserName = (EditText) findViewById(R.id.editTextUsername);
        edtPassword = (EditText) findViewById(R.id.editTextPassword);
        tvRemeberMe = (TextView) findViewById(R.id.tv_rember_me_label);
        btnLogin = (Button) findViewById(R.id.buttonLogin);
        tllUserName = (TextInputLayout) findViewById(R.id.input_layout_user_name);
        tllPassword = (TextInputLayout) findViewById(R.id.input_layout_password);
        rtlSingleContainer = (RelativeLayout) findViewById(R.id.container_single);
        tvGuestLogin = (TextView) findViewById(R.id.textViewTryDemo);
        wheel = (ProgressBar) findViewById(R.id.progress_wheel);
        tvRegister = (TextView) findViewById(R.id.textViewRegister);
        containerBoth = (RelativeLayout) findViewById(R.id.container_both);
        registerDivider = findViewById(R.id.centerDivider);
        tvTryDemoBoth = (TextView) findViewById(R.id.textViewTryDemo1);
        tvRegisterBoth = (TextView) findViewById(R.id.textViewRegisterBoth);
        tvSocialLoginBoth = (TextView) findViewById(R.id.textViewsocial);
        centerDividerSocial = findViewById(R.id.centerDividerSocial);
        tvDontHaveAccount = (TextView) findViewById(R.id.txtDonthaveAccount);
        rememberSwitch = (SwitchCompat) findViewById(R.id.switchRememberMe);
        tvDontHaveAccount1 = (TextView) findViewById(R.id.txtDonthaveAccount1);
        imgLogoLogin = (ImageView)findViewById(R.id.imageViewCometchatLogo);
        isGuestLoginEnabled=(boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.LOGIN_SETTINGS, SettingSubType.GUESTLOGIN));
        isRegistrationEnabled=(boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.REGISTER_ENABLED));
        String dontHaveAnAccount = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_DONT_HAVE_AN_ACCOUNT));
        Logger.error(TAG,""+isGuestLoginEnabled+" ## "+isRegistrationEnabled+" ## "+dontHaveAnAccount);
        tvDontHaveAccount.setText(dontHaveAnAccount);
        if(isGuestLoginEnabled && !isRegistrationEnabled){
            Logger.error(TAG,"Only GuestLogin enabled");
            rtlSingleContainer.setVisibility(View.VISIBLE);
            containerBoth.setVisibility(View.GONE);
            tvGuestLogin.setVisibility(View.VISIBLE);
            tvRegister.setVisibility(View.GONE);
            tvGuestLogin.setOnClickListener(this);
        }
        if(isRegistrationEnabled && !isGuestLoginEnabled){
            Logger.error(TAG,"Only Registration enabled ");
            rtlSingleContainer.setVisibility(View.VISIBLE);
            containerBoth.setVisibility(View.GONE);
            tvGuestLogin.setVisibility(View.GONE);
            tvRegister.setVisibility(View.VISIBLE);
            tvRegister.setOnClickListener(this);
        }
        if(isRegistrationEnabled && isGuestLoginEnabled){
            Logger.error(TAG,"Registration and GuestLogin both enabled");
            containerBoth.setVisibility(View.VISIBLE);
            rtlSingleContainer.setVisibility(View.GONE);
            registerDivider.setVisibility(View.VISIBLE);
            tvTryDemoBoth.setVisibility(View.VISIBLE);
            tvRegisterBoth.setVisibility(View.VISIBLE);
            tvSocialLoginBoth.setVisibility(View.GONE);
            centerDividerSocial.setVisibility(View.GONE);
            tvRegisterBoth.setOnClickListener(this);
            tvTryDemoBoth.setOnClickListener(this);
        }
    }

    private void openGuestLoginActivity() {
        startActivity(new Intent(getBaseContext(),CCGuestLoginActivity.class));
    }

    private void openRegistrationActivity() {
        Intent operRegistration = new Intent(getApplicationContext(), CCWebViewActivity.class);
        operRegistration.putExtra(IntentExtraKeys.REGISTRATION_URL, "REGISTRATION");
        operRegistration.putExtra(IntentExtraKeys.REGISTER_TITLE,"REGISTER");
        startActivity(operRegistration);

        if(PreferenceHelper.contains(SharedPrefrenceKeys.LoginKeys.USERNAME) && PreferenceHelper.contains(SharedPrefrenceKeys.LoginKeys.PASSWORD)){
            edtUserName.setText(PreferenceHelper.get(SharedPrefrenceKeys.LoginKeys.USERNAME));
            edtPassword.setText(PreferenceHelper.get(SharedPrefrenceKeys.LoginKeys.PASSWORD));
        }
    }


    private void setupThemeColor(){
        primaryColor = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS,SettingSubType.COLOR_PRIMARY));
        btnLogin.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
        tvGuestLogin.setTextColor(primaryColor);
        wheel.getIndeterminateDrawable().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
        rememberSwitch.getThumbDrawable().setColorFilter(primaryColor,PorterDuff.Mode.MULTIPLY);
        rememberSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
        tvTryDemoBoth.setTextColor(primaryColor);
        tvRegisterBoth.setTextColor(primaryColor);
        tvRegister.setTextColor(primaryColor);
        tvSocialLoginBoth.setTextColor(primaryColor);
        tvGuestLogin.setTextColor(primaryColor);
        if(!(LocalConfig.isWhiteLabelled())){
            imgLogoLogin.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void setupClickListners() {
        btnLogin.setOnClickListener(this);
        tvGuestLogin.setOnClickListener(this);
        rememberSwitch.setOnCheckedChangeListener(this);
        edtPassword.setOnEditorActionListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(b){
            rememberSwitch.getThumbDrawable().setColorFilter((int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS,SettingSubType.COLOR_PRIMARY)), PorterDuff.Mode.MULTIPLY);
            rememberSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
        }else {
            rememberSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"),PorterDuff.Mode.MULTIPLY);
            rememberSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.buttonLogin:
                Logger.error(TAG,"rolebase "+RolebaseFeatures.isRolebaseEnabled());
                if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.TYPE_NOT_CONNECTED) {
                    tllUserName.setErrorEnabled(false);
                    tllUserName.setError(null);
                    tllPassword.setErrorEnabled(false);
                    tllPassword.setError(null);
                    Toast.makeText(CCLoginActivity.this, "Please check internet connection.", Toast.LENGTH_SHORT).show();
                } else {
                    login();
                }
                break;

            case R.id.textViewTryDemo:
                startActivity(new Intent(CCLoginActivity.this, CCGuestLoginActivity.class));
                break;

            case R.id.textViewRegister:
                openRegistrationActivity();
                break;

            case R.id.textViewRegisterBoth:
                openRegistrationActivity();
                break;

            case R.id.textViewTryDemo1:
                startActivity(new Intent(CCLoginActivity.this,CCGuestLoginActivity.class));
                break;
        }
    }

    private void login() {
        btnLogin.setEnabled(false);
        startProgressWheel();

        final String username = edtUserName.getText().toString().trim();
        final String password = edtPassword.getText().toString().trim();

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(btnLogin.getApplicationWindowToken(), 0);

        if (TextUtils.isEmpty(username)) {
            tllUserName.setErrorEnabled(true);
            tllUserName.setError("UserName cannot be empty");
        } else{
            tllUserName.setErrorEnabled(false);
            tllUserName.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            tllPassword.setErrorEnabled(true);
            tllPassword.setError("Password cannot be empty");
        } else {
            tllPassword.setErrorEnabled(false);
            tllPassword.setError(null);
        }

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {

            Logger.error(TAG, "IS COD ? " + SessionData.getInstance().isCometOnDemand());

            if(!cometChat.isCometOnDemand()){
                cometChat.login(username, password, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject responce) {
                        Logger.error(TAG, "Login success responce = " + responce);
                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.LOGGED_IN, "1");
                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.USERNAME, username);
                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.PASSWORD,password);
                        if(rememberSwitch.isChecked()){
                            PreferenceHelper.save(PreferenceKeys.LoginKeys.REMEMBER_ME, CometChatKeys.LoginKeys.USER_REMEMBER);
                        }else {
                            if(PreferenceHelper.contains(PreferenceKeys.LoginKeys.REMEMBER_ME))
                                PreferenceHelper.removeKey(PreferenceKeys.LoginKeys.REMEMBER_ME);
                        }

                        /*cometChat.getPushChannels(CCLoginActivity.this, "4", new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"Push Channel success = "+jsonObject);
                                try {
                                    String userChannel = jsonObject.getJSONObject("user").getString("channel");
                                    String groupChannel = jsonObject.getJSONObject("groups").getJSONObject("_2").getString("0");
                                    Logger.error(TAG,"User object = "+jsonObject.getJSONObject("user").getString("channel"));
                                    Logger.error(TAG,"group object = "+jsonObject.getJSONObject("groups").getJSONObject("_2").getString("0"));
                                    FirebaseMessaging.getInstance().subscribeToTopic(userChannel);
                                    FirebaseMessaging.getInstance().subscribeToTopic(groupChannel);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {

                            }
                        });*/
                        LaunchHelper.launchCometChat(CCLoginActivity.this);
                        btnLogin.setEnabled(true);
                        stopProgressWheel();
                    }

                    @Override
                    public void failCallback(JSONObject responce) {
                        Logger.error(TAG, "Login fail responce = " + responce);

                        btnLogin.setEnabled(true);
                        stopProgressWheel();

                        try {
                            String errorCode = responce.getString("code");

                            if(errorCode.equals("204")) {
                                Toast.makeText(CCLoginActivity.this, "Incorrect username or password.",
                                        Toast.LENGTH_SHORT).show();
                            } else if(errorCode.equals("100")) {
                                Toast.makeText(CCLoginActivity.this, "Error in connection.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(CCLoginActivity.this, "Error in connection.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {

                String adduserURL = "http://api.cometondemand.net/api/createuser";
                VolleyHelper volleyHelper = new VolleyHelper(this, adduserURL, new VolleyAjaxCallbacks() {
                    @Override
                    public void successCallback(String s) {
                        Logger.error(TAG, "AddUser responce = " + s);
                        try {
                            JSONObject adduserResponce = new JSONObject(s);
                            long userid = -1;
                            if (adduserResponce.has("success")) {
                                userid = adduserResponce.getJSONObject("success").getJSONObject("data").getInt("userid");
                            } else if (adduserResponce.has("failed")) {
                                userid = adduserResponce.getJSONObject("failed").getJSONObject("data").getInt("userid");
                            }

                            Logger.error(TAG, "User id = " + userid);
                            if (userid != -1) {
                                cometChat.login(String.valueOf(userid), new Callbacks() {
                                    @Override
                                    public void successCallback(JSONObject jsonObject) {
                                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.LOGGED_IN, "1");
                                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.USERNAME, username);
                                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.PASSWORD,password);
                                        if(rememberSwitch.isChecked()){
                                            PreferenceHelper.save(PreferenceKeys.LoginKeys.REMEMBER_ME, CometChatKeys.LoginKeys.USER_REMEMBER);
                                        }else {
                                            if(PreferenceHelper.contains(PreferenceKeys.LoginKeys.REMEMBER_ME))
                                                PreferenceHelper.removeKey(PreferenceKeys.LoginKeys.REMEMBER_ME);
                                        }
                                        LaunchHelper.launchCometChat(CCLoginActivity.this);
                                        btnLogin.setEnabled(true);
                                        stopProgressWheel();
                                    }

                                    @Override
                                    public void failCallback(JSONObject jsonObject) {
                                        Logger.error(TAG, "user-id fail login value = " + jsonObject);

                                        btnLogin.setEnabled(true);
                                        stopProgressWheel();
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(String s, boolean b) {
                        Logger.error(TAG, "AddUser fail responce = " + s);
                    }
                });

                volleyHelper.addNameValuePair("action", "createuser");
                volleyHelper.addNameValuePair("username", username);
                volleyHelper.addNameValuePair("password", password);
                volleyHelper.sendAjax();
                /*cometChat.loginWithUID(CCLoginActivity.this, "SUPERHERO1", new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.LOGGED_IN, "1");
                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.USERNAME, username);
                        PreferenceHelper.save(SharedPrefrenceKeys.LoginKeys.PASSWORD,password);
                        if(rememberSwitch.isChecked()){
                            PreferenceHelper.save(PreferenceKeys.LoginKeys.REMEMBER_ME, CometChatKeys.LoginKeys.USER_REMEMBER);
                        }else {
                            if(PreferenceHelper.contains(PreferenceKeys.LoginKeys.REMEMBER_ME))
                                PreferenceHelper.removeKey(PreferenceKeys.LoginKeys.REMEMBER_ME);
                        }
                        LaunchHelper.launchCometChat(CCLoginActivity.this);
                        btnLogin.setEnabled(true);
                        stopProgressWheel();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "loginWithUID: failCallback: "+jsonObject);
                    }
                });*/

            }
        }else {
            btnLogin.setEnabled(true);
            stopProgressWheel();
        }
    }

    private void startProgressWheel() {
        if (null != wheel) {
            wheel.setVisibility(View.VISIBLE);
        }
    }

    private void stopProgressWheel() {
        if (null != wheel) {
            wheel.setProgress(0);
            wheel.setVisibility(View.INVISIBLE);
        }
    }
    private String getTransparentPrimaryColor(String t) {
        String p =  (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_STRING));
        p = t + (p.substring(1, p.length()));
        return p;
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
            login();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
           super.onBackPressed();

                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                   finishAffinity();
               } else {
                   System.exit(0);
               }
            }
}
