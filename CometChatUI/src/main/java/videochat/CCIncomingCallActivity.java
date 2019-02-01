/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package videochat;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.ProfileRoundedImageView;
import com.inscripts.enums.FeatureState;
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
import com.inscripts.helpers.CCPermissionHelper;

import org.json.JSONObject;

import Keys.BroadCastReceiverKeys;
import activities.CCSingleChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;

public class CCIncomingCallActivity extends AppCompatActivity implements OnClickListener {

	private static final java.lang.String TAG = CCIncomingCallActivity.class.getSimpleName();
	private ImageButton answerCallButton, rejectCallButton ,volumeControlButton;
	private TextView callerNameTextView, callPlaceHolder;
	private ProfileRoundedImageView buddyProfileImageView;
	private String roomName, buddyId;
	private Handler handler;
	private Runnable incomingTimeOutRunnable;
	public static CCIncomingCallActivity incomingCallActivity;
	private Vibrator vibrator;
	private SessionData session;
	private boolean isAudioOnlyCall;
	private Window window;
	private boolean isRinging = true;

    private int colorPrimary, colorPrimaryDark;
	private CometChat cometChat;
	private BroadcastReceiver broadcastReceiver;
	private FeatureState avCallState, audioCallState;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cc_activity_incoming_call);
        setCCTheme();
		incomingCallActivity = this;
		cometChat = CometChat.getInstance(this);
		try {
			window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
			window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		}catch (Exception e){
			e.printStackTrace();
		}
		Intent intent = getIntent();
		roomName = intent.getStringExtra(CometChatKeys.AVchatKeys.ROOM_NAME);
		buddyId = intent.getStringExtra(CometChatKeys.AVchatKeys.CALLER_ID);
		Logger.error(TAG,"Buddy Id = "+buddyId);
		isAudioOnlyCall = intent.getBooleanExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, false);
		answerCallButton = (ImageButton) findViewById(R.id.buttonAnswerCall);
		rejectCallButton = (ImageButton) findViewById(R.id.buttonRejectCall);
		callerNameTextView = (TextView) findViewById(R.id.textViewCallerName);
		volumeControlButton = (ImageButton) findViewById(R.id.buttonSpeaker);
		buddyProfileImageView = (ProfileRoundedImageView) findViewById(R.id.imageViewBuddyProfilePicture);

		callPlaceHolder = (TextView) findViewById(R.id.textViewCallPlaceholder);

		rejectCallButton.setOnClickListener(this);
		answerCallButton.setOnClickListener(this);

		rejectCallButton.getBackground().setColorFilter(Color.parseColor("#eb5160"), PorterDuff.Mode.SRC_ATOP);
		answerCallButton.getBackground().setColorFilter(Color.parseColor("#36b581"), PorterDuff.Mode.SRC_ATOP);
		volumeControlButton.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
		avCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.VIDEO_CALL_ENABLED));
		audioCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AUDIO_CALL_ENABLED));

		volumeControlButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {

				if(isRinging) {
					CommonUtils.stopVibrate(CCIncomingCallActivity.this);
					CommonUtils.stopRingtone();
					volumeControlButton.setImageResource(R.drawable.cc_ic_mutespeaker);
					isRinging = false;
				}else{
					CommonUtils.playRingtone(CCIncomingCallActivity.this, "incoming_call_sound.mp3");
					if (!session.isVibrateOn()) {
						long pattern[] = { 0, 800, 200, 500, 200, 500, 200 };
						vibrator = CommonUtils.getVibratorInstance(CCIncomingCallActivity.this);
						handler.postDelayed(incomingTimeOutRunnable, LocalConfig.INCOMING_CALL_TIMEOUT);
						vibrator.vibrate(pattern, 1);
						session.setVibrateOn(true);
					}
					volumeControlButton.setImageResource(R.drawable.cc_ic_volume_control);
					isRinging = true;
				}
			}

		});
		/* Reset call duration value */
		session = SessionData.getInstance();
		session.setAVChatCallStartTime(0);

		if(buddyId == null){
			buddyId = PreferenceHelper.get("FCMBuddyID");
		}
		Contact contact = Contact.getContactDetails(Long.parseLong(buddyId));
		if (null != contact) {
			if (contact.name.isEmpty()){
				callerNameTextView.setText("");
			}else{
				callerNameTextView.setText(contact.name);
			}
            LocalStorageFactory.loadImageUsingURL(this, contact.avatarURL, buddyProfileImageView, R.drawable.cc_default_avatar);
		}

		if(intent.hasExtra(CometChatKeys.AVchatKeys.CALLER_NAME)){
			callerNameTextView.setText(intent.getStringExtra(CometChatKeys.AVchatKeys.CALLER_NAME));
		}
		callPlaceHolder.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INCOMING_CALL)));
		handler = new Handler();
		incomingTimeOutRunnable = new Runnable() {

			@Override
			public void run() {
				session.setAvchatStatus(0);
				finish();
				sendNoAnswerAjax();
			}
		};

		if (!session.isVibrateOn()) {
			long pattern[] = { 0, 800, 200, 500, 200, 500, 200 };
			vibrator = CommonUtils.getVibratorInstance(this);
			handler.postDelayed(incomingTimeOutRunnable, LocalConfig.INCOMING_CALL_TIMEOUT);
			vibrator.vibrate(pattern, 1);
			session.setVibrateOn(true);
		}
		session.setAvchatStatus(1);
		CommonUtils.playRingtone(this, "incoming_call_sound.mp3");

		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Logger.error(TAG, "broadcastReceiver onReceive");
				Bundle extras = intent.getExtras();
				if (extras.containsKey(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALL_END)) {
                    Logger.error(TAG, "broadcastReceiver onReceive AVCHAT_CALL_END");
					endIncomingCall();
				}
				if (extras.containsKey(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALL_CANCEL)) {
                    Logger.error(TAG, "broadcastReceiver onReceive AVCHAT_CALL_CANCEL");
					cancelIncomingCall();
				}
				if (extras.containsKey(BroadCastReceiverKeys.AvchatKeys.CALL_CANCEL_FROM_NOTIFICATION)) {
					Logger.error(TAG, "broadcastReceiver onReceive: CALL_CANCEL_FROM_NOTIFICATION");
					finish();
				}
			}
		};

		Logger.error(TAG,"Closed Call Register registred");
		registerReceiver(broadcastReceiver, new IntentFilter(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY));
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

    private void setCCTheme() {
        colorPrimary = (int) CometChat.getInstance(this).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) CometChat.getInstance(this).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
