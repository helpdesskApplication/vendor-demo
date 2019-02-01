package adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.inscripts.custom.RoundedImageView;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.RecyclerViewCursorAdapter;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.pojos.CCSettingMapper;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;


public class ContactListAdapter extends RecyclerViewCursorAdapter<ContactListAdapter.ContactItemHolder> {

    private static final java.lang.String TAG = ContactListAdapter.class.getSimpleName();
    private Context context;
    int primaryColor;
    private CometChat cometChat;
    public ContactListAdapter(Context context , Cursor c) {
        super(c);
        this.context = context;
        cometChat = CometChat.getInstance(context);
        primaryColor = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
    }

    @Override
    public void onBindViewHolder(ContactItemHolder contactItemHolder, Cursor cursor) {
        contactItemHolder.userName.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(Contact.COLUMN_NAME))));
        contactItemHolder.userStatus.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(Contact.COLUMN_STATUS_MESSAGE))));

        contactItemHolder.avatar.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
        String avatarURL = cursor.getString(cursor.getColumnIndex(Contact.COLUMN_AVATAR_URL));
        LocalStorageFactory.loadImageUsingURL(context,avatarURL,contactItemHolder.avatar,R.drawable.cc_ic_default_avtar);


        contactItemHolder.view.setTag(R.string.contact_id,cursor.getLong(cursor.getColumnIndex(Contact.COLUMN_CONTACT_ID)));
        contactItemHolder.view.setTag(R.string.contact_name,Html.fromHtml(cursor.getString(cursor.getColumnIndex(Contact.COLUMN_NAME))).toString());

        boolean recentChatEnabled = (boolean)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.RECENT_CHAT_ENABLED));
        if (recentChatEnabled || 0 == cursor.getInt(cursor.getColumnIndex(Contact.COLUMN_UNREAD_COUNT))){
            contactItemHolder.unreadCount.setVisibility(View.GONE);
        } else {
            GradientDrawable drawable = (GradientDrawable) contactItemHolder.unreadCount.getBackground();
            drawable.setColor(primaryColor);
            contactItemHolder.unreadCount.setVisibility(View.VISIBLE);
            contactItemHolder.unreadCount.setText(String.valueOf(cursor.getInt(cursor.getColumnIndex(Contact.COLUMN_UNREAD_COUNT))));
        }

        switch (cursor.getString(cursor.getColumnIndex(Contact.COLUMN_STATUS)).toLowerCase()) {
            case CometChatKeys.StatusKeys.AVALIABLE:
                contactItemHolder.statusImage.setImageResource(R.drawable.cc_status_online);
                break;
            case CometChatKeys.StatusKeys.AWAY:
                contactItemHolder.statusImage.setImageResource(R.drawable.cc_status_available);
                break;
            case CometChatKeys.StatusKeys.BUSY:
                contactItemHolder.statusImage.setImageResource(R.drawable.cc_status_busy);
                break;
            case CometChatKeys.StatusKeys.OFFLINE:
                contactItemHolder.statusImage.setImageResource(R.drawable.cc_status_ofline);
                break;
            case CometChatKeys.StatusKeys.INVISIBLE:
                contactItemHolder.statusImage.setImageResource(R.drawable.cc_status_ofline);
                break;
            default:
                contactItemHolder.statusImage.setImageResource(R.drawable.cc_status_available);
                break;
        }


       /* contactItemHolder.view.setTag(R.string.buddy_id,cursor.getString(cursor.getColumnIndex(Contact.COLUMN_COMET_ID)));

        contactItemHolder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Buddy buddy = Buddy.getBuddyDetails((String) view.getTag(R.string.buddy_id));

                if(contactItemHolder.sml.isMenuOpen()){
                    contactItemHolder.sml.smoothCloseMenu();
                }else{
                    contactItemHolder.sml.smoothOpenBeginMenu();
                    contactItemHolder.sml.smoothCloseMenu();

                    Intent intent = new Intent(context, SingleChatActivity.class);
                    intent.putExtra(BroadcastReceiverKeys.IntentExtrasKeys.CONTACT_ID,buddy.contactId);
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL).isEmpty()) {
                        intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
                    }
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL).isEmpty()) {
                        intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
                    }
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL).isEmpty()) {
                        intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
                    }
                    if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL).isEmpty()) {
                        intent.putExtra("FileUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL));
                    }
                    intent.putExtra(BroadcastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, buddy.name);


                    SessionData.getInstance().setTopFragment(StaticMembers.TOP_FRAGMENT_ONE_ON_ONE);
                    context.startActivity(intent);

                    if (0L != buddy.unreadCount) {
                        buddy.unreadCount = 0;
                        buddy.save();
                        Intent iintent = new Intent(BroadcastReceiverKeys.HeartbeatKeys.ANNOUNCEMENT_BADGE_UPDATION);
                        iintent.putExtra(BroadcastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                        PreferenceHelper.getContext().sendBroadcast(iintent);
                        SessionData.getInstance().setChatbadgeMissed(true);
                        notifyDataSetChanged();
                    }
                }

            }
        });*/
    }

    @Override
    public ContactItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list_item, parent, false);
        return new ContactItemHolder(v);
    }

    static class ContactItemHolder extends RecyclerView.ViewHolder {

        public TextView userName;
        public TextView userStatus;
        public TextView unreadCount;
        public RoundedImageView avatar;
        public ImageView avtar_image;
        public ImageView statusImage;
        public View view;

        public ContactItemHolder(View view) {
            super(view);
            avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
            userName = (TextView) view.findViewById(R.id.textviewUserName);
            userStatus = (TextView) view.findViewById(R.id.textviewUserStatus);
            statusImage = (ImageView) view.findViewById(R.id.imageViewStatusIcon);
            unreadCount = (TextView) view.findViewById(R.id.textviewSingleChatUnreadCount);
            this.view = view;
        }
    }

}
