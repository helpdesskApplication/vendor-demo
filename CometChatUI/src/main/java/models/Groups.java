package models;

import android.content.Intent;
import android.util.Log;

import com.inscripts.enums.GroupType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.orm.SugarRecord;
import com.inscripts.orm.dsl.Column;
import com.inscripts.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import Keys.JsonParsingKeys;


public class Groups extends SugarRecord {
    public static final String TABLE_NAME = Groups.class.getSimpleName().toLowerCase(Locale.US);
    public static final String COLUMN_GROUP_ID = "group_id";
    public static final String COLUMN_LAST_UPDATED = "last_updated";
    public static final String COLUMN_UNREAD_COUNT = "unread_count";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_STATUS = "status";
    public static final String MEMBER_COUNT = "MEMBER_COUNT";
    public static final String TYPE = "TYPE";
    public static final String COLUMN_OWNER = "owner";
    public static final java.lang.String TAG = Groups.class.getSimpleName();

    /**
     * Chatroom id
     */
    @Column(name = COLUMN_GROUP_ID, unique = true, notNull = true)
    public long groupId;


    @Column(name = COLUMN_STATUS)
    public int status;

    /**
     * The parameter 's' which contain one of the 3 values, namely: 0 (another
     * user), 1 (created by self), 2 (created by moderator)
     */
    public int createdBy;
    public long lastUpdated, id;

    /**
     * The parameter 'type' which may contain one of the 3 values, namely: 0
     * (public), 1 (password protected), 2 (invite only)
     */
    public int type;
    public int memberCount, onlineCount;

    @Column(name = COLUMN_UNREAD_COUNT)
    public int unreadCount;

    @Column(name = COLUMN_NAME)
    public String name;

    @Column(name = COLUMN_OWNER)
    public int owner;

    public String password;

   // public boolean isModerator;

