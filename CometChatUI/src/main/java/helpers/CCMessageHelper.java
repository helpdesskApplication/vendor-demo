package helpers;

import android.content.Intent;
import android.os.Looper;

import com.inscripts.custom.EmoticonUtils;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import Keys.BroadCastReceiverKeys;
import Keys.JsonParsingKeys;
import Keys.MessageTypeKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import models.Bot;
import models.Contact;
import models.Conversation;
import models.GroupMessage;
import models.Groups;
import models.OneOnOneMessage;
import videochat.CCIncomingCallActivity;

public class CCMessageHelper {

    private static String TAG = CCMessageHelper.class.getSimpleName();

    public static String CHATROOM_VIDEO = "1";
    public static String ONE_ON_ONE_VIDEO = "0";
    private static CometChat cometChat;
    private static String translation = null;
    private static long initiatorID;

    private static FeatureState rttState;
    private static FeatureState groupChatState;

    public static void processOneOnOneMessage(JSONObject messagejson) {
        Logger.error(TAG, "Process One-On-One = " + messagejson);
        Logger.error(TAG,"is it running on main Thread ? "+(Looper.myLooper() == Looper.getMainLooper()));
        try {
            cometChat = CometChat.getInstance(PreferenceHelper.getContext());
            SessionData sessionData = SessionData.getInstance();
            String type = String.valueOf(messagejson.getInt(JsonParsingKeys.MESSAGE_TYPE));
            final long remoteid = messagejson.getLong(JsonParsingKeys.ID);
            long fromid;
            if (messagejson.get(JsonParsingKeys.FROM) instanceof String) {
                fromid = Long.parseLong(messagejson.getString(JsonParsingKeys.FROM));
            } else {
                fromid = messagejson.getLong(JsonParsingKeys.FROM);
            }
            final String message = messagejson.getString(JsonParsingKeys.MESSAGE);
            int self = messagejson.getInt(JsonParsingKeys.SELF);
            int old = messagejson.getInt(JsonParsingKeys.OLD);
            long toid = -1;
            boolean isNew = false;

            if (messagejson.has("to")) {
                toid = messagejson.getLong("to");
            }
            long timestamp = messagejson.getLong(JsonParsingKeys.SENT_TIMESTAMP);
            Logger.error(TAG, "Timestamp value = " + timestamp);
            int length = String.valueOf(timestamp).length();
            if (length == 10) {
                timestamp = timestamp * 1000;
            }
            OneOnOneMessage onOneMessage;
            if(messagejson.has("localmessageid")){
                onOneMessage = OneOnOneMessage.findByLocalId(messagejson.getString("localmessageid"));
                if(onOneMessage == null){
                    isNew = true;
                }else{
                    onOneMessage.remoteId = remoteid;
                    onOneMessage.sentTimestamp = timestamp;
                    onOneMessage.save();

                    /*Intent iintent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                    iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                    PreferenceHelper.getContext().sendBroadcast(iintent);*/
                    isNew = false;
                }
            }else {
                onOneMessage = OneOnOneMessage.findById(remoteid);
                if(onOneMessage == null)
                    isNew = true;
                else
                    isNew = false;
            }

            Logger.error(TAG, "isNew = " + isNew);

            if (isNew) {
                Contact contact = Contact.getContactDetails(fromid);
                if (contact != null) {
                    contact.unreadCount = contact.unreadCount + 1;
                    contact.save();
                    if (!(boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.RECENT_CHAT_ENABLED))) {
                        Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_CONTACT_LIST_KEY, 1);
                        PreferenceHelper.getContext().sendBroadcast(iintent);
                    }
                }
                Logger.error(TAG, "type = " + type);
                String url;
                int startIndex;
                String fileName;
                switch (type) {
                    case MessageTypeKeys.NORMAL_MESSAGE:

                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        onOneMessage.toId = toid;
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.sentTimestamp = timestamp;
                        setTextMessageType(onOneMessage);
                        onOneMessage.messageStatus = 1;

                        rttState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.REAL_TIME_TRANSLATION));
                        Logger.error(TAG,"RTT : " + rttState);
                        translation = message;
                        if((rttState == FeatureState.ACCESSIBLE) && (boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.NEW_LICENCE))
                                && PreferenceHelper.contains(PreferenceKeys.DataKeys.SELECTED_LANGUAGE) && self == 0){
                            Logger.error(TAG,"Real Time Translate is enable");

                            CommonUtils.translateMessage(message, new VolleyAjaxCallbacks() {
                                @Override
                                public void successCallback(String s) {
                                    Logger.error(TAG,"translateMessage: successCallback: "+s);
                                    translation = s+" ("+message+")";
                                    Logger.error(TAG,"translation : "+translation);

                                    OneOnOneMessage oneOnOneMessage = OneOnOneMessage.findById(remoteid);
                                    oneOnOneMessage.message = translation;
                                    Logger.error(TAG, "Process One-On-One 2 = " + oneOnOneMessage.message);
                                    oneOnOneMessage.save();
                                    Intent iintent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                                    iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                    PreferenceHelper.getContext().sendBroadcast(iintent);
                                }

                                @Override
                                public void failCallback(String s, boolean b) {
                                    Logger.error(TAG,"translateMessage: failCallback: "+s+" >>>"+b);
                                }
                            });
                        }
                        onOneMessage.message = message;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.IMAGE_MESSAGE:
                        Logger.error(TAG, "Image Message Value " + message);
