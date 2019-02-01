/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.helpers.CCPermissionHelper;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import adapters.InviteViaSmsAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import customsviews.CCContactsCompletionView;
import interfaces.CCContactsCallbacks;
import pojo.ContactPojo;
import utils.CCAsyncTaskContacts;

public class CCInviteViaSmsActivity extends AppCompatActivity implements TokenCompleteTextView.TokenListener {

    private static final String TAG = CCInviteViaSmsActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;
    private EditText inviteMessage;
    private TextView toLabel;
    private ProgressBar wheel;
    private Button inviteButton;

    private CCContactsCompletionView completionView;
    private InviteViaSmsAdapter adapter;
    private List<Object> contacts;
    private Context context;
    private int colorPrimary,colorPrimaryDark;
    private CometChat cometChat;
    String[] PERMISSIONS = { CCPermissionHelper.REQUEST_PERMISSION_READ_PHONE_STATE} ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_invite_via_sms);
        context = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_invite_sms_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupFields();
        setCCTheme();
        setupFieldListeners();

        CCPermissionHelper.requestPermissions(this, new String[]{CCPermissionHelper.REQUEST_PERMISSION_SEND_SMS,
                CCPermissionHelper.REQUEST_PERMISSION_READ_CONTACTS}, 1);

        setupFieldLanguages();
        executeContactsTask();
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
        wheel.getIndeterminateDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }

    private void setupFields() {
        inviteMessage = (EditText) findViewById(R.id.editTextInviteSMS);
        toLabel = (TextView) findViewById(R.id.textViewInviteLabel);

        inviteButton = (Button) findViewById(R.id.buttonInviteUser);
        inviteButton.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

        wheel = (ProgressBar) findViewById(R.id.progressWheel);
        wheel.setVisibility(View.VISIBLE);

        completionView = (CCContactsCompletionView) findViewById(R.id.inviteSMSSearch);
        //completionView.setEnabled(false);
        completionView.setAlpha(0.5F);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CCPermissionHelper.PERMISSION_PHONE_STATE){
            if(grantResults.length>0&& grantResults[0]==PackageManager.PERMISSION_GRANTED){
                sendSMS();
            }else {
                Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupFieldListeners() {
        inviteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(CCPermissionHelper.hasPermissions(context, PERMISSIONS)){
                    sendSMS();
                }else {
                    CCPermissionHelper.requestPermissions((Activity) context, PERMISSIONS, CCPermissionHelper.PERMISSION_PHONE_STATE);
                }
            }
        });
    }

    private void sendSMS() {
        String message = inviteMessage.getText().toString().trim();

        if (!TextUtils.isEmpty(message)) {
            contacts = completionView.getObjects();
            sendSMS(message);
        } else {
            Toast.makeText(CCInviteViaSmsActivity.this,
                    (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_EMPTY_MESSAGE)),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupFieldLanguages() {

        inviteMessage.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SMS_ANDROID)));
        setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SMS_ACTIONBAR)));


            inviteButton.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE)));
            this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_YOUR_FRIENDS)));
            toLabel.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_TO)));
            if (!TextUtils.isEmpty(inviteMessage.getText().toString())) {
                inviteMessage.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_MESSAGE)));
                inviteMessage.setSelection(((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_MESSAGE))).length());
            }
        }


    private void executeContactsTask() {
        new CCAsyncTaskContacts(this, new CCContactsCallbacks() {

            @Override
            public void successCallback(ArrayList<ContactPojo> allContacts) {
                if (0 < allContacts.size()) {
                    adapter = new InviteViaSmsAdapter(CCInviteViaSmsActivity.this, allContacts);
                    completionView.allowDuplicates(false);
                    completionView.setAdapter(adapter);
                    completionView.setTokenListener(CCInviteViaSmsActivity.this);
                    completionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Delete);
                    completionView.setHint((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_CONTACT_HINT)));
                    wheel.setVisibility(View.GONE);
                    //completionView.setEnabled(true);
                    completionView.setAlpha(1F);
                } else {
                    Toast.makeText(CCInviteViaSmsActivity.this,
                            (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_CONTACTS_FOUND)), Toast.LENGTH_LONG)
                            .show();
                    wheel.setVisibility(View.GONE);
                }
            }

            @Override
            public void failCallback(String errorMessage) {
                wheel.setVisibility(View.GONE);
            }
        }).execute();
    }

    private void sendSMS(String message) {
        if (null != contacts && contacts.size() > 0) {
            for (Object contact : contacts) {
                String phone = ((ContactPojo) contact).phone;
                if (!TextUtils.isEmpty(phone)) {
                    sendSMS(phone, message);
                }
            }
            finish();
        } else {
            Toast.makeText(CCInviteViaSmsActivity.this,
                    (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_CONTACT_SELECTED)), Toast.LENGTH_LONG).show();
            completionView.requestFocus();
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        String  SENT = "SMS_SENT";
        String  DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        ArrayList<PendingIntent> sentPendingIntentList = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntentList = new ArrayList<PendingIntent>();

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(CCInviteViaSmsActivity.this, "Invite sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(CCInviteViaSmsActivity.this, "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(CCInviteViaSmsActivity.this, "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(CCInviteViaSmsActivity.this, "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(CCInviteViaSmsActivity.this, "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                finish();
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context  arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(CCInviteViaSmsActivity.this, "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(CCInviteViaSmsActivity.this, "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();

        if (!TextUtils.isEmpty(message)) {
            ArrayList<String> smsParts = sms.divideMessage(message);
            for (int i = 0; i <smsParts.size() ; i++) {
                sentPendingIntentList.add(sentPI);
                deliveredPendingIntentList.add(deliveredPI);
            }
            sms.sendMultipartTextMessage(phoneNumber, null, smsParts, sentPendingIntentList, deliveredPendingIntentList);
        }
    }

    @Override
    public void onTokenAdded(Object arg0) {
        if (null != adapter && null != arg0) {
            adapter.remove((ContactPojo) arg0);
        }
    }

    @Override
    public void onTokenRemoved(Object arg0) {
        if (null != adapter && null != arg0) {
            adapter.add((ContactPojo) arg0);
        }
    }
}
