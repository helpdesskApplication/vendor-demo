package helpers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessaging;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.SubscribeCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.plugins.OtherPlugins;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import Keys.BroadCastReceiverKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import models.Bot;
import models.Contact;
import models.Groups;
import models.OneOnOneMessage;

public class CCSubcribe {
    private static final java.lang.String TAG = CCSubcribe.class.getSimpleName();
    static CometChat cometChat;

    /**
     *
     * @param context
     * The following function will subscribe to CometChat services and provide all the required real time data through Callbacks.
     */
    public static void  SubcribeToCometChat(Activity context){
        cometChat = CometChat.getInstance(context);
        cometChat.subscribe(true, new SubscribeCallbacks() {
            @Override
            public void gotOnlineList(JSONObject jsonObject) {
                Logger.error(TAG,"deleteBuddy : "+jsonObject);
                if(jsonObject.has("message")) {
                    jsonObject = new JSONObject();
                }

                Contact.updateAllContacts(jsonObject);
            }

            @Override
            public void gotBotList(JSONObject jsonObject) {
//                Logger.error(TAG,"gotBotList = "+jsonObject);
//                Bot.updateAllBots(jsonObject);
            }

            @Override
            public void gotRecentChatsList(JSONObject jsonObject) {
                Logger.error(TAG, "gotRecentChatsList  :  " + jsonObject.toString());
                CCMessageHelper.processRecentChatList(jsonObject);
            }

            @Override
            public void onError(JSONObject jsonObject) {
                Logger.error(TAG,"onError = "+jsonObject);
            }

            @Override
            public void onMessageReceived(JSONObject jsonObject) {
                Logger.error(TAG,"on Message Receive = "+jsonObject);
                if(PreferenceHelper.contains(PreferenceKeys.UserKeys.SINGLE_CHAT_FIREBASE_CHANNEL)){
                    if(!PreferenceHelper.contains("SUBSCRIBED")){
                        FirebaseMessaging.getInstance().subscribeToTopic(PreferenceHelper.get(PreferenceKeys.UserKeys.SINGLE_CHAT_FIREBASE_CHANNEL));
                    }else {
                        PreferenceHelper.save("SUBSCRIBED",1);
                    }
                }
                try {
                     if(jsonObject.has("count")){
                         JSONArray jsonArray = jsonObject.getJSONArray("Messages");
                         for(int i=0;i<jsonArray.length();i++){
                             CCMessageHelper.processOneOnOneMessage(jsonArray.getJSONObject(i));
                         }
                     }else{
                         CCMessageHelper.processOneOnOneMessage(jsonObject);
                     }
                    Intent messageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                    messageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                    PreferenceHelper.getContext().sendBroadcast(messageIntent);

                    Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                    iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
                    PreferenceHelper.getContext().sendBroadcast(iintent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void gotProfileInfo(JSONObject jsonObject) {
                Logger.error(TAG,"gotProfileInfo = "+jsonObject);
                if (null != jsonObject && CommonUtils.isJSONValid(jsonObject.toString())) {
                    SessionData data = SessionData.getInstance();
                    data.update(jsonObject);
                }
            }

            @Override
            public void gotAnnouncement(JSONObject jsonObject) {
                Logger.error(TAG,"gotAnnouncement = "+jsonObject);
            }

            @Override
            public void onAVChatMessageReceived(JSONObject jsonObject) {
                Logger.error(TAG,"onAVChatMessageReceived = "+jsonObject);
                CCMessageHelper.processOneOnOneMessage(jsonObject);

                Intent messageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                messageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                PreferenceHelper.getContext().sendBroadcast(messageIntent);

                Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
                PreferenceHelper.getContext().sendBroadcast(iintent);
            }

            @Override
            public void onActionMessageReceived(JSONObject jsonObject) {
                Logger.error(TAG,"onActionMessageReceived = "+jsonObject);
                try {
                    String action = jsonObject.getString("action");
                    String fromid = jsonObject.getString("from");

                    if(fromid!= null && action.equals("typing_start")){
                        Intent isTypingIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                        isTypingIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IS_TYPING, 1);
                        isTypingIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, fromid);
                        PreferenceHelper.getContext().sendBroadcast(isTypingIntent);

                        Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IS_TYPING, 1);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, fromid);
                        PreferenceHelper.getContext().sendBroadcast(iintent);
                    }else if(fromid!= null && action.equals("typing_stop")){
                        Intent isTypingIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                        isTypingIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.STOP_TYPING, 1);
                        isTypingIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, fromid);
                        PreferenceHelper.getContext().sendBroadcast(isTypingIntent);

                        Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.STOP_TYPING, 1);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, fromid);
                        PreferenceHelper.getContext().sendBroadcast(iintent);
                    }else if(action.equals("message_deliverd")){
                        final String msgId = jsonObject.getString("message_id");
                        OneOnOneMessage msg = OneOnOneMessage.findByRemoteId(msgId);
                        if(msg != null && msg.self == 1){
                            if (msg.messagetick != CometChatKeys.MessageTypeKeys.MESSAGE_READ) {
                                msg.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                                msg.save();

                                Intent iintent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                                iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.DELIVERED_MESSAGE, 1);
                                PreferenceHelper.getContext().sendBroadcast(iintent);
                            }
                        }else{
                            cometChat.savePendingDeliveredMessages(msgId);
                        }

                    }else if(action.equals("message_read")){
                        final String msgId = jsonObject.getString("message_id");
                        OneOnOneMessage msg = OneOnOneMessage.findById(msgId);

                        Logger.error(TAG,"msg = "+msg);
                        if (msg != null && msg.self == 1) {

                            msg.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_READ;
                            msg.save();

                            Intent iintent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.READ_MESSAGE, 1);
                            PreferenceHelper.getContext().sendBroadcast(iintent);
                        }else {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    OneOnOneMessage msg = OneOnOneMessage.findByRemoteId(msgId);
                                    if(msg != null && msg.self == 1){
                                        if (msg != null && msg.self == 1) {
                                            msg.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_READ;
                                            msg.save();

                                            Intent iintent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.READ_MESSAGE, 1);
                                            PreferenceHelper.getContext().sendBroadcast(iintent);
                                        }
                                    }
                                }
                            }, 3000);
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onGroupMessageReceived(JSONObject jsonObject) {
                Logger.error(TAG,"onGroupMessageReceived = "+jsonObject);
                try {
                    if(jsonObject.has("count")){
                        JSONArray jsonArray = jsonObject.getJSONArray("Messages");
                        Logger.error(TAG,"Grp message JsonArray = "+jsonArray);
                        Logger.error(TAG,"Grp message JsonArray length = "+jsonArray.length());
                        for(int i=0;i<jsonArray.length();i++){
                            Logger.error(TAG,"Grp message process called for "+ i);
                            CCMessageHelper.processGroupMessage(jsonArray.getJSONObject(i));
                        }
                    }else{
                        CCMessageHelper.processGroupMessage(jsonObject);
                    }

                    Intent messageBroadCast = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                    messageBroadCast.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                    PreferenceHelper.getContext().sendBroadcast(messageBroadCast);

                    Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                    iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
                    PreferenceHelper.getContext().sendBroadcast(iintent);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onGroupsError(JSONObject jsonObject) {
                Logger.error(TAG,"onGroupsError = "+jsonObject);
            }

            @Override
            public void onLeaveGroup(JSONObject jsonObject) {
                Logger.error(TAG,"onLeaveGroup = "+jsonObject);
            }

            @Override
            public void gotGroupList(JSONObject groupList) {
                Logger.error(TAG,"gotGroupList = "+groupList);
                Groups.updateAllGroups(groupList);
            }

            @Override
            public void gotGroupMembers(JSONObject jsonObject) {
                Logger.error(TAG,"gotGroupMembers = "+jsonObject);
            }

            @Override
            public void onGroupAVChatMessageReceived(JSONObject jsonObject) {
                Logger.error(TAG,"onChatroomAVChatMessageReceived = "+jsonObject);
                CCMessageHelper.processGroupMessage(jsonObject);

                Intent messageBroadCast = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                messageBroadCast.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                PreferenceHelper.getContext().sendBroadcast(messageBroadCast);

                Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
                PreferenceHelper.getContext().sendBroadcast(iintent);
            }

            @Override
            public void onGroupActionMessageReceived(JSONObject jsonObject) {
                Logger.error(TAG,"onChatroomActionMessageReceived = "+jsonObject);
                try {
                    String action_type = jsonObject.getString("action_type");
                    String chatRoomId = jsonObject.getString("chatroom_id");
                    if(action_type.equals("10")){
                        Intent finishGroupChatIntent = new Intent(BroadCastReceiverKeys.FINISH_GROUP_ACTIVITY);
                        finishGroupChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID,chatRoomId);
                        finishGroupChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.KICKED,BroadCastReceiverKeys.IntentExtrasKeys.KICKED);
                        finishGroupChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_GROUP_LIST_KEY, 1);
                        PreferenceHelper.getContext().sendBroadcast(finishGroupChatIntent);
                    }else if(action_type.equals("11")){
                        Intent finishGroupChatIntent = new Intent(BroadCastReceiverKeys.FINISH_GROUP_ACTIVITY);
                        finishGroupChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID,chatRoomId);
                        finishGroupChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.BANNED,BroadCastReceiverKeys.IntentExtrasKeys.BANNED);
                        finishGroupChatIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_GROUP_LIST_KEY, 1);
                        PreferenceHelper.getContext().sendBroadcast(finishGroupChatIntent);
                    }else if(action_type.equals("14")){
                        Groups.insertNewGroup(jsonObject.getJSONObject("group"));
                        if(jsonObject.getJSONObject("group").has("push_channel")){
                            Logger.error(TAG, "onGroupActionMessageReceived: push_channel: "+ jsonObject.getJSONObject("group").getString("push_channel"));
                            FirebaseMessaging.getInstance().subscribeToTopic(jsonObject.getJSONObject("group").getString("push_channel"));
                        }
                        Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_CONTACT_LIST_KEY, 1);
                        PreferenceHelper.getContext().sendBroadcast(iintent);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onLogout() {
                Logger.error(TAG,"onLogout");
            }
        });
    }

}
