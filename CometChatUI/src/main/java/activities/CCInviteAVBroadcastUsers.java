/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.StaticMembers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import adapters.BroadcastListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;

public class CCInviteAVBroadcastUsers extends AppCompatActivity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {

    private static final java.lang.String TAG = CCInviteAVBroadcastUsers.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    String roomName;
    private ListView listview;
    private TextView noUserView;
//    private Mobile mobileLangs;
//    private MobileTheme theme;
    private BroadcastListAdapter adapter;
    private static String checkBoxKeyForBundle = "checkBoxState";
    private SearchView searchView;
//    private Lang language;
    private String noBuddyText ;
    private ProgressBar wheel;
//    private Css css;
//    private Button cancelBtn, sendBtn;
    private CometChat cometChat;

    private int colorPrimary, colorPrimaryDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_invite_avbroadcast_users);

        cometChat = CometChat.getInstance(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_av_users_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        roomName = intent.getStringExtra(StaticMembers.INTENT_ROOM_NAME);
        listview = (ListView) findViewById(R.id.listviewBroadcast);
        wheel = (ProgressBar) findViewById(R.id.progressWheel);
        noUserView = (TextView) findViewById(R.id.noUsersOnline);
        noBuddyText = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NO_USERS));
        setCCTheme();

        ArrayList<String> savedCheckbox = new ArrayList<>();
        if (null != savedInstanceState) {
            savedCheckbox = savedInstanceState.getStringArrayList(checkBoxKeyForBundle);
        }
        this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_USERS)));
        setupInviteUserListView(savedCheckbox);
    }

    private void setCCTheme(){
        colorPrimary = (int) cometChat.getInstance(this).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getInstance(this).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this,colorPrimaryDark);
        wheel.getIndeterminateDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }

    private void setupInviteUserListView(final ArrayList<String> checkBoxState) {
        List<Contact> buddyList;
        try {
            buddyList = Contact.getAllContacts();
            if (null != buddyList && buddyList.size() > 0) {
                adapter = new BroadcastListAdapter(this, buddyList, checkBoxState);
                listview.setAdapter(adapter);
                listview.setOnItemClickListener(CCInviteAVBroadcastUsers.this);
                noUserView.setVisibility(View.GONE);
            } else {
                noUserView.setVisibility(View.VISIBLE);
                noUserView.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_USERS)));

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
    }

    private void startWheel() {
        wheel.setVisibility(View.VISIBLE);
    }

    private void stopWheel() {
        wheel.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_broadcast_message, menu);
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
                    }
                } else {
                    if (!searchView.isIconified()) {
                        searchView.setIconified(false);
                    }
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        if (listview != null && listview.getAdapter() != null) {
            searchText = searchText.replaceAll("^\\s+", "");
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
            list = Contact.searchContacts(searchText);
        } else {
            list = Contact.getAllContacts();
        }

        if (null == list || list.size() == 0) {
            if (!search) {
                noUserView.setText(noBuddyText);
            } else {
                noUserView.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_USER_FOUND)));
            }
            noUserView.setVisibility(View.VISIBLE);
        } else {
            noUserView.setVisibility(View.GONE);
        }

        adapter.clear();
        adapter.addAll(list);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.btn_send){
            try {
                if (null != adapter) {
                    startWheel();
                    if (adapter.getInvitedUsersCount() > 0) {
                        ArrayList<String> invitedUsers = adapter.getInviteUsersList();
                        JSONArray users = new JSONArray();
                        for(String userid : invitedUsers){
                            users.put(userid);
                        }
                        cometChat.inviteUsersInBroadcast(users, roomName, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                adapter.clearInviteList();
                                stopWheel();
                                finish();
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {

                            }
                        });
                    } else {
                        stopWheel();
                        Toast.makeText(getApplicationContext(),
                                (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SELECT_USERS)), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Logger.error("InviteuserActivity.java: inviteButtononClick exception =" + e.toString());
                e.printStackTrace();
            }
        }else if(id == R.id.action_selectAll){
            if (null != adapter){
                for (int i = 0; i < adapter.getCount(); i++) {
                    RelativeLayout rView = (RelativeLayout) adapter.getView(i, null, listview);
                    CheckBox cb = (CheckBox) rView.findViewById(R.id.checkBoxInviteUser);
                    adapter.addInvite(cb.getTag().toString());
                }
                adapter.notifyDataSetChanged();
            }
        }else if(id == R.id.action_deselectAll){
            if (null != adapter) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    RelativeLayout rView = (RelativeLayout) adapter.getView(i, null, listview);
                    CheckBox cb = (CheckBox) rView.findViewById(R.id.checkBoxInviteUser);
                    adapter.removeInvite(cb.getTag().toString());
                }
                adapter.notifyDataSetChanged();
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