//                        if (message.contains("/plugins/filetransfer/")) {
                            onOneMessage = new OneOnOneMessage();
                            onOneMessage.remoteId = remoteid;
                            onOneMessage.fromId = fromid;
                            onOneMessage.toId = toid;
                            onOneMessage.self = self;
                            onOneMessage.message = message;
                            onOneMessage.imageUrl = message;
                            onOneMessage.read = old;
                            onOneMessage.sentTimestamp = timestamp;
                            onOneMessage.type = MessageTypeKeys.IMAGE_DOWNLOADING;
                            onOneMessage.messageStatus = 1;
                            onOneMessage.save();


                            url = onOneMessage.imageUrl;

                            startIndex = url.indexOf(StaticMembers.FILE_PREFIX) + StaticMembers.FILE_PREFIX.length();
                            try {
                                fileName = URLEncoder.encode((url.substring(startIndex, url.length())), StaticMembers.UTF_8);
                            } catch (UnsupportedEncodingException e1) {
                                fileName = url.substring(startIndex, url.length());
                                e1.printStackTrace();
                            }

                            FileStorageHelper.saveIncomingImage(fileName, url, null, false, String.valueOf(remoteid), false);
//                        }

                        break;

                    case MessageTypeKeys.VIDEO_MESSAGE:
                        Logger.error(TAG, "Video Message Value " + message);
//                        if (message.contains("/plugins/filetransfer/")) {
                            onOneMessage = new OneOnOneMessage();
                            onOneMessage.remoteId = remoteid;
                            onOneMessage.fromId = fromid;
                            onOneMessage.toId = toid;
                            onOneMessage.self = self;
                            onOneMessage.message = message;
                            onOneMessage.imageUrl = message;
                            onOneMessage.read = old;
                            onOneMessage.sentTimestamp = timestamp;
                            onOneMessage.type = MessageTypeKeys.VIDEO_DOWNLOADING;
                            onOneMessage.messageStatus = 1;
                            onOneMessage.save();

                            new FileDownloadHelper().execute(String.valueOf(remoteid), message, ONE_ON_ONE_VIDEO, "1", "0");
//                        }
                        break;

                    case MessageTypeKeys.AUDIO_MESSAGE:
//                        if (message.contains("/plugins/filetransfer/")) {
                            onOneMessage = new OneOnOneMessage();
                            onOneMessage.remoteId = remoteid;
                            onOneMessage.fromId = fromid;
                            onOneMessage.toId = toid;
                            onOneMessage.self = self;
                            onOneMessage.read = old;
                            onOneMessage.message = message;
                            onOneMessage.sentTimestamp = timestamp;
                            onOneMessage.type = MessageTypeKeys.AUDIO_DOWNLOADING;
                            onOneMessage.messageStatus = 1;
                            onOneMessage.save();

                            new FileDownloadHelper().execute(String.valueOf(remoteid), message, ONE_ON_ONE_VIDEO, "0", "0");
