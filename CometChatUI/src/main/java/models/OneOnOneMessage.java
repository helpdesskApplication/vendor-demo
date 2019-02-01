/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package models;

import android.database.MatrixCursor;

import com.inscripts.keys.CometChatKeys;
import com.inscripts.orm.SugarRecord;
import com.inscripts.orm.dsl.Column;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONObject;

import java.util.List;

import Keys.MessageTypeKeys;

public class OneOnOneMessage extends SugarRecord {

    private static final java.lang.String TAG = OneOnOneMessage.class.getSimpleName();

    public static final String TABLE_NAME = "ONE_ON_ONE_MESSAGE";
    public static final String COLUMN_REMOTE_ID = "remote_id";
    public static final String COLUMN_FROM_ID = "from_id";
    public static final String COLUMN_TO_ID = "to_id";
    public static final String COLUMN_SENT_TIMESTAMP = "sent";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FROM = "from";
    public static final String COLUMN_TO = "to";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_SELF = "self";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_MESSAGE_INSERTED_BY = "inserted_by";
    public static final String COLUMN_MESSAGE_TICK = "messagetick";
    public static final String COLUMN_MESSAGE_TYPE = "messagetype";
    public static final String COLUMN_READ = "read";
    public static final String COLUMN_MESSAGE_STATUS = "messagestatus";
    private static final String COLUMN_RETRY_COUNT = "retry_count" ;

    @Column(name = COLUMN_REMOTE_ID, notNull = true)
    public long remoteId;

    @Column(name = COLUMN_READ)
    public int read;

    @Column(name = COLUMN_SELF)
    public int self;

    public long id, toId;

    @Column(name = COLUMN_SENT_TIMESTAMP)
    public long sentTimestamp;

    @Column(name = COLUMN_FROM_ID)
    public long fromId;

    @Column(name = COLUMN_MESSAGE)
    public String message;

    @Column(name = COLUMN_IMAGE_URL)
    public String imageUrl;

    @Column(name = COLUMN_MESSAGE_TYPE)
    public String type;

    /**
     * 1 => Inserted by response. 0 => Inserted by receive.
     */
    @Column(name = COLUMN_MESSAGE_INSERTED_BY)
    public int insertedBy;

    /**
     * 1=> Single tick, 2=> Double tick (Deliverd), 3=> Read tick ( Blue tick)
     */
    @Column(name = COLUMN_MESSAGE_TICK)
    public int messagetick = 1;

    /**
     * 0=> Unsent, 1=> Sent
     */
    @Column(name = COLUMN_MESSAGE_STATUS)
    public int messageStatus;

    @Column(name = COLUMN_RETRY_COUNT)
    public int retryCount = 3;

    public OneOnOneMessage(Long remoteId, long fromId, long toId, String message, long sentTimestamp, int read,
                           int self, String messageType, String imageUrl, Integer insertedBy, int messageTick,
                           int messageStatus) {
        this.remoteId = remoteId;
        this.fromId = fromId;
        this.message = message;
        this.toId = toId;
        this.sentTimestamp = sentTimestamp;
        this.read = read;
        this.self = self;
        this.type = messageType;
        this.imageUrl = imageUrl;
        this.insertedBy = insertedBy;
        this.messagetick = messageTick;
        this.messageStatus = messageStatus;
        retryCount = 3;
    }

    public OneOnOneMessage() {
    }

