package activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.heartbeats.CCHeartbeat;
import com.inscripts.helpers.CCPermissionHelper;
import com.inscripts.helpers.PopupHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.plugins.VideoChat;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Keys.BroadCastReceiverKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatcore.coresdk.MessageSDK;
import cometchat.inscripts.com.readyui.CCBroadcastMessageActivity;
import cometchat.inscripts.com.readyui.R;
import customsviews.BadgeView;
import fragments.ContactFragment;
import fragments.GroupFragment;
import fragments.RecentFragment;
import helpers.CCAnalyticsHelper;
import helpers.CCSubcribe;
import helpers.ContactHelper;
import helpers.CreditDeductionHelper;
import helpers.NotificationDataHelper;
import models.Contact;
import models.Conversation;
import models.Groups;
import services.AddFriendsService;
import videochat.CCIncomingCallActivity;
import java.util.HashMap;
import java.util.HashSet;

public class CometChatActivity extends AppCompatActivity implements OnAlertDialogButtonClickListener,RecentFragment.LongPressed {

    private static final java.lang.String TAG = CometChatActivity.class.getSimpleName();
    private ViewPager mViewPager;
    private RelativeLayout ccContainer,adRelativeView;
    private boolean isfull = true;
    private int colorPrimary, colorPrimaryDark;
    private Toolbar toolbar;
    private TabLayout tabs;
    private CCSettingMapper settingMapper;
    public static CometChatActivity cometChatActivity;
    private Boolean exit = false;

    private boolean isGroup;

    private String chatroomName;

    private CometChat cometChat;

    private ViewPagerAdapter adapter;

    private BroadcastReceiver customReceiver;

    private boolean recentEnabled;
    private boolean contactsEnabled;
    private FeatureState groupsEnabled,createGroup,createInviteGroup,createPublicGroup,createProtectedGroup,createPrivateGroup,inviteUsersToGroup;

