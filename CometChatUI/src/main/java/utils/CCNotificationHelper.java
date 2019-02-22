package utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.EncryptionHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Keys.BroadCastReceiverKeys;
import Keys.JsonParsingKeys;
import activities.CometChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import helpers.NotificationDataHelper;
import helpers.NotificationDataHelper;
import models.OneOnOneMessage;
import rolebase.Rolebase;
import services.DirectReplyService;
import videochat.CCIncomingCallActivity;

/**
 * Created by Jitvar on 3/28/2018.
 */

public class CCNotificationHelper {

    //asddas
    private static final java.lang.String TAG = CCNotificationHelper.class.getSimpleName();
    private static final String DELIMITER = "#:::#";
    private static Context context;
    public static final String MESSAGE = "m";
    public static final String DATA = "com.parse.Data";
    public static final String NEW_DATA = "cc.notification.Data";
    public static final String AV_CHAT = "avchat";
    public static final String FROM_ID = "fid";
    public static final String IS_CHATROOM = "isCR";
    public static final String ALERT = "alert";
    public static final String IS_ANNOUNCEMENT = "isANN";
    public static final String PUSH_CHANNEL = "push_channel";
    public static String TYPE = "t";

    private static String currentChannel = null;
    public static ArrayList<String> chatroomChannelList = new ArrayList<String>();
    public static int launcherIcon;
    public static int launcherScmallIcon;
    private static CometChat cometChat;
    private static FeatureState grpState;
    private static HashMap<Integer,ArrayList<String>> notifications = new HashMap<>();
     public static void processCCNotificationData(Context c , RemoteMessage remoteMessage , boolean closeWindow ,int LI , int LSI)  {
        context = c;
        PreferenceHelper.initialize(context);
        initializeSessionData();
        launcherIcon = LI;
        launcherScmallIcon = LSI;
        cometChat = CometChat.getInstance(context);
        PreferenceHelper.initializeRolebaseData();
        Logger.error(TAG,"onMessageReceive");
        try {
            Map<String, String> titleText = remoteMessage.getData();
            Logger.error(TAG,"Title text = "+titleText);
            JSONObject jsonData = new JSONObject();
            if (titleText.containsKey("action")) {
                jsonData.put("action",titleText.get("action"));
            }
            if (titleText.containsKey("t")) {
                jsonData.put("t", titleText.get("t"));
            }
            if (titleText.containsKey("alert")) {
                jsonData.put("alert", titleText.get("alert"));
            }
            if (titleText.containsKey("badge")) {
                jsonData.put("badge", titleText.get("badge"));
            }
            if (titleText.containsKey("sound")) {
                jsonData.put("sound", titleText.get("sound"));
            }
            if (titleText.containsKey("title")) {
                jsonData.put("title", titleText.get("title"));
            }
            if (titleText.containsKey("isCR")) {
                jsonData.put("isCR", titleText.get("isCR"));
            }
            if (titleText.containsKey("isANN")) {
                jsonData.put("isANN", titleText.get("isANN"));
            }

            Logger.error(TAG,"Json data = "+jsonData);



            Intent intent = new Intent(context, CometChatActivity.class);
            intent.putExtra("close_window",closeWindow);
            if (titleText.containsKey("m")) {
                jsonData.put("m",titleText.get("m").toString());
                JSONObject messageJson = new JSONObject(titleText.get("m"));
                String type = titleText.get(TYPE);
                String alert = titleText.get(ALERT);
                if (type.equals("C")){
                    if(!PreferenceHelper.get(PreferenceKeys.UserKeys.USER_ID).equals(messageJson.getString(FROM_ID))&&(PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID) == null || !PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID).equals(messageJson.getString("cid")))){
                        if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")){
                            if(jsonData.has("alert") && !jsonData.getString("alert").contains("CC^CONTROL_PLUGIN_")){
                                intent.putExtra(DATA,jsonData.toString());
                                    NotificationDataHelper.addToMap(Integer.parseInt(messageJson.getString("cid")),alert);
                                    processAndDisplayNotifications(alert, intent, Integer.parseInt(messageJson.getString("cid")), true);

                            }
                        }
                    }
                } else if (titleText.containsKey("isANN")) {
                    intent.putExtra(DATA,jsonData.toString());
                    if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")){
                        processAndDisplayNotifications(alert, intent, 0, false);
                    }
                } else {
                    Long buddyId = messageJson.getLong(FROM_ID);
                    long buddyWindowId = 0;
                    if (PreferenceHelper.contains(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID)) {
                        buddyWindowId = Long.parseLong(PreferenceHelper
                                .get(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID));
                    }
                        Logger.error(TAG,"Message Json has ? "+messageJson.has("m"));
                        if (messageJson.has("m")) {

                            if (type.contains("O_A")) {
                                Intent i = new Intent(context, CCIncomingCallActivity.class);
                                if (type.equals("O_AVC")) {
                                    i.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, false);
                                } else if (type.equals("O_AC")) {
                                    i.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, true);
                                }
                                if (titleText.containsKey("grp")) {

                                    String originalRoomname = titleText.get("grp");
                                    Logger.error(TAG, "originalRoomname : " + originalRoomname);
                                    String webrtc_channel = PreferenceHelper.get(PreferenceKeys.UserKeys.WEBRTC_CHANNEL);
                                    String roomNameMd5 = EncryptionHelper.encodeIntoMD5(webrtc_channel + originalRoomname);
                                    Logger.error(TAG, "roomName md5 : " + roomNameMd5);
                                    i.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomNameMd5);

                                    SessionData session = SessionData.getInstance();
                                    session.setAvChatRoomName(originalRoomname);
                                    session.setActiveAVchatUserID(String.valueOf(buddyId));
                                }
                                Logger.error(TAG,"MessageJsom = "+messageJson);
                                if (messageJson.has("sent")) {
                                    Long time = messageJson.getLong("sent") * 1000;
                                    if ((System.currentTimeMillis() - time) < 60000) {
                                        Logger.error(TAG,"buddyWindowId = "+buddyWindowId);
                                        Logger.error(TAG,"buddyId = "+buddyId);

                                        if (buddyWindowId != buddyId || buddyWindowId == 0) {
                                            try {
                                                Long messageid = messageJson.getLong("id");
                                                OneOnOneMessage avmessage = OneOnOneMessage.findById(messageid);
                                                Logger.error(TAG,"avmessage = "+avmessage);
                                                if (type.equals("O_AVC_CANCEL") || type.equals("O_AC_CANCEL")) {
                                                    Intent callCancelIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                                                    callCancelIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.CALL_CANCEL_FROM_NOTIFICATION, 1);
                                                    context.sendBroadcast(callCancelIntent);
                                                } else if (type.equals("O_AVC_END") || type.equals("O_AC_END")) {
                                                    Intent endCancelIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                                                    endCancelIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.CALL_END_FROM_NOTIFICATION, 1);
                                                    context.sendBroadcast(endCancelIntent);
                                                }else if (avmessage == null) {
                                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                                    i.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(buddyId));
                                                    i.putExtra(CometChatKeys.AVchatKeys.CALLER_NAME, messageJson.getString("name"));
                                                    context.startActivity(i);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } else {
                                        //Logger.error("Notification is arrived late ");
                                    }
                                }


                            } else {
                                Logger.error(TAG, "processCCNotificationData: buddyWindowId: "+buddyWindowId );
                                if (buddyWindowId != buddyId || buddyWindowId == 0) {
                                    intent.putExtra(DATA,jsonData.toString());
                                    if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")){
                                        NotificationDataHelper.addToMap(Integer.parseInt(String.valueOf(buddyId)),alert);
                                        processAndDisplayNotifications(alert, intent, Integer.parseInt(String.valueOf(buddyId)), false);
                                    }
                                }
                            }

                    }else{
                        intent.putExtra(DATA, jsonData.toString());
                        if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1") &&

                                !PreferenceHelper.contains(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID)){
                            NotificationDataHelper.addToMap(Integer.parseInt(String.valueOf(buddyId)),alert);
                            processAndDisplayNotifications(alert, intent, Integer.parseInt(String.valueOf(buddyId)), false);
                        }
                    }
                }
            }

        }catch (Exception e){
            //Logger.error("In firebase exception");
            e.printStackTrace();

        }
    }

    private static void processAndDisplayNotifications(String notificationText, Intent resultIntent, int notificationId,boolean isGroup){
        Logger.error(TAG, "processAndDisplayNotifications: " + notificationText + " notificationId: " + notificationId + " isGroup: " + isGroup);
        String formattedNotificationText = String.valueOf(Html.fromHtml(notificationText));
        String appName = context.getString(context.getApplicationInfo().labelRes);

        boolean isSoundActive = PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_SOUND).equals("1");
        boolean isVibrateActive = PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE).equals("1");

        boolean isUsingSystemSound = true;
        NotificationCompat.InboxStyle style;
        if (notificationId != 0) {
            ArrayList<String> notificationList = NotificationDataHelper.getFromMap(notificationId);
            style = new NotificationCompat.InboxStyle().setSummaryText(notificationList.size() + " new messages")
                    .setBigContentTitle(appName);
            for (int i = notificationList.size() - 1; i >= 0; i--) { // To display latest 7 messages in notification
                style.addLine(notificationList.get(i));
            }
        } else {
            style = new NotificationCompat.InboxStyle().setSummaryText("new Announcement");
            style.addLine(formattedNotificationText);
        }
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.putExtra("notificationId", notificationId);
        PendingIntent pIntent = PendingIntent.getActivity(context, notificationId, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        Notification summaryNotification = null;
        Intent replyIntent = new Intent(context, DirectReplyService.class);
        Logger.error(TAG, "processAndDisplayNotifications: passing data to service: id" + notificationId + "  isGroup: " + isGroup);
        replyIntent.putExtra("notificationId", notificationId);
        replyIntent.putExtra("isGroup", isGroup);
        int dummyuniqueInt = new Random().nextInt(200);
        PendingIntent pendingReplyIntent = PendingIntent.getService(context, notificationId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteInput remoteInput = new RemoteInput.Builder("key_text_reply")
                .setLabel("Send Message")
                .build();
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.cc_ic_send,
                "Reply",
                pendingReplyIntent
        ).addRemoteInput(remoteInput)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationChannel mChannel;
                String CHANNEL_ID = "";
                CharSequence channelName = "";
                int importance = 0;

                if (isSoundActive && isVibrateActive) {
                    Logger.error(TAG, "showNotification() 1: isSoundActive " + isSoundActive + " isVibrateActive " + isVibrateActive);
                    CHANNEL_ID = "Notification Channel 1";
                    channelName = "Notification Channel 1";   // The user-visible name of the channel.
                    importance = NotificationManager.IMPORTANCE_DEFAULT;
                } else if (!isSoundActive && !isVibrateActive) {
                    Logger.error(TAG, "showNotification() 2: isSoundActive " + isSoundActive + " isVibrateActive " + isVibrateActive);
                    CHANNEL_ID = "Notification Channel 2";
                    channelName = "Notification Channel 2";   // The user-visible name of the channel.
                    importance = NotificationManager.IMPORTANCE_LOW;
                } else if (!isSoundActive && isVibrateActive) {
                    Logger.error(TAG, "showNotification() 3: isSoundActive " + isSoundActive + " isVibrateActive " + isVibrateActive);
                    CHANNEL_ID = "Notification Channel 3";
                    channelName = "Notification Channel 3";   // The user-visible name of the channel.
                    importance = NotificationManager.IMPORTANCE_DEFAULT;
                } else if (!isVibrateActive && isSoundActive) {
                    Logger.error(TAG, "showNotification() 4: isSoundActive " + isSoundActive + " isVibrateActive " + isVibrateActive);
                    CHANNEL_ID = "Notification Channel 4";
                    channelName = "Notification Channel 4";   // The user-visible name of the channel.
                    importance = NotificationManager.IMPORTANCE_DEFAULT;
                }

                mChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);
                mChannel.setShowBadge(true);

                if (isSoundActive) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attributes);
                } else {
                    mChannel.setSound(null, null);
                }

                if (isVibrateActive) {
                    mChannel.enableVibration(true);
                } else {
                    Logger.error(TAG, "showNotifications() : isVibrateActive : " + isVibrateActive);
                    mChannel.enableVibration(true);
                    mChannel.setVibrationPattern(new long[]{0});
                }

                notificationManager.createNotificationChannel(mChannel);
                if (notificationId != 0) {
                    summaryNotification = mBuilder.setContentTitle(appName).setSmallIcon(launcherScmallIcon)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), launcherIcon))
                            .setContentText(formattedNotificationText).setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setStyle(style)
                            .addAction(replyAction)
                            .setColor((Integer) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY)))
                            .setChannelId(CHANNEL_ID)
                            .build();
                } else {
                    summaryNotification = mBuilder.setContentTitle(appName).setSmallIcon(launcherScmallIcon)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), launcherIcon))
                            .setContentText(formattedNotificationText).setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setStyle(style)
                            .setColor((Integer) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY)))
                            .setChannelId(CHANNEL_ID)
                            .build();
                }
            } else {

                if (notificationId != 0) {
                    summaryNotification = mBuilder.setContentTitle(appName).setSmallIcon(launcherScmallIcon)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), launcherIcon))
                            .setContentText(formattedNotificationText)
                            .setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setStyle(style)
                            .addAction(replyAction)
                            .setColor((Integer) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY)))
                            .build();
                } else {
                    summaryNotification = mBuilder.setContentTitle(appName).setSmallIcon(launcherScmallIcon)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), launcherIcon))
                            .setContentText(formattedNotificationText)
                            .setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setStyle(style)
                            .setColor((Integer) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY)))
                            .build();
                }

                if (isUsingSystemSound && isSoundActive) {
                    summaryNotification.defaults |= Notification.DEFAULT_SOUND;
                }

                if (isVibrateActive) {
                    summaryNotification.defaults |= Notification.DEFAULT_VIBRATE;
                }
            }
        } else {
            summaryNotification = mBuilder.setContentTitle(appName).setSmallIcon(launcherScmallIcon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), launcherIcon))
                    .setContentText(formattedNotificationText).setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .setStyle(style)
                    .setColor(Color.parseColor("#002832"))
                    .build();

            if (isUsingSystemSound && isSoundActive) {
                summaryNotification.defaults |= Notification.DEFAULT_SOUND;
            }

            if (isVibrateActive) {
                summaryNotification.defaults |= Notification.DEFAULT_VIBRATE;
            }
        }
        summaryNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (isUsingSystemSound && isSoundActive) {
            summaryNotification.defaults |= Notification.DEFAULT_SOUND;
        }

        if (isVibrateActive) {
            summaryNotification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        summaryNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
        summaryNotification.ledARGB = 0xff00ff00;
        summaryNotification.ledOnMS = 300;
        summaryNotification.ledOffMS = 2000;

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            summaryNotification.priority = Notification.PRIORITY_MAX;
        }

        if (notificationManager != null) {
            Logger.error(TAG, "processAndDisplayNotifications: notificationId: " + notificationId);
            notificationManager.notify(notificationId, summaryNotification);
        }
    }


    public static void processCCNotificationData(Context c , RemoteMessage remoteMessage , int LI , int LSI,Class targetClass){
        context = c;
        PreferenceHelper.initialize(context);
        initializeSessionData();
        launcherIcon = LI;
        launcherScmallIcon = LSI;
        cometChat = CometChat.getInstance(context);
        grpState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CREATE_GROUPS_ENABLED));
        Logger.error(TAG,"onMessageReceive");
        try {
            Map<String, String> titleText = remoteMessage.getData();
            Logger.error(TAG,"Title text = "+titleText);
            JSONObject jsonData = new JSONObject();
            if (titleText.containsKey("action")) {
                jsonData.put("action",titleText.get("action"));
            }
            if (titleText.containsKey("t")) {
                jsonData.put("t", titleText.get("t"));
            }
            if (titleText.containsKey("alert")) {
                jsonData.put("alert", titleText.get("alert"));
            }
            if (titleText.containsKey("badge")) {
                jsonData.put("badge", titleText.get("badge"));
            }
            if (titleText.containsKey("sound")) {
                jsonData.put("sound", titleText.get("sound"));
            }
            if (titleText.containsKey("title")) {
                jsonData.put("title", titleText.get("title"));
            }
            if (titleText.containsKey("isCR")) {
                jsonData.put("isCR", titleText.get("isCR"));
            }
            if (titleText.containsKey("isANN")) {
                jsonData.put("isANN", titleText.get("isANN"));
            }

            Logger.error(TAG,"Json data = "+jsonData);



            Intent intent = new Intent(context, targetClass);
            if (titleText.containsKey("m")) {
                jsonData.put("m",titleText.get("m").toString());
                JSONObject messageJson = new JSONObject(titleText.get("m"));
                String type = titleText.get(TYPE);
                String alert = titleText.get(ALERT);
                if (type.equals("C")){
                    if(!PreferenceHelper.get(PreferenceKeys.UserKeys.USER_ID).equals(messageJson.getString(FROM_ID))&&(PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID) == null || !PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID).equals(messageJson.getString("cid")))){
                        if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")){
                            if(jsonData.has("alert") && !jsonData.getString("alert").contains("CC^CONTROL_PLUGIN_")){
                                intent.putExtra(NEW_DATA,jsonData.toString());
                                if (grpState == FeatureState.ACCESSIBLE) {
                                    processAndDisplayNotifications(alert, intent, Integer.parseInt(messageJson.getString("cid")), true);
                                }
                            }
                        }
                    }
                } else if (titleText.containsKey("isANN")) {
                    intent.putExtra(NEW_DATA,jsonData.toString());
                    if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")){
                        processAndDisplayNotifications(alert, intent, 0, false);
                    }
                } else {
                    Long buddyId = messageJson.getLong(FROM_ID);
                    long buddyWindowId = 0;
                    if (PreferenceHelper.contains(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID)) {
                        buddyWindowId = Long.parseLong(PreferenceHelper
                                .get(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID));
                    }
                        if (messageJson.has("m")) {

                            if (type.contains("O_A")) {
                                Intent i = new Intent(context, CCIncomingCallActivity.class);
                                if (type.equals("O_AVC")) {
                                    i.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, false);
                                } else if (type.equals("O_AC")) {
                                    i.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, true);
                                }
                                if (titleText.containsKey("grp")) {

                                    String originalRoomname = titleText.get("grp");
                                    Logger.error(TAG, "originalRoomname : " + originalRoomname);
                                    String webrtc_channel = PreferenceHelper.get(PreferenceKeys.UserKeys.WEBRTC_CHANNEL);
                                    String roomNameMd5 = EncryptionHelper.encodeIntoMD5(webrtc_channel + originalRoomname);
                                    Logger.error(TAG, "roomName md5 : " + roomNameMd5);
                                    i.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, roomNameMd5);

                                    SessionData session = SessionData.getInstance();
                                    session.setAvChatRoomName(originalRoomname);
                                    session.setActiveAVchatUserID(String.valueOf(buddyId));
                                }
                                if (messageJson.has("sent")) {
                                    Long time = messageJson.getLong("sent") * 1000;
                                    if ((System.currentTimeMillis() - time) < 60000) {
                                        Logger.error(TAG,"buddyWindowId = "+buddyWindowId);
                                        Logger.error(TAG,"buddyId = "+buddyId);

                                        if (buddyWindowId != buddyId || buddyWindowId == 0) {
                                            try {
                                                Long messageid = messageJson.getLong("id");
                                                OneOnOneMessage avmessage = OneOnOneMessage.findById(messageid);
                                                Logger.error(TAG,"avmessage = "+avmessage);
                                                if (type.equals("O_AVC_CANCEL") || type.equals("O_AC_CANCEL")) {
                                                    Intent callCancelIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                                                    callCancelIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.CALL_CANCEL_FROM_NOTIFICATION, 1);
                                                    context.sendBroadcast(callCancelIntent);
                                                } else if (type.equals("O_AVC_END") || type.equals("O_AC_END")) {
                                                    Intent endCancelIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                                                    endCancelIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.CALL_END_FROM_NOTIFICATION, 1);
                                                    context.sendBroadcast(endCancelIntent);
                                                }else if (avmessage == null) {
                                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                                    i.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(buddyId));
                                                    i.putExtra(CometChatKeys.AVchatKeys.CALLER_NAME, messageJson.getString("name"));
                                                    context.startActivity(i);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } else {
                                        //Logger.error("Notification is arrived late ");
                                    }
                                }


                            } else {
                                if (buddyWindowId != buddyId || buddyWindowId == 0) {
                                    intent.putExtra(NEW_DATA,jsonData.toString());
                                    if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")){
                                        processAndDisplayNotifications(alert, intent, Integer.parseInt(String.valueOf(buddyId)), false);
                                    }
                                }
                            }

                    }else{
                        intent.putExtra(NEW_DATA, jsonData.toString());
                        if(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1") &&
                            !PreferenceHelper.contains(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID)){
                            processAndDisplayNotifications(alert, intent, Integer.parseInt(String.valueOf(buddyId)), false);
                        }
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();

        }
    }

    public static void unsubscribe(boolean isChatroom,boolean isclearAll) {
        try {
            if (isChatroom) {
                if(isclearAll){
                    if(PreferenceHelper.contains(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST)){
                        String channelListStr = PreferenceHelper.get(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST);
                        if (!TextUtils.isEmpty(channelListStr)) {
                            channelListStr = channelListStr.substring(1, channelListStr.length()-1);
                            //Logger.error("channel List string :   " + channelListStr);
                        }
                        List<String> items = Arrays.asList(channelListStr.split("\\s*,\\s*"));
                        //Logger.error("list size channel "+items.size());
                        if (items.size() > 0) {
                            for (int i = 0; i < items.size(); i++) {
                                if(!items.get(i).isEmpty())
                                    FirebaseMessaging.getInstance().unsubscribeFromTopic(items.get(i));
                            }
                        }
                    }
                }else {
                    currentChannel = PreferenceHelper.get(PreferenceKeys.DataKeys.CURRENT_GROUP_CHANNEL);
                    if(chatroomChannelList.contains(currentChannel)){
                        chatroomChannelList.remove(currentChannel);
                    }else if(PreferenceHelper.contains(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST)){
                        String channelListStr = PreferenceHelper.get(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST);
                        if (!TextUtils.isEmpty(channelListStr)) {
                            channelListStr = channelListStr.substring(1, channelListStr.length() - 1);
                            //Logger.error("channel List string :   " + channelListStr);
                        }
                        List<String> items = Arrays.asList(channelListStr.split("\\s*,\\s*"));
                        if (items.size() > 0) {
                            for (int i = 0; i < items.size(); i++) {
                                if(!chatroomChannelList.contains(items.get(i))){
                                    chatroomChannelList.add(items.get(i));
                                }

                            }
                        }
                        if (chatroomChannelList.contains(currentChannel)) {
                            chatroomChannelList.remove(currentChannel);
                        }
                    }
                    PreferenceHelper.save(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST, chatroomChannelList.toString());
                }
            }
            Logger.error(TAG, "Current Chatroom Push Channel1 : " + currentChannel);
            if (null != currentChannel) {
                Logger.error(TAG, "Current Chatroom Push Channel2 : " + currentChannel);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(currentChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Subscribes to a channel based on the parameter passed. Pass <b>true</b>
     * for chatrooms
     */
    public static void subscribe(boolean isChatroom, String channel) {
        Logger.error(TAG,"Firebase subcribe channel = "+channel);
        try {
            if (isChatroom) {
                if(chatroomChannelList == null){
                    chatroomChannelList = new ArrayList<String>();
                }
                currentChannel = channel;
                if(!chatroomChannelList.contains(channel)){
                    if(!PreferenceHelper.contains(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST)){
                        chatroomChannelList.add(channel);
                    }else{
                        String channelListStr = PreferenceHelper.get(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST);
                        if (!TextUtils.isEmpty(channelListStr)) {
                            channelListStr = channelListStr.substring(1, channelListStr.length() - 1);
                            //Logger.error("channel List string :   " + channelListStr);
                        }
                        List<String> items = Arrays.asList(channelListStr.split("\\s*,\\s*"));
                        //Logger.error("list size channel " + items.size());

                        if (items.size() > 0) {
                            for (int i = 0; i < items.size(); i++) {
                                if (!chatroomChannelList.contains(items.get(i))){
                                    chatroomChannelList.add(items.get(i));
                                }

                            }
                        }
                        if (!chatroomChannelList.contains(channel)){
                            chatroomChannelList.add(channel);
                        }
                    }
                }

                PreferenceHelper.save(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST, chatroomChannelList.toString());
            } else {
                currentChannel = channel;
            }
            if (null != currentChannel){
                try{
                    FirebaseMessaging.getInstance().subscribeToTopic(currentChannel);
                }catch (Throwable t){
                    t.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Clears all the notifications
     */
    public static void clearAllNotifications() {
        NotificationManager notificationManager = (NotificationManager) PreferenceHelper.getContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        PreferenceHelper.removeKey(PreferenceKeys.DataKeys.NOTIFICATION_STACK);
    }

    private static void initializeSessionData() {
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

        if (!PreferenceHelper.contains(PreferenceKeys.DataKeys.ACTIVE_CHATROOM_ID)) {
            PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_CHATROOM_ID, "0");
        }

        if (!PreferenceHelper.contains(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID)) {
            PreferenceHelper.save(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID, "1");
        }
    }
}