//        toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        CCUIHelper.setStatusBarColor(this,colorPrimaryDark);
    }

	private void sendNoAnswerAjax() {

		cometChat.sendNoAnswerCall(buddyId, new Callbacks() {
			@Override
			public void successCallback(JSONObject jsonObject) {
				Logger.error(TAG,"SendNoAnswerCall responce = "+jsonObject);
			}

			@Override
			public void failCallback(JSONObject jsonObject) {
				Logger.error(TAG,"SendNoAnswerCall fail responce= "+jsonObject);
			}
		});
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case CCPermissionHelper.PERMISSION_ACCEPT_CALL:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
					acceptAndLaunch();
				} else {
					Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
				}
				break;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonAnswerCall) {
			if (Build.VERSION.SDK_INT >= 16) {
				String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
						CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
				if(CCPermissionHelper.hasPermissions(this, PERMISSIONS)){
					acceptAndLaunch();
				}else{
					CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_ACCEPT_CALL);
				}
			}
        } else if (v.getId() == R.id.buttonRejectCall) {
            rejectIncomingCall();
            CommonUtils.stopVibrate(this);
            CommonUtils.stopRingtone();
            session.setAvchatStatus(0);
            finish();
        }
	}

	private void acceptAndLaunch(){
		CommonUtils.stopVibrate(this);
		CommonUtils.stopRingtone();
		sendAcceptedAjax();
//		finish();
	}

	private void rejectIncomingCall() {
		cancelIncomingCallTimer();

		if(isAudioOnlyCall){
			cometChat.rejectAudioChatRequest(buddyId, roomName, new Callbacks() {
				@Override
				public void successCallback(JSONObject jsonObject) {
					Logger.error(TAG,"rejectAudioChatRequest responce = "+jsonObject);
				}

				@Override
				public void failCallback(JSONObject jsonObject) {
					Logger.error(TAG,"rejectAudioChatRequest fail responce = "+jsonObject);
				}
			});
		}else{
			cometChat.rejectAVChatRequest(buddyId, roomName, new Callbacks() {
				@Override
				public void successCallback(JSONObject jsonObject) {
					Logger.error(TAG,"rejectAVChatRequest responce = "+jsonObject);
				}

				@Override
				public void failCallback(JSONObject jsonObject) {
					Logger.error(TAG,"rejectAVChatRequest fail responce = "+jsonObject);
				}
			});
		}
	}

	private void endIncomingCall() {
		cancelIncomingCallTimer();

		if(isAudioOnlyCall){
			cometChat.endAudioChatCall(buddyId, roomName, new Callbacks() {
				@Override
				public void successCallback(JSONObject jsonObject) {
					Logger.error(TAG,"endIncomingCall responce = "+jsonObject);
					finish();
				}

				@Override
				public void failCallback(JSONObject jsonObject) {
					Logger.error(TAG,"endIncomingCall fail responce = "+jsonObject);
				}
			});
		}else{
			cometChat.endAVChatCall(this, buddyId, roomName, new Callbacks() {
				@Override
				public void successCallback(JSONObject jsonObject) {
					Logger.error(TAG,"endAVChatCall successCallback responce = "+jsonObject);
					cancelIncomingCallTimer();
					session.setAvchatStatus(0);
					CommonUtils.stopRingtone();
					CommonUtils.stopVibrate(getApplicationContext());
					finish();
				}

				@Override
				public void failCallback(JSONObject jsonObject) {
					Logger.error(TAG,"endAVChatCall failCallback responce = "+jsonObject);
				}
			});
		}
	}

	private void cancelIncomingCall(){
		Logger.error(TAG, "cancelIncomingCall buddyId : "+buddyId);

		if(isAudioOnlyCall) {

			cometChat.cancelAudioChatRequest(buddyId, new Callbacks() {
				@Override
				public void successCallback(JSONObject jsonObject) {
					Logger.error(TAG, "cancelAudioChatRequest successCallback = " + jsonObject.toString());
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
					Logger.error(TAG, "cancelAVChatRequest successCallback = " + jsonObject);
					finish();
				}

				@Override
				public void failCallback(JSONObject jsonObject) {
					Logger.error(TAG, "cancelAVChatRequest failCallback = " + jsonObject);
				}
			});
		}
	}

	/**
	 * Send an accept acknowledgment to the server.
	 */
	private void sendAcceptedAjax() {
		Logger.error(TAG,"Is Audio call ? "+isAudioOnlyCall);
		if(isAudioOnlyCall){
			if(audioCallState != FeatureState.ACCESSIBLE){
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCIncomingCallActivity.this);
				alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).show();
			}else {
				cometChat.acceptAudioChatRequest(buddyId, new Callbacks() {
					@Override
					public void successCallback(JSONObject jsonObject) {
                        /*VideoChat.startVideoCall(roomName, true, !isAudioOnlyCall, CCIncomingCallActivity.this, CCVideoChatActivity.class,
                                buddyId);
                        finish();*/
					}

					@Override
					public void failCallback(JSONObject jsonObject) {
						Logger.error(TAG,"Accept audio ChatRequest fail = "+jsonObject);
					}
				});
				VideoChat.startVideoCall(roomName, true, !isAudioOnlyCall, CCIncomingCallActivity.this, CCVideoChatActivity.class,
						buddyId);
				finish();
			}
		}else{
			if(avCallState != FeatureState.ACCESSIBLE){
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCIncomingCallActivity.this);
				alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).show();
			}else {
				Toast.makeText(incomingCallActivity, "Connecting....", Toast.LENGTH_SHORT).show();
				cometChat.acceptAVChatRequest(buddyId, new Callbacks() {
					@Override
					public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "acceptAVChatRequest successCallback: "+jsonObject );
                        VideoChat.startVideoCall(roomName, true, !isAudioOnlyCall, CCIncomingCallActivity.this, CCVideoChatActivity.class,
                                buddyId);
                        finish();
					}

					@Override
					public void failCallback(JSONObject jsonObject) {
						Logger.error(TAG, "Accept AVChatRequest fail = " + jsonObject);
					}
				});
			}
		}
	}

	@Override
	public void finish() {
		cancelIncomingCallTimer();
		session.setAvchatStatus(0);
		CommonUtils.stopRingtone();
		CommonUtils.stopVibrate(getApplicationContext());
		super.finish();
	}

	private void cancelIncomingCallTimer() {
		if (null != handler && null != incomingTimeOutRunnable) {
			handler.removeCallbacks(incomingTimeOutRunnable);
			handler.removeCallbacksAndMessages(null);
		} else {
			Logger.error("handler is null = " + (handler == null) + "runnable is null ="
					+ (incomingTimeOutRunnable == null));
		}
	}

	@Override
	public void onBackPressed() {
	}
}