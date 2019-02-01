/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package models;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.orm.SugarRecord;
import com.inscripts.orm.dsl.Column;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONObject;

import java.util.List;

import Keys.MessageTypeKeys;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;

public class GroupMessage extends SugarRecord {

    private static final java.lang.String TAG = GroupMessage.class.getSimpleName();

    private static final String ID = "id";
    private static final String TABLE_NAME = "GROUP_MESSAGE";
    public static final String COLUMN_REMOTE_ID = "remote_id";
    public static final String COLUMN_CHATROOM_ID = "chatroom_id";
    public static final String COLUMN_SENT_TIMESTAMP = "sent_timestamp";
    public static final String COLUMN_FROM = "from";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_SENT = "sent";
    public static final String COLUMN_FROM_ID = "fromid";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_TEXT_COLOR = "text_color";
    public static final String COLUMN_MESSAGE_INSERTED_BY = "inserted_by";
    public static final String COLUMN_MESSAGE_TYPE = "message_type";
    public static final String COLUMN_SENDER_NAME = "sender_name";
    public static final String COLUMN_MESSAGE_STATUS = "messagestatus";
    public static final String COLUMN_RETRY_COUNT = "retry_count";

    /**
     * Chatroom message id
     */
    @Column(name = COLUMN_REMOTE_ID, notNull = true)
    public long remoteId;

    @Column(name = COLUMN_FROM_ID)
    public long fromId;


    public long id, chatroomId;

    @Column(name = COLUMN_SENT_TIMESTAMP)
    public long sentTimestamp;

    @Column(name = COLUMN_SENDER_NAME)
    public String senderName;

    @Column(name = COLUMN_MESSAGE_TYPE)
    public String type;

    @Column(name = COLUMN_TEXT_COLOR)
    public String textColor;

    @Column(name = COLUMN_IMAGE_URL)
    public String imageUrl;

    @Column(name = COLUMN_MESSAGE)
    public String message;

    /**
     * 1 => Inserted by response. 0 => Inserted by receive.
     */
    @Column(name = COLUMN_MESSAGE_INSERTED_BY)
    public int insertedBy;

    /**
     * 0=> Unsent, 1=> Sent
     */
    @Column(name = COLUMN_MESSAGE_STATUS)
    public int messageStatus;

    @Column(name = COLUMN_RETRY_COUNT)
    public int retryCount = 3;

    private static CometChat cometChat;

    public GroupMessage() {
    }

    public GroupMessage(long remoteId, long userId, Long chatroomId, String message, long sentTimestamp,
                        String senderName, String type, String imageUrl, String textColor, int msgInsertedBy,
                        int messageStatus) {
        this.remoteId = remoteId;
        this.fromId = userId;
        this.chatroomId = chatroomId;
        this.message = message;
        this.sentTimestamp = sentTimestamp;
        this.senderName = senderName;
        this.type = type;
        this.imageUrl = imageUrl;
        this.textColor = textColor;
        this.insertedBy = msgInsertedBy;
        this.messageStatus = messageStatus;
//        retryCount = 3;
    }

