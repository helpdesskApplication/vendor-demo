/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.jsonphp.JsonPhp;
import com.inscripts.jsonphp.Lang;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.plugins.ChatroomManager;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import adapters.UnbanChatroomUserAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Groups;
import pojo.UnbanUsers;


public class UnbanChatroomUserActivity extends AppCompatActivity {
    private static final String TAG = UnbanChatroomUserActivity.class.getSimpleName() ;
    private UnbanChatroomUserAdapter adapter;
    private ListView listview;
    private ArrayList<UnbanUsers> unban;
    private TextView tv;
    private Button cancel, send;

    ArrayList<String> positionChecked = new ArrayList<>();
    Lang lang = JsonPhp.getInstance().getLang();
    private String title;
    private CometChat cometChat;
    private long groupId;
    private Toolbar toolbar;
    private List<Long> unbanListLong = new ArrayList<>();
    private static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unban_chatroom_user);
        Intent intent = getIntent();
        unban = new ArrayList<>();

        cometChat = CometChat.getInstance(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        title = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,
                SettingSubType.LANG_UNBAN_CHATROOM_TITLE));
        getSupportActionBar().setTitle(title);

        int color = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS,
                SettingSubType.COLOR_PRIMARY));
        toolbar.setBackgroundColor(color);

        groupId = intent.getLongExtra("GroupID", -1);
        cancel = (Button) findViewById(R.id.buttonCancel);
        send = (Button) findViewById(R.id.buttonUnbanUser);
        listview = (ListView) findViewById(R.id.listView);
        tv = (TextView) findViewById(R.id.noUser);

        String textNoUser = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,
                SettingSubType.LANG_UNBAN_CHATROOM_NO_USER));
        if (textNoUser != null) {
            tv.setText(textNoUser);
        }

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*unbanListLong.clear();

                List<UnbanUsers> unbanUsersList = adapter.getListCheckedUnbannedUsers();
                for (UnbanUsers unbanUsers : unbanUsersList) {
                    Logger.error("names : " + unbanUsers.getName());
                    unbanListLong.add(unbanUsers.getId());
                }

                if (unbanListLong.size() > 0) {
                    sendUnbanUserList(groupId);
                } else {
                    Toast.makeText(UnbanChatroomUserActivity.this, "Please select users.",
                            Toast.LENGTH_SHORT).show();
                }*/
            }
        });

        cometChat.getGroupMembers(String.valueOf(groupId), new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error("UnbanChatroom", "Get Chatroom Member responce = " + jsonObject);
                try {
                    processUsersJson(jsonObject.getJSONObject("users"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error("UnbanChatroom", "Get Chatroom Member fail responce = " + jsonObject);
            }
        });
    }

    private void processUsersJson(JSONObject chatroomMembersjson) throws JSONException {
        Logger.error(TAG,"processChatroomMemberJson: "+chatroomMembersjson);
        Iterator iterator = chatroomMembersjson.keys();
        unban = new ArrayList<>();
        while (iterator.hasNext()) {
            UnbanUsers member = new UnbanUsers();
            JSONObject data = chatroomMembersjson.getJSONObject(iterator.next().toString());
            if (data.getString("b").equals("1")) {
                member.setId(data.getLong("id"));
                member.setAvatarUrl(CommonUtils.processAvatarUrl(data.getString("a")));
                if (SessionData.getInstance().getId() == member.getId()) {
                    member.setName("You");
                } else {
                    member.setName(data.getString("n"));
                }
                unban.add(member);
            }
        }
        tv.setVisibility(View.GONE);

        adapter = new UnbanChatroomUserAdapter(this, unban, positionChecked);
        listview.setAdapter(adapter);
    }

    public void sendUnbanUserList(long groupId) {

        progressDialog = ProgressDialog.show(this, "", "Please wait...");
        progressDialog.setCancelable(false);

        Groups chatroom = Groups.getGroupDetails(groupId);
        String unbanUsersList = unbanListLong.toString();
        Logger.error("UnbanChatroomUserActivity list : " + unbanUsersList);
        Logger.error("UnbanChatroomUserActivity roomId : " + groupId);
        Logger.error("UnbanChatroomUserActivity password : " + chatroom.password);
        Logger.error("UnbanChatroomUserActivity name : " + chatroom.name);

        ChatroomManager.unbanUserChatroom(this, unbanUsersList, groupId, chatroom.password, chatroom.name, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error("UnbanChatroomUserActivity successCallback : " + jsonObject);
                Toast.makeText(UnbanChatroomUserActivity.this, "Users unbanned successfully.", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error("UnbanChatroomUserActivity failCallback : " + jsonObject);
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_invite_user, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.custom_action_search);
        searchMenuItem.setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.custom_action_done ){
            unbanListLong.clear();

            List<UnbanUsers> unbanUsersList = adapter.getListCheckedUnbannedUsers();
            for (UnbanUsers unbanUsers : unbanUsersList) {
                Logger.error("names : " + unbanUsers.getName());
                unbanListLong.add(unbanUsers.getId());
            }

            if (unbanListLong.size() > 0) {
                sendUnbanUserList(groupId);
            } else {
                Toast.makeText(UnbanChatroomUserActivity.this, "Please select users.",
                        Toast.LENGTH_SHORT).show();
            }
        }
        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
