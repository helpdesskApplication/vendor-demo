package adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.custom.RoundedImageView;
import com.inscripts.custom.StickyHeaderAdapter;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.RecyclerViewCursorAdapter;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;


import java.security.acl.Group;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;
import models.Groups;


public class GroupListAdapter extends RecyclerViewCursorAdapter<GroupListAdapter.GroupItemHolder> implements
        StickyHeaderAdapter<GroupListAdapter.GroupHeaderItemHolder> {

    private static final java.lang.String TAG = GroupListAdapter.class.getSimpleName();
    private Context context;
    private int primaryColor;
    private Cursor cursor;
    private CometChat cometChat;

    public GroupListAdapter(Context context, Cursor c) {
        super(c);
        this.context = context.getApplicationContext();
        if(cursor != null){
            this.cursor.close();
        }
        this.cursor = c;
        cometChat = CometChat.getInstance(context);
        primaryColor = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
    }

    public void closeCursor(){
        if(cursor != null){
            cursor.close();
        }
    }

    @Override
    public void onBindViewHolder(GroupListAdapter.GroupItemHolder groupItemHolder, Cursor cursor) {
        boolean recentChatEnabled = (boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.RECENT_CHAT_ENABLED));

        int memberCount = cursor.getInt(cursor.getColumnIndex(Groups.MEMBER_COUNT));
        int groupType = cursor.getInt(cursor.getColumnIndex(Groups.TYPE));
        Logger.error(TAG,"MEMBER_COUNT : "+memberCount);
        if(recentChatEnabled) {
            groupItemHolder.unreadCount.setVisibility(View.INVISIBLE);
        }
        else{
            int unreadCount = cursor.getInt(cursor.getColumnIndex(Groups.COLUMN_UNREAD_COUNT));

            if (0 == unreadCount) {
                groupItemHolder.unreadCount.setVisibility(View.INVISIBLE);
            } else {
                GradientDrawable drawable = (GradientDrawable) groupItemHolder.unreadCount.getBackground();
                //drawable.setColor(Color.parseColor(PreferenceHelper.get(PreferenceKeys.Colors.COLOR_PRIMARY)));
                drawable.setColor(primaryColor);
                groupItemHolder.unreadCount.setVisibility(View.VISIBLE);
                groupItemHolder.unreadCount.setText(String.valueOf(unreadCount));
            }
        }
        groupItemHolder.container.setTag(R.string.group_id,cursor.getLong(cursor.getColumnIndex(Groups.COLUMN_GROUP_ID)));
        groupItemHolder.chatroomNameField.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(Groups.COLUMN_NAME))));
        groupItemHolder.imageviewchatroomAvatar.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
        groupItemHolder.usersOnline.setText(String.valueOf(memberCount));
        if(groupType == 1)
            groupItemHolder.protectedStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public GroupListAdapter.GroupItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_list_item, parent, false);
        return new GroupListAdapter.GroupItemHolder(v);
    }


    /**   Header View Methods      **/


    @Override
    public long getHeaderId(int position) {
        return getGroupByPosition(position).status;
    }

    @Override
    public GroupHeaderItemHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_list_header, parent, false);
        return new GroupHeaderItemHolder(view);
    }
    @Override
    public void onBindHeaderViewHolder(GroupHeaderItemHolder viewholder, int position ,long key) {
        int status = getGroupByPosition(position).status;
        if(status == 0){
            String langOtherGroup = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_OTHER_GROUP));
            viewholder.txtGroupStatus.setText(langOtherGroup);
        }
        else{
            String langJoinedGroup = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINED_GROUP));
            viewholder.txtGroupStatus.setText(langJoinedGroup);
        }
    }

    static class GroupItemHolder extends RecyclerView.ViewHolder {

        public TextView chatroomNameField,unreadCount,usersOnline,usersOnlineMessage;
        public ImageView imageviewchatroomAvatar,protectedStatus;
        public RelativeLayout container;

        public GroupItemHolder(View view) {
            super(view);
            chatroomNameField = (TextView) view.findViewById(R.id.textviewChatroomName);
            unreadCount = (TextView) view.findViewById(R.id.textviewChatroomUnreadCount);
            usersOnline = (TextView) view.findViewById(R.id.textViewChatroomUsersOnline);
            imageviewchatroomAvatar = (ImageView) view.findViewById(R.id.imageviewchatroomAvatar);
            usersOnlineMessage = (TextView) view.findViewById(R.id.textviewUsersOnlineMessage);
            protectedStatus = (ImageView) view.findViewById(R.id.imageViewGroupProtected);
            container = (RelativeLayout) view;
        }
    }

    static class GroupHeaderItemHolder extends RecyclerView.ViewHolder{

        public TextView txtGroupStatus;
        public GroupHeaderItemHolder(View view) {
            super(view);
            txtGroupStatus = (TextView) view.findViewById(R.id.txt_group_status);
        }
    }

    public void setCursor(Cursor cursor){
        this.cursor = cursor;
    }

    public Groups getGroupByPosition(int position) {
        Groups group = null;
        if(cursor.moveToPosition(position)) {
            group = new Groups();
            group.id = cursor.getLong(cursor.getColumnIndex(Groups.COLUMN_GROUP_ID));
            group.status = cursor.getInt(cursor.getColumnIndex(Groups.COLUMN_STATUS));
            group.memberCount = cursor.getInt(cursor.getColumnIndex(Groups.MEMBER_COUNT));
        }
        return group;
    }

}
