package videochat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.ProfileRoundedImageView;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.ConnectivityHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.StaticMembers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import Keys.BroadCastReceiverKeys;
import activities.CCInviteAVBroadcastUsers;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import fm.android.conference.webrtc.App;
import helpers.CreditDeductionHelper;
import models.Contact;
import models.Groups;
import rolebase.RolebaseFeatures;

public class CCVideoChatActivity extends AppCompatActivity implements View.OnTouchListener,View.OnClickListener,GestureDetector.OnGestureListener{


    private static final java.lang.String TAG = CCVideoChatActivity.class.getSimpleName();
    private App app;
    private CometChat cometChat;
    private String callid;
    private boolean isGrpConfrence,isBroadCast,iamBroadcaster;
    private RelativeLayout container;
    private ImageButton endCallButton, muteControlToggle, videoOnOffToggle, speakerToggle, inviteUser;
    private boolean video=true, audio=true, isSpeakerOn = true,isAudioOnly;
    private ProgressBar wheel;
    private ProfileRoundedImageView profileView;
    private String buddyID;
    private String grpID;

    private BroadcastReceiver broadcastReceiver;
    private GestureDetector gestureDetector;
    private boolean isScreenshare;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;
    private String initiatorId;
    private Timer timer;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager notificationManager;
    private String name;
    private TextView counterTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.cc_video_chat);
        cometChat = CometChat.getInstance(this);
        processIntentData(getIntent());
        setupFields();
        setFieldListners();
        startCallSession();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.error(TAG, "broadcastReceiver onReceive");
                Bundle extras = intent.getExtras();
                //if (extras.containsKey(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY)) {
                //CCVideoChatActivity.this.finish();
                //}
                long fromID = intent.getLongExtra("from",-1);
                long chatRoomID = intent.getLongExtra("groupID", -1);
                Logger.error(TAG,"From id = "+fromID);
                Logger.error(TAG,"chatRoomID = "+chatRoomID);
                Logger.error(TAG,"grpID = "+grpID);

                Logger.error(TAG,"buddy id = "+buddyID);
                if(fromID!=-1 && buddyID != null &&buddyID.equals(fromID+"")){
                    endCallSession(false);
                }
                Logger.error(TAG,"initiator id: "+initiatorId);
                if(fromID!=-1 && grpID != null &&grpID.equals(chatRoomID+"") && initiatorId != null && initiatorId.equals(fromID+"")){
                    endCallSession(false);
                }
                if (extras.containsKey(BroadCastReceiverKeys.AvchatKeys.CALL_END_FROM_NOTIFICATION)) {
                    finish();
                }

            }
        };
        registerReceiver(broadcastReceiver,
                new IntentFilter(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY));

        Logger.error(TAG,"123456 onCreate called");
        PreferenceHelper.save(BroadCastReceiverKeys.AvchatKeys.CALL_SESSION_ONGOING,1);
        timer = new Timer();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getBaseContext());
    }


    @Override
    protected void onResume() {
        super.onResume();
        cometChat.resumeMediaStreaming(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        cometChat.pauseMediaStreaming(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        PreferenceHelper.save(BroadCastReceiverKeys.AvchatKeys.CALL_SESSION_ONGOING,0);
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    private void setupFields() {
        container = (RelativeLayout) findViewById(R.id.container);
        endCallButton = (ImageButton) findViewById(R.id.buttonEndCall);
        muteControlToggle = (ImageButton) findViewById(R.id.buttonMuteSound);
        videoOnOffToggle = (ImageButton) findViewById(R.id.buttonVideoOnOff);
        speakerToggle = (ImageButton) findViewById(R.id.buttonSpeakerToggle);
        inviteUser = (ImageButton) findViewById(R.id.buttonInviteUser);
        wheel = (ProgressBar) findViewById(R.id.progressWheel);
        profileView = (ProfileRoundedImageView) findViewById(R.id.imageViewAudioOnlyDefaultAvatar);

        if(isBroadCast && !iamBroadcaster){
            muteControlToggle.setVisibility(View.GONE);
            videoOnOffToggle.setVisibility(View.GONE);
            speakerToggle.setVisibility(View.GONE);
            wheel.setVisibility(View.VISIBLE);
        }else if(isBroadCast && iamBroadcaster){
            speakerToggle.setVisibility(View.GONE);
            inviteUser.setVisibility(View.VISIBLE);
        }

        if(isAudioOnly){
            profileView.setVisibility(View.VISIBLE);
            wheel.setVisibility(View.VISIBLE);
            if(!isGrpConfrence) {
                Contact contact = Contact.getContactDetails(buddyID);
                if (contact != null) {
                    LocalStorageFactory.loadImageUsingURL(this, contact.avatarURL, profileView, R.drawable.cc_default_avatar);
                }
            }else {
                profileView.setImageResource(R.drawable.cc_ic_group_grey);
            }

            speakerToggle.setVisibility(View.VISIBLE);
            videoOnOffToggle.setVisibility(View.GONE);

            /* for proximity sensors*/

            try {
                field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            } catch (Throwable ignored) {
            }

            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(field, getLocalClassName());
            if (!isSpeakerOn) {
                if(!wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
            }
        }

        inviteUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //isInviteClicked = true;
                Intent intent = new Intent(getApplicationContext(), CCInviteAVBroadcastUsers.class);
                intent.putExtra(StaticMembers.INTENT_ROOM_NAME, callid);
                CCVideoChatActivity.this.startActivity(intent);
            }
        });
        counterTextView = findViewById(R.id.counter);
        if(!isAudioOnly){
            counterTextView.setVisibility(View.GONE);
        }
    }

    private void setFieldListners(){
        endCallButton.setOnClickListener(this);
        videoOnOffToggle.setOnClickListener(this);
        muteControlToggle.setOnClickListener(this);
        speakerToggle.setOnClickListener(this);


        gestureDetector = new GestureDetector(this, this);

        if(!isAudioOnly){
            gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    cometChat.toggleCamera();
                    return true;
                }

                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    return false;
                }
            });
            container.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetector.onTouchEvent(event);
                }
            });
        }
    }

    public void processIntentData(Intent intent){
        callid = intent.getStringExtra(CometChatKeys.AVchatKeys.ROOM_NAME);
        isGrpConfrence = intent.getBooleanExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG,false);
        isBroadCast = intent.getBooleanExtra(StaticMembers.INTENT_AVBROADCAST_FLAG,false);
        iamBroadcaster = intent.getBooleanExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG,false);
        video = intent.getBooleanExtra(StaticMembers.INTENT_VIDEO_FLAG, true);
        buddyID = intent.getStringExtra("CONTACT_ID");
        Logger.error(TAG,"buddyID: "+buddyID);
        grpID = intent.getStringExtra("GRP_ID");
        isScreenshare = intent.getBooleanExtra(StaticMembers.SCREENSHARE_MODE,false);
        Logger.error(TAG,"grpID = "+grpID);
        isAudioOnly = !video;
        initiatorId = buddyID;
        if(intent.hasExtra("GRP_ID") && grpID != null && !grpID.equals("0")){
            Groups group = Groups.getGroupDetails(grpID);
            name = group.name;
        }else if(buddyID != null && !buddyID.equals("0")){
            Contact contact = Contact.getContactDetails(buddyID);
            name = contact.name;
        }
        /*if (buddyID != null && !buddyID.equals("0")) {
            Contact contact = Contact.getContactDetails(buddyID);
            name = contact.name;
        }else if(grpID != null && !grpID.equals("0")) {
            Groups group = Groups.getGroupDetails(grpID);
            name = group.name;
        }*/
    }


    void startCallSession(){
        Logger.error(TAG,"Callid = "+callid);
        if(isScreenshare){
            cometChat.startScreenShare(this,callid, container, new Callbacks() {
                    @Override
                    public void successCallback(final JSONObject jsonObject) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(jsonObject.get("ConnectionState").equals("Connected")){
                                        wheel.setVisibility(View.GONE);
                                        if(ConnectivityHelper.isPoorConnection(CCVideoChatActivity.this)){
                                            Toast.makeText(CCVideoChatActivity.this, "Poor connection detected it might affect your session.", Toast.LENGTH_SHORT).show();
                                        }
                                    }else if(jsonObject.get("ConnectionState").equals("Failed")){
                                        wheel.setVisibility(View.VISIBLE);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"avbroadcast startAVBroadCast failCallback response : "
                                +jsonObject.toString());
                    }
            });
        } else if(isAudioOnly){
            cometChat.startAudioChatCall(this,callid, container, new Callbacks() {
                @Override
                public void successCallback(final JSONObject jsonObject) {
                    Logger.error(TAG, "successCallback: startAudioChatCall: "+jsonObject );
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                cometChat.switchSpeakers(CCVideoChatActivity.this,false);
                                isSpeakerOn = false;
                                speakerToggle.setBackgroundResource(R.drawable.cc_custom_round_call_phone_selector);

                                if(jsonObject.get("ConnectionState").equals("Connected")){
                                    wheel.setVisibility(View.GONE);
                                    final int[] i = {0};
                                    timer.scheduleAtFixedRate(new TimerTask() {
                                        @Override
                                        public void run() {
                                            final String time;
                                            int seconds = i[0]++;
                                            int minutes = seconds / 60;
                                            seconds     = seconds % 60;
                                            int hours = minutes / 60;
                                            time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    counterTextView.setText(time);
                                                }
                                            });

                                        }
                                    },0,1000);
                                    showCounterNotification(name,true);
                                    if(ConnectivityHelper.isPoorConnection(CCVideoChatActivity.this)){
                                        Toast.makeText(CCVideoChatActivity.this, "Poor connection detected it might affect your session.", Toast.LENGTH_SHORT).show();
                                    }
                                }else if(jsonObject.get("ConnectionState").equals("Failed")){
                                    wheel.setVisibility(View.VISIBLE);
                                }
                                if(jsonObject.get("ConnectionState").equals("Closed")){
                                    cancelCounterNotification();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG, "failCallback successCallback : " + jsonObject.toString());
                }
            });
        }else{
            if(isBroadCast){
                wheel.setVisibility(View.VISIBLE);
                Logger.error(TAG,"callid : "+callid);
                Logger.error(TAG,"iamBroadcaster : "+iamBroadcaster);
                cometChat.startAVBroadCast(this, callid, iamBroadcaster, container, new Callbacks() {
                    @Override
                    public void successCallback(final JSONObject jsonObject) {
//                        if(!iamBroadcaster){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if(jsonObject.get("ConnectionState").equals("Connected")){
                                            wheel.setVisibility(View.GONE);
                                            if(ConnectivityHelper.isPoorConnection(CCVideoChatActivity.this)){
                                                Toast.makeText(CCVideoChatActivity.this, "Poor connection detected it might affect your session.", Toast.LENGTH_SHORT).show();
                                            }
                                        }else if(jsonObject.get("ConnectionState").equals("Failed")){
                                            wheel.setVisibility(View.VISIBLE);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
//                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"avbroadcast startAVBroadCast failCallback response : "
                                +jsonObject.toString());
                    }
                });
            }else{
                Logger.error(TAG,"start AvCall called with confrence ? "+isGrpConfrence);
                if(isGrpConfrence){
                    cometChat.joinConference(this, callid , container, new Callbacks() {
                        @Override
                        public void successCallback(final JSONObject jsonObject) {
                            Logger.error(TAG, "successCallback: joinConference"+jsonObject);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    wheel.setVisibility(View.GONE);
                                    try {
                                        if(jsonObject.get("ConnectionState").equals("Connected")){
                                            showCounterNotification(name,false);
                                            if(ConnectivityHelper.isPoorConnection(CCVideoChatActivity.this)) {
                                                Toast.makeText(CCVideoChatActivity.this, "Poor connection detected it might affect your session.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        if(jsonObject.get("ConnectionState").equals("Closed")){
                                            cancelCounterNotification();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    if(ConnectivityHelper.isPoorConnection(CCVideoChatActivity.this)) {
                                        Toast.makeText(CCVideoChatActivity.this, "Poor connection detected it might affect your session.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                        }
                    });
                }else {
                    cometChat.startAVChatCall(this, callid , container, new Callbacks() {
                        @Override
                        public void successCallback(final JSONObject jsonObject) {
                            Logger.error(TAG, "successCallback: startAVChatCall: "+jsonObject );
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if(jsonObject.get("ConnectionState").equals("Connected")){
                                            wheel.setVisibility(View.GONE);
                                            final int[] i = {0};
                                           /* timer.scheduleAtFixedRate(new TimerTask() {
                                                @Override
                                                public void run() {
                                                    showCounterNotification(false);
                                                }
                                            },0,1000);*/
                                            showCounterNotification(name,false);
                                            if(ConnectivityHelper.isPoorConnection(CCVideoChatActivity.this)){
                                                Toast.makeText(CCVideoChatActivity.this, "Poor connection detected it might affect your session.", Toast.LENGTH_SHORT).show();
                                            }
                                        }else if(jsonObject.get("ConnectionState").equals("Failed")){
                                            wheel.setVisibility(View.VISIBLE);
                                        }
                                        if(jsonObject.get("ConnectionState").equals("Closed")){
                                            cancelCounterNotification();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {

                        }
                    });
                }
            }
        }
    }

    private void cancelCounterNotification() {
        try {
            if (timer != null) {
                timer.cancel();
            }
        } catch (IllegalArgumentException e){
            Logger.error(TAG, "cancelCounterNotification: exception: "+e.getLocalizedMessage());
        }
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(100000);
    }

    private void showCounterNotification(String name, boolean isAudioOnly) {
        String text;
        if(isAudioOnly){
            text = "Ongoing voice call";
        }else {
            text = "Ongoing video call";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "AVCall Channel";
            String channelName = "AVCall Channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
            mBuilder.setContentText(text)
                    .setContentTitle(name)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(isAudioOnly ? R.drawable.ic_phone_call : R.drawable.ic_video_call)
                    .setOngoing(true)
                    .setChannelId(CHANNEL_ID)
                    .setUsesChronometer(true)
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(100000,mBuilder.build());
        }else {
            mBuilder.setContentText(text)
                    .setContentTitle(name)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(isAudioOnly ? R.drawable.ic_phone_call : R.drawable.ic_video_call)
                    .setOngoing(true)
                    .setUsesChronometer(true)
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(100000,mBuilder.build());
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Logger.error(TAG,"gestureDetector = "+gestureDetector);
        if (gestureDetector == null || !gestureDetector.onTouchEvent(event)) {
            return v.onTouchEvent(event);
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.buttonEndCall){
            Logger.error(TAG, "buttonEndCall");
            endCallSession(true);
        }else if(id == R.id.buttonVideoOnOff){
            if(video){
                cometChat.pauseVideo(true);
                v.setBackgroundResource(R.drawable.cc_custom_round_video_button_off_selector);
            }
            else{
                cometChat.pauseVideo(false);
                v.setBackgroundResource(R.drawable.cc_custom_round_video_button_selector);
            }
            video = !video;
        }else if(id == R.id.buttonMuteSound){
            if(audio){
                cometChat.muteAudio(true);
                v.setBackgroundResource(R.drawable.cc_custom_round_audio_button_off_selector);
            }else{
                cometChat.muteAudio(false);
                v.setBackgroundResource(R.drawable.cc_custom_round_audio_button_selector);
            }
            audio = !audio;
        }else if(id == R.id.buttonSpeakerToggle){
            if (isSpeakerOn) {
                cometChat.switchSpeakers(this,false);
                isSpeakerOn = false;
                v.setBackgroundResource(R.drawable.cc_custom_round_call_phone_selector);
                if(wakeLock !=null && !wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
            } else {
                cometChat.switchSpeakers(this,true);
                isSpeakerOn = true;
                v.setBackgroundResource(R.drawable.cc_custom_round_call_speaker_selector);
                if(wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {

    }


    private void endCallSession(boolean self){

        Logger.error(TAG, "endCallSession: self : " + self );
        Logger.error(TAG, "endCallSession: isScreenshare : " + isScreenshare );
        Logger.error(TAG, "endCallSession: isBroadCast : " + isBroadCast );
        Logger.error(TAG, "endCallSession: isGrpConfrence : " + isGrpConfrence );
        if(RolebaseFeatures.isRolebaseEnabled() && PreferenceHelper.contains("IS_INITIATOR")){
            CreditDeductionHelper.stopCreditDeduction();
            PreferenceHelper.removeKey("IS_INITIATOR");
        }
        if(!self){
            cometChat.endCallSession(this);
            finish();
        }
        else if(isScreenshare){
            cometChat.endCallSession(this);
            finish();
        }else if(isBroadCast){
            if(isGrpConfrence){
                cometChat.endBroadcast(iamBroadcaster, grpID, callid,true, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "End broadcast responce isGrpConfrence = "+jsonObject);
                        finish();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "End broadcast responce fail isGrpConfrence = "+jsonObject);
                    }
                });
                finish();
            }else{
                Logger.error(TAG, "endCallSession: endBroadcast bbbb : ");
                cometChat.endBroadcast(iamBroadcaster, buddyID, callid, false,  new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"End broadcast responce = "+jsonObject);
                        finish();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"End broadcast responce fail = "+jsonObject);
                    }
                });
                Logger.error(TAG, "endCallSession: endBroadcast ccccc : ");
                finish();
                Logger.error(TAG, "endCallSession: endBroadcast ddddd : ");
            }
        }else if(isAudioOnly){
            Logger.error(TAG, "end isAudioOnly");
            if(isGrpConfrence){
                Logger.error(TAG,"Groud id = "+grpID);
                finish();
            }else {
                cometChat.endAudioChatCall(buddyID, callid, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"End audio Call responce = "+jsonObject);
                        finish();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"End audio Call fail responce = "+jsonObject);
                    }
                });
                finish();
            }

        }else{
            if(isGrpConfrence){
                finish();
            }else{
                cometChat.endAVChatCall(this,buddyID, callid, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"End Call success "+jsonObject);
                        finish();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"End Call fail "+jsonObject);
                    }
                });
                finish();
            }
        }
    }

    @Override
    public void finish() {
        CommonUtils.stopRingtone();
        CommonUtils.stopVibrate(getApplicationContext());
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        cometChat.endCallSession(this);
        super.finish();
    }


    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}