//                        }
                        break;

                    case MessageTypeKeys.STICKER:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        onOneMessage.toId = toid;
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.STICKER;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.HANDWRITE_MESSAGE:
                        Logger.error(TAG,"Handwrite message: "+message);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.read = old;
                        onOneMessage.type = MessageTypeKeys.HANDWRITE_DOWNLOADING;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        url = onOneMessage.imageUrl;

                        startIndex = url.indexOf(StaticMembers.FILE_PREFIX) + StaticMembers.FILE_PREFIX.length();
                        try {
                            fileName = URLEncoder.encode((url.substring(startIndex, url.length())), StaticMembers.UTF_8);
                        } catch (UnsupportedEncodingException e1) {
                            fileName = url.substring(startIndex, url.length());
                            e1.printStackTrace();
                        }
                        if(fromid == SessionData.getInstance().getId()){
                            Contact contact1 = Contact.getContactDetails(toid);
                            if(contact1 != null){
                                CCMessageHelper.addSingleChatConversation(contact1, onOneMessage, false);
                            }
                        }

                        FileStorageHelper.saveIncomingImage(fileName, url, null, false, String.valueOf(remoteid), true);
                        break;

                    case MessageTypeKeys.WHITEBOARD_MESSAGE:

                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.WHITEBOARD_MESSAGE;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.WRITEBOARD_MESSAGE:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.WRITEBOARD_MESSAGE;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.AVBROADCAST_REQUEST_SENT:

                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        onOneMessage.toId = toid;
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.NORMAL_MESSAGE;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();


                        break;

                    case MessageTypeKeys.AVCHAT_INCOMING_CALL:

                        Logger.error(TAG, "AVChat incoming call = " + messagejson);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "Call from " + Contact.getContactDetails(fromid).name;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_INCOMING_CALL;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();
                        Logger.error(TAG,"CallSession ONGOING = "+PreferenceHelper.get(BroadCastReceiverKeys.AvchatKeys.CALL_SESSION_ONGOING));
                        if(PreferenceHelper.get(BroadCastReceiverKeys.AvchatKeys.CALL_SESSION_ONGOING).equals("1")){
                            cometChat.sendBusyTone(String.valueOf(fromid), new Callbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"Send Bussy tome success = "+jsonObject);
                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"Send Bussy tome fail = "+jsonObject);
                                }
                            });
                        }else {
                            if ((System.currentTimeMillis() - timestamp) < 10000) {
                                Intent intent = new Intent(PreferenceHelper.getContext(), CCIncomingCallActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, messagejson.getString("callid"));
                                intent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(fromid));
                                PreferenceHelper.getContext().startActivity(intent);
                            }
                        }
                        break;

                    case MessageTypeKeys.AUDIO_CHAT_INCOMING_CALL:
                        Logger.error(TAG, "Audio Chat incoming call = " + messagejson);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "Call from " + Contact.getContactDetails(fromid).name;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_INCOMING_CALL;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.save();
                        if(PreferenceHelper.get(BroadCastReceiverKeys.AvchatKeys.CALL_SESSION_ONGOING).equals("1")){
                            cometChat.sendBusyTone(String.valueOf(fromid), new Callbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"Send Bussy tome success = "+jsonObject);
                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"Send Bussy tome fail = "+jsonObject);
                                }
                            });
                        }else {
                            if ((System.currentTimeMillis() - timestamp) < 10000) {
                                Intent intent = new Intent(PreferenceHelper.getContext(), CCIncomingCallActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, messagejson.getString("callid"));
                                intent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(fromid));
                                intent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, true);
                                PreferenceHelper.getContext().startActivity(intent);
                            }
                        }
                        break;

                    case MessageTypeKeys.AVCHAT_INCOMING_CALL_END:
                        Logger.error(TAG,"AV incomming call end called");
                        try {
                            onOneMessage = new OneOnOneMessage();
                            onOneMessage.remoteId = remoteid;
                            onOneMessage.fromId = fromid;
                            if (toid == -1) {
                                onOneMessage.toId = sessionData.getId();
                            } else {
                                onOneMessage.toId = toid;
                            }
                            onOneMessage.self = self;
                            if (self == 0) {
                                onOneMessage.message = "Call ended from " + Contact.getContactDetails(fromid).name;
                            } else {
                                onOneMessage.message = "This call has been ended";
                            }

                            onOneMessage.read = old;
                            onOneMessage.imageUrl = message;
                            onOneMessage.sentTimestamp = timestamp;
                            onOneMessage.type = MessageTypeKeys.AVCHAT_INCOMING_CALL_END;
                            onOneMessage.messageStatus = 1;
                            onOneMessage.save();
                        } catch(Exception ex) {
                            Logger.error(TAG, "AVCHAT_INCOMING_CALL_END exception : " + ex.toString());
                            ex.printStackTrace();
                        }


                        //close audio-video chat activity
                        if(self == 0){
                            Intent closeIntent1 = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                            closeIntent1.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALL_CANCEL, "1");
                            closeIntent1.putExtra("from", fromid);
                            PreferenceHelper.getContext().sendBroadcast(closeIntent1);
                            Logger.error(TAG, "AVChat incoming call end1 = broadcast sent");
                        }
                        break;

                    case MessageTypeKeys.AVCHAT_OUTGOING_CALL_CANCEL:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        if (self == 0) {
                            onOneMessage.message = "Call cancelled from " + Contact.getContactDetails(fromid).name;
                        } else {
                            onOneMessage.message = "This call has been cancelled";
                        }

                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_INCOMING_CALL_END;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        //close audio-video chat activity
                        Intent closeIntentAVCallCacel = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                        closeIntentAVCallCacel.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY, "1");
                        closeIntentAVCallCacel.putExtra("from", fromid);
                        PreferenceHelper.getContext().sendBroadcast(closeIntentAVCallCacel);
                        break;

                    case MessageTypeKeys.AVCHAT_OUTGOING_CALL_END:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        if (self == 0) {
                            onOneMessage.message = "Call ended from " + Contact.getContactDetails(fromid).name;
                        } else {
                            onOneMessage.message = "This call has been ended";
                        }
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_INCOMING_CALL_END;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        //close audio-video chat activity
                        Intent closeIntentAVCallEnd = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                        closeIntentAVCallEnd.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY, "1");
                        closeIntentAVCallEnd.putExtra("from", fromid);
                        PreferenceHelper.getContext().sendBroadcast(closeIntentAVCallEnd);
                        break;

                    case MessageTypeKeys.AVCHAT_INCOMING_CALL_CONNECTED_END:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        if (self == 0) {
                            onOneMessage.message = "Call ended from " + Contact.getContactDetails(fromid).name;
                        } else {
                            onOneMessage.message = "This call has been ended";
                        }
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_INCOMING_CALL_END;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        //close audio-video chat activity
                        Intent closeIntent2 = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                        closeIntent2.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALL_END, "1");
                        closeIntent2.putExtra("from", fromid);
                        PreferenceHelper.getContext().sendBroadcast(closeIntent2);
                        Logger.error(TAG, "AVChat incoming call end2 = broadcast sent");
                        break;

                    case MessageTypeKeys.AVCHAT_CALL_ACCEPTED:

                        Logger.error(TAG, "AV Chat Call accepted = " + messagejson.getString("callid"));
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "Call to " + Contact.getContactDetails(fromid).name;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_CALL_ACCEPTED;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        Intent broadcastIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.EVENT_AVCHAT_ACCEPTED);
                        broadcastIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALLER_ID, String.valueOf(fromid));
                        broadcastIntent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, messagejson.getString("callid"));
                        PreferenceHelper.getContext().sendBroadcast(broadcastIntent);
                        break;

                    case MessageTypeKeys.AVBROADCAST_REQUEST:
                        Logger.error(TAG, "AV broadcast " + message);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "AV Broadcast request from " + Contact.getContactDetails(fromid).name;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = messagejson.getString("callid");
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVBROADCAST_REQUEST;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.AVBROADCAST_INVITE:
                        Logger.error(TAG, "AV broadcast invite = " + messagejson);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid == -1) {
                            onOneMessage.toId = sessionData.getId();
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "Has sent you audio-video broadcast request.";
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = messagejson.getString("callid");
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVBROADCAST_REQUEST;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.AVBROADCAST_END:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid != -1) {
                            onOneMessage.toId = toid;
                        } else {
                            onOneMessage.toId = sessionData.getId();
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "This broadcast has ended.";
                        onOneMessage.read = old;
                        //onOneMessage.imageUrl = messagejson.getString("callid");
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVBROADCAST_END;
                        onOneMessage.messageStatus = 1;
//                        onOneMessage.save();

                        Logger.error(TAG, "from id : " + fromid + " toid : " + toid + " sessionData.getId() : " + sessionData.getId());
                        OneOnOneMessage.updateBroadcastMessage(String.valueOf(fromid));

                        //close video chat activity
                        Intent closeIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                        closeIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY, "1");
                        closeIntent.putExtra("from", fromid);
                        PreferenceHelper.getContext().sendBroadcast(closeIntent);
                        break;

                    case MessageTypeKeys.GROUP_INVITE:
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid != -1) {
                            onOneMessage.toId = toid;
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = message;
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.GROUP_INVITE;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;

                    case MessageTypeKeys.SCREENSHARE_MESSAGE:
                        Logger.error(TAG,"Screenshare message value = "+messagejson);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid != -1) {
                            onOneMessage.toId = toid;
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = message;
                        onOneMessage.read = old;
                        onOneMessage.imageUrl = messagejson.getString("callid");
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.SCREENSHARE_MESSAGE;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        break;


                    case MessageTypeKeys.AVCHAT_BUSY_CALL:
                        Logger.error(TAG,"AVCHAT_BUSY_CALL = "+messagejson);
                        onOneMessage = new OneOnOneMessage();
                        onOneMessage.remoteId = remoteid;
                        onOneMessage.fromId = fromid;
                        if (toid != -1) {
                            onOneMessage.toId = toid;
                        } else {
                            onOneMessage.toId = toid;
                        }
                        onOneMessage.self = self;
                        onOneMessage.message = "The user is busy right now.";
                        onOneMessage.read = old;
//                        onOneMessage.imageUrl = messagejson.getString("callid");
                        onOneMessage.sentTimestamp = timestamp;
                        onOneMessage.type = MessageTypeKeys.AVCHAT_BUSY_CALL;
                        onOneMessage.messageStatus = 1;
                        onOneMessage.save();

                        if(self == 0){
                            Intent closeIntent1 = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                            closeIntent1.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY, "1");
                            closeIntent1.putExtra("from", fromid);
                            PreferenceHelper.getContext().sendBroadcast(closeIntent1);
                            Logger.error(TAG, "AVChat busy call end1 = broadcast sent");
                        }

                        break;



                    case MessageTypeKeys.BOT_RESPONSE:

                        List<Bot> botList = Bot.getAllbots();
                        Logger.error("Bot list size "+botList.size());

                        if (botList.size() == 0) {
                            cometChat.getAllBots(new Callbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {
                                    Logger.errorLong(TAG,"getAllBots(): successCallback: "+jsonObject);
                                    try {
                                        if(jsonObject.has(CometChatKeys.AjaxKeys.BOT_LIST) && jsonObject.get(CometChatKeys.AjaxKeys.BOT_LIST) instanceof JSONObject){
                                            Bot.updateAllBots(jsonObject.getJSONObject(CometChatKeys.AjaxKeys.BOT_LIST));

                                        }else {
                                            Logger.error(TAG,"delete All Bots");
                                            Bot.deleteAll(Bot.class);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"getAllBots(): failCallback: "+jsonObject);
                                }
                            });
                        }
                            onOneMessage = new OneOnOneMessage();
                            onOneMessage.remoteId = remoteid;
                            onOneMessage.fromId = fromid;
                            onOneMessage.toId = toid;
                            onOneMessage.self = 0;
                            onOneMessage.message = message;
                            onOneMessage.read = old;
                            onOneMessage.sentTimestamp = timestamp;
                            onOneMessage.type = MessageTypeKeys.BOT_RESPONSE;
                            onOneMessage.messageStatus = 1;
                            onOneMessage.save();




                        break;

                    case MessageTypeKeys.FILE_MESSAGE:
                        if (message.contains("/plugins/filetransfer/")) {
                            onOneMessage = new OneOnOneMessage();
                            onOneMessage.remoteId = remoteid;
                            onOneMessage.fromId = fromid;
                            onOneMessage.toId = toid;
                            onOneMessage.self = self;
                            onOneMessage.read = old;
                            onOneMessage.message = message;
                            onOneMessage.sentTimestamp = timestamp;
                            onOneMessage.type = MessageTypeKeys.FILE_MESSAGE;
                            onOneMessage.messageStatus = 1;
                            onOneMessage.save();
                        }

                }
            }

            if(onOneMessage!= null){
                if(cometChat.isMessageinPendingDeliveredList(String.valueOf(remoteid))){
                    onOneMessage.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                    onOneMessage.save();
                }
            }

            if (onOneMessage != null) {
                long contactId;
                if(fromid == SessionData.getInstance().getId() ){
                    contactId = toid;
                }else {
                    contactId = fromid;
                }

                final Contact contact = Contact.getContactDetails(contactId);
                final CometChat cometChat = CometChat.getInstance(PreferenceHelper.getContext());
                if (contact == null) {
                    final OneOnOneMessage finalOnOneMessage = onOneMessage;
                    final boolean finalIsNew = isNew;
                    final OneOnOneMessage finalOnOneMessage1 = onOneMessage;
                    cometChat.getUserInfo(String.valueOf(contactId), new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Contact contactnew = Contact.insertNewBuddy(jsonObject);
                            addSingleChatConversation(contactnew, finalOnOneMessage, finalIsNew);
                            cometChat.sendDeliverdReceipt(String.valueOf(remoteid), contactnew.cometid, new Callbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {
                                    Logger.error(TAG, "sendDeliverdReceipt success = " + jsonObject);
                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {
                                    Logger.error(TAG, "sendDeliverdReceipt success = " + jsonObject);
                                }
                            });
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "User info fail = " + jsonObject);
                        }
                    });

                } else {
                    addSingleChatConversation(contact, onOneMessage, isNew);
                    cometChat.sendDeliverdReceipt(String.valueOf(remoteid), contact.cometid, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "sendDeliverdReceipt success = " + jsonObject);
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "sendDeliverdReceipt success = " + jsonObject);
                        }
                    });
                }

            }


        } catch (JSONException e) {
            Logger.error(TAG, " e2 = " + e.toString());
            e.printStackTrace();
        }
    }


    public static void processGroupMessage(JSONObject messagejson) {
        Logger.error(TAG, "Group MessageJson = " + messagejson);
        cometChat = CometChat.getInstance(PreferenceHelper.getContext());
        try {
            SessionData sessionData = SessionData.getInstance();
            String type;
            try {
                type = String.valueOf(messagejson.getInt(JsonParsingKeys.MESSAGE_TYPE));
            }catch (JSONException e) {
                type = "10";
            }
            final long remoteId = messagejson.getLong(JsonParsingKeys.ID);
            long fromId = messagejson.getLong(JsonParsingKeys.FROM_ID);
            String senderName = messagejson.getString(JsonParsingKeys.FROM);
            long chatroomId = messagejson.getLong(JsonParsingKeys.CHATROOM_ID);
            final String message = messagejson.getString(JsonParsingKeys.MESSAGE);
            String textColor = "";
            if (messagejson.has(JsonParsingKeys.TEXT_COLOR))
                textColor = messagejson.getString(JsonParsingKeys.TEXT_COLOR);
            boolean isNew = false;

            long timestamp = messagejson.getLong(JsonParsingKeys.SENT_TIMESTAMP);
            long toid = -1;

            int length = String.valueOf(timestamp).length();
            if (length == 10) {
                timestamp = timestamp * 1000;
            }
            GroupMessage groupMessage = GroupMessage.findById(remoteId);
            Logger.error(TAG, "Remote id = " + remoteId + " && Message Type = " + type + " && group message value = " + groupMessage);
            Logger.error(TAG, "groupMessage: " + groupMessage);

            if(messagejson.has("localmessageid")){
                groupMessage = GroupMessage.findByLocalId(messagejson.getString("localmessageid"));
                if(groupMessage == null){
                    isNew = true;
                }else{
                    groupMessage.remoteId = remoteId;
                    groupMessage.sentTimestamp = timestamp;
                    groupMessage.save();
                    isNew = false;
                }
            }else{
                groupMessage = GroupMessage.findById(remoteId);
                if(groupMessage == null){
                    isNew = true;
                }else {
                    isNew = false;
                }
            }

            Groups groups1 = Groups.getGroupDetails(chatroomId);
            if (groups1 != null && isNew) {
                groups1.unreadCount = groups1.unreadCount + 1;
                Logger.error(TAG, "group unreadCount : " + groups1.unreadCount);
                groups1.save();
                groupChatState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GROUP_CHAT_ENABLED));
                if (groupChatState == FeatureState.INACCESSIBLE || groupChatState == FeatureState.INVISIBLE) {
                    Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                    iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_CONTACT_LIST_KEY, 1);
                    PreferenceHelper.getContext().sendBroadcast(iintent);
                }
            }

            if (isNew) {
                switch (type) {
                    case MessageTypeKeys.NORMAL_MESSAGE:
                        //if (fromId != sessionData.getId()) {

                        Logger.error(TAG,"Real time translation = "+cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.REAL_TIME_TRANSLATION)));
                        Logger.error(TAG,"new license = "+cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.NEW_LICENCE)));
                        Logger.error(TAG,"Selected language = "+PreferenceHelper.contains(PreferenceKeys.DataKeys.SELECTED_LANGUAGE));
                        Logger.error(TAG,"From id = "+fromId);
                        Logger.error(TAG,"Session id = "+SessionData.getInstance().getId());

                            groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                    timestamp, senderName, MessageTypeKeys.NORMAL_MESSAGE, "",
                                    textColor, 0, 1);

                            rttState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.REAL_TIME_TRANSLATION));
                            if((rttState == FeatureState.ACCESSIBLE)
                                    && (boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.NEW_LICENCE))
                                    && PreferenceHelper.contains(PreferenceKeys.DataKeys.SELECTED_LANGUAGE) && fromId != SessionData.getInstance().getId()){
                                Logger.error(TAG,"Real Time Translate is enable");

                                CommonUtils.translateMessage(message, new VolleyAjaxCallbacks() {
                                    @Override
                                    public void successCallback(String s) {
                                        Logger.error(TAG,"translateMessage: successCallback: "+s);
                                        translation = s+" ("+message+")";
                                        Logger.error(TAG,"translation : "+translation);

                                        GroupMessage groupMessage = GroupMessage.findById(remoteId);
                                        groupMessage.message = translation;
                                        Logger.error(TAG, "Process group message 2 = " + groupMessage.message);
                                        groupMessage.save();
                                        Intent iintent = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                                        iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                        PreferenceHelper.getContext().sendBroadcast(iintent);
                                    }

                                    @Override
                                    public void failCallback(String s, boolean b) {
                                        Logger.error(TAG,"translateMessage: failCallback: "+s+" >>>"+b);
                                    }
                                });
                            }
                            groupMessage.save();

                        Logger.error(TAG,"Group Message 2 = "+groupMessage);

                       // }
                        break;

                    case MessageTypeKeys.IMAGE_MESSAGE:
                        Logger.error(TAG, "Image Message Value " + message);
