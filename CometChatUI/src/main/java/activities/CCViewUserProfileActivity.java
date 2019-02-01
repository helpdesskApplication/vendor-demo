package activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.ProfileRoundedImageView;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.StaticMembers;
import com.inscripts.helpers.CCPermissionHelper;

import org.json.JSONObject;

import Keys.BroadCastReceiverKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import customsviews.ConfirmationWindow;
import models.Contact;
import videochat.CCOutgoingCallActivity;

/**
 * Created by inscripts-236 on 7/9/17.
 */

public class CCViewUserProfileActivity extends AppCompatActivity {
    private static final String TAG = CCViewUserProfileActivity.class.getSimpleName();
    private Toolbar toolbar;
    private CometChat cometChat;
    private long contactId;
    private int colorPrimary, colorPrimaryDark;
    protected TextView name, statusMessage, tvVideoCall, tvAudioCall;
    protected ImageView buddyStatus,imgVideo,imgAudio;
    private ProfileRoundedImageView buddyProfileImage;
    protected RelativeLayout videoCallContainer, audioCallContainer;
    private RelativeLayout ccContainer;
    private Contact contact;
    private String contactName;
    private FeatureState avCallState;
    private FeatureState audioCallState;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_viewuserprofile);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            ccContainer = (RelativeLayout) findViewById(R.id.cc_view_profile_container);
            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar, R.drawable.cc_rounded_corners_colored);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setTitle((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_VIEW_PROFILE)));
        Intent intent = getIntent();
        contactId = intent.getLongExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, 0);
        Logger.error(TAG, "buddyId: " + contactId);
        initializeFeatureState();
        setUpFields();
        setCCTheme();
        contact = Contact.getContactDetails(contactId);
        if (contact != null) {
            contactName = contact.name;
            name.setText(contactName);
            statusMessage.setText(contact.statusMessage);
            String profileUrl = contact.avatarURL;
            if (null != profileUrl) {
                if (!profileUrl.contains(URLFactory.PROTOCOL_PREFIX) && !profileUrl.contains(URLFactory.PROTOCOL_PREFIX_SECURE)) {
                    profileUrl = URLFactory.getBaseURL() + profileUrl;
                }
                Logger.error(TAG,"Photo URL: "+profileUrl);
                LocalStorageFactory.loadImageUsingURL(this, profileUrl, buddyProfileImage, R.drawable.cc_ic_default_avtar);
            }
            String status = contact.status;
            if (TextUtils.isEmpty(status)) {
                buddyStatus.setImageResource(R.drawable.cc_status_available);
            } else {
                switch (status) {
                    case CometChatKeys.StatusKeys.AVALIABLE:
                        buddyStatus.setImageResource(R.drawable.cc_status_available);
                        break;
                    case CometChatKeys.StatusKeys.AWAY:
                        buddyStatus.setImageResource(R.drawable.cc_status_ofline);
                        break;
                    case CometChatKeys.StatusKeys.BUSY:
                        buddyStatus.setImageResource(R.drawable.cc_status_busy);
                        break;
                    case CometChatKeys.StatusKeys.OFFLINE:
                        buddyStatus.setImageResource(R.drawable.cc_status_ofline);
                        break;
                    default:
                        buddyStatus.setImageResource(R.drawable.cc_status_available);
                }
            }
        }
        videoCallContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (avCallState == FeatureState.INACCESSIBLE) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCViewUserProfileActivity.this);
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else {
                    makeVideoCall();
                }
            }
        });
        audioCallContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (avCallState == FeatureState.INACCESSIBLE) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCViewUserProfileActivity.this);
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else {
                    makeAudioCall();
                }
            }
        });
    }

    private void initializeFeatureState() {
        avCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.VIDEO_CALL_ENABLED));
        audioCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AUDIO_CALL_ENABLED));
    }

    private void makeVideoCall() {
        if (CommonUtils.isConnected()) {
            if (Build.VERSION.SDK_INT >= 16) {
                String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                        CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
                if (CCPermissionHelper.hasPermissions(getBaseContext(), PERMISSIONS)) {
                    showCallPopup(false);
                } else {
                    CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_VIDEO_CALL);
                }
            } else {
                Toast.makeText(getBaseContext(), "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
    }

    private void makeAudioCall() {
        if (CommonUtils.isConnected()) {
            if (Build.VERSION.SDK_INT >= 16) {
                String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                        CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
                if (CCPermissionHelper.hasPermissions(getBaseContext(), PERMISSIONS)) {
                    showCallPopup(true);
                } else {
                    CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_AUDIO_CALL);
                }
            } else {
                Toast.makeText(getBaseContext(), "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
    }

    private void showCallPopup(final boolean isAudioOnlyCall) {
        try {
            String yes = StaticMembers.POSITIVE_TITLE, no = StaticMembers.NEGATIVE_TITLE;
            ConfirmationWindow cWindow = new ConfirmationWindow(this, yes, no) {

                @Override
                protected void setNegativeResponse() {
                    super.setNegativeResponse();
                }

                @Override
                protected void setPositiveResponse() {
                    super.setPositiveResponse();
                    initiateCall(isAudioOnlyCall);
                }
            };
            if (isAudioOnlyCall) {
                cWindow.setMessage("Call "
                        + contactName + "?");
            } else {
                cWindow.setMessage("Call " + contactName
                        + "?");
            }

            cWindow.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("HandlerLeak")
    private void initiateCall(final boolean isAudioOnlyCall) {
        if (isAudioOnlyCall) {

            cometChat.sendAudioChatRequest(String.valueOf(contactId), new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "sendAudioChatRequest Success callback = " + jsonObject);
                    try {
                        Intent intent = new Intent(CCViewUserProfileActivity.this, CCOutgoingCallActivity.class);
                        intent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(contactId));
                        intent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, isAudioOnlyCall);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "Fail callback = " + jsonObject);
                }
            });
        } else {
            cometChat.sendAVChatRequest(String.valueOf(contactId), new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "sendAVChatRequest responc = " + jsonObject);
                    try {
                        Intent intent = new Intent(CCViewUserProfileActivity.this, CCOutgoingCallActivity.class);
                        intent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(contactId));
                        intent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, isAudioOnlyCall);
                        intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME,jsonObject.getString("callid"));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "sendAVChatRequest fail responc = " + jsonObject);
                }
            });
        }
    }

    private void setUpFields() {
        name = (TextView) findViewById(R.id.textViewProfileBuddyName);
        buddyStatus = (ImageView) findViewById(R.id.imageViewProfileBuddyStatus);
        statusMessage = (TextView) findViewById(R.id.textViewProfileBuddyStatusMessage);
        tvVideoCall = (TextView) findViewById(R.id.textViewBuddyVideoCall);
        tvAudioCall = (TextView) findViewById(R.id.textViewBuddyAudioCall);
        buddyProfileImage = (ProfileRoundedImageView) findViewById(R.id.imageViewProfileBuddyPic);
        videoCallContainer = (RelativeLayout) findViewById(R.id.relativeLayoutBuddyVideoCall);
        audioCallContainer = (RelativeLayout) findViewById(R.id.relativeLayoutBuddyAudioCall);
        imgVideo = (ImageView) findViewById(R.id.imageViewBuddyVideoCall);
        imgAudio = (ImageView) findViewById(R.id.imageViewBuddyAudioCall);
        animateAudioVideoImages();
        if (avCallState == FeatureState.INVISIBLE) {
            videoCallContainer.setVisibility(View.GONE);
        }

        if (audioCallState == FeatureState.INVISIBLE) {
            audioCallContainer.setVisibility(View.GONE);
        }
    }



    private void setCCTheme() {
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this, colorPrimaryDark);
        imgAudio.getDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgVideo.getDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CCPermissionHelper.PERMISSION_AUDIO_CALL:
                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    showCallPopup(true);
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_VIDEO_CALL:
                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED ) {
                    showCallPopup(false);
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void animateAudioVideoImages() {
        ObjectAnimator scaleVideoImageX = ObjectAnimator.ofFloat(imgVideo, "scaleX", 0.0f,1.0f);
        ObjectAnimator scaleVideoImageY = ObjectAnimator.ofFloat(imgVideo, "scaleY", 0.0f,1.0f);
        ObjectAnimator scaleAudioImageX = ObjectAnimator.ofFloat(imgAudio, "scaleX", 0.0f,1.0f);
        ObjectAnimator scaleAudioImageY = ObjectAnimator.ofFloat(imgAudio, "scaleY", 0.0f,1.0f);
        scaleVideoImageX.setDuration(500);
        scaleVideoImageY.setDuration(500);
        scaleAudioImageX.setDuration(500);
        scaleAudioImageY.setDuration(500);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleAudioImageX,scaleAudioImageY,scaleVideoImageX,scaleVideoImageY);
        animatorSet.start();
    }
}