    public GroupMessage(JSONObject message) {
        try {
            this.remoteId = message.getLong(ID);
            this.fromId = message.getLong(COLUMN_FROM_ID);
            this.senderName = message.getString(COLUMN_FROM);
            if (message.has("chatroomid")) {
                this.chatroomId = Long.parseLong(message.getString("chatroomid"));
            } else {
                this.chatroomId = SessionData.getInstance().getCurrentChatroom();
            }
            this.message = message.getString(COLUMN_MESSAGE);
            this.sentTimestamp = message.getLong(COLUMN_SENT);
            this.type = message.getString(CometChatKeys.MessageTypeKeys.MESSAGE_TYPE);
            this.imageUrl = message.getString(COLUMN_IMAGE_URL);
            this.textColor = message.getString(COLUMN_TEXT_COLOR);
            this.insertedBy = message.getInt(COLUMN_MESSAGE_INSERTED_BY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<GroupMessage> getAllMessages(Long chatroomId) {
        cometChat = CometChat.getInstance(PreferenceHelper.getContext());
        String messageLimit = LocalConfig.getMessageLimit();
        messageLimit = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.HISTORY_MESSAGE_LIMIT)) ;
        return findWithQuery(GroupMessage.class, "SELECT * " + COLUMN_FROM + " ( SELECT *" + COLUMN_FROM + " `" + TABLE_NAME + "` WHERE `"
                + COLUMN_CHATROOM_ID + "`=" + chatroomId + " ORDER BY `" + COLUMN_SENT_TIMESTAMP + "` DESC LIMIT "
                + messageLimit + ") ORDER BY `" + COLUMN_SENT_TIMESTAMP + "` ASC;", new String[0]);
    }

    public static String getAllMessagesQuery(Long chatroomId) {
        return "SELECT * " + COLUMN_FROM + " ( SELECT *" + COLUMN_FROM + " `" + TABLE_NAME + "` WHERE `"
                + COLUMN_CHATROOM_ID + "`=" + chatroomId + " ORDER BY `" + COLUMN_SENT_TIMESTAMP + "` ASC);";
    }

    public static GroupMessage findById(String messageId) {
        List<GroupMessage> list = find(GroupMessage.class, "`" + COLUMN_REMOTE_ID + "` = ?", messageId);
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static GroupMessage findById(Long messageId) {
        return findById(String.valueOf(messageId));
    }

    public static void deleteMessage(String messageId) {
        String whereClause = "`" + COLUMN_REMOTE_ID + "` = ?";
        String[] whereArgs = {messageId};
        deleteAll(GroupMessage.class, whereClause, whereArgs);
    }

    public static void clearConversation(Long chatroomId) {
        String whereClause = "`" + COLUMN_CHATROOM_ID + "` = ?";
        String[] whereArgs = {String.valueOf(chatroomId)};
        deleteAll(GroupMessage.class, whereClause, whereArgs);
        Conversation.deleteConversationByGroupID(String.valueOf(chatroomId));
    }

    public static GroupMessage findByLocalId(String messageId) {
        List<GroupMessage> list = find(GroupMessage.class, "`" + ID + "` = ?", messageId);
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /*public static List<GroupMessage> getAllUnsendMessages(){
        String query = "SELECT * from `"+ TABLE_NAME + "` WHERE `" + COLUMN_REMOTE_ID + "`= " +"0;";
        Logger.error("Query = "+query);
        return findWithQuery(GroupMessage.class,query,new String[0]);
    }*/

    public static List<GroupMessage> getAllUnsentMessages(){
        String query = "SELECT * FROM "+ TABLE_NAME + " WHERE " + COLUMN_MESSAGE_STATUS + " = " +"0 "
                + " ORDER BY " + COLUMN_REMOTE_ID +" ASC";
        Logger.error("Query = "+query);
        return findWithQuery(GroupMessage.class,query,new String[0]);
    }

    /*public static long getLargestRemoteId() {

        long largestRemoteId = 0;

        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_REMOTE_ID +" ASC";
        List<GroupMessage> groupMessageList = findWithQuery(GroupMessage.class, query, null);
        if(groupMessageList.size() > 0) {
            largestRemoteId = groupMessageList.get(groupMessageList.size() - 1).remoteId;
        }

        Logger.error(TAG, "largestRemoteId : " + largestRemoteId);

        return largestRemoteId;
    }*/

    public static void updateBroadcastMessage(String chatroomId) {
        Logger.error(TAG, "chatroomId : " + chatroomId);

        List<GroupMessage> list = find(GroupMessage.class, COLUMN_CHATROOM_ID + "=? AND " + COLUMN_MESSAGE_TYPE + "=?",
                new String[]{chatroomId, MessageTypeKeys.GRP_AVBROADCAST_REQUEST});
        Logger.error(TAG, "list.size() : " + list.size());
        if (list != null && list.size() > 0) {

            for(GroupMessage groupMessage : list) {
                groupMessage.type = MessageTypeKeys.AVBROADCAST_EXPIRED;
                groupMessage.save();
            }
        }
    }

    @Override
    public String toString() {
        return "localID "+id+" Remote id: "+remoteId +" retryCounnt " +retryCount+" messageStatus "+messageStatus+ " Message : " + message + " Type : " + type + " From id : " + fromId;
    }
}