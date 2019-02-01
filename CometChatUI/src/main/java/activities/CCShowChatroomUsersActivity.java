package activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import adapters.ChatroomUsersListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatcore.coresdk.MessageSDK;
import cometchat.inscripts.com.readyui.R;
import models.Groups;
import pojo.ChatroomMembers;

public class CCShowChatroomUsersActivity extends AppCompatActivity {

    private java.lang.String TAG = CCShowChatroomUsersActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;
    private int colorPrimary, colorPrimaryDark;
    private long groupID;
    private CometChat cometChatroom;
    private ArrayList<ChatroomMembers> members;
    private ChatroomUsersListAdapter adapter;
    private RecyclerView recyclerView;
    private CometChat cometChat;
    private String banStr;

    private int createdBy;
    private boolean isModerator = false;
    private boolean isOwner = false;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ccshow_chatroom_users);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.ViewChatroomUsersRecyclerView);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        ccContainer = (RelativeLayout) findViewById(R.id.cc_show_user_container);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
        }
        banStr = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_BAN_CHATROOM_USER));

        Logger.error(TAG, "banStr : " + banStr);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Users");
        setCCTheme();
        processIntent(getIntent());

        cometChatroom = CometChat.getInstance(this);
        cometChatroom.getGroupMembers(String.valueOf(groupID), new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "Get Chatroom Member responce = " + jsonObject);
                try {
                    processUsersJson(jsonObject.getJSONObject("users"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "Get Chatroom Member fail responce = " + jsonObject);
            }
        });

        /*recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getBaseContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                ChatroomMembers member = members.get(position);
                Long userId = member.getId();
                Logger.error(TAG, "userID " + userId);
                if (userId == SessionData.getInstance().getId()) {
                    startActivity(new Intent(getBaseContext(), CometChatActivity.class));
                    finish();
                } else {
                    Contact contact = Contact.getContactDetails(userId);
                    if (contact != null) {
                        Intent intent = new Intent(getApplicationContext(), CCSingleChatActivity.class);
                        intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, userId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CCShowChatroomUsersActivity.this, "contact Not Available", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));*/
    }

    private void processIntent(Intent intent) {

        if (intent.hasExtra("GroupID")) {
            groupID = intent.getLongExtra("GroupID", -1);
            createdBy = Groups.getGroupDetails(groupID).createdBy;
            Logger.error(TAG, "createdBy : " + createdBy);
        }

        if(intent.hasExtra("ismoderator")) {
            isModerator = intent.getBooleanExtra("ismoderator", false);
        }

        if(intent.hasExtra("isOwner")) {
            Logger.error(TAG,"Has isOwner ? ");
            isOwner = intent.getBooleanExtra("isOwner", false);
        }

        Logger.error(TAG,"isOwner ? "+isOwner);
        if(intent.hasExtra("user_id")){
            userId = intent.getLongExtra("user_id",0);
        }
    }

    private void processUsersJson(JSONObject chatroomMembersjson) throws JSONException {
        Logger.error(TAG,"processUsersJson: "+chatroomMembersjson);
        Iterator iterator = chatroomMembersjson.keys();
        members = new ArrayList<>();
        //boolean isOwner;
        while (iterator.hasNext()) {
            ChatroomMembers member = new ChatroomMembers();
            JSONObject data = chatroomMembersjson.getJSONObject(iterator.next().toString());
            if (data.getString("b").equals("0")) {
                member.setId(data.getLong("id"));
                member.setAvatarUrl(CommonUtils.processAvatarUrl(data.getString("a")));
                member.setBanned(data.getInt("b"));
                if (SessionData.getInstance().getId() == member.getId()) {
                    member.setName("You");
                } else {
                    member.setName(data.getString("n"));
                }
                if(data.has("ismoderator")){
                    member.setModerator(Boolean.parseBoolean(data.getString("ismoderator")));
                }
                if(userId == 0){
                    member.setOwner(false);
                }else if(userId == data.getLong("id")){
                    member.setOwner(true);
                }
                Logger.error(TAG,"Member: "+member);
                Logger.error(TAG,"Member: "+member.getName() +" isOwner : " + member.isOwner());
                members.add(member);
            }
        }

        /*if (createdBy == 1) {
            isOwner = true;
        } else {
            isOwner = false;
        }*/
        boolean canBanUnban = false;
        if(isModerator|| isOwner){
            canBanUnban = true;
        }
        adapter = new ChatroomUsersListAdapter(this, members,groupID, canBanUnban);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void setCCTheme() {
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this, colorPrimaryDark);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            MessageSDK.closeCometChatWindow(this, ccContainer);
        }

        return super.onOptionsItemSelected(item);
    }

    /*@Override
    public boolean onContextItemSelected(MenuItem item) {
        String title = (String) item.getTitle();
        if (title.equals(banStr)) {
            *//*final int clickedPosition = adapter.getClickedPosition();
            ChatroomMembers chatroomMembers = members.get(clickedPosition);
            long userId = chatroomMembers.getId();
            final String userName = chatroomMembers.getName();
            Logger.error(TAG, "clicked name : " + chatroomMembers.getName());
            Logger.error(TAG, "clicked id : " + userId);
            Logger.error(TAG, "createdBy : " + createdBy);
            Logger.error(TAG, "getId() : " + SessionData.getInstance().getId());
            Logger.error(TAG, "isismoderator : " + chatroomMembers.isModerator());
            Logger.error(TAG,"isOwner: "+chatroomMembers.isOwner());
            if (!chatroomMembers.isModerator() || !chatroomMembers.isOwner()) {

                progressDialog = ProgressDialog.show(this, "", "Please wait...");
                progressDialog.setCancelable(false);

                ChatroomManager.banUserChatroom(userId, groupID, 1, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {

                        Logger.error(TAG, "banUserChatroom successCallback : " + jsonObject);
                        progressDialog.dismiss();

                        Toast.makeText(CCShowChatroomUsersActivity.this, userName + " banned from this group",
                                Toast.LENGTH_SHORT).show();
                        members.remove(clickedPosition);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        progressDialog.dismiss();

                        Logger.error(TAG, "banUserChatroom failCallback : " + jsonObject.toString());
                        try {
                            String status = jsonObject.getString("status");
                            if (status.equals("no_internet")) {
                                Toast.makeText(CCShowChatroomUsersActivity.this,
                                        "Please check your internet connection.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(CCShowChatroomUsersActivity.this, "Failure.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                Toast.makeText(CCShowChatroomUsersActivity.this, "Can not ban owner/moderator.", Toast.LENGTH_SHORT).show();
            }*//*
        }
        if(title.equals("Kick")){
            final int clickedPosition = adapter.getClickedPosition();
            final ChatroomMembers chatroomMembers = members.get(clickedPosition);
            long userId = chatroomMembers.getId();
            final String userName = chatroomMembers.getName();
            if (!chatroomMembers.isModerator() || !chatroomMembers.isOwner()) {

                progressDialog = ProgressDialog.show(this, "", "Please wait...");
                progressDialog.setCancelable(false);

                ChatroomManager.kickUserFromGroup(userId, 1, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"kickUserFromGroup: successCallback(): "+jsonObject);
                        progressDialog.dismiss();
                        members.remove(chatroomMembers);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"kickUserFromGroup: failCallback(): "+jsonObject);
                        progressDialog.dismiss();
                    }
                });
            } else {
                Toast.makeText(CCShowChatroomUsersActivity.this, "Can not ban owner/moderator.", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }*/
}
