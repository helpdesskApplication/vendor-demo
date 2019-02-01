package activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.custom.CustomAlertDialogHelper;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.heartbeats.CCHeartbeat;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.orm.SugarRecord;
import com.inscripts.plugins.ChatroomManager;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatcore.coresdk.MessageSDK;
import cometchat.inscripts.com.readyui.R;
import models.Bot;
import models.Contact;
import models.Conversation;
import models.GroupMessage;
import models.Groups;
import models.OneOnOneMessage;
import models.Status;


public class CCSettingsActivity extends AppCompatActivity implements OnAlertDialogButtonClickListener {

    private static final java.lang.String TAG = CCSettingsActivity.class.getSimpleName();
    private static final int EDIT_USER_NAME = 1;
    private static final int LOGOUT = 2;

    private Toolbar toolbar;
    private RelativeLayout ccContainer;
    private RelativeLayout viewProfile;
    private RelativeLayout viewBots,viewChatSetting,viewNotificationSetting,viewLanguage,viewBlockedUser,viewGames,viewShareApp,viewInviteContact,viewEditGuestname,viewAnnouncements;
    private ImageView ivViewProfile;
    private TextView tvLanguageSubtitle,tvBlockUserSubtitle,tvNotificationSettingSub,tvBots,tvViewProfile,tvChatSettings,tvNotificationsSettings,tvLanguage,tvBlockedUsers,tvGames,tvShareapp,tvInvite,tvAnnouncement;
    private ImageView imgBots,imgChatSetting,imgNotificationSetting,imgSettinglanguage,imgBlockUser,imgSettingGames,imgShareApp,imginvitePhoneContacts,imgAnnouncements;
    private int noBlockedUser;
    private Button btnLogout;
    private int colorPrimary,colorPrimaryDark;
    private CometChat cometChat;
    private FeatureState botsState;
    private FeatureState blockUserState;
    private FeatureState realTimeTranslationState;
    private FeatureState gamesState;
    private FeatureState announcementState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ccsettings);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        ccContainer = (RelativeLayout) findViewById(R.id.cc_settings_container);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();
        initializeFeatureState();
        setupFields();
        setupFieldsTheme();

        this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_MORE)));


        setupFieldListeners();
        setBlockedUserText();
        setNotificationSubTitle();
    }

    private void initializeFeatureState() {
        botsState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.BOTS_ENABLE));
        blockUserState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.BLOCKED_USER_ENABLED));
        realTimeTranslationState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.REAL_TIME_TRANSLATION));
        gamesState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.SINGLE_PLAYER_GAMES_ENABLED));
        announcementState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.ANNOUNCEMENTS_ENABLED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvLanguageSubtitle.setText(PreferenceHelper.get(PreferenceKeys.DataKeys.SELECTED_LANGUAGE_FULL));
        setBlockedUserText();
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
        viewProfile = (RelativeLayout) findViewById(R.id.ll_view_profile);
        viewChatSetting = (RelativeLayout) findViewById(R.id.ll_chat_setting);
        viewNotificationSetting = (RelativeLayout) findViewById(R.id.ll_notification_setting);
        viewLanguage = (RelativeLayout) findViewById(R.id.ll_setting_language);
        viewBlockedUser = (RelativeLayout) findViewById(R.id.ll_block_user);
        viewGames = (RelativeLayout) findViewById(R.id.ll_setting_games);
        viewShareApp = (RelativeLayout) findViewById(R.id.ll_share_app);
        viewInviteContact = (RelativeLayout) findViewById(R.id.ll_invite_contact);
        viewBots = (RelativeLayout) findViewById(R.id.ll_bots);
        viewAnnouncements = (RelativeLayout) findViewById(R.id.ll_announcements);

        ivViewProfile = (ImageView) findViewById(R.id.iv_view_profile);
        imgChatSetting = (ImageView) findViewById(R.id.setting_chat_setting);
        imgNotificationSetting = (ImageView) findViewById(R.id.setting_notification_setting);
        imgSettinglanguage = (ImageView) findViewById(R.id.setting_language);
        imgBlockUser = (ImageView) findViewById(R.id.setting_block_user);
        imgSettingGames = (ImageView) findViewById(R.id.setting_games);
        tvNotificationSettingSub = (TextView) findViewById(R.id.notification_setting_subtitle);
        imgShareApp = (ImageView) findViewById(R.id.img_share_app);
        imginvitePhoneContacts = (ImageView) findViewById(R.id.img_invite_contact);
        imgBots = (ImageView) findViewById(R.id.image_view_bots);
        imgAnnouncements = (ImageView) findViewById(R.id.iv_announcements);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        tvLanguageSubtitle = (TextView) findViewById(R.id.setting_language_subtitle);
        tvBlockUserSubtitle = (TextView) findViewById(R.id.block_user_subtitle);
        tvBots = (TextView) findViewById(R.id.text_view_bots);
        tvViewProfile = (TextView) findViewById(R.id.tv_view_profile);
        tvChatSettings = (TextView) findViewById(R.id.chat_setting_title);
        tvNotificationsSettings = (TextView) findViewById(R.id.notification_setting_title);
        tvLanguage = (TextView) findViewById(R.id.setting_language_title);
        tvBlockedUsers = (TextView) findViewById(R.id.block_user_title);
        tvGames = (TextView) findViewById(R.id.setting_games_title);
        tvShareapp = (TextView) findViewById(R.id.setting_share_title);
        tvInvite = (TextView) findViewById(R.id.textInvitePhoneContact);
        tvAnnouncement = (TextView) findViewById(R.id.tv_announcement);

        if(LocalConfig.isApp){
            btnLogout.setVisibility(View.VISIBLE);
        }else{
            btnLogout.setVisibility(View.GONE);
        }

        Log.e(TAG, "Bot Text: "+(String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BOTS)) );
        if(botsState != FeatureState.INVISIBLE){
            tvBots.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BOTS)));
        }else {
            viewBots.setVisibility(View.GONE);
        }
        if((Boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.SHOW_TICKS)) || (Boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.LAST_SEEN_ENABLED))){
            tvChatSettings.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CHAT_SETTINGS)));
        }else {
            viewChatSetting.setVisibility(View.GONE);
        }
        if(realTimeTranslationState == FeatureState.INVISIBLE){
            viewLanguage.setVisibility(View.GONE);
        }
        if(blockUserState != FeatureState.INVISIBLE){
tvBlockedUsers.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BLOCKED_USERS)));
        }else {
            viewBlockedUser.setVisibility(View.GONE);
        }
        tvViewProfile.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_VIEW_PROFILE)));
        tvNotificationsSettings.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NOTIFICATION_SETTINGS)));
        if(gamesState != FeatureState.INVISIBLE){
            tvGames.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_GAMES)));
        }else {
            viewGames.setVisibility(View.GONE);
        }
        if((Boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.SHARE_APP_ENABLED))){
tvShareapp.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SHARE_APP)));
        }else {
            viewShareApp.setVisibility(View.GONE);
        }
        if((Boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.INVITE_VIA_SMS_ENABLED))){
tvInvite.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_USERS)));
        }else {
            viewInviteContact.setVisibility(View.GONE);
        }
        if(announcementState != FeatureState.INVISIBLE){
            tvAnnouncement.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_ANNOUNCEMENTS)));
        }else {
            viewAnnouncements.setVisibility(View.GONE);
        }
        btnLogout.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGOUT)));
    }

    private void setupFieldsTheme() {
        ivViewProfile.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgBots.getBackground().setColorFilter(colorPrimary,PorterDuff.Mode.SRC_ATOP);
        imgChatSetting.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgNotificationSetting.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgSettinglanguage.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgBlockUser.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgSettingGames.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imginvitePhoneContacts.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgShareApp.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        imgAnnouncements.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        btnLogout.getBackground().setColorFilter(Color.parseColor("#eb5160"), PorterDuff.Mode.SRC_ATOP);
    }


    private void setupFieldListeners() {

        viewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(CCSettingsActivity.this, CCViewProfileActivity.class);
                startActivity(profileIntent);
            }
        });

        viewBots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (botsState == FeatureState.INACCESSIBLE) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSettingsActivity.this);
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else {
                    startActivity(new Intent(CCSettingsActivity.this,CCBotsActivity.class));
                }
            }
        });

        viewChatSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chatSettingIntent = new Intent(CCSettingsActivity.this,CCChatSettingsActivity.class);
                chatSettingIntent.putExtra("isChatSetting",true);
                startActivity(chatSettingIntent);
            }
        });

        viewNotificationSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chatSettingIntent = new Intent(CCSettingsActivity.this,CCChatSettingsActivity.class);
                chatSettingIntent.putExtra("isChatSetting",false);
                startActivity(chatSettingIntent);
            }
        });

        viewLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (realTimeTranslationState == FeatureState.INACCESSIBLE) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSettingsActivity.this);
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else {
                    startActivity(new Intent(CCSettingsActivity.this, CCSelectLanguageActivity.class));
                }
            }
        });

        viewBlockedUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent unblockIntent = new Intent(CCSettingsActivity.this,CCUnblockuserActivity.class);
                startActivity(unblockIntent);
            }
        });

        viewGames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gamesState == FeatureState.INACCESSIBLE) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSettingsActivity.this);
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else {
                    startActivity(new Intent(CCSettingsActivity.this,CCSinglePlayerGameActivity.class));
                }
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkUserConfirmation();
            }
        });

        viewShareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_MESSAGE))==null)) {
                    shareIntent.putExtra(Intent.EXTRA_TEXT, LocalConfig.getDefaultInviteMessage());
                } else {
                    shareIntent.putExtra(Intent.EXTRA_TEXT,(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_MESSAGE) ));
                }
                shareIntent.setType("text/plain");
                startActivity(shareIntent);
            }
        });

        viewInviteContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CCSettingsActivity.this, CCInviteViaSmsActivity.class));
            }
        });

        viewAnnouncements.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (announcementState == FeatureState.INACCESSIBLE) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSettingsActivity.this);
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else {
                    startActivity(new Intent(CCSettingsActivity.this,CCAnnouncementsActivity.class));
                }
            }
        });

    }

    private void checkUserConfirmation() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogview = inflater.inflate(R.layout.cc_custom_dialog, null);
        TextView dialogueTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
        EditText dialogueInput = (EditText) dialogview.findViewById(R.id.edittextDialogueInput);
        dialogueInput.setVisibility(View.GONE);
        dialogueTitle.setVisibility(View.GONE);