    public static OneOnOneMessage findById(String messageId) {
        List<OneOnOneMessage> list = find(OneOnOneMessage.class, "`" + COLUMN_REMOTE_ID + "`=?", messageId);
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static OneOnOneMessage findById(long messageId) {
        return findById(String.valueOf(messageId));
    }

    public OneOnOneMessage(JSONObject message) {
        try {
            this.remoteId = message.getLong(COLUMN_ID);
            this.fromId = message.getLong(COLUMN_FROM);
            this.toId = message.getLong(COLUMN_TO);
            this.message = message.getString(COLUMN_MESSAGE);
            this.sentTimestamp = message.getLong(COLUMN_SENT_TIMESTAMP);
            this.read = message.getInt(CometChatKeys.AjaxKeys.OLD);
            this.self = message.getInt(COLUMN_SELF);
            this.type = message.getString(CometChatKeys.MessageTypeKeys.MESSAGE_TYPE);
            this.imageUrl = message.getString(COLUMN_IMAGE_URL);
            this.insertedBy = message.getInt(COLUMN_MESSAGE_INSERTED_BY);
            this.messagetick = message.getInt(COLUMN_MESSAGE_TICK);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearConversation(String buddyId) {
        String yourId = String.valueOf(SessionData.getInstance().getId());
        String whereClause = "`" + COLUMN_FROM_ID + "` = ? AND `" + COLUMN_TO_ID + "` = ? OR `" + COLUMN_TO_ID
                + "` = ? AND `" + COLUMN_FROM_ID + "` = ?";
        String[] whereArgs = {yourId, buddyId, yourId, buddyId};
        deleteAll(OneOnOneMessage.class, whereClause, whereArgs);
        Conversation.deleteConversationByBuddyId(buddyId);
    }

    public static List<OneOnOneMessage> getAllMessages(String fromId, String toId) {
        String messageLimit = "60";
        return findWithQuery(OneOnOneMessage.class, "SELECT * " + COLUMN_FROM + " (SELECT * " + COLUMN_FROM + "`" + TABLE_NAME + "` WHERE `"
                + COLUMN_FROM_ID + "`= " + toId + " AND `" + COLUMN_TO_ID + "` = " + fromId + " OR `" + COLUMN_TO_ID
                + "`=" + toId + " AND `" + COLUMN_FROM_ID + "` = " + fromId + " ORDER BY `" + COLUMN_ID
                + "`) ORDER BY `" + COLUMN_ID + "` ASC;", new String[0]);
    }


    public static String getAllMessagesQuery(String fromId, String toId) {
        String messageLimit = "60";

        Logger.error("MessageLimit = "+messageLimit);
        /*Logger.error("Query = "+"SELECT * " + COLUMN_FROM + " (SELECT * " + COLUMN_FROM + "`" + TABLE_NAME + "` WHERE `"
                + COLUMN_FROM_ID + "`= " + toId + " AND `" + COLUMN_TO_ID + "` = " + fromId + " OR `" + COLUMN_TO_ID
                + "`=" + toId + " AND `" + COLUMN_FROM_ID + "` = " + fromId + " ORDER BY `" + COLUMN_ID
                + "` DESC LIMIT " + messageLimit + ") ORDER BY `" + COLUMN_ID + "` ASC;");*/

        return "SELECT * " + COLUMN_FROM + " (SELECT * " + COLUMN_FROM + "`" + TABLE_NAME + "` WHERE `"
                + COLUMN_FROM_ID + "`= " + toId + " AND `" + COLUMN_TO_ID + "` = " + fromId + " OR `" + COLUMN_TO_ID
                + "`=" + toId + " AND `" + COLUMN_FROM_ID + "` = " + fromId + " ORDER BY `" + COLUMN_ID
                + "` DESC LIMIT " + messageLimit + ") ORDER BY `" + COLUMN_SENT_TIMESTAMP + "` ASC;";

       /* return "SELECT * " + COLUMN_FROM + " (SELECT * " + COLUMN_FROM + "`" + TABLE_NAME + "` WHERE `"
                + COLUMN_FROM_ID + "`= " + toId + " AND `" + COLUMN_TO_ID + "` = " + fromId + " OR `" + COLUMN_TO_ID
                + "`=" + toId + " AND `" + COLUMN_FROM_ID + "` = " + fromId + " ORDER BY `" + COLUMN_REMOTE_ID
                + "`)";*/
    }

    public static List<OneOnOneMessage> getAllMessages(long fromId, long toId) {
        return getAllMessages(String.valueOf(fromId), String.valueOf(toId));
    }

    public static OneOnOneMessage findByLocalId(long messageId) {
        List<OneOnOneMessage> list = find(OneOnOneMessage.class, "`" + COLUMN_ID + "`=?", String.valueOf(messageId));
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static OneOnOneMessage findByLocalId(String messageId) {
        List<OneOnOneMessage> list = find(OneOnOneMessage.class, "`" + COLUMN_ID + "`=?", messageId);
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static OneOnOneMessage findByRemoteId(String messageId) {
        List<OneOnOneMessage> list = find(OneOnOneMessage.class, "`" + COLUMN_REMOTE_ID + "`=?", String.valueOf(messageId));
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /*public static List<OneOnOneMessage> getAllUnsendMessages(){

        String query = "SELECT * from `"+ TABLE_NAME + "` WHERE `" + COLUMN_REMOTE_ID + "`= " +"0 ORDER BY `" + COLUMN_ID + "` ASC;";

        return findWithQuery(OneOnOneMessage.class,query,new String[0]);
    }*/

    public static List<OneOnOneMessage> getAllUnsentMessages(){

        String query = "SELECT * from "+ TABLE_NAME + " WHERE " + COLUMN_MESSAGE_STATUS + " = " +" 0 ORDER BY " + COLUMN_ID + " ASC";

        return findWithQuery(OneOnOneMessage.class,query,new String[0]);
    }

    public static long getLargestRemoteId() {

        long largestRemoteId = 0;

        //String query = "SELECT * FROM " + TABLE_NAME ;
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_REMOTE_ID + " ASC";
        List<OneOnOneMessage> oneOnOneMessageList = findWithQuery(OneOnOneMessage.class, query, null);
        if(oneOnOneMessageList.size() > 0) {
            largestRemoteId = oneOnOneMessageList.get(oneOnOneMessageList.size() - 1).remoteId;
        }

        Logger.error(TAG, "largestRemoteId : " + largestRemoteId);

        return largestRemoteId;
    }

    public static void updateBroadcastMessage(String fromId) {
        Logger.error(TAG, "from id : " + fromId);

        List<OneOnOneMessage> list = find(OneOnOneMessage.class, COLUMN_FROM_ID + "=? AND " + COLUMN_MESSAGE_TYPE + "=?",
                new String[]{fromId, MessageTypeKeys.AVBROADCAST_REQUEST});
        Logger.error(TAG, "list.size() : " + list.size());
        if (list != null && list.size() > 0) {

            for(OneOnOneMessage oneOnOneMessage : list) {
                oneOnOneMessage.type = MessageTypeKeys.AVBROADCAST_EXPIRED;
                oneOnOneMessage.save();
            }
        }
    }

    @Override
    public String toString() {
        return "Id: "+"Remote id: "+remoteId + " Message : " + message +" type "+type+" messageStatus "+messageStatus+" retryCount: "+retryCount+" From id : " + fromId + " To id : " + toId + " Self : " + self + "messageTick : "+messagetick;
    }
}