//                        if (message.contains("/plugins/filetransfer/")) {

                                groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                        timestamp, senderName, MessageTypeKeys.IMAGE_DOWNLOADING,message,
                                        textColor, 0, 1);


                                groupMessage.save();


                                String url = message;
                                int startIndex = url.indexOf(StaticMembers.FILE_PREFIX) + StaticMembers.FILE_PREFIX.length();
                                String fileName;
                                try {
                                    fileName = URLEncoder.encode((url.substring(startIndex, url.length())), StaticMembers.UTF_8);
                                } catch (UnsupportedEncodingException e1) {
                                    fileName = url.substring(startIndex, url.length());
                                    e1.printStackTrace();
                                }

                                FileStorageHelper.saveIncomingImage(fileName, url, null, true, String.valueOf(remoteId), false);

//                        }

                        break;

                    case MessageTypeKeys.VIDEO_MESSAGE:
                        Logger.error(TAG, "Video Message Value " + message);
//                        if (message.contains("/plugins/filetransfer/")) {
                            if (fromId != sessionData.getId()) {
                                groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                        timestamp, senderName, MessageTypeKeys.VIDEO_DOWNLOADING, "",
                                        textColor, 0, 1);

                                groupMessage.save();

                                new FileDownloadHelper().execute(String.valueOf(remoteId), message, CHATROOM_VIDEO, "1", "0");
                            }
