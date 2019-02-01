/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.inscripts.custom.RoundedImageView;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;

public class BroadcastListAdapter extends ArrayAdapter<Contact> {

    private ArrayList<String> inviteList;
    private int colorPrimary;
    private CometChat cometChat;
    private Context context;
    public BroadcastListAdapter(Context context, List<Contact> objects, ArrayList<String> savedCheckbox) {
        super(context, 0, objects);
        this.context = context;
        inviteList = new ArrayList<String>();
        if (null != savedCheckbox) {
            for (String i : savedCheckbox) {
                inviteList.add(i);
            }
        }
        cometChat = CometChat.getInstance(context);
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        Logger.error("colorPrimary : "+colorPrimary);
    }



    public void toggleInvite(String id) {
        if (inviteList.contains(id)) {
            inviteList.remove(id);
        } else {
            inviteList.add(id);
        }
    }
    public void addInvite(String id) {
        if (!inviteList.contains(id)) {
            inviteList.add(id);
        }
    }
    public void removeInvite(String id) {
        if (inviteList.contains(id)) {
            inviteList.remove(id);
        }
    }

    public int getInvitedUsersCount() {
        return inviteList.size();
    }

    public ArrayList<String> getInviteUsersList() {
        return inviteList;
    }

    public void clearInviteList() {
        inviteList.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (null == view) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_broadcast_list_item, parent, false);
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_custom_list_item_broadcast, parent,
                    false);
            holder = new ViewHolder();

            holder.userName = (TextView) view.findViewById(R.id.textViewUserToInvite);
            holder.userStatus = (TextView) view.findViewById(R.id.textViewUserStatusToInvite);
            holder.checkBox = (CheckBox) view.findViewById(R.id.checkBoxInviteUser);
            holder.statusImage = (ImageView) view.findViewById(R.id.imageViewStatusIconToInvite);
            holder.avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Contact contact = getItem(position);
        String contactId = String.valueOf(contact.contactId);

        /**  Replacing Picasso with glide **/
        /*LocalStorageFactory.getPicassoInstance().load(buddy.avatarURL).placeholder(R.drawable.default_avatar)
                .resize(55, 55).into(holder.avatar);*/

        LocalStorageFactory.loadImageUsingURL(context,contact.avatarURL,holder.avatar,R.drawable.cc_default_avatar);


        holder.userName.setText(Html.fromHtml(contact.name));
        holder.userStatus.setText(Html.fromHtml(contact.statusMessage));

        holder.checkBox.setChecked(inviteList.contains(contactId));
        holder.checkBox.setTag(contactId);


        ColorStateList colorStateList = new ColorStateList(
                new int[][]{

                        new int[]{-android.R.attr.state_enabled},
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[] {
                        Color.BLACK //disabled
                        , Color.parseColor("#E0E0E0")
                        , colorPrimary

                }
        );


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.checkBox.setButtonTintList(colorStateList);
            }


		/* Set color according to the status. */
        switch (contact.status) {
            case CometChatKeys.StatusKeys.AVALIABLE:
                holder.statusImage.setImageResource(R.drawable.cc_status_available);
                break;
            case CometChatKeys.StatusKeys.AWAY:
                holder.statusImage.setImageResource(R.drawable.cc_status_ofline);
                break;
            case CometChatKeys.StatusKeys.BUSY:
                holder.statusImage.setImageResource(R.drawable.cc_status_busy);
                break;
            case CometChatKeys.StatusKeys.OFFLINE:
                holder.statusImage.setImageResource(R.drawable.cc_status_ofline);
                break;
            case CometChatKeys.StatusKeys.INVISIBLE:
                holder.statusImage.setImageResource(R.drawable.cc_status_ofline);
                break;
            default:
                holder.statusImage.setImageResource(R.drawable.cc_status_available);
                break;
        }
        return view;
    }

    private static class ViewHolder {
        public TextView userName;
        public TextView userStatus;
        public CheckBox checkBox;
        public RoundedImageView avatar;
        public ImageView statusImage;
    }
}