//        dialogueTitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGOUT_MESSAGE)));

        new CustomAlertDialogHelper(this, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGOUT_MESSAGE)), dialogview, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGOUT)), "", (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL)), this, LOGOUT,true);
    }

    @Override
    public void onButtonClick(final android.app.AlertDialog alertDialog, View v, int which, int popupId) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            switch (popupId) {
                case LOGOUT:
                    CCHeartbeat.getLaunchCallbackListner().onLogout();
                    alertDialog.dismiss();

                    MessageSDK.closeCometChatWindow(CometChatActivity.cometChatActivity, ccContainer);
                    finish();
                    break;
            }
        }else{
            alertDialog.dismiss();
        }
    }

    private void setBlockedUserText(){
       cometChat.getBlockedUsersCount(new Callbacks() {
           @Override
           public void successCallback(JSONObject jsonObject) {
               Logger.error(TAG,"getBlockedUsersCount(): successCallback: "+jsonObject);
               if(jsonObject.has("count")){
                   try {
                       int getBlockedUsersCount = jsonObject.getInt("count");
                       tvBlockUserSubtitle.setText(getBlockedUsersCount+"");
                   } catch (JSONException e) {
                       e.printStackTrace();
                   }
               }
           }

           @Override
           public void failCallback(JSONObject jsonObject) {
               Logger.error(TAG,"getBlockedUsersCount(): failCallback: "+jsonObject);
           }
       });
    }

    private void setNotificationSubTitle(){
        String notification = PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON);
        String sound = PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_SOUND);
        String vibarate = PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE);

        if(notification != null && !notification.equals("0")){
            if(sound != null && vibarate != null){
                if(sound.equals("1") && vibarate.equals("1")){
                    tvNotificationSettingSub.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SOUND))+" "+(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_AND))+(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_VIBRATE)));
                }else if(vibarate.equals("1")) {
                    tvNotificationSettingSub.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_VIBRATE)));
                }else if(sound.equals("1")){
                    tvNotificationSettingSub.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SOUND)));
                }
            }else {
                tvNotificationSettingSub.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SOUND))+" "+(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_AND))+(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_VIBRATE)));
            }
        }else{
            tvNotificationSettingSub.setText("off");
        }
    }


}
