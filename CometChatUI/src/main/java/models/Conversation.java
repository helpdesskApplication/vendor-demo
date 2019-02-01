/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package models;

import com.inscripts.orm.SugarRecord;
import com.inscripts.orm.dsl.Column;
import com.inscripts.utils.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Conversation extends SugarRecord {

    public static final String TABLE_NAME = Conversation.class.getSimpleName().toLowerCase(Locale.US);
    public static final String COLUMN_CONVERSATION_ID = "conversation_id";
    public static final String COLUMN_BUDDY_ID = "buddy_id";
    public static final String COLUMN_CHATROOM_ID = "chatroom_id";
    public static final String COLUMN_LAST_MESSAGE = "last_message";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AVTAR_URL = "avtar_url";
    public static final String COLUMN_UNREAD_COUNT = "unread_count";

    @Column(name = COLUMN_CONVERSATION_ID, notNull = true)
    public long conversationId;

    @Column(name = COLUMN_BUDDY_ID)
    public long buddyID;

    @Column(name = COLUMN_CHATROOM_ID)
    public long chatroomID;

    @Column(name = COLUMN_LAST_MESSAGE)
    public String lstMessage;

    @Column(name = COLUMN_TIMESTAMP)
    public long timestamp;

    @Column(name = COLUMN_NAME)
    public String name;

    @Column(name = COLUMN_AVTAR_URL)
    public String avtarUrl;

    @Column(name = COLUMN_UNREAD_COUNT)
    public long unreadCount;

    public static List<Conversation> getAllConversations() {
        return findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME + "` ORDER BY `" + COLUMN_TIMESTAMP +
                /*+ "` , `"+ COLUMN_STATUS+*/"` DESC;", new String[0]);
    }

    public static String getAllConversationQuery(){
        return  "SELECT * FROM `" + TABLE_NAME + "` ORDER BY `" + COLUMN_TIMESTAMP +
                /*+ "` , `"+ COLUMN_STATUS+*/"` DESC;";
    }

    public static String getSearchConversationQuery(String searchText){
        return "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_NAME + "` LIKE '%" + searchText
                + "%' ORDER BY `" + COLUMN_TIMESTAMP + "` DESC";
    }

    public static boolean isNewBuddyConversation(String buddyID) {
        Set<Long> ids = new HashSet<>();
        ids.add(Long.parseLong(buddyID));
        String csvWithQuote = ids.toString().replace("[", "'").replace("]", "'").replace(", ", "','");

        List<Conversation> list = findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_BUDDY_ID + "` IN ("
                + csvWithQuote + ");", new String[0]);
        if (null == list || list.size() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isConverSationAvailable(){
        List<Conversation> list = findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME + "` ORDER BY `" + COLUMN_TIMESTAMP +
                "` DESC;", new String[0]);
        Logger.error("Conversation list size: "+list.size());
        if(list != null && list.size()>0){
            return true;
        }else {
            return false;
        }
    }

    public static boolean isNewChatroomConversation(String chatroomID) {
        Set<Long> ids = new HashSet<>();
        ids.add(Long.parseLong(chatroomID));
        String csvWithQuote = ids.toString().replace("[", "'").replace("]", "'").replace(", ", "','");

        List<Conversation> list = findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_CHATROOM_ID + "` IN ("
                + csvWithQuote + ");", new String[0]);
        if (null == list || list.size() == 0) {
            return true;
        } else {
            return false;
        }
    }



    public static Conversation getConversationByBuddyID(String buddyID){

        List<Conversation> list = findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME
                + "` WHERE `" + COLUMN_BUDDY_ID + "` = "+buddyID+";", new String[0]);

        if (null != list && list.size() > 0) {
            return list.get(0);
        }

        return null;
    }

    public static Conversation getConversationByChatroomID(String chatroomID){

        List<Conversation> list = findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_CHATROOM_ID + "` = "+chatroomID+";", new String[0]);

        Logger.error("deleteGroup conversationSize : "+list.size());
        if (null != list && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public static int getUnreadConversationCount() {
        List<Conversation> conversations = findWithQuery(Conversation.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_UNREAD_COUNT + "` >" + 0);
        if (conversations == null || conversations.size() == 0) {
            return 0;
        } else {
            return conversations.size();
        }
    }

    public static void deleteConversationByBuddyId(String buddyID){
        Conversation.deleteAll(Conversation.class,COLUMN_BUDDY_ID+" = ?",buddyID);
    }

    public static void deleteConversationByGroupID(String groupId){
        Conversation.deleteAll(Conversation.class,COLUMN_CHATROOM_ID+" = ?",groupId);
    }

    public static void deleteAllContacts() {
        Conversation.deleteAll(Conversation.class, COLUMN_BUDDY_ID + " != ?", "0");
    }

    public static void deleteAllGroups() {
        Conversation.deleteAll(Conversation.class, COLUMN_CHATROOM_ID + " != ?", "0");
    }
}
