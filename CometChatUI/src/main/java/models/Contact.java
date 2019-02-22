package models;

import android.content.Intent;

import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.orm.SugarRecord;
import com.inscripts.orm.dsl.Column;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import Keys.BroadCastReceiverKeys;
import Keys.JsonParsingKeys;

public class Contact extends SugarRecord {
    private static final java.lang.String TAG = Contact.class.getSimpleName();
    public static final String TABLE_NAME = Contact.class.getSimpleName().toLowerCase(Locale.US);
    public static final String COLUMN_CONTACT_ID = "contact_id";
    public static final String COLUMN_LAST_UPDATED = "last_updated";
    public static final String COLUMN_UNREAD_COUNT = "unread_count";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_COMET_ID = "cometid";
    public static final String COLUMN_LASTSEEN = "lastseen";
    public static final String LSTN = "lstn";
    public static final String SHOW_USER = "showuser";
    public static final String COLUMN_STATUS_MESSAGE = "status_message";
    public static final String COLUMN_AVATAR_URL = "avatar_url";
    public static final String COLUMN_STATUS = "status";

    @Column(name = COLUMN_CONTACT_ID, unique = true, notNull = true)
    public long contactId;
    public long lastUpdated, id;
    public int lstn, showuser = 1;

    @Column(name = COLUMN_UNREAD_COUNT)
    public int unreadCount;

    @Column(name = COLUMN_NAME)
    public String name;

    @Column(name = COLUMN_STATUS_MESSAGE)
    public String statusMessage;

    @Column(name = COLUMN_AVATAR_URL)
    public String avatarURL;

    @Column(name = COLUMN_STATUS)
    public String status;

    public String link, cometid = "", lastseen = "";



    public static Contact getContactDetails(Long buddyId) {
        return getContactDetails(String.valueOf(buddyId));
    }

