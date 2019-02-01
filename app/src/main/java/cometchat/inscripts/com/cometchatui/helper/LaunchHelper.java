package cometchat.inscripts.com.cometchatui.helper;


import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.inscripts.activities.CCCodLoginActivity;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.LaunchCallbacks;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.orm.SugarRecord;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatui.CCGuestLoginActivity;
import cometchat.inscripts.com.cometchatui.CCLoginActivity;
import cometchat.inscripts.com.cometchatui.services.MyFirebaseMessagingService;
import models.Bot;
import models.Contact;
import models.Conversation;
import models.GroupMessage;
import models.Groups;
import models.OneOnOneMessage;
import models.Status;
import utils.CCNotificationHelper;

public class LaunchHelper {
    private static final String TAG = LaunchHelper.class.getSimpleName();

    public static void launchCometChat(final Activity activity){
        CometChat cometChat = CometChat.getInstance(activity);

        cometChat.launchCometChat(activity, true, new LaunchCallbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "Success Callback = " + jsonObject);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "failCallback = " + jsonObject);
            }

            @Override
            public void userInfoCallback(JSONObject jsonObject) {
                Logger.error(TAG,"UserInfo Callback = "+jsonObject);
                try {
                    if (jsonObject.has("push_channel") && !TextUtils.isEmpty(jsonObject.getString(MyFirebaseMessagingService.PUSH_CHANNEL))){
                        PreferenceHelper.save(PreferenceKeys.UserKeys.SINGLE_CHAT_FIREBASE_CHANNEL, jsonObject.getString(MyFirebaseMessagingService.PUSH_CHANNEL));
                        FirebaseMessaging.getInstance().subscribeToTopic(PreferenceHelper.get(PreferenceKeys.UserKeys.SINGLE_CHAT_FIREBASE_CHANNEL));
                    }
                    if (jsonObject.has("push_an_channel") && !TextUtils.isEmpty(jsonObject.getString("push_an_channel"))) {
                        FirebaseMessaging.getInstance().subscribeToTopic(jsonObject.getString("push_an_channel"));
                        PreferenceHelper.save(PreferenceKeys.UserKeys.ANN_FIREBASE_CHANNEL, jsonObject.getString("push_an_channel"));
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void chatroomInfoCallback(JSONObject jsonObject) {
                Logger.error(TAG,"ChatroomInfo Callback = "+jsonObject);
                try {
                    if (jsonObject.has("action") && !TextUtils.isEmpty(jsonObject.getString("action")) && jsonObject.get("action").equals("join")) {
                        PreferenceHelper.save(PreferenceKeys.DataKeys.CURRENT_CHATROOM_ID, jsonObject.getString("group_id"));
                        if (jsonObject.has(MyFirebaseMessagingService.PUSH_CHANNEL) && !TextUtils.isEmpty(jsonObject.getString(MyFirebaseMessagingService.PUSH_CHANNEL))) {
                            CCNotificationHelper.subscribe(true, jsonObject.getString(MyFirebaseMessagingService.PUSH_CHANNEL));
                            PreferenceHelper.save(PreferenceKeys.DataKeys.CURRENT_GROUP_CHANNEL, jsonObject.getString(MyFirebaseMessagingService.PUSH_CHANNEL));
                        }
                    }

                    if (jsonObject.has("action") && !TextUtils.isEmpty(jsonObject.getString("action")) && jsonObject.get("action").equals("leave")) {
                        CCNotificationHelper.unsubscribe(true, false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onMessageReceive(JSONObject jsonObject) {
                Logger.error(TAG, "onMessageReceive = " + jsonObject);
            }

            @Override
            public void error(JSONObject jsonObject) {
                Logger.error(TAG, "onError = " + jsonObject);
            }

            @Override
            public void onWindowClose(JSONObject jsonObject) {
                Logger.error(TAG, "onWindowClose = " + jsonObject);
            }

            @Override
            public void onLogout() {
                Logger.error(TAG,"onLogout called");
                clearDataBase();
                CometChat cometchat = CometChat.getInstance(activity);
                cometchat.logout(new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"onLogout(): "+jsonObject);
                        unsubscribeChannel();

                        CCNotificationHelper.clearAllNotifications();
                        Intent intent;
                        if (PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN_AS_GUEST)) {
                            intent = new Intent(PreferenceHelper.getContext(), CCGuestLoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        } else if (PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN_AS_COD)) {
                            intent =  new Intent(PreferenceHelper.getContext(), CCCodLoginActivity.class);
                            intent.putExtra("Url", PreferenceHelper.get(PreferenceKeys.LoginKeys.COD_LOGIN_URL));
                        } else {
                            intent = new Intent(PreferenceHelper.getContext(), CCLoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        if (PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN_AS_COD)) {
                            activity.startActivityForResult(intent, 11111);
                        } else {
                            PreferenceHelper.getContext().startActivity(intent);
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {

                    }
                });
            }
        });
    }

    private static void unsubscribeChannel() {
        Logger.error(TAG,"unsubscribeChannel Called");
        if (!TextUtils.isEmpty(PreferenceHelper.get(PreferenceKeys.UserKeys.SINGLE_CHAT_FIREBASE_CHANNEL))) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(PreferenceHelper.get(PreferenceKeys.UserKeys.SINGLE_CHAT_FIREBASE_CHANNEL));
        }

        if (!TextUtils.isEmpty(PreferenceHelper.get(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST))) {
            String chatRoomChannelList = PreferenceHelper.get(PreferenceKeys.UserKeys.CHATROOM_CHANNEL_LIST);
            if (!TextUtils.isEmpty(chatRoomChannelList)) {
                chatRoomChannelList = chatRoomChannelList.substring(1, chatRoomChannelList.length()-1);
            }
            List<String> items = Arrays.asList(chatRoomChannelList.split("\\s*,\\s*"));
            if (items.size() > 0) {
                for (int i = 0; i < items.size(); i++) {
                    if(!items.get(i).isEmpty())
                        Logger.error(TAG,"CHATROOM_CHANNEL : "+items.get(i));
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(items.get(i));
                }
            }
        }

        if (!TextUtils.isEmpty(PreferenceHelper.get(PreferenceKeys.UserKeys.ANN_FIREBASE_CHANNEL))) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(PreferenceHelper.get(PreferenceKeys.UserKeys.ANN_FIREBASE_CHANNEL));
        }
    }

    private static void clearDataBase() {
        SugarRecord.deleteAll(OneOnOneMessage.class);
        SugarRecord.deleteAll(Groups.class);
        SugarRecord.deleteAll(Conversation.class);
        SugarRecord.deleteAll(GroupMessage.class);
        SugarRecord.deleteAll(Status.class);
        SugarRecord.deleteAll(Contact.class);
        SugarRecord.deleteAll(Bot.class);
    }
}
