package Keys;

import android.content.Intent;

/**
 * Created by Jitvar on 5/24/2017.
 */

public class BroadCastReceiverKeys {

    public static final String LIST_DATA_UPDATED_BROADCAST = "LIST_DATA_UPDATED_BROADCAST";
    public static final String MESSAGE_DATA_UPDATED_BROADCAST = "LIST_DATA_UPDATED_BROADCAST";
    public static final String GROUP_MESSAGE_DATA_UPDATED_BROADCAST = "GROUP_LIST_DATA_UPDATED_BROADCAST";
    public static final String FINISH_GROUP_ACTIVITY = "FINISH_GROUP_ACTIVITY";


    public static class IntentExtrasKeys{
        public static final String REFRESH_CONTACT_LIST_KEY = "REFRESH_CONTACT_LIST_KEY";
        public static final String REFRESH_GROUP_LIST_KEY = "REFRESH_GROUP_LIST_KEY";
        public static final String REFRESH_RECENT_LIST_KEY = "REFRESH_RECENT_LIST_KEY";
        public static final String CONTACT_ID = "CONTACT_ID";
        public static final String CONTACT_NAME = "CONTACT_NAME";

        public static final String NEW_MESSAGE = "NEW_MESSAGE";
        public static final String UPDATE_LAST_SEEN = "UPDATE_LAST_SEEN";
        public static final String READ_MESSAGE = "READ_MESSAGE";
        public static final String DELIVERED_MESSAGE = "DELIVERED_MESSAGE";
        public static final String IS_TYPING = "IS_TYPING";
        public static final String STOP_TYPING = "STOP_TYPING";
        public static final String OPEN_SETTINGS = "OPEN_SETTINGS";
        public static final String BUDDY_ID = "BUDDY_ID";
        public static final String BUDDY_NAME = "BUDDY_NAME";
        public static final String CLOSE_WINDOW_ENABLED = "CLOSE_WINDOW_ENABLED";
        public static final String IMAGE_ITEM_POSITION = "clicked_position";
        public static final String GROUP_ID = "GROUP_ID";
        public static final String KICKED = "kicked" ;
        public static final String BANNED = "banned";
        public static final String FILE_DOWNLOAD = "FILE_DOWNLOAD";
    }

    public static class ListUpdatationKeys {
        public static final String REFRESH_BOT_LIST_FRAGMENT = "REFRESH_BOT_LIST_FRAGMENT";
        public static final String REFRESH_RECENT_FRAGMENT = "REFRESH_RECENT_FRAGMENT";
        public static final String REFRESH_BUDDY_LIST_FRAGMENT = "REFRESH_FULL_BUDDY_LIST_FRAGMENT";
        public static final String REFRESH_FULL_CHATROOM_LIST_FRAGMENT = "REFRESH_FULL_CHATROOM_LIST_FRAGMENT";
    }

    public static class HeartbeatKeys {
        public static final String ONE_ON_ONE_HEARTBEAT_NOTIFICATION = "ONE_ON_ONE_HEARTBEAT_UPDATAION";
        public static final String CHATROOM_HEARTBEAT_UPDATAION = "CHATROOM_HEARTBEAT_UPDATAION";
        public static final String ANNOUNCEMENT_UPDATATION = "ANNOUNCEMENT_UPDATATION";
        public static final String ANNOUNCEMENT_BADGE_UPDATION = "ANNOUNCEMENT_BADGE_UPDATION";
    }

    /**
     * Keys to send broadcast releated to AVchat
     */
    public static class AvchatKeys {
        public static final String EVENT_AVCHAT_ACCEPTED = "CALL_ACCEPTED";
        public static final String AVCHAT_CALLER_ID = "CALLER_ID";
        public static final String AVCHAT_CLOSE_ACTIVITY = "CLOSE_ACTIVITY";
        public static final String AVCHAT_CALL_END = "CALL_END";
        public static final String AVCHAT_CALL_CANCEL = "CALL_CANCEL";
        public static final String CALL_SESSION_ONGOING ="CALL_SESSION_ONGOING";
        public static final String CALL_CANCEL_FROM_NOTIFICATION = "CALL_CANCEL_FROM_NOTIFICATION";
        public static final String CALL_END_FROM_NOTIFICATION = "CALL_END_FROM_NOTIFICATION";
    }

    public static class NewMessageKeys {
        public static final String EVENT_NEW_MESSAGE_ONE_ON_ONE = "EVENT_NEW_MESSAGE_ONE_ON_ONE";
        public static final String EVENT_NEW_MESSAGE_CHATROOM = "EVENT_NEW_MESSAGE_CHATROOM";
    }
    public static final String EVENT_NEW_MESSAGE_ONE_ON_ONE = "EVENT_NEW_MESSAGE_ONE_ON_ONE";
    public static final String EVENT_NEW_MESSAGE_CHATROOM = "EVENT_NEW_MESSAGE_CHATROOM";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
}
