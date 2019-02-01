/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys.AjaxKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import adapters.UnblockUserListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;

public class CCUnblockuserActivity extends AppCompatActivity {

    private static final java.lang.String TAG = CCUnblockuserActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private ListView blockUserList;
    private UnblockUserListAdapter adapter;
    private TextView noUsers;
    private String checkBoxKeyForBundle = "checkBoxState", userNames = "userNames", userIds = "userIds";
    private ArrayList<String> savedCheckbox, blockedUserIds;
    private ArrayList<String> blockedUserNames;
    private ProgressBar wheel;

    private boolean isChecked;
    private int userCount;
    private int colorPrimary,colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_unblock_user);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_unblock_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        wheel = (ProgressBar) findViewById(R.id.progressWheel);
        setCCTheme();

        String title = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_MANAGE_BLOCKED_USERS));
        this.setTitle(title);

        blockUserList = (ListView) findViewById(R.id.listViewBlockedUser);
        noUsers = (TextView) findViewById(R.id.textViewNoBlockUser);

        savedCheckbox = new ArrayList<String>();
        if (null != savedInstanceState) {
            savedCheckbox = savedInstanceState.getStringArrayList(checkBoxKeyForBundle);

            blockedUserIds = savedInstanceState.getStringArrayList(userIds);
            blockedUserNames = savedInstanceState.getStringArrayList(userNames);
        }

        if (blockedUserNames == null && blockedUserIds == null) {
            startWheel();

            cometChat.getBlockedUserList(new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"getBlockedUserList(): successCallback: "+jsonObject);
                    try {
                        blockedUserNames = new ArrayList<>();
                        blockedUserIds = new ArrayList<>();
                        Iterator<String> iterator = jsonObject.keys();
                        while (iterator.hasNext()) {
                            JSONObject user = jsonObject.getJSONObject(iterator.next());
                            blockedUserIds.add(user.getString(AjaxKeys.ID));
                            blockedUserNames.add(user.getString(AjaxKeys.NAME));
                        }
                        PreferenceHelper.save("blocked_user_no",blockedUserNames.size());
                        setupBlockedList();
                        stopWheel();
                    } catch (Exception e) {
                        e.printStackTrace();
                        stopWheel();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {

                }
            });
        } else {
            setupBlockedList();
        }

    }

    private void setCCTheme(){
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this,colorPrimaryDark);
        wheel.getIndeterminateDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_unblock_user, menu);
        MenuItem inviteUserItem =  menu.findItem(R.id.custom_action_unblock);

        if(isChecked){
            inviteUserItem.setVisible(true);
        }else{
            inviteUserItem.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String errorText = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_UNBLOCK_ERROR_MESSAGE));

        if(item.getItemId() == R.id.custom_action_unblock){
            if (adapter.getCount() > 0) {
                ArrayList<String> checkedUsers = adapter.getCheckedUsersList();
                userCount = checkedUsers.size();
                if (checkedUsers.size() == 0) {
                    Toast.makeText(getApplicationContext(), (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SELECT_ATLEAST_ONE_USER)), Toast.LENGTH_SHORT).show();
                } else {
                    for (String toId : checkedUsers) {
                        cometChat.unblockUser(toId, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"unblockUser(): successCallback: "+jsonObject);
                                userCount--;
                                try {
                                    if (jsonObject.getString(AjaxKeys.RESULT).equals("0")) {
                                        Toast.makeText(getApplicationContext(), errorText, Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Logger.error(TAG,"userCount: "+userCount);
                                if(userCount == 0){
                                    finish();
                                }
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"unblockUser(): failCallback: "+jsonObject);
                            }
                        });
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), noUsers.getText(), Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBlockedList() {
        adapter = new UnblockUserListAdapter(getApplicationContext(), blockedUserNames, blockedUserIds, savedCheckbox);
        blockUserList.setAdapter(adapter);
        blockUserList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxUnblockUser);
                checkBox.setChecked(!checkBox.isChecked());
                adapter.toggleUnblock(checkBox.getTag().toString());

                ArrayList<String> checkedUsers = adapter.getCheckedUsersList();
                if(checkedUsers.size()>0){
                    isChecked = true;
                    invalidateOptionsMenu();
                }else{
                    isChecked = false;
                    invalidateOptionsMenu();
                }
            }
        });

        if (blockedUserNames.size() == 0) {
            noUsers.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_BLOCKED_USERS)));
            noUsers.setVisibility(View.VISIBLE);
            noUsers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != adapter) {
            ArrayList<String> blockedUsers = adapter.getCheckedUsersList();
            outState.clear();
            outState.putStringArrayList(checkBoxKeyForBundle, blockedUsers);
            if (null != blockedUsers) {
                outState.putStringArrayList(userNames, blockedUserNames);
            }
            if (null != blockedUserIds) {
                outState.putStringArrayList(userIds, blockedUserIds);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void finish() {
        super.finish();
        stopWheel();
    }

    private void startWheel() {
//        wheel.spin();
        wheel.setVisibility(View.VISIBLE);
    }

    private void stopWheel() {
//        wheel.stopSpinning();
//        wheel.setProgress(0f);
        wheel.setVisibility(View.GONE);
    }
}