    private int recentTabIndex, contactsTabIndex, groupsTabIndex;
    public final String DATA = "com.parse.Data";
    boolean isBound = false;
    private boolean setBackButton;
    private View dialogView;
    private CreditDeductionHelper cdHelper;
    private FeatureState groupState,broadcastMessageState;
    private Gson gson;
    private HashSet<String> phoneNumberSet = new HashSet<>();
    private String commaSeperatedContactsString;
    private Menu menu;
    private HashSet<Integer> longPressedContactIds;
    private HashSet<Integer> longPressedGroupIds;
    @Override
    protected void onStop() {
        super.onStop();
        cdHelper.stopCreditDeduction();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comet_chat);
        PreferenceHelper.initialize(this);
//        FlurryAgent.logEvent("CometchatActivity");
        CCAnalyticsHelper.logFeatureEvent("CometChatActivity");
        if(PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN)
                && PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN).equals("1")) {

            Logger.error(TAG, "LOGGED_IN : " + PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN));

            cometChatActivity = this;
            cometChat = CometChat.getInstance(this);
            handleIntent(cometChatActivity, getIntent());
            String groupUserId = getIntent().getStringExtra("group_user_id");
            checkUserGroupRedirection(groupUserId);

            setupFields();
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(getString(R.string.app_name));
            Logger.error(TAG, "Title: " + this.getTitle());
            if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
                ccContainer = (RelativeLayout) findViewById(R.id.cc_container);
                CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationIcon(R.drawable.cc_ic_action_cancel);
            }
            setupTabs();
            setCCTheme();
            CCSubcribe.getInstance(this).SubcribeToCometChat();
            initializeSessionData();
            initializeFeatureStates();
            customReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Logger.error(TAG, "customReceiver");
                    //Bundle extras = intent.getExtras();
                    //if(extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY)){
                    //updateRecentListBadge();
                    //}
                    refreshFragments();
                }
            };

            registerReceiver(customReceiver,
                    new IntentFilter(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST));

            String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE};
            CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_PHONE_STORAGE);

            cometChat.sendLaunchSuccess();
            PreferenceHelper.save(BroadCastReceiverKeys.AvchatKeys.CALL_SESSION_ONGOING, 0);
        } else {
            Toast.makeText(getApplicationContext(), "Please login to continue.", Toast.LENGTH_SHORT).show();
            MessageSDK.closeCometChatWindow(this, ccContainer);
        }
        Logger.error(TAG, "onCreate: "+System.currentTimeMillis() );

        if (((boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.LOGIN_SETTINGS, SettingSubType.IS_PHONE_NUMBER_LOGIN)))) {
            if (CCPermissionHelper.hasPermissions(getBaseContext(),CCPermissionHelper.REQUEST_PERMISSION_READ_CONTACTS)) {
                commaSeperatedContactsString = "" + new ContactHelper(this).getContactList();
                commaSeperatedContactsString = commaSeperatedContactsString.substring(1, commaSeperatedContactsString.length() - 1);
                Logger.error(TAG, "onCreate: contacts: "+commaSeperatedContactsString );
                Intent serviceIntent = new Intent();
                serviceIntent.putExtra("CONTACT_LIST", commaSeperatedContactsString);
                serviceIntent.putExtra("UID", PreferenceHelper.get("USERNAME"));
                AddFriendsService.enqueWork(this,serviceIntent);
            } else {
                CCPermissionHelper.requestPermissions(CometChatActivity.this,new String[]{CCPermissionHelper.REQUEST_PERMISSION_READ_CONTACTS},CCPermissionHelper.PERMISSION_READ_CONTACTS);
            }
        }
    }

    private void initializeFeatureStates() {
        groupState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GROUP_CHAT_ENABLED));
        broadcastMessageState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.BROADCAST_MESSAGE_ENABLED));
        createGroup = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CREATE_GROUPS_ENABLED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (customReceiver !=null) {
            unregisterReceiver(customReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshFragments();
    }

    private void refreshFragments() {
        try {

            Fragment recentFragment = adapter.getItem(recentTabIndex);
            if (recentFragment != null && recentFragment instanceof RecentFragment
                    && recentFragment.isAdded()) {
                ((RecentFragment)recentFragment).refreshFragment();
            }

            Fragment contactFragment = adapter.getItem(contactsTabIndex);
            if (contactFragment != null && contactFragment instanceof ContactFragment
                    && contactFragment.isAdded()) {
                ((ContactFragment)contactFragment).refreshFragment();
            }

            Fragment groupFragment = adapter.getItem(groupsTabIndex);
            if (groupFragment != null && groupFragment instanceof GroupFragment
                    && groupFragment.isAdded()) {
                ((GroupFragment)groupFragment).refreshFragment();
            }

            updateRecentListBadge();
        }catch(Exception e){
            e.printStackTrace();
        }
        updateRecentListBadge();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CCPermissionHelper.PERMISSION_PHONE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "PERMISSION NOT GRANTED.. SOME OF THE FEATURES MIGHT NOT WORK", Toast.LENGTH_SHORT).show();
                }
                break;
            case CCPermissionHelper.PERMISSION_READ_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission for reading contacts is not granted", Toast.LENGTH_SHORT).show();
                }
        }
    }

    /*private void handleIntent() {

    }*/

    public void handleIntent(final Context context, Intent mainIntent) {
        int id = 0;
        String groupUserId = mainIntent.getStringExtra("group_user_id");
        isGroup = mainIntent.getBooleanExtra("is_group", false);
        setBackButton = mainIntent.getBooleanExtra("set_back_button_enable",true);
        Logger.error(TAG,"closeWindowEnable ? "+setBackButton);
        if(mainIntent.hasExtra("notificationId")){
            id = mainIntent.getIntExtra("notificationId", 0);
            NotificationDataHelper.deleteFromMap(id);
        }
        try {
            PreferenceHelper.initialize(this);
            if (PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN)
                    && "1".equals(PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN))) {
                String action = mainIntent.getAction();

                Logger.error(TAG, "ACTION_SEND action : " + action);

                NotificationManager notificationManager = (NotificationManager) PreferenceHelper.getContext().getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    Logger.error(TAG, "handleIntent: notificationId: "+id);
                    notificationManager.cancel(id);
                }
                PreferenceHelper.removeKey(PreferenceKeys.DataKeys.NOTIFICATION_STACK);

                if (mainIntent.hasExtra("isFirebaseNotification")) {

                    String messgeStr = mainIntent.getStringExtra("m");
                    JSONObject message = new JSONObject(messgeStr);
                    long buddyId = message.getLong("fid");
                    String notificationMessage = message.getString("m");

                    Long time = message.getLong("sent") * 1000;

                    if (message.has("cid")) {
                        long chatroomId;
                        if (message.has("cid")) {
                            chatroomId = Long.parseLong("cid");
                        } else {
                            chatroomId = Long.parseLong(PreferenceHelper.get(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID));
                        }
                        Pattern pattern = Pattern.compile("@(.*?):");
                        Matcher matcher = pattern.matcher(notificationMessage);
                        matcher.find();
                        notificationMessage = matcher.group(1);

                        Groups chatroom = Groups.getGroupDetails(chatroomId);
                        if (null != chatroom) {
                            chatroom.unreadCount = 0;
                            chatroom.save();

                            Intent chatroomIntent = new Intent(context, CCGroupChatActivity.class);
                            chatroomIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
                            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, notificationMessage);
                            context.startActivity(chatroomIntent);

                            Intent intent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.CHATROOM_HEARTBEAT_UPDATAION);
                            intent.putExtra(
                                    BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_FULL_CHATROOM_LIST_FRAGMENT, 1);
                            context.sendBroadcast(intent);

                            if(!setBackButton)
                                finish();
                        } else {
                            // Chatroom doesn't exist locally.
                            Logger.error("chatroom is not present in our db");
                            CCHeartbeat.getInstance().setForceHeartbeat(context);
                        }
                    } else {
                        String messageType = mainIntent.getStringExtra("t");
                        if (messageType.equals("O_AC")) {
                            if ((System.currentTimeMillis() - time) < 60000) {
                                String roomName = mainIntent.getStringExtra("grp");
                                Intent avChatIntent = new Intent(context, CCIncomingCallActivity.class);
                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, buddyId);
                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomName);
                                avChatIntent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, true);
                                PreferenceHelper.save("FCMBuddyID", buddyId);
                                startActivity(avChatIntent);
                            }

                        } else if (messageType.equals("O_AVC")) {

                            if ((System.currentTimeMillis() - time) < 60000) {
                                String roomName = mainIntent.getStringExtra("grp");
                                Intent avChatIntent = new Intent(context, CCIncomingCallActivity.class);
                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, buddyId);
                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomName);
                                PreferenceHelper.save("FCMBuddyID", buddyId);
                                startActivity(avChatIntent);
                            }
                        } else {
                            Contact buddy = Contact.getContactDetails(buddyId);
                            buddy.unreadCount = 0;
                            buddy.save();

                            int colonPosition = notificationMessage.indexOf(":");
                            notificationMessage = notificationMessage.substring(0, colonPosition);
                            Logger.error(TAG,"launch Intent 1 called");
                            Intent singleChatIntent = new Intent(context, CCSingleChatActivity.class);
                            singleChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.BUDDY_ID, buddyId);
                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.BUDDY_NAME, notificationMessage);
                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
                            context.startActivity(singleChatIntent);

                            Intent intent = new Intent(
                                    BroadCastReceiverKeys.HeartbeatKeys.ONE_ON_ONE_HEARTBEAT_NOTIFICATION);
                            intent.putExtra(BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_BUDDY_LIST_FRAGMENT, 1);
                            context.sendBroadcast(intent);

                            Intent iintent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.ANNOUNCEMENT_BADGE_UPDATION);
                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                            context.sendBroadcast(iintent);
                            SessionData.getInstance().setChatbadgeMissed(true);
                        }
                    }
                }
                if (mainIntent.hasExtra(DATA)) {
                    handleNotificationData(context, mainIntent);
                } else if (Intent.ACTION_SEND.equals(action)) {
                    Logger.error(TAG, "ACTION_SEND action : " + action);
                    Bundle extras = mainIntent.getExtras();
                    String type = mainIntent.getType();
                    Logger.error(TAG, "ACTION_SEND type " + type);
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
                        Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
                        Logger.error(TAG, "ACTION_SEND uri " + uri);
                        if (!type.isEmpty() && CommonUtils.checkImageType(type)) {
                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_IMAGE_URL, String.valueOf(uri));
                        } else if (!type.isEmpty() && CommonUtils.checkVideoType(type)) {
                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_VIDEO_URL, String.valueOf(uri));
                        } else if (!type.isEmpty() && CommonUtils.checkAudioType(type)) {
                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_AUDIO_URL, String.valueOf(uri).replace("file://", ""));
                        } else if (!type.isEmpty() && (CommonUtils.checkApplicationType(type) || CommonUtils.checkTextType(type))) {
                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_FILE_URL, String.valueOf(uri));
                        } else {
                            String last = uri.getLastPathSegment();
                            if (!CommonUtils.setFileType(last, uri)) {
                                    Toast.makeText(getApplicationContext(), (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_FILE_NOT_SUPPORTED)), Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_FILE_NOT_SUPPORTED)), Toast.LENGTH_LONG).show();
                    }
                    mainIntent.removeExtra(Intent.EXTRA_STREAM);
                } else{
                    Logger.error(TAG, "ACTION_SEND no action");
                }
            } else {
                Logger.error(TAG, "User is not logged in");
                Logger.error(TAG, "ACTION_SEND User is not logged in");
                finish();
            }
        } catch (Exception e) {
            Logger.error(TAG, "ACTION_SEND e : " + e.toString());
            e.printStackTrace();
        }
    }

    private void handleNotificationData(final Context context, Intent mainIntent) throws JSONException {
        Logger.error(TAG, "ACTION_SEND mainIntent.hasExtra(DATA)");

        JSONObject json = new JSONObject(mainIntent.getStringExtra(DATA));
        String messgeStr = json.getString("m");
        JSONObject message = new JSONObject(messgeStr);

        final String[] notificationMessage = {message.getString("m")};

        if (json.has("isCR")) {
            openGroup(context, message, message.getString("m"));
        } else if (json.has("isANN")) {
            Intent announcementIntent = new Intent(context, CCAnnouncementsActivity.class);
            announcementIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(announcementIntent);
        } else {
        /*
         * Directly show Incoming call screen if it is an av
         * chat request.
         */
            final long buddyId = message.getLong("fid");
            if (json.has("avchat")
                    && json.getString("avchat").equals("1")) {
                String roomName = VideoChat.getAVRoomName(message.getString("m"),
                        false);

                Intent avChatIntent = new Intent(context, CCIncomingCallActivity.class);
                avChatIntent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, buddyId);
                avChatIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomName);
                context.startActivity(avChatIntent);
            } else {

                //Logger.error("normal message");

                final Contact buddy = Contact.getContactDetails(buddyId);

                if(buddy!= null){
                    buddy.unreadCount = 0;
                    buddy.save();

                    Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(buddyId));
                    if (0L != conversation.unreadCount) {
                        conversation.unreadCount = 0;
                        conversation.save();
                    }

                    int colonPosition = notificationMessage[0].indexOf(":");

                                notificationMessage[0] = notificationMessage[0].substring(0, colonPosition);
                                Logger.error(TAG,"launch Intent 2 called");
                                Intent singleChatIntent = new Intent(context, CCSingleChatActivity.class);
                                singleChatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, buddyId);
                                singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, CommonUtils.ucWords(notificationMessage[0]));
                                singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
                                startActivity(singleChatIntent);

                    Intent intent = new Intent(
                            BroadCastReceiverKeys.HeartbeatKeys.ONE_ON_ONE_HEARTBEAT_NOTIFICATION);
                    intent.putExtra(BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_BUDDY_LIST_FRAGMENT, 1);
                    context.sendBroadcast(intent);

                    Intent iintent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.ANNOUNCEMENT_BADGE_UPDATION);
                    iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                    context.sendBroadcast(iintent);
                    SessionData.getInstance().setChatbadgeMissed(true);
                    if (!setBackButton)
                        finish();
                } else {
                    cometChat.getUserInfo(String.valueOf(buddyId), new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Contact contactnew = Contact.insertNewBuddy(jsonObject);
                            contactnew.unreadCount = 0;
                            contactnew.save();

                            Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(buddyId));

                            if (conversation!= null && 0L != conversation.unreadCount) {
                                conversation.unreadCount = 0;
                                conversation.save();
                            }

                            int colonPosition = notificationMessage[0].indexOf(":");

                            notificationMessage[0] = notificationMessage[0].substring(0, colonPosition);
                            Logger.error(TAG,"launch Intent 3 called");
                            Intent singleChatIntent = new Intent(context, CCSingleChatActivity.class);
                            singleChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, buddyId);
                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, notificationMessage[0]);
                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
                            context.startActivity(singleChatIntent);

                            Intent intent = new Intent(
                                    BroadCastReceiverKeys.HeartbeatKeys.ONE_ON_ONE_HEARTBEAT_NOTIFICATION);
                            intent.putExtra(BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_BUDDY_LIST_FRAGMENT, 1);
                            context.sendBroadcast(intent);

                                        Intent iintent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.ANNOUNCEMENT_BADGE_UPDATION);
                                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                        context.sendBroadcast(iintent);
                                        SessionData.getInstance().setChatbadgeMissed(true);
                            if (!setBackButton)
                                finish();
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "User info fail = " + jsonObject);
                        }
                    });
                }
            }
        }
    }

    private void openGroup(Context context, JSONObject message, String notificationMessage) throws JSONException {
        long chatroomId;
        if (message.has("cid")) {
            chatroomId = Long.parseLong(message.getString("cid"));
        } else {
            chatroomId = Long.parseLong(PreferenceHelper.get(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID));
        }
        Pattern pattern = Pattern.compile("@(.*?):");
        Matcher matcher = pattern.matcher(notificationMessage);
        matcher.find();
        notificationMessage = matcher.group(1);

        Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(chatroomId));
        if (conversation != null) {
            conversation.unreadCount = 0;
            conversation.save();
        }

        Groups chatroom = Groups.getGroupDetails(chatroomId);
        if (null != chatroom) {
            chatroom.unreadCount = 0;
            chatroom.save();

            Intent chatroomIntent = new Intent(context, CCGroupChatActivity.class);
            chatroomIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_ID, String.valueOf(chatroomId));
            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, notificationMessage);
            context.startActivity(chatroomIntent);

            Intent intent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.CHATROOM_HEARTBEAT_UPDATAION);
            intent.putExtra(BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_FULL_CHATROOM_LIST_FRAGMENT, 1);
            context.sendBroadcast(intent);

            if(!setBackButton)
                finish();
        } else {
            // Chatroom doesn't exist locally.
            Logger.error("chatroom is not present in our db");
            CCHeartbeat.getInstance().setForceHeartbeat(context);
        }
    }

    private void checkUserGroupRedirection(String groupUserId) {
        Logger.error(TAG, "checkUserGroupRedirection : called");
        if (groupUserId != null) {
            if (!isGroup) {
                openUserChat(groupUserId);
            } else if (isGroup) {
                Groups groups = Groups.getGroupDetails(groupUserId);
                chatroomName = groups.name;
                openGroupChat(groupUserId);
            }
            if(!setBackButton)
                finish();
        }
    }


    private void openUserChat(final String groupUserId) {
        Contact contact = Contact.getContactDetails(groupUserId);
        if(contact!=null) {
            Logger.error(TAG, "launch Intent 4 called");
            Intent intent = new Intent(this, CCSingleChatActivity.class);
            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, Long.valueOf(groupUserId));
            if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL).isEmpty()) {
                intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
            }
            if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL).isEmpty()) {
                intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
            }
            if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL).isEmpty()) {
                intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
            }
            if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL).isEmpty()) {
                intent.putExtra("FileUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL));
            }
            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, contact.name);
            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
            SessionData.getInstance().setTopFragment(StaticMembers.TOP_FRAGMENT_ONE_ON_ONE);
            startActivity(intent);
        }else{
            cometChat.getUserInfo(groupUserId, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Contact contactnew = Contact.insertNewBuddy(jsonObject);
                    contactnew.unreadCount = 0;
                    contactnew.save();

                    Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(groupUserId));

                    if (conversation!= null && 0L != conversation.unreadCount) {
                        conversation.unreadCount = 0;
                        conversation.save();
                    }
                    Intent intent = new Intent(CometChatActivity.this, CCSingleChatActivity.class);
                    intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, Long.valueOf(groupUserId));
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL).isEmpty()) {
                        intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
                    }
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL).isEmpty()) {
                        intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
                    }
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL).isEmpty()) {
                        intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
                    }
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL).isEmpty()) {
                        intent.putExtra("FileUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL));
                    }
                    intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, contactnew.name);
                    intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
                    SessionData.getInstance().setTopFragment(StaticMembers.TOP_FRAGMENT_ONE_ON_ONE);
                    startActivity(intent);
                }

                @Override
                public void failCallback(JSONObject jsonObject) {

                }
            });
        }
    }

    public void openGroupChat(final String groupUserId) {
        Groups group = Groups.getGroupDetails(groupUserId);
        if(group!=null) {
            Intent intent = new Intent(CometChatActivity.this, CCGroupChatActivity.class);
            intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, groupUserId);
            intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, group.name);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_IMAGE_URL)) {
                intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
            }
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_VIDEO_URL)) {
                intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
            }
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_AUDIO_URL)) {
                intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
            }
            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
            CometChatActivity.this.startActivity(intent);
        }else{
            cometChat.getGroupInfo(groupUserId, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Groups newGroup = Groups.insertNewGroup(jsonObject);
                    Intent intent = new Intent(CometChatActivity.this, CCGroupChatActivity.class);
                    intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, groupUserId);
                    intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, newGroup.name);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_IMAGE_URL)) {
                        intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
                    }
                    if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_VIDEO_URL)) {
                        intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
                    }
                    if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_AUDIO_URL)) {
                        intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
                    }
                    intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED, setBackButton);
                    CometChatActivity.this.startActivity(intent);
                }

                @Override
                public void failCallback(JSONObject jsonObject) {

                }
            });
        }

    }

    private void setupFields() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        tabs = (TabLayout) findViewById(R.id.tabs);
        mViewPager = (ViewPager) findViewById(R.id.container);
        adRelativeView = (RelativeLayout) findViewById(R.id.cc_rel_adView);
        if(LocalConfig.isApp && !TextUtils.isEmpty((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.AD_UNIT_ID)))){
            Logger.error(TAG,"AD_UNIT_ID : "+(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.AD_UNIT_ID)));
            adRelativeView.setVisibility(View.VISIBLE);
            AdView mAdView = new AdView(getBaseContext());
            mAdView.setAdSize(AdSize.BANNER);
            mAdView.setAdUnitId((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.AD_UNIT_ID)));
            ((RelativeLayout)adRelativeView).addView(mAdView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }else adRelativeView.setVisibility(View.GONE);

        dialogView = View.inflate(getBaseContext(), R.layout.cc_custom_dialog, null);
    }

    private void setupTabs() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());

        String recentTabText = cometChat.getCCSetting(
                new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_RECENT_TAB)).toString();
        String contactTabText = cometChat.getCCSetting(
                new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CONTACT_TAB)).toString();
        String groupTabText = cometChat.getCCSetting(
                new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_GROUP_TAB)).toString();

        recentEnabled = (boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,
                SettingSubType.RECENT_CHAT_ENABLED));
        contactsEnabled = (boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,
                SettingSubType.SINGLE_CHAT_ENABLED));
        groupsEnabled = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,
                SettingSubType.GROUP_CHAT_ENABLED));

        if (recentEnabled) {
             adapter.addFragment(new RecentFragment(), recentTabText);
        }

        if (contactsEnabled) {
            adapter.addFragment(new ContactFragment(), contactTabText);
        }

        if (groupsEnabled != FeatureState.INVISIBLE) {
            adapter.addFragment(new GroupFragment(), groupTabText);
        }

        mViewPager.setAdapter(adapter);
        tabs.setupWithViewPager(mViewPager);

        for(int i=0; i < tabs.getTabCount(); i++) {
            String title = (String) adapter.getPageTitle(i);
            Logger.error(TAG, "Tab title : " + title + " Tab index : " + i);

            if(title.equals(recentTabText)){
                recentTabIndex = i;
            } else if(title.equals(contactTabText)) {
                contactsTabIndex = i;
            } else if(title.equals(groupTabText)) {
                groupsTabIndex = i;
            }
        }

        if(recentEnabled) {
            LinearLayout viewRecent = (LinearLayout) LayoutInflater
                    .from(CometChatActivity.this).inflate(R.layout.custom_tab, null);
            TextView tvRecent = (TextView) viewRecent.findViewById(R.id.cc_tab_title);
            tvRecent.setTextColor(tabs.getTabTextColors());
            BadgeView badgeViewRecent = (BadgeView) viewRecent.findViewById(R.id.cc_batch_view);
            badgeViewRecent.setVisibility(View.GONE);
            tvRecent.setText(recentTabText);
            tabs.getTabAt(recentTabIndex).setCustomView(viewRecent);
        }

        if(contactsEnabled) {
            LinearLayout viewContacts = (LinearLayout) LayoutInflater
                    .from(CometChatActivity.this).inflate(R.layout.custom_tab, null);
            TextView tvContacts = (TextView) viewContacts.findViewById(R.id.cc_tab_title);
            tvContacts.setTextColor(tabs.getTabTextColors());
            BadgeView badgeViewRecent = (BadgeView) viewContacts.findViewById(R.id.cc_batch_view);
            badgeViewRecent.setVisibility(View.GONE);
            tvContacts.setText(contactTabText);
            tabs.getTabAt(contactsTabIndex).setCustomView(viewContacts);
        }

        if(groupsEnabled != FeatureState.INVISIBLE) {
            LinearLayout viewGroups = (LinearLayout) LayoutInflater
                    .from(CometChatActivity.this).inflate(R.layout.custom_tab, null);
            TextView tvGroups = (TextView) viewGroups.findViewById(R.id.cc_tab_title);
            tvGroups.setTextColor(tabs.getTabTextColors());
            BadgeView badgeViewRecent = (BadgeView) viewGroups.findViewById(R.id.cc_batch_view);
            badgeViewRecent.setVisibility(View.GONE);
            tvGroups.setText(groupTabText);
            tabs.getTabAt(groupsTabIndex).setCustomView(viewGroups);
        }

        if(contactsEnabled && !Conversation.isConverSationAvailable()){
          mViewPager.setCurrentItem(contactsTabIndex);
        }

        updateRecentListBadge();
    }

    private void setCCTheme() {
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        tabs.setBackgroundColor(colorPrimary);
        CCUIHelper.setStatusBarColor(this, colorPrimaryDark);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_cometchat, menu);
        if (groupState == FeatureState.INVISIBLE && broadcastMessageState == FeatureState.INVISIBLE){
           menu.findItem(R.id.custom_compose).setVisible(false);
        }
        if ((longPressedContactIds != null && longPressedContactIds.size() > 0) || (longPressedGroupIds != null && longPressedGroupIds.size() > 0)) {
            menu.findItem(R.id.delete_conversation).setVisible(true);
        }else {
            menu.findItem(R.id.delete_conversation).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            MessageSDK.closeCometChatWindow(this, ccContainer);
            cometChat. sendCloseCCWindowResponce();
        } else if (id == R.id.custom_action_search) {
            return true;
        } else if (id == R.id.custom_compose) {
            showCustomActionBarPopup(findViewById(R.id.custom_compose));
        } else if (id == R.id.custom_setting) {
            startActivity(new Intent(CometChatActivity.this, CCSettingsActivity.class));
        }else if (id == R.id.delete_conversation){
            if (longPressedContactIds != null) {
                Logger.error(TAG, "onOptionsItemSelected: contact ids: "+longPressedContactIds.toString() );
                Iterator value = longPressedContactIds.iterator();
                while (value.hasNext()){
                    Conversation.deleteConversationByBuddyId(value.next()+"");
                }
            }
            if (longPressedGroupIds != null) {
                Logger.error(TAG, "onOptionsItemSelected: group id: "+longPressedGroupIds.toString() );
                Iterator value = longPressedGroupIds.iterator();
                while (value.hasNext()){
                    Conversation.deleteConversationByGroupID(value.next()+"");
                }
            }
            Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
            sendBroadcast(iintent);
            menu.findItem(R.id.delete_conversation).setVisible(false);
            longPressedGroupIds = null;
            longPressedContactIds = null;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(LocalConfig.isApp){
            if (exit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    finishAffinity();
                }else{
                    System.exit(0);
                }
            } else {
                Toast.makeText(this, "Press Back again to Exit.",
                        Toast.LENGTH_SHORT).show();
                exit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = false;
                    }
                }, 3 * 1000);
            }
        }else{
            MessageSDK.closeCometChatWindow(this, ccContainer);
            cometChat.sendCloseCCWindowResponce();
        }
    }

    private void showCustomActionBarPopup(View view) {
        final PopupWindow showPopup = PopupHelper.newBasicPopupWindow(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.cc_custom_main_compose_action_bar_menu, null);
        showPopup.setContentView(popupView);

        RelativeLayout newGroup = (RelativeLayout) popupView.findViewById(R.id.ll_new_group);
        RelativeLayout newBroadcast = (RelativeLayout) popupView.findViewById(R.id.ll_new_broadcast);

        ImageView newGroupmageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_new_group);
        ImageView newBroadcastImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_new_broadcast);

        newGroupmageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        newBroadcastImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

        TextView tvNewGroup = (TextView) popupView.findViewById(R.id.tv_new_group);
        TextView tvNewBroadcast = (TextView) popupView.findViewById(R.id.tv_new_broadcast);

        tvNewGroup.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CREATE_NEW_GROUP)).toString());
        tvNewBroadcast.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_BROADCAST_MESSAGE)).toString());

        Logger.error(TAG,"groups state: "+createGroup.name());
        if (createGroup == FeatureState.INVISIBLE) {
            newGroup.setVisibility(View.GONE);
        } else {
            newGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup.dismiss();
                    if(createGroup == FeatureState.ACCESSIBLE){
                        Intent intent = new Intent(CometChatActivity.this, CCCreateChatroomActivity.class);
                        startActivity(intent);
                    }else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CometChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    }

                }
            });
        }
        Logger.error(TAG,"broadcast message state: "+broadcastMessageState.name());
        if (broadcastMessageState == FeatureState.INVISIBLE) {
            newBroadcast.setVisibility(View.GONE);
        } else {
            newBroadcast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup.dismiss();
                    if(broadcastMessageState == FeatureState.ACCESSIBLE){
                        Intent intent = new Intent(CometChatActivity.this, CCBroadcastMessageActivity.class);
                        startActivity(intent);
                    }else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CometChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    }
                }
            });
        }
        showPopup.setWidth(Toolbar.LayoutParams.WRAP_CONTENT);
        showPopup.setHeight(Toolbar.LayoutParams.WRAP_CONTENT);
        showPopup.setAnimationStyle(R.style.Animations_GrowFromTop);
        showPopup.showAsDropDown(view);
    }

    @Override
    public void onLongPressed(int id , boolean isGroup) {
        if(longPressedContactIds == null){
            if (!isGroup) {
                longPressedContactIds = new HashSet<>();
                longPressedContactIds.add(id);
            }
        }else {
            if (!isGroup) {
                if (longPressedContactIds.contains(id)) {
                    longPressedContactIds.remove(id);
                }else {
                    longPressedContactIds.add(id);
                }
            }
        }
        if(longPressedGroupIds == null){
            if (isGroup) {
                longPressedGroupIds = new HashSet<>();
                longPressedGroupIds.add(id);
            }
        }else {
            if (isGroup) {
                if (longPressedGroupIds.contains(id)) {
                    longPressedGroupIds.remove(id);
                }else {
                    longPressedGroupIds.add(id);
                }
            }
        }

        if ((longPressedContactIds != null && longPressedContactIds.size() > 0) || (longPressedGroupIds != null && longPressedGroupIds.size() > 0)) {
            menu.findItem(R.id.delete_conversation).setVisible(true);
        }else {
            menu.findItem(R.id.delete_conversation).setVisible(false);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }
    private void initializeSessionData() {
        if (!PreferenceHelper.contains(PreferenceKeys.UserKeys.NOTIFICATION_ON)) {
            PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_ON, "1");
        }
        if (!PreferenceHelper.contains(PreferenceKeys.UserKeys.READ_TICK)) {
            PreferenceHelper.save(PreferenceKeys.UserKeys.READ_TICK, "1");
        }
        if (!PreferenceHelper.contains(PreferenceKeys.UserKeys.LAST_SEEN_SETTING)) {
            PreferenceHelper.save(PreferenceKeys.UserKeys.LAST_SEEN_SETTING, "1");
        }
        if (!PreferenceHelper.contains(PreferenceKeys.UserKeys.TYPING_SETTING)) {
            PreferenceHelper.save(PreferenceKeys.UserKeys.TYPING_SETTING, "1");
        }
        if (!PreferenceHelper.contains(PreferenceKeys.UserKeys.NOTIFICATION_SOUND)) {
            PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_SOUND, "1");
        }

        if (!PreferenceHelper.contains(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE)) {
            PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE, "1");
        }
    }

    private void updateRecentListBadge() {

        Logger.error(TAG, "Conversation unread count : " + Conversation.getUnreadConversationCount());
        Logger.error(TAG, "Contact unread count : " + Contact.getUnreadContactsCount());
        Logger.error(TAG, "Group unread count : " + Groups.getUnreadGroupsCount());

        if (recentEnabled) {

            SessionData.getInstance().setChatbadgeMissed(false);
            try {

                String badgeCount = String.valueOf(Conversation.getUnreadConversationCount());
                Logger.error(TAG, "Conversations badge count = " + badgeCount);
                TabLayout.Tab tab = tabs.getTabAt(recentTabIndex);
                LinearLayout view = (LinearLayout) tab.getCustomView();
                BadgeView bg = (BadgeView) view.findViewById(R.id.cc_batch_view);
                if (Integer.parseInt(badgeCount) > 0) {
                    bg.setVisibility(View.VISIBLE);
                    bg.setText(badgeCount);
                } else {
                    bg.setVisibility(View.GONE);
                }

                tab.setCustomView(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {

            if(contactsEnabled) {
                String badgeCount = String.valueOf(Contact.getUnreadContactsCount());
                Logger.error(TAG, "Contacts badge count = " + badgeCount);
                TabLayout.Tab tab = tabs.getTabAt(contactsTabIndex);
                LinearLayout view = (LinearLayout) tab.getCustomView();
                BadgeView bg = (BadgeView) view.findViewById(R.id.cc_batch_view);
                if (Integer.parseInt(badgeCount) > 0) {
                    bg.setVisibility(View.VISIBLE);
                    bg.setText(badgeCount);
                } else {
                    bg.setVisibility(View.GONE);
                }

                tab.setCustomView(view);
            }

            if(groupsEnabled != FeatureState.INVISIBLE) {
                String badgeCount = String.valueOf(Groups.getUnreadGroupsCount());
                Logger.error(TAG, "Groups badge count = " + badgeCount);
                TabLayout.Tab tab = tabs.getTabAt(groupsTabIndex);
                LinearLayout view = (LinearLayout) tab.getCustomView();
                BadgeView bg = (BadgeView) view.findViewById(R.id.cc_batch_view);
                if (Integer.parseInt(badgeCount) > 0) {
                    bg.setVisibility(View.VISIBLE);
                    bg.setText(badgeCount);
                } else {
                    bg.setVisibility(View.GONE);
                }

                tab.setCustomView(view);
            }
        }
    }

    /*@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.hasExtra(BroadCastReceiverKeys.IntentExtrasKeys.OPEN_SETTINGS)) {
                if (mViewPager != null) {
                    mViewPager.setCurrentItem(mViewPager.getChildCount());
                }
            } else {
                PreferenceHelper.initialize(getApplicationContext());
                handleIntent(getApplicationContext(), intent);
            }
        }
        NotificationHelper.clearAllNotifications();
    }*/

//    public void handleIntent(Context context, Intent mainIntent) {

//                            Logger.error("chatroom is not present in our db");
//                            // Chatroom doesn't exist locally.
//                        } else {
//                            context.sendBroadcast(intent);
//                                    BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_FULL_CHATROOM_LIST_FRAGMENT, 1);
//                            intent.putExtra(
//                            Intent intent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.CHATROOM_HEARTBEAT_UPDATAION);
//
//                            context.startActivity(chatroomIntent);
//                            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, notificationMessage);
//                            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
//                            chatroomIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            Intent chatroomIntent = new Intent(context, CCGroupChatActivity.class);
//
//                            chatroom.save();
//                            chatroom.unreadCount = 0;
//                        if (null != chatroom) {
//                        Groups chatroom = Groups.getGroupDetails(chatroomId);
//
//                        }
//                            conversation.save();
//                            conversation.unreadCount = 0;
//                        if(conversation != null){
//                        Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(chatroomId));
//
//                        notificationMessage = matcher.group(1);
//                        matcher.find();
//                        Matcher matcher = pattern.matcher(notificationMessage);
//                        Pattern pattern = Pattern.compile("@(.*?):");
//                        }
//                            chatroomId = Long.parseLong(PreferenceHelper.get(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID));
//                        }else{
//                            chatroomId = Long.parseLong(message.getString("cid"));
//                        if (message.has("cid")){
//                        long chatroomId;
//                    if (json.has(NotificationHelper.IS_CHATROOM)) {
//
//                    String notificationMessage = message.getString(NotificationHelper.MESSAGE);
//
//                    JSONObject message = new JSONObject(messgeStr);
//                    String messgeStr = json.getString(NotificationHelper.MESSAGE);
//                    JSONObject json = new JSONObject(mainIntent.getStringExtra(NotificationHelper.DATA));
//
//                if (mainIntent.hasExtra(NotificationHelper.DATA)) {
//
//                }
//                    }
//                        }
//                            SessionData.getInstance().setChatbadgeMissed(true);
//                            getContext().sendBroadcast(iintent);
//                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
//                            Intent iintent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.ANNOUNCEMENT_BADGE_UPDATION);
//
//                            context.sendBroadcast(intent);
//                            intent.putExtra(BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_BUDDY_LIST_FRAGMENT, 1);
//                                    BroadCastReceiverKeys.HeartbeatKeys.ONE_ON_ONE_HEARTBEAT_NOTIFICATION);
//                            Intent intent = new Intent(
//
//                            context.startActivity(singleChatIntent);
//                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, notificationMessage);
//                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, buddyId);
//                            singleChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            Intent singleChatIntent = new Intent(context, CCSingleChatActivity.class);
//
//                            notificationMessage = notificationMessage.substring(0, colonPosition);
//                            int colonPosition = notificationMessage.indexOf(":");
//
//                            buddy.save();
//                            buddy.unreadCount = 0;
//                            Contact buddy = Contact.getContactDetails(buddyId);
//                        }else{
//                            }
//                                startActivity(avChatIntent);
//                                PreferenceHelper.save("FCMBuddyID",buddyId);
//                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomName);
//                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, buddyId);
//                                Intent avChatIntent = new Intent(/*context, IncomingCallActivity.class*/);
//                                String roomName = mainIntent.getStringExtra(NotificationHelper.FireBasePushNotificationKeys.ROOMNAME);
//                            if ((System.currentTimeMillis() - time) < 60000) {
//
//                        }else if(messageType.equals(NotificationHelper.FireBasePushNotificationKeys.IS_AV_CALL)){
//
//                            }
//                                startActivity(avChatIntent);
//                                PreferenceHelper.save("FCMBuddyID",buddyId);
//                                avChatIntent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, true);
//                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomName);
//                                avChatIntent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, buddyId);
//                                Intent avChatIntent = new Intent(/*context, IncomingCallActivity.class*/);
//                                String roomName = mainIntent.getStringExtra("grp");
//                            if ((System.currentTimeMillis() - time) < 60000) {
//                        if(messageType.equals(NotificationHelper.FireBasePushNotificationKeys.IS_AUDIO_CALL)){
//                        String messageType = mainIntent.getStringExtra(NotificationHelper.FireBasePushNotificationKeys.MESSAGE_TYPE);
//                    }else{
//                        }
//                            CCHeartbeat.getInstance().setForceHeartbeat();
//                            Logger.error("chatroom is not present in our db");
//                            // Chatroom doesn't exist locally.
//                        } else {
//                            context.sendBroadcast(intent);
//                                    BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_FULL_CHATROOM_LIST_FRAGMENT, 1);
//                            intent.putExtra(
//                            Intent intent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.CHATROOM_HEARTBEAT_UPDATAION);
//
//                            context.startActivity(chatroomIntent);
//                            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, notificationMessage);
//                            chatroomIntent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
//                            chatroomIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            Intent chatroomIntent = new Intent(context, CCGroupChatActivity.class);
//
//                            chatroom.save();
//                            chatroom.unreadCount = 0;
//                        if (null != chatroom) {
//                        Groups chatroom = Groups.getGroupDetails(chatroomId);
//
//                        notificationMessage = matcher.group(1);
//                        matcher.find();
//                        Matcher matcher = pattern.matcher(notificationMessage);
//                        Pattern pattern = Pattern.compile("@(.*?):");
//                        }
//                            chatroomId = Long.parseLong(PreferenceHelper.get(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID));
//                        }else{
//                            chatroomId = Long.parseLong(message.getString("cid"));
//                        if (message.has("cid")){
//                        long chatroomId;
//                    if (message.has("cid")){
//
//                    Long time = message.getLong("sent") * 1000;
//
//                    String notificationMessage = message.getString(NotificationHelper.MESSAGE);
//                    long buddyId = message.getLong(NotificationHelper.FROM_ID);
//                    JSONObject message = new JSONObject(messgeStr);
//                    String messgeStr = mainIntent.getStringExtra(NotificationHelper.MESSAGE);
//
//                if(mainIntent.hasExtra(.IS_FIREBASE_NOTIFICATION)){
//
//
//                NotificationHelper.clearAllNotifications();
//
//                String action = mainIntent.getAction();
//                    && "1".equals(PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN))) {
//            if (PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN)
//        try {
//                            CCHeartbeat.getInstance().setForceHeartbeat();
//                        }
//                    } else if (json.has(NotificationHelper.IS_ANNOUNCEMENT)) {
//                    } else {
//                        /*
//                         * Directly show Incoming call screen if it is an av
//                         * chat request.
//                         */
//                        long buddyId = message.getLong(NotificationHelper.FROM_ID);
//                        if (json.has(NotificationHelper.AV_CHAT)
//                                && json.getString(NotificationHelper.AV_CHAT).equals("1")) {
//                            String roomName = VideoChat.getAVRoomName(message.getString(NotificationHelper.MESSAGE),
//                                    false);
//
//                            Intent avChatIntent = new Intent(/*context, CCIncomingCallActivity.class*/);
//                            avChatIntent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, buddyId);
//                            avChatIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomName);
//                            context.startActivity(avChatIntent);
//                        } else {
//
//                            //Logger.error("normal message");
//
//                            Contact buddy = Contact.getContactDetails(buddyId);
//                            buddy.unreadCount = 0;
//                            buddy.save();
//
//                            Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(buddyId));
//                            if (0L != conversation.unreadCount) {
//                                conversation.unreadCount = 0;
//                                conversation.save();
//                            }
//
//                            int colonPosition = notificationMessage.indexOf(":");
//                            notificationMessage = notificationMessage.substring(0, colonPosition);
//
//                            Intent singleChatIntent = new Intent(context, CCSingleChatActivity.class);
//                            singleChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, buddyId);
//                            singleChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, notificationMessage);
//                            context.startActivity(singleChatIntent);
//
//                            Intent intent = new Intent(
//                                    BroadCastReceiverKeys.HeartbeatKeys.ONE_ON_ONE_HEARTBEAT_NOTIFICATION);
//                            intent.putExtra(BroadCastReceiverKeys.ListUpdatationKeys.REFRESH_BUDDY_LIST_FRAGMENT, 1);
//                            context.sendBroadcast(intent);
//
//                            Intent iintent = new Intent(BroadCastReceiverKeys.HeartbeatKeys.ANNOUNCEMENT_BADGE_UPDATION);
//                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
//                            getContext().sendBroadcast(iintent);
//                            SessionData.getInstance().setChatbadgeMissed(true);
//                        }
//                    }
//                } else if (Intent.ACTION_SEND.equals(action)) {
//                    /*Bundle extras = mainIntent.getExtras();
//                    String type = mainIntent.getType();
//                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
//                        Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
//                        if (!type.isEmpty() && CommonUtils.checkImageType(type)) {
//                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_IMAGE_URL, String.valueOf(uri));
//                        } else if (!type.isEmpty() && CommonUtils.checkVideoType(type)) {
//                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_VIDEO_URL, String.valueOf(uri));
//                        } else if (!type.isEmpty() && CommonUtils.checkAudioType(type)) {
//                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_AUDIO_URL, String.valueOf(uri));
//                        } else if (!type.isEmpty() && (CommonUtils.checkApplicationType(type) || CommonUtils.checkTextType(type))) {
//                            PreferenceHelper.save(PreferenceKeys.DataKeys.SHARE_FILE_URL, String.valueOf(uri));
//                        } else {
//                            String last = uri.getLastPathSegment();
//                            if (!CommonUtils.setFileType(last, uri)) {
//                                if (null != lang && null != lang.getMobile().get178()) {
//                                    Toast.makeText(getApplicationContext(), lang.getMobile().get178(), Toast.LENGTH_LONG).show();
//                                } else {
//                                    Toast.makeText(getApplicationContext(), "File format not supported. ", Toast.LENGTH_LONG).show();
//                                }
//                            }
//                        }
//                    } else {
//                        if (null != lang && null != lang.getMobile().get178()) {
//                            Toast.makeText(getApplicationContext(), lang.getMobile().get178(), Toast.LENGTH_LONG).show();
//                        } else {
//                            Toast.makeText(getApplicationContext(), "File format not supported. ", Toast.LENGTH_LONG).show();
//                        }
//                    }
//                    mainIntent.removeExtra(Intent.EXTRA_STREAM);*/
//                }
//            } else {
//                // logout the user
//                CCHeartbeat.getLaunchCallbackListner().onLogout();
//                /*Intent intent = new Intent(context, LoginActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                context.startActivity(intent);*/
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


    public void fetchDataBaseFile(){
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "/data/data/" + getPackageName() + "/databases/inscripts_cc.db";
                String backupDBPath = "backupname.db";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            Logger.error(TAG,"Databes pull exception = "+e);
        }
    }

    @Override
    public void onButtonClick(AlertDialog alertDialog, View view, int i, int i1) {

    }
}
