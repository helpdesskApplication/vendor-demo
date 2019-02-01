package cometchat.inscripts.com.cometchatui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.activities.CCCodLoginActivity;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatui.helper.LaunchHelper;
import cometchat.inscripts.com.readyui.CCReadyUI;
import utils.NetworkUtil;

public class CCUrlInitializerActivity extends AppCompatActivity implements View.OnClickListener {
    private static final java.lang.String TAG = CCUrlInitializerActivity.class.getSimpleName();
    private static final int REQUEST_CODE_COD_LOGIN = 11111;
    private EditText urlField;
    private Button btnLogin;

    private TextInputLayout tilBaseUrl;
    private CometChat cometChat;
    private ProgressBar progressBar;
    private RelativeLayout rlError;

    //Configuration
    private boolean isCometOnDemand = true;
    private String siteURL = "";
    private String licenceKey = "";
    private String apiKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        cometChat = CometChat.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_initializer);
        setupFields();
        setupClickListeners();
        urlField.setText(siteURL);
    }

    private void setupFields() {
        urlField = findViewById(R.id.editTextURL);
        btnLogin = findViewById(R.id.buttonDemoLogin);
        tilBaseUrl = findViewById(R.id.input_layout_txt_url);
        rlError = findViewById(R.id.rlError);
        progressBar = findViewById(R.id.progress_weel);

        if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.TYPE_NOT_CONNECTED) {
            progressBar.setVisibility(View.INVISIBLE);
            rlError.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.buttonDemoLogin:
                if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.TYPE_NOT_CONNECTED) {
                    rlError.setVisibility(View.VISIBLE);
                } else {
                    initCheck();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_COD_LOGIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(CCUrlInitializerActivity.this, "Login Successful", Toast.LENGTH_LONG).show();
                cometChat.loginWithBaseData(data.getStringExtra(PreferenceKeys.DataKeys.BASE_DATA), new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        PreferenceHelper.save(PreferenceKeys.LoginKeys.LOGGED_IN, CometChatKeys.LoginKeys.USER_LOGGED_IN);
                        PreferenceHelper.save(PreferenceKeys.LoginKeys.LOGGED_IN_AS_COD, "1");
                        initializeAndLaunchCometChat();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Toast.makeText(CCUrlInitializerActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
                    }
                });
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(CCUrlInitializerActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(CCUrlInitializerActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initCheck() {
        tilBaseUrl.setError(null);

        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        rlError.setVisibility(View.INVISIBLE);


        /** To Close Soft Keyboard **/
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(btnLogin.getApplicationWindowToken(), 0);

        if (isCometOnDemand) {
            //cloud

            siteURL = "";
            checkLaunchLogin();
        } else {
            //self hosted

            siteURL = urlField.getText().toString().trim();

            if (!TextUtils.isEmpty(siteURL)) {

                cometChat.checkCometChatUrl(this, siteURL, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject responce) {
                        Logger.error(TAG, "checkCometChatUrl successCallback = " + responce.toString());
                        try {
                            siteURL = responce.getString("final_url");
                            tilBaseUrl.setError(null);
                            checkLaunchLogin();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject responce) {
                        Logger.error(TAG, "checkCometChatUrl failCallback = " + responce.toString());

                        btnLogin.setEnabled(true);
                        progressBar.setVisibility(View.INVISIBLE);
                        rlError.setVisibility(View.VISIBLE);

                        try {
                            tilBaseUrl.setError(responce.getString("message"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.INVISIBLE);
                tilBaseUrl.setError("URL cannot be empty!");
            }
        }
    }

    private void checkLaunchLogin() {
        cometChat.initializeCometChat(siteURL, licenceKey, apiKey, isCometOnDemand, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "initializeCometChat successCallback : " + jsonObject.toString());
                Logger.error(TAG, "LocalConfig.isApp : " + LocalConfig.isApp);
                if (LocalConfig.isApp) {

                    Logger.error(TAG, "SessionData.getInstance().isCometOnDemand() : " + SessionData.getInstance().isCometOnDemand());
                    if (SessionData.getInstance().isCometOnDemand()) {

                        String sendurl = "http://" + PreferenceHelper.get(PreferenceKeys.DataKeys.COD_ID) + URLFactory.getCodLoginUrl();
                        Logger.error(TAG, "sendurl : " + sendurl);
                        VolleyHelper helper = new VolleyHelper(getApplicationContext(), sendurl, new VolleyAjaxCallbacks() {
                            @Override
                            public void successCallback(String response) {
                                Logger.error(TAG, "successCallback : " + response);

                                try {
                                    JSONObject json = new JSONObject(response);
                                    if (json.has("failed")) {
                                        JSONObject jsonObject = json.getJSONObject("failed");
                                        String message = "Error Message";
                                        if (jsonObject.has("message")) {
                                            message = jsonObject.getString("message");
                                        }

                                        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                                                CCUrlInitializerActivity.this);
                                        alertDialog2.setTitle("Error");
                                        alertDialog2.setMessage(message);
                                        alertDialog2.setCancelable(false);
                                        alertDialog2.setIcon(android.R.drawable.ic_dialog_alert);
                                        alertDialog2.setNegativeButton("OK",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.cancel();
                                                    }
                                                });
                                        alertDialog2.show();

                                    } else {
                                        JSONObject jsonObject = json.getJSONObject("success");
                                        String url = null;
                                        int ccAuth = 0, guest = 0;

                                        Logger.error(TAG, "jsonObject : " + jsonObject.toString());

                                        if (jsonObject.has("redirect_url")) {
                                            url = "http://" + jsonObject.getString("redirect_url");
                                        }
                                        if (jsonObject.has("use_ccauth")) {
                                            ccAuth = jsonObject.getInt("use_ccauth");
                                        }
                                        if (jsonObject.has("guest_mode")) {
                                            guest = jsonObject.getInt("guest_mode");
                                        }
                                        if (jsonObject.has("version")) {
                                            PreferenceHelper.save(PreferenceKeys.LoginKeys.VERSION_CODE, jsonObject.getString("version"));
                                        }

                                        final String Url = url;
                                        final int CCAuth = ccAuth;
                                        final int Guest = guest;
                                        Logger.debug(TAG, "URL before JSON call : " + url);
//                                                if (CCAuth == 1) {
//                                                            i = new Intent(getApplicationContext(), SocialAuthActivity.class);
//                                                        } else if(Guest == 1) {
//                                                            i = new Intent(getApplicationContext(), CoDLoginTypesActivity.class);
//                                                            i.putExtra("Url", Url);
//                                                        } else {
                                        Intent i = new Intent(getApplicationContext(), CCCodLoginActivity.class);
                                        i.putExtra("Url", Url);
//                                                        }
                                        startActivityForResult(i, REQUEST_CODE_COD_LOGIN);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failCallback(String response, boolean isNoInternet) {
                                Logger.error("No Internet");
                            }
                        });
                        if (LocalConfig.isWhiteLabelled()) {
                            helper.addNameValuePair(CometChatKeys.AjaxKeys.PLATFORM, "whitelabeledapp");
                        } else {
                            helper.addNameValuePair(CometChatKeys.AjaxKeys.PLATFORM, "brandedapp");
                        }
                        helper.sendAjax();

                    } else {
                        startActivity(new Intent(CCUrlInitializerActivity.this, CCLoginActivity.class));

                        btnLogin.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    startActivity(new Intent(CCUrlInitializerActivity.this, CCLoginActivity.class));

                    /*cometChat.loginWithUID(CCUrlInitializerActivity.this,"CC1", new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {


                            cometChat.getPushChannels(CCUrlInitializerActivity.this, "11", new Callbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"getPushChannels = "+jsonObject);
                                    FirebaseMessaging.getInstance().subscribeToTopic("C_130d65816b0b6e1a2f266b261ee44690a");
                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {

                                }
                            });

                            *//*LaunchHelper.launchCometChat(CCUrlInitializerActivity.this);

                            cometChat.launchCometChat(CCUrlInitializerActivity.this, false, "CC2", false, false, new LaunchCallbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {

                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {

                                }

                                @Override
                                public void userInfoCallback(JSONObject jsonObject) {

                                }

                                @Override
                                public void chatroomInfoCallback(JSONObject jsonObject) {

                                }

                                @Override
                                public void onMessageReceive(JSONObject jsonObject) {

                                }

                                @Override
                                public void error(JSONObject jsonObject) {

                                }

                                @Override
                                public void onWindowClose(JSONObject jsonObject) {

                                }

                                @Override
                                public void onLogout() {

                                }
                            });*//*
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {

                        }
                    });*/
                    btnLogin.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                try {
                    if (((String) jsonObject.get("code")).equalsIgnoreCase(CometChatKeys.ErrorKeys.CODE_LICENSE_VERIFICATION_FAILED)) {
                        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                                CCUrlInitializerActivity.this);
                        alertDialog2.setTitle("Error");
                        alertDialog2.setMessage(jsonObject.getString("message"));
                        alertDialog2.setCancelable(false);
                        alertDialog2.setIcon(android.R.drawable.ic_dialog_alert);
                        alertDialog2.setNegativeButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                        alertDialog2.show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initializeAndLaunchCometChat() {
        CCReadyUI.initializeCometChat(CCUrlInitializerActivity.this, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                LaunchHelper.launchCometChat(CCUrlInitializerActivity.this);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "failCallback = " + jsonObject);
            }
        });

    }
}