    public static Contact getContactDetails(String contactID) {
        Set<Long> ids = new HashSet<>();
        ids.add(Long.parseLong(contactID));
        String csvWithQuote = ids.toString().replace("[", "'").replace("]", "'").replace(", ", "','");

        List<Contact> list = findWithQuery(Contact.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_CONTACT_ID + "` IN ("
                + csvWithQuote + ");", new String[0]);
        if (null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static void updateAllContacts(JSONObject buddyList) {
        try {
            if (buddyList.length() > 0) {
                Logger.error("deleteBuddy : buddyList.length() > 0");
                ArrayList<Contact> contacts = new ArrayList<>();
                Iterator<String> keys = buddyList.keys();
                Set<Long> ids = new HashSet<>();

                while (keys.hasNext()) {
                    try {
                        JSONObject buddy = buddyList.getJSONObject(keys.next());
                        Contact contactPojo = Contact.getContactDetails(buddy.getLong(JsonParsingKeys.ID));
                        if (null == contactPojo) {
                            contactPojo = new Contact();
                            contactPojo.contactId = buddy.getLong(JsonParsingKeys.ID);
                            contactPojo.lastUpdated = 0;
                        }
                        contactPojo.showuser = 1;
                        contactPojo.name = CommonUtils.ucWords(buddy.getString(JsonParsingKeys.NAME));
                        contactPojo.link = buddy.getString(JsonParsingKeys.LINK);
                        contactPojo.avatarURL = CommonUtils.processAvatarUrl(buddy.getString(JsonParsingKeys.AVATAR_LINK));
                        if (buddy.has(JsonParsingKeys.LSTN)) {
                            String lstn = buddy.getString(JsonParsingKeys.LSTN);
                            if (lstn == null || lstn.isEmpty() || lstn.equals("") || lstn.equals("null")) {
                                contactPojo.lstn = 0;
                            } else {
                                contactPojo.lstn = 1;
                            }
                        }
                        if (buddy.has(JsonParsingKeys.LASTSEEN)) {
                            String lastseen = buddy.getString(JsonParsingKeys.LASTSEEN);

                            if (lastseen == null || lastseen.equals("0") || lastseen.equals("") || lastseen.equals("null")) {
                                contactPojo.lastseen = String.valueOf(CommonUtils.correctIncomingTimestamp(0L));
                            } else {
                                if (PreferenceHelper.get(PreferenceKeys.DataKeys.SERVER_DIFFERENCE) != null) {
                                    Long n = CommonUtils.correctIncomingTimestamp(Long.parseLong(lastseen)) + Long.parseLong(PreferenceHelper.get(PreferenceKeys.DataKeys.SERVER_DIFFERENCE));
                                    contactPojo.lastseen = String.valueOf(n);
                                } else {
                                    contactPojo.lastseen = String.valueOf(CommonUtils.correctIncomingTimestamp(Long.parseLong(lastseen)));
                                }
                            }
                        }

                        contactPojo.status = buddy.getString(JsonParsingKeys.STATUS);
                        contactPojo.statusMessage = buddy.getString(JsonParsingKeys.STATUS_MESSAGE);
                        if (buddy.has(JsonParsingKeys.CSCHANNEL)) {
                            contactPojo.cometid = buddy.getString(JsonParsingKeys.CSCHANNEL);
                        }
                        contacts.add(contactPojo);
                        ids.add(contactPojo.contactId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                String csvWithQuote = ids.toString().replace("[", "'").replace("]", "'").replace(", ", "','");
                String whereClause = "`" + COLUMN_CONTACT_ID + "` NOT IN (" + csvWithQuote + ")";

                List<Contact> removedbuddy = findWithQuery(Contact.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE " + whereClause);
                for (Contact contact : removedbuddy) {
                    contact.showuser = 0;
                }
                Contact.saveInTx(removedbuddy);
                saveInTx(contacts);
            } else {
                SugarRecord.deleteAll(Contact.class);
                Conversation.deleteAllContacts();
            }

            Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
            iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_CONTACT_LIST_KEY, 1);
            PreferenceHelper.getContext().sendBroadcast(iintent);

            Intent messageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
            messageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.UPDATE_LAST_SEEN, 1);
            PreferenceHelper.getContext().sendBroadcast(messageIntent);

        } catch (Exception e) {
            Logger.error("deleteBuddy : e - " + e);
            e.printStackTrace();
        }
    }

    public static Contact insertNewBuddy(JSONObject buddy) {
        Contact contactPojo = new Contact();
        try {
                contactPojo.contactId = buddy.getLong(JsonParsingKeys.ID);
                contactPojo.name = CommonUtils.ucWords(buddy.getString(JsonParsingKeys.NAME));
                contactPojo.link = buddy.getString(JsonParsingKeys.LINK);
                contactPojo.avatarURL = CommonUtils.processAvatarUrl(buddy.getString(JsonParsingKeys.AVATAR_LINK));
                contactPojo.status = buddy.getString(JsonParsingKeys.STATUS);
                contactPojo.statusMessage = buddy.getString(JsonParsingKeys.STATUS_MESSAGE);
                contactPojo.lastUpdated = System.currentTimeMillis();
                contactPojo.showuser = 1;
                if (buddy.has(JsonParsingKeys.LSTN)) {
                    String lstn = buddy.getString(JsonParsingKeys.LSTN);
                    if (lstn == null || lstn.isEmpty() || lstn.equals("") || lstn.equals("null")) {
                        contactPojo.lstn = 1;
                    } else {
                        contactPojo.lstn = Integer.parseInt(lstn);
                    }
                }
                if (buddy.has(JsonParsingKeys.LASTSEEN)) {
                    String lastseen = buddy.getString(JsonParsingKeys.LASTSEEN);

                    if (lastseen == null || lastseen.equals("0") || lastseen.equals("") || lastseen.equals("null")) {
                        contactPojo.lastseen = String.valueOf(CommonUtils.correctIncomingTimestamp(0L));
                    } else {
                        if (PreferenceHelper.get(PreferenceKeys.DataKeys.SERVER_DIFFERENCE) != null) {
                            Long n = CommonUtils.correctIncomingTimestamp(Long.parseLong(lastseen)) + Long.parseLong(PreferenceHelper.get(PreferenceKeys.DataKeys.SERVER_DIFFERENCE));
                            contactPojo.lastseen = String.valueOf(n);
                        } else {
                            contactPojo.lastseen = String.valueOf(CommonUtils.correctIncomingTimestamp(Long.parseLong(lastseen)));
                        }
                    }
                }
                if (buddy.has(JsonParsingKeys.CSCHANNEL)) {
                    contactPojo.cometid = buddy.getString(JsonParsingKeys.CSCHANNEL);
                }
                contactPojo.save();
        } catch (Exception e) {
        }
        return contactPojo;
    }

    /* Get all users who are not a part of the chatroom. */
    public static List<Contact> getExternalBuddies(Set<String> ids) {
        String csvWithQuote = ids.toString().replace("[", "'").replace("]", "'").replace(", ", "','");
        return findWithQuery(Contact.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_CONTACT_ID + "` NOT IN ("
                + csvWithQuote + ");", new String[0]);
    }

    public static List<Contact> searchContacts(String searchText) {
        String rawQuery = "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_NAME + "` LIKE '%" + searchText
                + "%' AND `" + SHOW_USER + "`=1 ORDER BY `" + COLUMN_LAST_UPDATED + "` DESC";
        return findWithQuery(Contact.class, rawQuery, new String[0]);
    }

    public static List<Contact> getAllContacts() {
        return findWithQuery(Contact.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + SHOW_USER + "` = 1 ORDER BY `" + COLUMN_LAST_UPDATED
                + "` DESC;", new String[0]);
    }

    public static String getAllContactsQuery(){
        return "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + SHOW_USER + "` = 1 ORDER BY `" + COLUMN_LAST_UPDATED
                + "` DESC;";
    }

    public static String searchContactsQuery(String searchText) {
        String rawQuery = "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_NAME + "` LIKE '%" + searchText
                + "%' AND `" + SHOW_USER + "`=1 ORDER BY `" + COLUMN_LAST_UPDATED + "` DESC";
        return rawQuery;
    }

    public static int getUnreadContactsCount() {
        List<Contact> contactList = findWithQuery(Contact.class, "SELECT * FROM `" + TABLE_NAME
                + "` WHERE `" + COLUMN_UNREAD_COUNT + "` >" + 0);
        if (contactList != null || contactList.size() > 0) {
            return contactList.size();
        }
        return 0;
    }

    public static List<Contact> getContactWithLimit(int limit){
        return findWithQuery(Contact.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + SHOW_USER + "` = 1 ORDER BY `" + COLUMN_LAST_UPDATED
                + "` DESC LIMIT "+ limit+";", new String[0]);
    }
}
