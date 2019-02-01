package activities;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONObject;

import java.util.ArrayList;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Status;

public class CCUpdateStatusMessageActivity extends AppCompatActivity {

    private static final String TAG = CCUpdateStatusMessageActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private EditText edtStatusMessage;
    private Button btnUpdate;
    private TextView txtMessageCount;

    private int MAX_LENGTH = 140;
    private String previousStatus;
    private int colorPrimary,colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_update_status_message);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_status_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();
        setupFields();
        setupFieldListeners();

        // Remove Later
        this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_STATUS_MESSAGE)));
        setupFieldValues();
    }

    private void setCCTheme(){
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this,colorPrimaryDark);
    }

    private void setupFields() {
        edtStatusMessage = (EditText) findViewById(R.id.editTextStatusMessage);
        btnUpdate = (Button) findViewById(R.id.button_update_status_message);
        txtMessageCount = (TextView) findViewById(R.id.txt_count);
    }

    private void setupFieldValues() {
        btnUpdate.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_UPDATE)));
        btnUpdate.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        previousStatus = PreferenceHelper.get(PreferenceKeys.UserKeys.STATUS_MESSAGE);

        if(previousStatus != null){
            edtStatusMessage.setText(previousStatus);
            edtStatusMessage.setSelection(previousStatus.length());
            int statusLenght = previousStatus.length();
            String m = String.valueOf(MAX_LENGTH-statusLenght);
            txtMessageCount.setText(m);
        }
    }

    private void setupFieldListeners() {
        edtStatusMessage.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int statusLenght = edtStatusMessage.getText().toString().length();
                String m = String.valueOf(MAX_LENGTH-statusLenght);
                txtMessageCount.setText(m);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String newStatusMessage = edtStatusMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(newStatusMessage)) {
                    cometChat.updateStatusMessage(newStatusMessage, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Logger.error(TAG,"updateStatusMessage(): successCallback: "+jsonObject);
                            PreferenceHelper.save(PreferenceKeys.UserKeys.STATUS_MESSAGE, newStatusMessage);

                            ArrayList<Status> statusesList = (ArrayList<Status>) Status.getStatusFromMessage(newStatusMessage);
                            if (null == statusesList || statusesList.size() == 0) {
                                Status.insertStatus(newStatusMessage);
                            }
                            Toast.makeText(CCUpdateStatusMessageActivity.this, "Status Updated", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG,"updateStatusMessage(): failCallback: "+jsonObject);
                        }
                    });

                } else {
                    edtStatusMessage.setError((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_ENTER_STATUS)));
                    edtStatusMessage.setText("");
                }
            }
        });
    }

}
