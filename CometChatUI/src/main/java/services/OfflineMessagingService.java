package services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;

import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import Keys.BroadCastReceiverKeys;
import Keys.JsonParsingKeys;
import Keys.MessageTypeKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import models.GroupMessage;
import models.OneOnOneMessage;

/**
 * Created by inscripts-236 on 31/8/17.
 */

public class OfflineMessagingService extends JobIntentService {
    private static final java.lang.String TAG =OfflineMessagingService.class.getSimpleName() ;
    private static final int JOB_ID = 1000;
    private List<OneOnOneMessage> unsendMessageList;
    private List<GroupMessage> unsendChatroomMessageList;
    private CometChat cometChat;

    public OfflineMessagingService() {
        super();
        cometChat=CometChat.getInstance(this);
    }


    public static void enqueueWork(Context context, Intent work){
        enqueueWork(context,OfflineMessagingService.class,JOB_ID,work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        PreferenceHelper.initialize(this);
        Logger.error(TAG,"is it running on main Thread ? "+(Looper.myLooper() == Looper.getMainLooper()));
        //unsendMessageList = OneOnOneMessage.getAllUnsendMessages();
        unsendMessageList = OneOnOneMessage.getAllUnsentMessages();
        for(Iterator iterator = unsendMessageList.iterator(); iterator.hasNext();){
            final OneOnOneMessage oneOnOneMessage = (OneOnOneMessage) iterator.next();
            if(oneOnOneMessage.retryCount > 0){
                oneOnOneMessage.retryCount = --oneOnOneMessage.retryCount;
                oneOnOneMessage.save();
            }else {
                oneOnOneMessage.messageStatus = 2;
                oneOnOneMessage.save();
                sendUpdateMessageDataBroadcast();
            }
            Logger.error("OneOnOneMessage : " + oneOnOneMessage);
            switch (oneOnOneMessage.type){
                case MessageTypeKeys.NORMAL_MESSAGE:
                    if(oneOnOneMessage.retryCount > 0){
                    sendTextMessage(oneOnOneMessage,null);
                    }
                    break;
                case MessageTypeKeys.STICKER:
                    if (oneOnOneMessage.retryCount > 0) {
                        sendSticker(oneOnOneMessage,null);
                    }
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    if (oneOnOneMessage.retryCount > 0) {
                        sendImageMessage(oneOnOneMessage,null);
                    }
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    if (oneOnOneMessage.retryCount > 0) {
                        sendVideoMessage(oneOnOneMessage,null);
                    }
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    if (oneOnOneMessage.retryCount > 0) {
                        sendAudioMessage(oneOnOneMessage,null);
                    }
                    break;
            }
        }
        //unsendChatroomMessageList = GroupMessage.getAllUnsendMessages();
        unsendChatroomMessageList = GroupMessage.getAllUnsentMessages();
        for(Iterator iterator = unsendChatroomMessageList.iterator();iterator.hasNext();){
            final GroupMessage groupMessage = (GroupMessage) iterator.next();
            if(groupMessage.retryCount > 0){
                groupMessage.retryCount = --groupMessage.retryCount;
                groupMessage.save();
            }else {
                groupMessage.messageStatus = 2;
                groupMessage.save();
                sendUpdateGroupMessageBroadcast();
            }
            Logger.error(TAG,"GroupMessage: "+groupMessage);
            switch (groupMessage.type){
                case MessageTypeKeys.NORMAL_MESSAGE:
                    sendTextMessage(null,groupMessage);
                    break;
                case MessageTypeKeys.STICKER:
                    sendSticker(null,groupMessage);
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    sendImageMessage(null,groupMessage);
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    sendVideoMessage(null,groupMessage);
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    sendAudioMessage(null,groupMessage);
                    break;
            }
        }
    }

    /*@Override
    protected void onHandleIntent(@Nullable Intent intent) {
        PreferenceHelper.initialize(this);
        Logger.error(TAG,"is it running on main Thread ? "+(Looper.myLooper() == Looper.getMainLooper()));
        //unsendMessageList = OneOnOneMessage.getAllUnsendMessages();
        unsendMessageList = OneOnOneMessage.getAllUnsentMessages();
        for(Iterator iterator = unsendMessageList.iterator(); iterator.hasNext();){
            final OneOnOneMessage oneOnOneMessage = (OneOnOneMessage) iterator.next();
            Logger.error("OneOnOneMessage : " + oneOnOneMessage);
            switch (oneOnOneMessage.type){
                case MessageTypeKeys.NORMAL_MESSAGE:
                    sendTextMessage(oneOnOneMessage,null);
                    break;
                case MessageTypeKeys.STICKER:
                    sendSticker(oneOnOneMessage,null);
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    sendImageMessage(oneOnOneMessage,null);
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    sendVideoMessage(oneOnOneMessage,null);
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    sendAudioMessage(oneOnOneMessage,null);
                    break;
            }
        }
        //unsendChatroomMessageList = GroupMessage.getAllUnsendMessages();
        unsendChatroomMessageList = GroupMessage.getAllUnsentMessages();
        for(Iterator iterator = unsendChatroomMessageList.iterator();iterator.hasNext();){
            final GroupMessage groupMessage = (GroupMessage) iterator.next();
            Logger.error(TAG,"GroupMessage: "+groupMessage);
            switch (groupMessage.type){
                case MessageTypeKeys.NORMAL_MESSAGE:
                    sendTextMessage(null,groupMessage);
                    break;
                case MessageTypeKeys.STICKER:
                    sendSticker(null,groupMessage);
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    sendImageMessage(null,groupMessage);
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    sendVideoMessage(null,groupMessage);
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    sendAudioMessage(null,groupMessage);
                    break;
            }
        }
    }*/

    private void sendAudioMessage(final OneOnOneMessage oneOnOneMessage, final GroupMessage groupMessage) {
        if(groupMessage==null){
            Logger.error(TAG,"Audio File: "+oneOnOneMessage.message);
            cometChat.sendAudioFile(oneOnOneMessage.getId(), new File(oneOnOneMessage.message), String.valueOf(oneOnOneMessage.toId), false, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAudioMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        OneOnOneMessage message = OneOnOneMessage.findByLocalId(localMessageId);
                        if (message != null) {
                            message.remoteId = id;
                            message.messageStatus = 1;
                            message.save();
                            unsendMessageList.remove(oneOnOneMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateMessageDataBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAudioMessage() : failCallback : "+jsonObject);
                }
            });
        }else {
            cometChat.sendAudioFile(groupMessage.getId(), new File(groupMessage.message), String.valueOf(groupMessage.chatroomId), true, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAudioMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        GroupMessage groupMess = GroupMessage.findByLocalId(String.valueOf(localMessageId));
                        Logger.error(TAG,"groupMess: "+groupMess);
                        if (groupMess != null) {
                            groupMess.remoteId = id;
                            groupMess.messageStatus = 1;
                            groupMess.save();
                            unsendMessageList.remove(groupMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateGroupMessageBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAudioMessage() : failCallback : "+jsonObject);
                }
            });
        }
    }

    private void sendVideoMessage(final OneOnOneMessage oneOnOneMessage, final GroupMessage groupMessage) {
        if(groupMessage==null){
            Logger.error(TAG,"File: "+new File(oneOnOneMessage.message));
            cometChat.sendVideo(oneOnOneMessage.getId(), new File(oneOnOneMessage.message), String.valueOf(oneOnOneMessage.toId), false, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendVideoMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        OneOnOneMessage message = OneOnOneMessage.findByLocalId(localMessageId);
                        if (message != null) {
                            message.remoteId = id;
                            message.messageStatus = 1;
                            message.save();
                            unsendMessageList.remove(oneOnOneMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateMessageDataBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendVideoMessage() : failCallback : "+jsonObject);
                }
            });
        }else {
            cometChat.sendVideo(groupMessage.getId(), new File(groupMessage.message), String.valueOf(groupMessage.chatroomId), true, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendVideoMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        GroupMessage groupMess = GroupMessage.findByLocalId(String.valueOf(localMessageId));
                        Logger.error(TAG,"groupMess: "+groupMess);
                        if (groupMess != null) {
                            groupMess.remoteId = id;
                            groupMess.messageStatus = 1;
                            groupMess.save();
                            unsendMessageList.remove(groupMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateGroupMessageBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendVideoMessage() : failCallback : "+jsonObject);
                }
            });
        }
    }

    private void sendImageMessage(final OneOnOneMessage oneOnOneMessage, final GroupMessage groupMessage) {
        if(groupMessage==null){
            Logger.error(TAG,"File: "+new File(oneOnOneMessage.message).exists());

            cometChat.sendImage(oneOnOneMessage.getId(), new File(oneOnOneMessage.message), String.valueOf(oneOnOneMessage.toId), false, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {

                    Logger.error(TAG,"sendImageMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        OneOnOneMessage message = OneOnOneMessage.findByLocalId(localMessageId);
                        if (message != null) {
                            message.remoteId = id;
                            message.messageStatus = 1;
                            message.save();
                            unsendMessageList.remove(oneOnOneMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateMessageDataBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendImageMessage() : failCallback : "+jsonObject);
                }
            });

        }else {
            cometChat.sendImage(groupMessage.getId(), new File(groupMessage.message), String.valueOf(groupMessage.chatroomId), true, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendImageMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        GroupMessage groupMess = GroupMessage.findByLocalId(String.valueOf(localMessageId));
                        Logger.error(TAG,"groupMess: "+groupMess);
                        if (groupMess != null) {
                            groupMess.remoteId = id;
                            groupMess.messageStatus = 1;
                            groupMess.save();
                            unsendMessageList.remove(groupMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateGroupMessageBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendImageMessage() : failCallback : "+jsonObject);
                }
            });
        }
    }


    private void sendTextMessage(final OneOnOneMessage oneOnOneMessage , final GroupMessage groupMessage){
        if(groupMessage == null){
            cometChat.sendMessage(oneOnOneMessage.getId(),String.valueOf(oneOnOneMessage.toId),oneOnOneMessage.message,false, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendTextMessage() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        Long timestamp = jsonObject.getLong(CometChatKeys.AjaxKeys.SENT);
                        OneOnOneMessage message = OneOnOneMessage.findByLocalId(localMessageId);
                        if (message != null) {
                            message.remoteId = id;
                            message.messageStatus = 1;
                            message.sentTimestamp = timestamp * 1000;
                            message.save();
                            unsendMessageList.remove(oneOnOneMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateMessageDataBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendTextMessage() : failCallback : "+jsonObject);
                }
            });
        }else{
            cometChat.sendMessage(groupMessage.getId(),String.valueOf(groupMessage.chatroomId),groupMessage.message, true, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendTextMessage() group: successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        GroupMessage groupMess = GroupMessage.findByLocalId(String.valueOf(localMessageId));
                        Logger.error(TAG,"groupMess: "+groupMess);
                        if (groupMess != null) {
                            groupMess.remoteId = id;
                            groupMess.messageStatus = 1;
                            groupMess.save();
                            unsendMessageList.remove(groupMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateGroupMessageBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendTextMessage() : failCallback : "+jsonObject);
                }
            });
        }
    }

    private void sendSticker(final OneOnOneMessage oneOnOneMessage, final GroupMessage groupMessage){
        if (groupMessage==null){
            cometChat.sendSticker(oneOnOneMessage.getId(), oneOnOneMessage.message, String.valueOf(oneOnOneMessage.toId), false, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendSticker() : successCallback : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        OneOnOneMessage message = OneOnOneMessage.findByLocalId(localMessageId);
                        if (message != null) {
                            message.remoteId = id;
                            message.messageStatus = 1;
                            message.save();
                            unsendMessageList.remove(oneOnOneMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateMessageDataBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendSticker() : successCallback : "+jsonObject);
                }
            });
        }else {
            cometChat.sendSticker(groupMessage.getId(), groupMessage.message, String.valueOf(groupMessage.chatroomId), true, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendSticker() : successCallback grp : "+jsonObject);
                    try {
                        long localMessageId = jsonObject.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                        Long id = jsonObject.getLong(CometChatKeys.AjaxKeys.ID);
                        GroupMessage message = GroupMessage.findByLocalId(String.valueOf(localMessageId));
                        if (message != null) {
                            message.remoteId = id;
                            message.messageStatus = 1;
                            message.save();
                            unsendMessageList.remove(groupMessage);
                            Logger.error(TAG,"unsendMessageList after removing: "+unsendMessageList.size());
                            if(unsendMessageList.size() == 0){
                                sendUpdateGroupMessageBroadcast();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendSticker() : successCallback : "+jsonObject);
                }
            });
        }
    }

    private void sendUpdateMessageDataBroadcast() {
        Logger.error(TAG,"Send Broadcast");
        Intent intent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
        intent.putExtra(BroadCastReceiverKeys.NEW_MESSAGE, 1);
        sendBroadcast(intent);
    }

    private void sendUpdateGroupMessageBroadcast() {
        Logger.error(TAG,"Send Broadcast grp");
        Intent intent = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
        intent.putExtra(BroadCastReceiverKeys.NEW_MESSAGE, 1);
        sendBroadcast(intent);
    }
}