    public static void updateAllGroups(JSONObject groupList) {

        try {
            if (groupList.length() > 0) {
                ArrayList<Groups> groups = new ArrayList<Groups>();
                Iterator<String> keys = groupList.keys();
                Set<Long> ids = new HashSet<Long>();

                while (keys.hasNext()) {
                    try {
                        JSONObject groupJson = groupList.getJSONObject(keys.next());
                        Groups group = getGroupDetails(groupJson.getLong(JsonParsingKeys.ID));
                        if (null == group) {
                            group = new Groups();
                            group.groupId = groupJson.getLong(JsonParsingKeys.ID);
                            group.lastUpdated = System.currentTimeMillis();
                        }
                        group.groupId = groupJson.getLong(JsonParsingKeys.ID);
                        group.name = groupJson.getString(JsonParsingKeys.GROUP_NAME);
                        group.memberCount = groupJson.getInt(JsonParsingKeys.ONLINE);
                        group.type = groupJson.getInt(JsonParsingKeys.TYPE);
                        group.password = groupJson.getString(JsonParsingKeys.GROUP_PASSWORD);
                        group.createdBy = groupJson.getInt("createdby");
                        if (groupJson.has(JsonParsingKeys.GROUP_OWNER)) {
                            if (groupJson.get(JsonParsingKeys.GROUP_OWNER) instanceof Boolean) {
                                group.owner = groupJson.getBoolean(JsonParsingKeys.GROUP_OWNER) ? 1 : 0;
                            } else {
                                group.owner = groupJson.getInt(JsonParsingKeys.GROUP_OWNER);
                            }
                        }


                      /*  if(groupJson.has(JsonParsingKeys.IS_MODERATOR))
                        group.isModerator = groupJson.getBoolean(JsonParsingKeys.IS_MODERATOR);*/

                        if (groupJson.has(JsonParsingKeys.GROUP_STATUS)) {
                            group.status = groupJson.getInt(JsonParsingKeys.GROUP_STATUS);
                        } else {
                            group.status = 0;
                        }
                        groups.add(group);
                        ids.add(group.groupId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                String csvWithQuote = ids.toString().replace("[", "'").replace("]", "'").replace(", ", "','");
                String whereClause = "`" + COLUMN_GROUP_ID + "` NOT IN (" + csvWithQuote + ")";
                // String whereArgs = csvWithQuote;
                //deleteAll(Groups.class, whereClause, new String[0]);
                List<Groups> grpListToDelete = findWithQuery(Groups.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE " + whereClause, new String[0]);
                if (!grpListToDelete.isEmpty()) {
                    for(Groups grp : grpListToDelete ){
                        Logger.error(TAG, "Group info : " + grp);
                        Conversation.deleteConversationByGroupID(String.valueOf(grp.groupId));
                        deleteGroup(grp.groupId);
                    }
                }
                Log.e("groupJoin", "not delete");
                saveInTx(groups);
            } else {
                Log.e("groupJoin", "delete");
                deleteAll(Groups.class);
                Conversation.deleteAllGroups();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Groups getGroupDetails(Long chatroomId) {
        return getGroupDetails(String.valueOf(chatroomId));
    }

    public static Groups getGroupDetails(String chatroomId) {
        List<Groups> list = find(Groups.class, "`" + COLUMN_GROUP_ID + "` = ?", chatroomId);
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static List<Groups> getAllGroups() {
        return findWithQuery(Groups.class, "SELECT * FROM `" + TABLE_NAME + "` ORDER BY `" + COLUMN_STATUS +
                /*+ "` , `"+ COLUMN_STATUS+*/"` DESC;", new String[0]);
    }

    public static List<Groups> getAllJoinedGroups() {
        return findWithQuery(Groups.class, "SELECT * FROM `" + TABLE_NAME + " WHERE `" + COLUMN_STATUS +" ` = " + 1 + ";", new String[0]);
    }

    public static String getAllGroupsQuery() {
        Logger.error(TAG,"Get All group query called");
        return "SELECT * FROM `" + TABLE_NAME + "` ORDER BY `" + COLUMN_STATUS + "` DESC;";
    }

    public static String getGroupsSearchQuery(String searchText){
        return "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_NAME + "` LIKE '%" + searchText
                + "%' ORDER BY `" + COLUMN_LAST_UPDATED + "` DESC";
    }

    public static Groups insertNewGroup(JSONObject group) {
        Logger.error(TAG,"insertNewGroup: "+group);
        Groups groups = new Groups();
        try {
            if(group.has("group_id")){
                groups.groupId = group.getLong("group_id");
            } else if (group.has("crid")) {
                groups.groupId = group.getLong("crid");
            } else {
                groups.groupId = group.getLong(JsonParsingKeys.ID);
            }
//            groups.name = group.getString(JsonParsingKeys.GROUP_NAME);
            groups.type = group.getInt(JsonParsingKeys.TYPE);
            groups.lastUpdated = System.currentTimeMillis();
            if (group.has("groupname")) {
                groups.name = group.getString("groupname");
            } else if(group.has("chatroomname")) {
                groups.name = group.getString("chatroomname");
            } else if(group.has("name")){
                groups.name = group.getString("name");
            }
            groups.memberCount = 1;
            if(group.has("type")){
                groups.type = group.getInt("type");
            }
            groups.password = group.getString("password");
            if (group.has("owner") && (group.get("owner") instanceof Boolean)) {
                groups.owner = group.getBoolean("owner") ? 1 : 0;
            } else {
                groups.owner = group.getInt("owner");
            }
            groups.status = 1;
            groups.createdBy = group.getInt("createdby");
            groups.save();
        } catch (JSONException e) {
            Logger.error("insertNewGroup", e.toString());
        }
        return groups;
    }

    public static void deleteGroup(long groupId) {

        Groups group = getGroupDetails(groupId);
        if(group !=null && group.groupId!=0) {
            group.delete();
        }

        Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(groupId));
        if(conversation!=null && conversation.chatroomID != 0){
            conversation.delete();
        }
    }

    public static void renameGroup(long groupId, String groupName) {

        Groups group = getGroupDetails(groupId);
        if(group != null && group.groupId!=0) {
            group.name = groupName;
            group.save();
        }

        Conversation conversation = Conversation.getConversationByChatroomID(String.valueOf(groupId));
        if(conversation != null && conversation.chatroomID != 0){
            conversation.name = groupName;
            conversation.save();
        }
    }

    public static int getUnreadGroupsCount() {
        List<Groups> groupsList = findWithQuery(Groups.class, "SELECT * FROM `" + TABLE_NAME
                + "` WHERE `" + COLUMN_UNREAD_COUNT + "` >" + 0);
        if (groupsList != null || groupsList.size() > 0) {
            return groupsList.size();
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Group name : " + name + "\nGroup Id : " + groupId + "\n Group Type : " + type + "\n Group Createdby : " + createdBy + "\n Group owner : " + owner;
    }
}