//                        }
                        break;

                    case MessageTypeKeys.AUDIO_MESSAGE:
                        Logger.error(TAG, "Audio Message Value " + message);
//                        if (message.contains("/plugins/filetransfer/")) {
                            if (fromId != sessionData.getId()) {
                                groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                        timestamp, senderName, MessageTypeKeys.AUDIO_DOWNLOADING, "",
                                        textColor, 0, 1);

                                groupMessage.save();

                                new FileDownloadHelper().execute(String.valueOf(remoteId), message, CHATROOM_VIDEO, "0", "0");
                            }
//                        }
                        break;

                    case MessageTypeKeys.STICKER:
                        Logger.error(TAG, "Sticker Message Value = " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.STICKER, "",
                                textColor, 0, 1);

                        groupMessage.save();
                        break;

                    case MessageTypeKeys.HANDWRITE_MESSAGE:
                        Logger.error(TAG, "Handwrite Message Value = " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.HANDWRITE_DOWNLOADING, message,
                                textColor, 0, 1);

                        groupMessage.save();
                        url = message;

                        startIndex = url.indexOf(StaticMembers.FILE_PREFIX) + StaticMembers.FILE_PREFIX.length();
                        try {
                            fileName = URLEncoder.encode((url.substring(startIndex, url.length())), StaticMembers.UTF_8);
                        } catch (UnsupportedEncodingException e1) {
                            fileName = url.substring(startIndex, url.length());
                            e1.printStackTrace();
                        }

                        FileStorageHelper.saveIncomingImage(fileName, url, null, true, String.valueOf(remoteId), true);
                        break;

                    case MessageTypeKeys.WRITEBOARD_MESSAGE:
                        Logger.error(TAG, "WRITEBOARD MESSAGE");
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message, timestamp,
                                senderName, MessageTypeKeys.WRITEBOARD_MESSAGE, "", textColor, 0, 1);
                        groupMessage.save();
                        break;

                    case MessageTypeKeys.AVCHAT_INCOMING_CALL:
                        initiatorID = fromId;
                        Logger.error(TAG, "avchat message value = " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.AVCHAT_INCOMING_CALL, messagejson.getString("call_id"),
                                textColor, 0, 1);

                        groupMessage.save();

                        break;

                    case MessageTypeKeys.AUDIOCHAT_INCOMING_CALL:
                        initiatorID = fromId;
                        Logger.error(TAG, "audio message value = " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.AUDIOCHAT_INCOMING_CALL, messagejson.getString("call_id"),
                                textColor, 0, 1);

                        groupMessage.save();
                        break;

                    case MessageTypeKeys.GRP_AVBROADCAST_REQUEST:
                        Logger.error(TAG, "Group AVBROADCAST = " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.GRP_AVBROADCAST_REQUEST, messagejson.getString("call_id"),
                                textColor, 0, 1);

                        groupMessage.save();
                        break;

                    case MessageTypeKeys.AVBROADCAST_END:
                        Logger.error(TAG, "Group AVBROADCAST_END = " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, "This broadcast has ended.",
                                timestamp, senderName, MessageTypeKeys.AVBROADCAST_END, "",
                                textColor, 0, 1);

