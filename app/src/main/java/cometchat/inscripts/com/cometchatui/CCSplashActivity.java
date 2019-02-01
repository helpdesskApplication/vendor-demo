package cometchat.inscripts.com.cometchatui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.utils.Logger;

import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatui.helper.LaunchHelper;
import cometchat.inscripts.com.cometchatui.keys.SharedPrefrenceKeys;
import cometchat.inscripts.com.readyui.CCReadyUI;


public class CCSplashActivity extends AppCompatActivity {
    private static final java.lang.String TAG = CCSplashActivity.class.getSimpleName();
    private final int PERMISSION_LAUNCH_COMETCHAT = 11;

    private ProgressBar progressWheel;
    private RelativeLayout rlError;
    private TextView tvTryAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressWheel = (ProgressBar) findViewById(R.id.progressWheel);
        rlError = (RelativeLayout) findViewById(R.id.rlError);
        tvTryAgain = (TextView) findViewById(R.id.tvTryAgain);

//        progressWheel.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        checkAndLaunchCometchat();

        tvTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndLaunchCometchat();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LAUNCH_COMETCHAT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndLaunchCometchat();
                } else {
                    Toast.makeText(this, "CometChat requires this permission to launch...", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void checkAndLaunchCometchat() {

        CometChat.getInstance(this);

        rlError.setVisibility(View.GONE);
        progressWheel.setVisibility(View.VISIBLE);

        if(PreferenceHelper.contains(SharedPrefrenceKeys.LoginKeys.LOGGED_IN) && PreferenceHelper.get(SharedPrefrenceKeys.LoginKeys.LOGGED_IN).equals("1") && PreferenceHelper.contains(PreferenceKeys.LoginKeys.REMEMBER_ME) && PreferenceHelper.get(PreferenceKeys.LoginKeys.REMEMBER_ME).equals("1")){
            CCReadyUI.initializeCometChat(this, new Callbacks() {

                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "initializeCometChat successCallback : " + jsonObject.toString());
                    LaunchHelper.launchCometChat(CCSplashActivity.this);
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    progressWheel.setVisibility(View.GONE);
                    rlError.setVisibility(View.VISIBLE);
                    Logger.error(TAG, "initializeCometChat failCallback : " + jsonObject.toString());
                }
            });
        }else{
            startActivity(new Intent(CCSplashActivity.this,CCUrlInitializerActivity.class));
            finish();
        }

        //finish();
    }
}
