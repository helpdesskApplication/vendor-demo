/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.StaticMembers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import adapters.InviteUserListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;

//import cometchat.inscripts.com.cometchatcore.coresdk.CometChatroom;


public class CCInviteUserActivity extends AppCompatActivity implements OnItemClickListener, SearchView.OnQueryTextListener {

    private long chatroomId;
    private InviteUserListAdapter adapter;
    private ListView inviteUserListView;
    private String chatroomName;
    private static String checkBoxKeyForBundle = "checkBoxState";
    private TextView noUsersOnline;
//    private MobileTheme theme;
//    private Css css;
    private SearchView searchView;
//    private Lang language;
    private String noBuddyText = StaticMembers.NO_BUDDY_TEXT;
    private static String onoSearchText = "";
    private boolean isSearching = false;
    private Toolbar toolbar;
    private RelativeLayout ccContainer;
    private boolean isChecked;
    private CometChat cometChatroom;
    private java.lang.String TAG = CCInviteUserActivity.class.getSimpleName();
    private CometChat cometChat;
    private int colorPrimary;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_invite_users);
        Intent intent = getIntent();
        chatroomId = intent.getLongExtra(StaticMembers.INTENT_CHATROOM_ID, 0);
        chatroomName = intent.getStringExtra(StaticMembers.INTENT_CHATROOM_NAME);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        cometChat = CometChat.getInstance(this);
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        ccContainer = (RelativeLayout) findViewById(R.id.invite_user_container);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else{
            toolbar.setBackgroundColor(colorPrimary);
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Invite Users");

        cometChatroom = CometChat.getInstance(this);
        inviteUserListView = (ListView) findViewById(R.id.listViewInviteUsers);
        noUsersOnline = (TextView) findViewById(R.id.textviewNoUsersToInvite);

        noBuddyText =(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NO_USERS));

        ArrayList<String> savedCheckbox = new ArrayList<>();

        if (null != savedInstanceState) {
            savedCheckbox = savedInstanceState.getStringArrayList(checkBoxKeyForBundle);
        }

        try {
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.JSON_CHATROOM_MEMBERS)) {
                JSONObject chatroomUserList = new JSONObject(
                        PreferenceHelper.get(PreferenceKeys.DataKeys.JSON_CHATROOM_MEMBERS));
                setupInviteUserListView(chatroomUserList, savedCheckbox);
            } else {
                Logger.error("No CR member in pref");
                setupInviteUserListView(null, savedCheckbox);
            }
        } catch (Exception e) {
            Logger.error(this.getClass().getSimpleName() + ": exception in oncreate " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_invite_user, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.custom_action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(Html.fromHtml("<font color = #ffffff>" + (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SEARCH))
                + "</font>"));

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

            @SuppressLint("NewApi")
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (TextUtils.isEmpty(searchView.getQuery())) {
                        searchView.setIconified(true);
                        isSearching = false;
                    }
                } else {
                    if (!searchView.isIconified()) {
                        searchView.setIconified(false);
                    }
                }
            }
        });

        MenuItem inviteUserItem =  menu.findItem(R.id.custom_action_done);
        if(isChecked){
            inviteUserItem.setVisible(true);
        }else{
            inviteUserItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void setupInviteUserListView(final JSONObject chatroomMemberList, final ArrayList<String> checkBoxState) {
        List<Contact> buddyList;
        try {
            if (null != chatroomMemberList) {
                Iterator<String> iter = chatroomMemberList.keys();
                Set<String> ids = new HashSet<>();

                while (iter.hasNext()) {
                    ids.add(chatroomMemberList.getJSONObject(iter.next()).getString(CometChatKeys.AjaxKeys.ID));
                }
                buddyList = Contact.getExternalBuddies(ids);
            } else {
                //buddyList = Buddy.getAllBuddies();
                buddyList = Contact.getAllContacts();
            }

            if (null != buddyList && buddyList.size() > 0) {
                adapter = new InviteUserListAdapter(this, buddyList, checkBoxState);
                inviteUserListView.setAdapter(adapter);
                inviteUserListView.setOnItemClickListener(CCInviteUserActivity.this);
                noUsersOnline.setVisibility(View.GONE);
                /*inviteButton.setAlpha(1F);
                inviteButton.setEnabled(true);*/
            } else {
                noUsersOnline.setVisibility(View.VISIBLE);
                noUsersOnline.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_USERS)));
              /*  inviteButton.setAlpha(0.5F);
                inviteButton.setEnabled(false);*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            ArrayList<String> invitedUsers = adapter.getInviteUsersList();
            outState.clear();
            outState.putStringArrayList(checkBoxKeyForBundle, invitedUsers);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxInviteUser);
        checkBox.setChecked(!checkBox.isChecked());
        adapter.toggleInvite(checkBox.getTag().toString());
        // Logger.error("List: " + adapter.getInviteUsersList());
        ArrayList<String> invitedUsers = adapter.getInviteUsersList();
        if(invitedUsers.size()>0){
            isChecked = true;
            invalidateOptionsMenu();
        }else{
            isChecked = false;
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        if (inviteUserListView != null && inviteUserListView.getAdapter() != null) {
            searchText = searchText.replaceAll("^\\s+", "");
            if (!searchView.isIconified() && !TextUtils.isEmpty(searchText)) {
                onoSearchText = searchText;
            }
            if (!TextUtils.isEmpty(searchText)) {
                searchUser(searchText, true);
            } else {
                searchUser(searchText, false);
            }
        }
        return false;
    }

    private void searchUser(String searchText, boolean search) {
        List<Contact> list;
        if (search) {
            isSearching = true;
            list = Contact.searchContacts(searchText);
        } else {
            isSearching = false;
            //list = Buddy.getAllBuddies();
            list = Contact.getAllContacts();
        }

        if (null == list || list.size() == 0) {
            if (!search) {
                noUsersOnline.setText(noBuddyText);
            } else {
                noUsersOnline.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_USERS)));
            }
            noUsersOnline.setVisibility(View.VISIBLE);
        } else {
            noUsersOnline.setVisibility(View.GONE);
        }

        adapter.clear();
        adapter.addAll(list);
        //adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }else if(item.getItemId() == R.id.custom_action_done) {
            try {
                if (null != adapter) {
                    if (adapter.getInvitedUsersCount() > 0) {
                        ArrayList<String> invitedUsers = adapter.getInviteUsersList();
                        JSONArray users = new JSONArray();
                        for (String id : invitedUsers) {
                            users.put(id);
                        }
                        cometChatroom.inviteUser(users, String.valueOf(chatroomId),chatroomName, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG, "inviteUser successCallback : " + jsonObject);
                                adapter.clearInviteList();
//                                finish();
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG, "inviteUser failCallback : " + jsonObject);
                            }
                        });
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(),(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SELECT_USERS)), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Logger.error(TAG, "InviteuserActivity.java: inviteButtononClick exception =" + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0,0);
    }
}
