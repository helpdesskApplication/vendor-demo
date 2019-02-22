package adapters;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.inscripts.custom.EmojiTextView;
import com.inscripts.custom.RoundedImageView;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.RecyclerViewCursorAdapter;
import com.inscripts.plugins.Smilies;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import fragments.RecentFragment;
import models.Contact;
import models.Conversation;
import models.Groups;

public class RecentListAdapter extends RecyclerViewCursorAdapter<RecentListAdapter.RecentItemHolder> {

    private static final String TAG = RecentListAdapter.class.getSimpleName();
    private Context context;
    private int colorPrimary;
    private boolean isTyping;
    private String isTypingContactId;
    private CometChat cometChat;

    public RecentListAdapter(Activity context, RecentFragment recentFragment, Cursor c) {
        super(c);
        this.context = context;
        cometChat = CometChat.getInstance(context);
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
    }

    public void setIsTyping(boolean isTyping, String contactId) {
        this.isTyping = isTyping;
        this.isTypingContactId = contactId;
    }

    @Override
    public void onBindViewHolder(final RecentItemHolder holder, Cursor cursor) {
        final boolean isChatroom = cursor.getLong(cursor.getColumnIndex(Conversation.COLUMN_CHATROOM_ID)) != 0;
        long chatroomId = 0;
        if(isChatroom){
            chatroomId = cursor.getLong(cursor.getColumnIndex(Conversation.COLUMN_CHATROOM_ID));
            Logger.error(TAG, "onBindViewHolder: chatroomId: "+chatroomId);
        }
        CharSequence userName = "";
        if (cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_NAME)) != null) {
            userName = Html.fromHtml(cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_NAME)));
        }
        holder.userName.setText(userName);
        holder.statusImage.setVisibility(View.INVISIBLE);
        holder.userStatus.setVisibility(View.INVISIBLE);
        holder.userLastMessage.setVisibility(View.VISIBLE);

        String lastMessage = cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_LAST_MESSAGE));

        Logger.error(TAG, "lastMessage : " + lastMessage);

        if (!isChatroom && isTyping && isTypingContactId.equals(cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_BUDDY_ID)))) {
            holder.userLastMessage.setText("typing...");
            holder.userLastMessage.setTextColor(colorPrimary);
        } else {
            if(lastMessage == null){
                holder.userLastMessage.setText("");
            }else {
                if (lastMessage.contains("<img class=\"cometchat_smiley\"")) {
                    holder.userLastMessage.setEmojiText("Smileys",(int)context.getResources().getDimension(R.dimen.emoji_size));
                } else {
                    holder.userLastMessage.setEmojiText(lastMessage,(int)context.getResources().getDimension(R.dimen.emoji_size));
                }
            }
            holder.userLastMessage.setTextColor(Color.parseColor("#a2a2a5"));
        }

        if (isChatroom) {
            holder.avatar.setVisibility(View.INVISIBLE);
            holder.chatroomAvtar.setVisibility(View.VISIBLE);
            holder.chatroomAvtar.setImageResource(R.drawable.cc_ic_group);
            holder.chatroomAvtar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
            Groups groups = Groups.getGroupDetails(chatroomId);
            if (groups != null && groups.type == 1) {
                holder.imgLock.setVisibility(View.VISIBLE);
            }else {
                holder.imgLock.setVisibility(View.GONE);
            }
        } else {
            holder.avatar.setVisibility(View.VISIBLE);
            holder.chatroomAvtar.setVisibility(View.GONE);
            holder.avatar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
            String avatarURL = cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_AVTAR_URL));
            LocalStorageFactory.loadImageUsingURL(context, avatarURL, holder.avatar, R.drawable.cc_ic_default_avtar);
        }

        String unreadCount = String.valueOf(cursor.getInt(cursor.getColumnIndex(Conversation.COLUMN_UNREAD_COUNT)));
        Logger.error(TAG, userName + "unreadCount : " + unreadCount);
        if (0 == cursor.getInt(cursor.getColumnIndex(Conversation.COLUMN_UNREAD_COUNT))) {
            holder.unreadCount.setVisibility(View.GONE);
        } else {
            GradientDrawable drawable = (GradientDrawable) holder.unreadCount.getBackground();
            drawable.setColor(colorPrimary);
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(cursor.getInt(cursor.getColumnIndex(Contact.COLUMN_UNREAD_COUNT))));
        }


        holder.view.setTag(R.string.contact_id, cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_BUDDY_ID)));
        holder.view.setTag(R.string.group_id, cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_CHATROOM_ID)));
    }

    @Override
    public RecentItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.contact_list_item, parent, false);
        return new RecentItemHolder(v);
    }

    static class RecentItemHolder extends RecyclerView.ViewHolder {

        public TextView userName;
        public TextView userStatus;
        public EmojiTextView userLastMessage;
        public TextView unreadCount;
        public RoundedImageView avatar;
        public ImageView chatroomAvtar;
        public ImageView statusImage;
        public ImageView imgLock;
        public View view;

        public RecentItemHolder(View view) {
            super(view);
            avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
            chatroomAvtar = (ImageView) view.findViewById(R.id.imageviewchatroomAvatar);
            userName = (TextView) view.findViewById(R.id.textviewUserName);
            userStatus = (TextView) view.findViewById(R.id.textviewUserStatus);
            statusImage = (ImageView) view.findViewById(R.id.imageViewStatusIcon);
            unreadCount = (TextView) view.findViewById(R.id.textviewSingleChatUnreadCount);
            userLastMessage = (EmojiTextView) view.findViewById(R.id.textviewLastMessage);
            imgLock = view.findViewById(R.id.imgLock);
            this.view = view;
        }
    }
}

