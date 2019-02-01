package services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import com.inscripts.custom.EmoticonUtils;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.plugins.Smilies;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import Keys.JsonParsingKeys;
import Keys.MessageTypeKeys;
import activities.CCGroupChatActivity;
import activities.CCSingleChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import helpers.CCMessageHelper;
import helpers.NotificationDataHelper;
import models.Contact;
import models.Conversation;
import models.GroupMessage;
import models.Groups;
import models.OneOnOneMessage;

public class DirectReplyService extends IntentService {
    private static final String TAG = "DirectReplyService";
    private NotificationManager notificationManager;

    public DirectReplyService() {
        super("DirectReplyService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        final int notificationId = intent.getIntExtra("notificationId", 0);
        boolean isGroup = intent.getBooleanExtra("isGroup", false);
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
            NotificationDataHelper.deleteFromMap(notificationId );
        }
        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence("key_text_reply");
            Logger.error(TAG, "onHandleIntent: replyText "+replyText + " notificationId: "+notificationId);
            if(isGroup){
                final GroupMessage messagePojo = new GroupMessage(0,
                        SessionData.getInstance().getId(), ((long)(notificationId)), replyText.toString(), System.currentTimeMillis(), "",
                        MessageTypeKeys.NORMAL_MESSAGE, "", "#FFF", 1, 0);
                addGroupMessageToConversation(messagePojo,notificationId);
                CometChat.getInstance(getApplicationContext()).sendMessage(messagePojo.getId(), String.valueOf(notificationId), replyText.toString(), "#FFF",true, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject sendResponse) {
                        notificationManager.cancel(notificationId);
                        // Make unread Count zero after reply
                        Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(notificationId));
                        if (conversation != null) {
                            conversation.unreadCount = 0;
                            conversation.save();
                        }
                        try {
                            Logger.error(TAG, "send Response Messagee cometGroup : "+sendResponse);
                            long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                            Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                            String messageAfterSuccess = sendResponse.getString(CometChatKeys.AjaxKeys.MESSAGE);
                            if (messageAfterSuccess.contains("<img class=\"cometchat_smiley\"")) {
                                messageAfterSuccess = Smilies.convertImageTagToEmoji(messageAfterSuccess);
                                Logger.error(TAG, "emoji message: " + messageAfterSuccess);
                            }
                            GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                            Logger.error(TAG, "send Response mess : " + mess);
                            if (mess != null) {
                                mess.remoteId = id;
                                mess.messageStatus = 1;
                                //mess.message = String.valueOf(Html.fromHtml(messageAfterSuccess));
                                mess.save();
//                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        notificationManager.cancel(notificationId);
                        com.inscripts.utils.Logger.error(TAG, "Send Message fail responce = " + jsonObject);
                    }
                });
            }else {
                final OneOnOneMessage mess = new OneOnOneMessage(0L,
                        SessionData.getInstance().getId(), notificationId, replyText.toString(), System.currentTimeMillis(), 1, 1, EmoticonUtils.isEmojiMessage(replyText.toString())?MessageTypeKeys.EMOJI_MESSAGE:
                        MessageTypeKeys.NORMAL_MESSAGE, "", 1, CometChatKeys.MessageTypeKeys.MESSAGE_SENT, 0);

                if(EmoticonUtils.isOnlySmileyMessage(replyText.toString())){
                    mess.imageUrl = MessageTypeKeys.NO_BACKGROUND;
                }
                addOneOnOneMessageToConversation(mess,notificationId);
                CometChat.getInstance(getApplicationContext()).sendMessage(mess.getId(), String.valueOf(notificationId), replyText.toString(),false, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject sendResponse) {
                        notificationManager.cancel(notificationId);
                        // Make unread Count zero after reply
                        Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(notificationId));
                        if (conversation != null) {
                            conversation.unreadCount = 0;
                            conversation.save();
                        }
                        try {
                            Logger.error(TAG, "sendResponse Message - "+sendResponse);
                            long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                            Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                            Long timestamp = sendResponse.getLong(CometChatKeys.AjaxKeys.SENT);
                            Logger.error(TAG, "sendResponse : id - "+id);
                            String messageAfterSuccess = sendResponse.getString(CometChatKeys.AjaxKeys.MESSAGE);
                            if (messageAfterSuccess.contains("<img class=\"cometchat_smiley\"")) {
                                messageAfterSuccess = Smilies.convertImageTagToEmoji(messageAfterSuccess);
                                Logger.error(TAG, "emoji message: " + messageAfterSuccess);
                            }
                            OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                            if (mess != null) {
                                mess.remoteId = id;
                                mess.sentTimestamp = timestamp * 1000;
                                mess.messageStatus = 1;
                                //mess.message = String.valueOf(Html.fromHtml(messageAfterSuccess));
                                if(CometChat.getInstance(getApplicationContext()).isMessageinPendingDeliveredList(String.valueOf(id))){
                                    mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                                }
                                mess.save();
                                Logger.error(TAG,"Remote id = "+id);
                            }
                        } catch (JSONException e) {
                            Logger.error("sendResponse : e - "+e.toString());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        notificationManager.cancel(notificationId);
                        Logger.error(TAG, "Send Message fail = " + jsonObject);
                    }
                });
            }
        }
    }

    private void addGroupMessageToConversation(GroupMessage message,int notificationId) {
            message.save();
            Logger.error(TAG, "Chatroomid = " + notificationId);
            CCMessageHelper.addGroupConversation(Groups.getGroupDetails((long) notificationId), message);
    }

    private void addOneOnOneMessageToConversation(OneOnOneMessage message, int notificationId) {
        message.save();
        Contact contact = Contact.getContactDetails((long) notificationId);
        CCMessageHelper.addSingleChatConversation(contact, message, false);
    }
}
