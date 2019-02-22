package cometchat.inscripts.com.readyui;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.Keyboards.SmileyKeyBoard;
import com.inscripts.Keyboards.adapter.EmojiGridviewImageAdapter;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import adapters.BroadcastListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import helpers.CCAnalyticsHelper;
import models.Contact;

public class CCBroadcastMessageActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,EmojiGridviewImageAdapter.EmojiClickInterface , SearchView.OnQueryTextListener{

    private static final java.lang.String TAG = CCBroadcastMessageActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer,chatFooter;
    private int colorPrimary,colorPrimaryDark;
    private EditText messageField;
    private ImageButton sendBtn, smilieyButton;
    private ListView listview;
    private static String checkBoxKeyForBundle = "checkBoxState";
    private BroadcastListAdapter adapter;
    private TextView noUserView;
    private ProgressBar wheel;
    private CometChat cometChat;
    private SearchView searchView;

    private SmileyKeyBoard smileyKeyBoard;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_message);
        setupFields();
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Broadcast Message");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        CCAnalyticsHelper.logFeatureEvent("CCBroadcastMessageActivity");
        setCCTheme();
        setFieldListners();
        ArrayList<String> savedCheckbox = new ArrayList<>();
        if (null != savedInstanceState) {
            savedCheckbox = savedInstanceState.getStringArrayList(checkBoxKeyForBundle);
        }
        setupUserListView(savedCheckbox);
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

    private void setupFields(){
        toolbar = (Toolbar) findViewById(R.id.broadcast_message_toolbar);
        ccContainer = (RelativeLayout) findViewById(R.id.cc_broadcast_message_container);
        messageField = (EditText) findViewById(R.id.editTextChatMessage);
        listview = (ListView) findViewById(R.id.listviewBroadcast);
        sendBtn = (ImageButton) findViewById(R.id.buttonSendMessage);
        smilieyButton = (ImageButton) findViewById(R.id.img_btn_chat_more);
        chatFooter = (RelativeLayout) findViewById(R.id.relativeLayoutControlsHolder);
        noUserView = (TextView) findViewById(R.id.noUsersOnline);
        wheel = (ProgressBar) findViewById(R.id.progressWheel);
        smileyKeyBoard = new SmileyKeyBoard();
        smileyKeyBoard.enable(this, this, R.id.footer_for_emoticons, messageField);
        smileyKeyBoard.checkKeyboardHeight(chatFooter);
        smileyKeyBoard.enableFooterView(messageField);
    }

    private void setFieldListners(){
        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    sendBtn.setEnabled(true);
                    sendBtn.setAlpha(1F);
                } else {
                    sendBtn.setEnabled(false);
                    sendBtn.setAlpha(0.5F);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CCAnalyticsHelper.logFeatureEvent("Send Broadcast Message");
                try {
                    if (null != adapter) {

                        if(!messageField.getText().toString().trim().isEmpty()){
                            if (adapter.getInvitedUsersCount() > 0) {
                                startWheel();
                                final ArrayList<String> invitedUsers = adapter.getInviteUsersList();
                                final JSONArray jsonArray = new JSONArray();
                                for (String id : invitedUsers) {
                                    jsonArray.put(Long.parseLong(id));
                                }
                                Logger.error(TAG,"Selected users = "+jsonArray.toString());
                                Logger.error(TAG,"message value = "+messageField.getText());
                                cometChat.broadcastMessage(messageField.getText().toString().trim(), jsonArray, new Callbacks() {
                                    @Override
                                    public void successCallback(JSONObject jsonObject) {
                                        Logger.error(TAG,"Send BroadCast responce = "+jsonObject);
//                                        Toast.makeText(CCBroadcastMessageActivity.this, "Broadcast Message Sent", Toast.LENGTH_SHORT).show();
//                                        finish();
                                    }

                                    @Override
                                    public void failCallback(JSONObject jsonObject) {
                                        Logger.error(TAG,"Send BroadCast fail responce = "+jsonObject);
                                    }
                                });
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SELECT_USERS)), Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            messageField.setText("");
                            Toast.makeText(CCBroadcastMessageActivity.this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        smilieyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smileyKeyBoard.showKeyboard(chatFooter);
            }
        });
    }

    private void setupUserListView(final ArrayList<String> checkBoxState) {
        List<Contact> buddyList;
        try {
            buddyList = Contact.getAllContacts();
            if (null != buddyList && buddyList.size() > 0) {
                adapter = new BroadcastListAdapter(this, buddyList, checkBoxState);
                listview.setAdapter(adapter);
                listview.setOnItemClickListener(CCBroadcastMessageActivity.this);
                noUserView.setVisibility(View.GONE);
            } else {
                noUserView.setVisibility(View.VISIBLE);
                noUserView.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_USERS)));
                sendBtn.setAlpha(0.5F);
                sendBtn.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        inflater.inflate(R.menu.menu_broadcast_message, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.custom_action_search);


        menu.findItem(R.id.action_selectAll);
        menu.findItem(R.id.action_deselectAll);
        menu.findItem(R.id.btn_send).setVisible(false);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(Html.fromHtml("<font color = #ffffff>Search Users</font>"));
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
    public boolean onOptionsItemSelected(MenuItem item) {

        int i1 = item.getItemId();
        if (i1 == R.id.action_selectAll) {
            if (null != adapter) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    RelativeLayout rView = (RelativeLayout) adapter.getView(i, null, listview);
                    CheckBox cb = (CheckBox) rView.findViewById(R.id.checkBoxInviteUser);
                    adapter.addInvite(cb.getTag().toString());
                }
                adapter.notifyDataSetChanged();
            }

        } else if (i1 == R.id.action_deselectAll) {
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
    public void getClickedEmoji(int i) {
      smileyKeyBoard.getClickedEmoji(i);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
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
            //list = Buddy.getAllBuddies();
            list = Contact.getAllContacts();
        }

        if (null == list || list.size() == 0) {
            noUserView.setVisibility(View.VISIBLE);
        } else {
            noUserView.setVisibility(View.GONE);
        }

        adapter.clear();
        adapter.addAll(list);
        //adapter.notifyDataSetChanged();
    }
}
