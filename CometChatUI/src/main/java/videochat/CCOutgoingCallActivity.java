/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package videochat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.inscripts.custom.ProfileRoundedImageView;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.plugins.VideoChat;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONObject;

import Keys.BroadCastReceiverKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;

public class CCOutgoingCallActivity extends AppCompatActivity implements OnClickListener {

    private static final java.lang.String TAG = CCOutgoingCallActivity.class.getSimpleName();
    private ImageButton cancelCallButton, volumeControlButton;
    private TextView callerNameTextView, callingTextview;
    private BroadcastReceiver receiver;
    private ProfileRoundedImageView buddyProfileImageView;
    private String roomName, buddyId;
    public static CCOutgoingCallActivity outgoingCallActivity;
    private SessionData session;
    private boolean isAudioCall;
    private boolean isRinging = true;

    private int colorPrimary, colorPrimaryDark;

    private BroadcastReceiver broadcastReceiver;
    private CometChat cometChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outgoingCallActivity = this;
        setContentView(R.layout.cc_activity_outgoing_call);

        setCCTheme();
        Intent intent = getIntent();
        cometChat = CometChat.getInstance(this);
        buddyId = intent.getStringExtra(CometChatKeys.AVchatKeys.CALLER_ID);
        isAudioCall = intent.getBooleanExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, false);

        cancelCallButton = (ImageButton) findViewById(R.id.buttonCancelCall);
        volumeControlButton = (ImageButton) findViewById(R.id.buttonSpeaker);
        callerNameTextView = (TextView) findViewById(R.id.textViewOutgoingCallerName);
        buddyProfileImageView = (ProfileRoundedImageView) findViewById(R.id.imageViewOutgoingProfilePicture);
        callingTextview = (TextView) findViewById(R.id.textViewCallingText);

        callingTextview.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALLING)));


        cancelCallButton.getBackground().setColorFilter(Color.parseColor("#eb5160"), PorterDuff.Mode.SRC_ATOP);
        volumeControlButton.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

        volumeControlButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRinging) {
                    CommonUtils.stopRingtone();
                    volumeControlButton.setImageResource(R.drawable.cc_ic_mutespeaker);
                    isRinging = false;
                } else {
                    CommonUtils.playRingtone(CCOutgoingCallActivity.this, "outgoing_call_sound.mp3");
                    volumeControlButton.setImageResource(R.drawable.cc_ic_volume_control);
                    isRinging = true;
                }
            }
        });


        cancelCallButton.setOnClickListener(this);

        Contact contact = Contact.getContactDetails(Long.parseLong(buddyId));
        if (null != contact) {
            callerNameTextView.setText(contact.name);
            LocalStorageFactory.loadImageUsingURL(this, contact.avatarURL, buddyProfileImageView, R.drawable.cc_default_avatar);
        }

        session = SessionData.getInstance();
        roomName = intent.getStringExtra(CometChatKeys.AVchatKeys.ROOM_NAME);
        Logger.error(TAG, "RoomName = " + roomName);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {

                String caller = intent.getStringExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALLER_ID);
                if (caller.equalsIgnoreCase(buddyId)) {
                    CommonUtils.stopRingtone();

                    if (isAudioCall) {
                        roomName = intent.getStringExtra(CometChatKeys.AVchatKeys.ROOM_NAME);
                        Logger.error(TAG, "RoomName = " + roomName);
                        VideoChat.startVideoCall(roomName, true, false, CCOutgoingCallActivity.this,
                                CCVideoChatActivity.class, buddyId);
                    } else {
                        VideoChat.startVideoCall(roomName, true, true, CCOutgoingCallActivity.this,
                                CCVideoChatActivity.class, buddyId);
                    }
                    finish();
                }
            }
        };

        CommonUtils.playRingtone(this, "outgoing_call_sound.mp3");
        session.setAvchatStatus(2);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                session.setAvchatStatus(0);
                CommonUtils.stopRingtone();
                finish();
            }
        }, LocalConfig.OUTGOING_CALL_TIMEOUT);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.error(TAG,"avchat busytone broadcast ");
                Bundle extras = intent.getExtras();
                if (extras.containsKey(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY)) {
                    endOutgoingCall();
                }
            }
        };
        registerReceiver(broadcastReceiver,
                new IntentFilter(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY));
    }

    private void setCCTheme() {
        colorPrimary = (int) CometChat.getInstance(this).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) CometChat.getInstance(this).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
//        toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        CCUIHelper.setStatusBarColor(this, colorPrimaryDark);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonCancelCall) {
            endOutgoingCall();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (receiver != null) {
            registerReceiver(receiver, new IntentFilter(BroadCastReceiverKeys.AvchatKeys.EVENT_AVCHAT_ACCEPTED));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void endOutgoingCall() {
        Logger.error(TAG, "endOutgoingCall buddyId : "+buddyId);

        if(isAudioCall) {

            cometChat.cancelAudioChatRequest(buddyId, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "cancelAudioChatRequest successCallback = " + jsonObject.toString());
                    session.setAvchatStatus(0);
                    CommonUtils.stopRingtone();
                    finish();
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "cancelAudioChatRequest failCallback = " + jsonObject.toString());
                }
            });
        }else {

            cometChat.cancelAVChatRequest(buddyId, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG, " cancelAVChatRequest successCallback = " + jsonObject);
                    session.setAvchatStatus(0);
                    CommonUtils.stopRingtone();
                    finish();
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG, " cancelAVChatRequest failCallback = " + jsonObject);
                }
            });
        }
    }
}