//                        groupMessage.save();

                        GroupMessage.updateBroadcastMessage(String.valueOf(chatroomId));

                        //close video chat activity
                        Intent closeIntent = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                        closeIntent.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY, "1");
                        closeIntent.putExtra("from", fromId);
                        closeIntent.putExtra("groupID", chatroomId);
                        PreferenceHelper.getContext().sendBroadcast(closeIntent);
                        break;
                    case MessageTypeKeys.WHITEBOARD_MESSAGE:
                        Logger.error(TAG, "Whiteboard message value : " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.WHITEBOARD_MESSAGE, "", textColor, 0, 1);
                        groupMessage.save();
                        break;

                    case MessageTypeKeys.SCREENSHARE_MESSAGE:
                        Logger.error(TAG, "Screenshare message value : " + message);
                        groupMessage = new GroupMessage(remoteId, fromId, chatroomId, message,
                                timestamp, senderName, MessageTypeKeys.SCREENSHARE_MESSAGE,messagejson.getString("callid") , textColor, 0, 1);
                        groupMessage.save();
                        break;

                    case MessageTypeKeys.BOT_RESPONSE:
                        Logger.error(TAG,"Bot response : "+message);
                        List<Bot> botList = Bot.getAllbots();
                        Logger.error("Bot list size "+botList.size());
                        if (botList.size() == 0) {
                            cometChat.getAllBots(new Callbacks() {
                                @Override
                                public void successCallback(JSONObject jsonObject) {
                                    Logger.errorLong(TAG,"getAllBots(): successCallback: "+jsonObject);
                                    try {
                                        if(jsonObject.has(CometChatKeys.AjaxKeys.BOT_LIST) && jsonObject.get(CometChatKeys.AjaxKeys.BOT_LIST) instanceof JSONObject){
                                            Bot.updateAllBots(jsonObject.getJSONObject(CometChatKeys.AjaxKeys.BOT_LIST));

                                        }else {
                                            Logger.error(TAG,"delete All Bots");
                                            Bot.deleteAll(Bot.class);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void failCallback(JSONObject jsonObject) {
                                    Logger.error(TAG,"getAllBots(): failCallback: "+jsonObject);
                                }
                            });
                        }
                        groupMessage = new GroupMessage(remoteId,fromId,chatroomId,message,timestamp,senderName,MessageTypeKeys.BOT_RESPONSE,message,textColor,0,1);
                        groupMessage.save();
                        break;

                    case MessageTypeKeys.AVCHAT_INCOMING_CALL_CONNECTED_END:
                        Logger.error(TAG,"Group AV chat end message: "+message);
                        Logger.error(TAG,"from: "+fromId);
                        Logger.error(TAG,"initiator id: "+initiatorID);
                        Logger.error(TAG,"groupID: "+chatroomId);
                        if (initiatorID == fromId) {
                            Logger.error(TAG,"conference ended by initiator");
                            groupMessage = new GroupMessage(remoteId, fromId, chatroomId, "This call has been ended",timestamp,
                                    senderName, MessageTypeKeys.AVCHAT_INCOMING_CALL_CONNECTED_END, message,textColor, 1, 1);
                            groupMessage.save();
                        }

                        Intent closeIntent2 = new Intent(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CLOSE_ACTIVITY);
                        closeIntent2.putExtra(BroadCastReceiverKeys.AvchatKeys.AVCHAT_CALL_END, "1");
                        closeIntent2.putExtra("groupID", chatroomId);
                        closeIntent2.putExtra("from", fromId);
                        PreferenceHelper.getContext().sendBroadcast(closeIntent2);
                        Logger.error(TAG, "AVChat incoming call end2 = broadcast sent");
                        break;

                    case MessageTypeKeys.FILE_MESSAGE:
                        Logger.error(TAG,"File Message : "+message);
                        groupMessage = new GroupMessage(remoteId,fromId,chatroomId,message,timestamp,senderName,MessageTypeKeys.FILE_MESSAGE,message,textColor,0,1);
                        groupMessage.save();
                        break;
                }
                Logger.error(TAG, "fromId = " + fromId);
                Logger.error(TAG, "SessionData.getInstance().getId() : "+SessionData.getInstance().getId());
                if (fromId != SessionData.getInstance().getId()) {
                    Groups groups = Groups.getGroupDetails(chatroomId);
                    if (null != groupMessage) {
                        Logger.error(TAG,"message to save: "+groupMessage.message);
                        addGroupConversation(groups, groupMessage);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public static void addSingleChatConversation(Contact contact, OneOnOneMessage message, boolean isnew) {
        if (Conversation.isNewBuddyConversation(String.valueOf(contact.contactId))) {
            Logger.error(TAG, "addSingleChatConversation : Is New Conversation for name = "+contact.name);
            Conversation conversation = new Conversation();
            conversation.buddyID = contact.contactId;
            conversation.timestamp = message.sentTimestamp;
            conversation.lstMessage = processLastMessageType(message.message, message.type);
            conversation.name = contact.name;
            conversation.avtarUrl = contact.avatarURL;
            String contactID = "-1";
            if (PreferenceHelper.get(JsonParsingKeys.WINDOW_ID) != null) {
                contactID = PreferenceHelper.get(JsonParsingKeys.WINDOW_ID);
            }
            if (isnew && !contactID.equals(String.valueOf(contact.contactId))) {
                conversation.unreadCount = conversation.unreadCount + 1;
            }
            conversation.save();
        } else {
            Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(contact.contactId));
            Logger.error(TAG, "addSingleChatConversation : Update Conversation for name = "+contact.name);
            if (conversation != null) {
                conversation.timestamp = message.sentTimestamp;
                conversation.lstMessage = processLastMessageType(message.message, message.type);
                conversation.name = contact.name;
                String contactID = "-1";
                if (PreferenceHelper.get(JsonParsingKeys.WINDOW_ID) != null) {
                    contactID = PreferenceHelper.get(JsonParsingKeys.WINDOW_ID);
                }
                PreferenceHelper.get(JsonParsingKeys.WINDOW_ID);
                if (isnew && !contactID.equals(String.valueOf(contact.contactId))) {
                    conversation.unreadCount = conversation.unreadCount + 1;
                }
                conversation.avtarUrl = contact.avatarURL;
                conversation.save();
            }
        }
    }

    public static void addGroupConversation(Groups group, GroupMessage message) {
        if (group != null) {

            if (Conversation.isNewChatroomConversation(String.valueOf(group.groupId))) {
                Logger.error(TAG, "addGroupConversation : Is New Conversation");
                Conversation conversation = new Conversation();
                conversation.chatroomID = group.groupId;
                conversation.timestamp = message.sentTimestamp;
                conversation.lstMessage = processLastMessageType(message.message, message.type);
                conversation.name = group.name;
                conversation.avtarUrl = "";
                String groupID = "-1";
                if (PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID) != null) {
                    groupID = PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID);
                }
                if (!groupID.equals(String.valueOf(conversation.chatroomID)))
                    conversation.unreadCount = conversation.unreadCount + 1;
                conversation.save();
            } else {
                Logger.error(TAG, "addGroupConversation : Update Conversation");
                Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(group.groupId));
                if (conversation != null) {
                    conversation.timestamp = message.sentTimestamp;
                    conversation.lstMessage = processLastMessageType(message.message, message.type);
                    String groupID = "-1";
                    if (PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID) != null) {
                        groupID = PreferenceHelper.get(JsonParsingKeys.GRP_WINDOW_ID);
                    }

                    Logger.error(TAG, "GRP Window ID = " + groupID);
                    if (!groupID.equals(String.valueOf(conversation.chatroomID)) && message.fromId!=SessionData.getInstance().getId())
                        conversation.unreadCount = conversation.unreadCount + 1;
                    conversation.name = group.name;
                    conversation.avtarUrl = "";
                    conversation.save();
                }
            }
           /* Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY, 1);
            PreferenceHelper.getContext().sendBroadcast(iintent);*/
        }
    }


    public static void processRecentChatList(JSONObject recentObject) {
        Logger.error(TAG, "Process Recent ChatList called : " + recentObject);
        try {
            for (Iterator iterator = recentObject.keys(); iterator.hasNext(); ) {
                String key = (String) iterator.next();
                JSONObject msg = new JSONObject(recentObject.getString(key)).getJSONObject("m");

                if (key.contains("_")) { //is chatroom
                    if((FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GROUP_CHAT_ENABLED)) == FeatureState.ACCESSIBLE){
                        processGroupMessage(msg);
                    }else{
                        long chatroomId = msg.getLong("chatroomid");
                        Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(chatroomId));
                        assert conversation != null;
                        if (conversation != null) {
                            conversation.delete();
                        }
                    }
                } else { // is buddy
                    processOneOnOneMessage(msg);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public static String processLastMessageType(String message, String type) {
        switch (type) {
            case MessageTypeKeys.IMAGE_MESSAGE:
            case MessageTypeKeys.IMAGE_DOWNLOADING:
                return CometChatKeys.RecentMessageTypes.IMAGE;

            case MessageTypeKeys.AUDIO_MESSAGE:
            case MessageTypeKeys.AUDIO_DOWNLOADING:
                return CometChatKeys.RecentMessageTypes.AUDIO;

            case MessageTypeKeys.STICKER:
                return CometChatKeys.RecentMessageTypes.STICKER;

            case MessageTypeKeys.VIDEO_MESSAGE:
            case MessageTypeKeys.VIDEO_DOWNLOADING:
                return CometChatKeys.RecentMessageTypes.VIDEO;

            case MessageTypeKeys.HANDWRITE_MESSAGE:
            case MessageTypeKeys.HANDWRITE_DOWNLOADING:
                return CometChatKeys.RecentMessageTypes.HANDWRITE;

            case MessageTypeKeys.BOT_RESPONSE:
                return CometChatKeys.RecentMessageTypes.BOT_RESPONCE;

            case MessageTypeKeys.WHITEBOARD_MESSAGE:
                return CometChatKeys.RecentMessageTypes.WHITEBOARD_REQUEST;

            case MessageTypeKeys.WRITEBOARD_MESSAGE:
                return CometChatKeys.RecentMessageTypes.WRITEBOARD_REQUEST;

            case MessageTypeKeys.GROUP_INVITE:
                return CometChatKeys.RecentMessageTypes.GROUP_INVITE;

            case MessageTypeKeys.FILE_MESSAGE:
                case MessageTypeKeys.FILE_DOWNLOADING:
                case MessageTypeKeys.FILE_DOWNLOADED:
                return CometChatKeys.RecentMessageTypes.FILE;

            default:
                return message;
        }
    }

    private static void setTextMessageType(OneOnOneMessage oneOnOneMessage){
        Logger.error(TAG,"oneOnOne text Message = "+oneOnOneMessage.message);
        if(EmoticonUtils.isEmojiMessage(oneOnOneMessage.message)){
            if(EmoticonUtils.isOnlySmileyMessage(oneOnOneMessage.message)){
                oneOnOneMessage.imageUrl = MessageTypeKeys.NO_BACKGROUND;
            }
            oneOnOneMessage.type = MessageTypeKeys.EMOJI_MESSAGE;
        }else{
            oneOnOneMessage.type = MessageTypeKeys.NORMAL_MESSAGE;
        }
    }
}
