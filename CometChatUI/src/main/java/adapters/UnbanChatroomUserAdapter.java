/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.custom.RoundedImageView;
import com.inscripts.factories.LocalStorageFactory;

import java.util.ArrayList;
import java.util.List;

import cometchat.inscripts.com.readyui.R;
import pojo.UnbanUsers;

public class UnbanChatroomUserAdapter extends ArrayAdapter<UnbanUsers> {
    LayoutInflater inflater;
    Context context;
    ArrayList list;
    ArrayList<String> positionChecked;
    ArrayList<Long> checkedID = new ArrayList<>();

    List<UnbanUsers> listCheckedUnbannedUsers = new ArrayList<>();

    public UnbanChatroomUserAdapter(Context context, ArrayList<UnbanUsers> objects, ArrayList<String> positionChecked) {
        super(context, 0, objects);
        this.context = context;
        this.positionChecked = positionChecked;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private class ViewHolder {
        private RelativeLayout rlUnban;
        private TextView tv;
        private RoundedImageView imageView;
        private CheckBox cb;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        if (null == convertView) {
            convertView = inflater.inflate(R.layout.custom_list_item_unban_user, null);
            holder = new ViewHolder();
            holder.rlUnban = (RelativeLayout) convertView.findViewById(R.id.rlUnban);
            holder.tv = (TextView) convertView.findViewById(R.id.textViewChatroomMemberName);
            holder.imageView = (RoundedImageView) convertView.findViewById(R.id.imageViewShowUserAvatar);
            holder.cb = (CheckBox) convertView.findViewById(R.id.checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final UnbanUsers member = getItem(position);
        holder.tv.setText(Html.fromHtml(member.getName()));

        LocalStorageFactory.loadImageUsingURL(context, member.getAvatarUrl(), holder.imageView, R.drawable.default_avatar);

        holder.rlUnban.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (holder.cb.isChecked()) {
                    holder.cb.setChecked(false);
                    member.setChecked(false);
                    createList(member, false);
                    notifyDataSetChanged();
                } else {
                    holder.cb.setChecked(true);
                    member.setChecked(true);
                    createList(member, true);
                    notifyDataSetChanged();
                }
            }
        });

        return convertView;
    }

    private void createList(UnbanUsers unbanUsers1, boolean add) {
        long userId = unbanUsers1.getId();
        if (add) {
            boolean found = false;
            for (UnbanUsers unbanUsers : listCheckedUnbannedUsers) {
                long checkedUnbannedId = unbanUsers.getId();
                if (userId == checkedUnbannedId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                listCheckedUnbannedUsers.add(unbanUsers1);
            }
        } else {
            for (UnbanUsers unbanUsers : listCheckedUnbannedUsers) {
                long checkedUnbannedId = unbanUsers.getId();
                if (userId == checkedUnbannedId) {
                    listCheckedUnbannedUsers.remove(unbanUsers1);
                    break;
                }
            }
        }
    }

    public List<UnbanUsers> getListCheckedUnbannedUsers() {
        return listCheckedUnbannedUsers;
    }
}
