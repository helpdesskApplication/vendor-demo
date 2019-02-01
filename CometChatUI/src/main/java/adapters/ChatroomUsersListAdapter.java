package adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.RoundedImageView;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.plugins.ChatroomManager;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import Keys.BroadCastReceiverKeys;
import activities.CCShowChatroomUsersActivity;
import activities.CCSingleChatActivity;
import activities.CometChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;
import pojo.ChatroomMembers;

/**
 * Created by Inscripts on 16/06/17.
 */

public class ChatroomUsersListAdapter extends RecyclerView.Adapter<ChatroomUsersListAdapter.MyViewHolder>{

    private static final String TAG = ChatroomUsersListAdapter.class.getSimpleName();
    private List<ChatroomMembers> chatroomMembersList;
    private Context context;
    private long groupID;
    private boolean canBanUnban;
    private CometChat cometChat;
    public ChatroomUsersListAdapter(Context context, List<ChatroomMembers> chatroomMembersList,long groupID, boolean canBanUnban) {
        this.chatroomMembersList = chatroomMembersList;
        this.context = context;
        this.canBanUnban = canBanUnban;
        this.groupID = groupID;
        cometChat = CometChat.getInstance(context);

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cc_custom_list_item_chatroom_members, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final ChatroomMembers member = chatroomMembersList.get(position);
        holder.tv.setText(Html.fromHtml(member.getName()));
        LocalStorageFactory.loadImageUsingURL(context, member.getAvatarUrl(), holder.imageView, R.drawable.cc_default_avatar);
        if(canBanUnban && !holder.tv.getText().toString().equals("You")){
            holder.ownerOptions.setVisibility(View.VISIBLE);
        }else{
            holder.ownerOptions.setVisibility(View.GONE);
        }

        holder.tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSingleChatActivity(member.getId());
            }
        });
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSingleChatActivity(member.getId());
            }
        });
        Logger.error(TAG,"member Id: "+member.getId());
        holder.ownerOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(holder.rlUser,member);
            }
        });

    }

    private void openSingleChatActivity(long userId) {
        if (userId == SessionData.getInstance().getId()) {
//            context.startActivity(new Intent(context, CometChatActivity.class));
            ((Activity)context).finish();
        } else {
            Contact contact = Contact.getContactDetails(userId);
            if (contact != null) {
                Intent intent = new Intent(context, CCSingleChatActivity.class);
                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, userId);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "contact Not Available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPopup(RelativeLayout imgView, final ChatroomMembers member) {
        PopupMenu popup = new PopupMenu(context,imgView);
        popup.inflate(R.menu.cc_kick_ban_menu);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.kick){
                    kickUser(member);
                    Toast.makeText(context,"Kicked "+member.getName(),Toast.LENGTH_SHORT).show();
                }
                if(item.getItemId() == R.id.ban){
                    banUser(member);
                    Toast.makeText(context,"Banned "+member.getName(),Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        popup.setGravity(Gravity.END);
        popup.show();
    }

    private void kickUser(final ChatroomMembers member) {
        long userId = member.getId();
        final String userName = member.getName();
        if (!member.isModerator() || !member.isOwner()) {

            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Please wait...");
            progressDialog.setCancelable(false);

            cometChat.kickUserFromGroup(userId, groupID, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"kickUserFromGroup: successCallback(): "+jsonObject);
                    progressDialog.dismiss();
                    chatroomMembersList.remove(member);
                    Toast.makeText(context, userName+ " has been kicked from this group", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"kickUserFromGroup: failCallback(): "+jsonObject);
                    progressDialog.dismiss();
                }
            });
        } else {
            Toast.makeText(context, "Cannot kick Owner/Moderator.", Toast.LENGTH_SHORT).show();
        }
    }

    private void banUser(final ChatroomMembers member) {
        long userId = member.getId();
        final String userName = member.getName();
        if (!member.isModerator() || !member.isOwner()) {

            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Please wait...");
            progressDialog.setCancelable(false);

            cometChat.banUserFromGroup(userId, groupID, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {

                    Logger.error(TAG, "banUserChatroom successCallback : " + jsonObject);
                    progressDialog.dismiss();

                    Toast.makeText(context, userName + " banned from this group",
                            Toast.LENGTH_SHORT).show();
                    chatroomMembersList.remove(member);
                    notifyDataSetChanged();
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    progressDialog.dismiss();

                    Logger.error(TAG, "banUserChatroom failCallback : " + jsonObject.toString());
                    try {
                        String status = jsonObject.getString("status");
                        if (status.equals("no_internet")) {
                            Toast.makeText(context,
                                    "Please check your internet connection.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failure.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Toast.makeText(context, "Cannot ban Owner/Moderator.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return chatroomMembersList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rlUser;
        private TextView tv;
        private RoundedImageView imageView;
        private ImageView ownerOptions;
        public MyViewHolder(View view) {
            super(view);
            rlUser = (RelativeLayout) view.findViewById(R.id.rlUser);
            tv = (TextView) view.findViewById(R.id.textViewChatroomMemberName);
            imageView = (RoundedImageView) view.findViewById(R.id.imageViewShowUserAvatar);
            ownerOptions = (ImageView) view.findViewById(R.id.ownerOptions);
//            view.setOnCreateContextMenuListener(ChatroomUsersListAdapter.this);
        }
    }

    /*@Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

        if(canBanUnban) {
            String banStr = (String) CometChat.getInstance(context).getCCSetting(new CCSettingMapper(
                    SettingType.LANGUAGE, SettingSubType.LANG_BAN_CHATROOM_USER));
            String kickStr = "Kick";
            contextMenu.add(kickStr);
            contextMenu.add(banStr);
        }
    }*/

//    public int getClickedPosition() {
//        return clickedPosition;
//    }
}