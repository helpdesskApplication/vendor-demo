package fragments;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.CustomAlertDialogHelper;
import com.inscripts.custom.RecyclerTouchListener;
import com.inscripts.custom.StickyHeaderDecoration;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.DataCursorLoader;
import com.inscripts.helpers.EncryptionHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.ClickListener;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONException;
import org.json.JSONObject;

import Keys.BroadCastReceiverKeys;
import activities.CCGroupChatActivity;
import adapters.GroupListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Conversation;
import models.Groups;

/**
 * A simple {@link Fragment} subclass.
 */
public class GroupFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnAlertDialogButtonClickListener,SearchView.OnQueryTextListener {


    private static final java.lang.String TAG = GroupFragment.class.getSimpleName();
    private final int GROUPS_LOADER = 1,GROUPS_SEARCH_LOADER =2;
    private RecyclerView groupsRecyclerView;

    private GroupListAdapter groupListAdapter;
    private StickyHeaderDecoration decor;
    private CometChat cometChatroom;
    private long chatroomId;
    private String chatroomName, chatroomPassword;
    private static ProgressDialog progressDialog;
    private SearchView searchView;
    private boolean isSearchStart = true, lastSearchisZero = false, isSearching = false;
    private static String onoSearchText = "";
    private Group grpNoGroups;
    private TextView tvNoGroups;
    private CometChat cometChat;
    private FeatureState groupState;
    private BroadcastReceiver groupBroadcastReceiver;
    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.error(TAG, "onReceive: groupBroadcastReceiver");
                final Bundle extras = intent.getExtras();
                if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_GROUP_LIST_KEY)) {
                    if(extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID)){
                        String key;
                        final String groupId = extras.getString(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID);
                        Logger.error(TAG, "onReceive: groupId : "+groupId);
                        cometChat.leaveGroup(Long.parseLong(groupId), new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG, "leaveGroup : successCallback : "+jsonObject);
                                Groups groups = Groups.getGroupDetails(groupId);
                                groups.status = 0;
                                groups.save();
                                if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.KICKED)) {
                                    Toast.makeText(getActivity(), "You have been kicked from " + chatroomName, Toast.LENGTH_SHORT).show();
                                } else if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.BANNED)) {
                                    Toast.makeText(getActivity(), "You have been banned from " + chatroomName, Toast.LENGTH_SHORT).show();
                                }
                                if (groupListAdapter !=null) {
                                    getLoaderManager().restartLoader(GROUPS_LOADER, null, GroupFragment.this);
                                }
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG, "leaveGroup : failCallback : "+jsonObject);
                            }
                        });
                        /*Groups groups = Groups.getGroupDetails(groupId);
                        groups.status = 0;
                        groups.save();*/
                    }

                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        if(groupBroadcastReceiver!= null){
            getActivity().registerReceiver(groupBroadcastReceiver,
                    new IntentFilter(BroadCastReceiverKeys.FINISH_GROUP_ACTIVITY));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(groupBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_chatroom, container, false);
        cometChat = CometChat.getInstance(getContext());
        groupsRecyclerView = view.findViewById(R.id.groups_recycler_view);
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        grpNoGroups = view.findViewById(R.id.grpNoGroups);
        tvNoGroups = view.findViewById(R.id.tvNoGroups);

        if (getLoaderManager().getLoader(GROUPS_LOADER) == null) {
            getLoaderManager().initLoader(GROUPS_LOADER, null, this);
        } else {
            getLoaderManager().restartLoader(GROUPS_LOADER, null, this);
        }
        cometChatroom = CometChat.getInstance(this.getContext());
        initializeFeatureStates();
        tvNoGroups.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NO_GROUPS)).toString());
        groupsRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), groupsRecyclerView, new ClickListener() {

            @Override
            public void onClick(View view, int position) {

                long groupID = (long) view.getTag(R.string.group_id);
                initGroupJoin(String.valueOf(groupID));
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        if(LocalConfig.isApp && !TextUtils.isEmpty((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AD_UNIT_ID)))){
            CommonUtils.setBottomMarginToRecyclerView(groupsRecyclerView);
        }

        return view;
    }

    private void initializeFeatureStates() {
        groupState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GROUP_CHAT_ENABLED));
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // menu.findItem(R.id.custom_action_create_chatroom).setVisible(false);
        try {
            MenuItem searchMenuItem = menu.findItem(R.id.custom_action_search);
            searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            searchView.setOnQueryTextListener(this);

            searchView.setQueryHint(Html.fromHtml("<font color = #ffffff>Search</font>"));


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

        } catch (Exception e) {
            Logger.error("onCreateOptionsMenu in oneononefragment.java Exception = " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection;
        switch (id) {

            case GROUPS_LOADER:
                selection = Groups.getAllGroupsQuery();
                return new DataCursorLoader(getContext(), selection, null);

            case GROUPS_SEARCH_LOADER:
                selection = Groups.getGroupsSearchQuery(args.getString("search_key"));
                return new DataCursorLoader(getContext(), selection, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Logger.error(TAG,"onLoadFinished called data = "+data.getCount());
        Logger.error(TAG,"deleteGroup data = "+data.getCount());

        if(groupState == FeatureState.ACCESSIBLE) {
            grpNoGroups.setVisibility(View.GONE);

            groupListAdapter = new GroupListAdapter(getContext(), data);
            groupsRecyclerView.setAdapter(groupListAdapter);

            if(data.getCount() == 0){
                grpNoGroups.setVisibility(View.VISIBLE);
            }else{
                grpNoGroups.setVisibility(View.GONE);
            }

            if(decor != null) {
                groupsRecyclerView.removeItemDecoration(decor);
            }
            decor = new StickyHeaderDecoration(groupListAdapter);
            groupsRecyclerView.addItemDecoration(decor, 0);

            groupListAdapter.swapCursor(data);
        } else {
            grpNoGroups.setVisibility(View.VISIBLE);
            tvNoGroups.setText(getString(R.string.rolebase_warning));
        }

        /*if (loader.getId() == GROUPS_SEARCH_LOADER) {
                groupListAdapter = new GroupListAdapter(getContext(), data);
                groupsRecyclerView.setAdapter(groupListAdapter);
            } else {

            if (data.getCount() > 0) {
                tvNoGroups.setVisibility(View.GONE);
            } else {
                tvNoGroups.setVisibility(View.VISIBLE);
            }

            //if(groupListAdapter == null){
                groupListAdapter = new GroupListAdapter(getContext(), data);
                if(groupState == FeatureState.INACCESSIBLE){
                    tvNoGroups.setVisibility(View.VISIBLE);
                    tvNoGroups.setText(getString(R.string.rolebase_warning));
                }else {
    //                tvNoGroups.setVisibility(View.GONE);
                    groupsRecyclerView.setAdapter(groupListAdapter);
                }

            if (decor != null) {
                groupsRecyclerView.removeItemDecoration(decor);
            }
            decor = new StickyHeaderDecoration(groupListAdapter);
            groupsRecyclerView.addItemDecoration(decor, 0);
            //}else{
            groupListAdapter.setCursor(data);
            groupListAdapter.swapCursor(data);
            //}
        }*/
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case GROUPS_LOADER:
                if (groupListAdapter != null) {
                    groupListAdapter.swapCursor(null);
                }
                break;
            case GROUPS_SEARCH_LOADER:
                if (groupListAdapter != null) {
                    groupListAdapter.swapCursor(null);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onButtonClick(AlertDialog dialog, View v, int which, int popupId) {
        final EditText chatroomPasswordInput = (EditText) v.findViewById(R.id.edittextDialogueInput);

        if (which == DialogInterface.BUTTON_NEGATIVE) { // Cancel
            dialog.dismiss();
        } else if (which == DialogInterface.BUTTON_POSITIVE) { // Join
            try {
                /*progressDialog = ProgressDialog.show(getContext(), "",(String)cometChatroom.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                progressDialog.setCancelable(false);*/
                chatroomPassword = chatroomPasswordInput.getText().toString();
                if (chatroomPassword.length() == 0) {
                    chatroomPasswordInput.setText("");
                    chatroomPasswordInput.setError("Incorrect password");
                    progressDialog.dismiss();
                } else {
                    try {
                        chatroomPassword = EncryptionHelper.encodeIntoShaOne(chatroomPassword);
                        dialog.dismiss();
                        joinGroup();


                    } catch (Exception e) {
                        Logger.error("Error at SHA1:UnsupportedEncodingException FOR PASSWORD "
                                + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                Logger.error("chatroomFragment.java onButtonClick() : Exception=" + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    public void initGroupJoin(String groupID) {
        Groups chatroom = Groups.getGroupDetails(groupID);
        Logger.error(TAG, "initGroupJoin chatroom : " + chatroom);

        final ProgressDialog progressDialog;

        try {
            if (CommonUtils.isConnected()) {

                chatroom.unreadCount = 0;
                chatroom.save();
                //getLoaderManager().restartLoader(GROUPS_LOADER, null, GroupFragment.this);

                Logger.error(TAG, "initGroupJoin chatroom.groupId : " + chatroom.groupId);
                chatroomId = chatroom.groupId;
                Logger.error(TAG, "initGroupJoin chatroom.password : " + chatroom.password);
                chatroomPassword = chatroom.password;
                Logger.error(TAG, "initGroupJoin chatroom.createdBy : " + chatroom.createdBy);
                int createdBy = chatroom.createdBy;
                Logger.error(TAG, "initGroupJoin chatroom.name : " + chatroom.name);
                chatroomName = chatroom.name;
                Logger.error(TAG, "initGroupJoin chatroom.type : " + chatroom.type);
                Logger.error(TAG, "initGroupJoin join password : " + chatroom.password);
               /* if (createdBy == 1 || createdBy == 2) {

                    progressDialog = ProgressDialog.show(getContext(), "",(String)cometChatroom.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                    progressDialog.setCancelable(false);
                    progressDialog.dismiss();
                    joinGroup();
                }*/  if (createdBy == 0 || createdBy != SessionData.getInstance().getId()) {
                    switch (chatroom.type) {
                        case CometChatKeys.ChatroomKeys.TypeKeys.PUBLIC:
                            progressDialog = ProgressDialog.show(getContext(), "",(String)cometChatroom.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup();
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.PASSWORD_PROTECTED:

                            View dialogview = getActivity().getLayoutInflater().inflate(R.layout.cc_custom_dialog, null);
                            TextView tvTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
                            tvTitle.setText("");
                            new CustomAlertDialogHelper(getContext(), "Group Password", dialogview, "OK",
                                    "", "Cancel", GroupFragment.this, 1,false);
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.INVITE_ONLY:
                            progressDialog = ProgressDialog.show(getContext(), "",(String)cometChatroom.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup();
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.PRIVATE:
                            progressDialog = ProgressDialog.show(getContext(), "",(String)cometChatroom.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup();
                            break;
                        default:
                            break;
                    }
                }else {

                    if(chatroom.type == CometChatKeys.ChatroomKeys.TypeKeys.PASSWORD_PROTECTED && TextUtils.isEmpty(chatroomPassword)) {
                        View dialogview = getActivity().getLayoutInflater().inflate(R.layout.cc_custom_dialog, null);
                        TextView tvTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
                        tvTitle.setText("");
                        new CustomAlertDialogHelper(getContext(), "Group Password", dialogview, "OK",
                                "", "Cancel", GroupFragment.this, 1,false);
                    } else {
                        progressDialog = ProgressDialog.show(getContext(), "",
                                (String) cometChatroom.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                        progressDialog.setCancelable(false);
                        progressDialog.dismiss();
                        joinGroup();
                    }
                }
            } else {
                //Toast.makeText(getContext(), language.getMobile().get24(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinGroup(){
        progressDialog = ProgressDialog.show(getActivity(), "", (CharSequence) cometChatroom.getCCSetting(
                new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_JOINING_GROUP)));
        progressDialog.setCancelable(false);

        cometChatroom.joinGroup(String.valueOf(chatroomId), chatroomName, chatroomPassword, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "JoinChatroom success : " + jsonObject);
                Groups grp = Groups.getGroupDetails(chatroomId);
                if(grp!=null){
                    grp.status = 1;
                    grp.save();
                }
                Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(chatroomId));
                Logger.error(TAG, "JoinChatroom success conversation : " + conversation);
                if(conversation != null) {
                    conversation.unreadCount = 0;
                    conversation.save();
                }

                boolean isModerator = false;
                boolean isOwner = false;
                try {
                    if(jsonObject.has("isModerator")){
                        isModerator = (boolean) jsonObject.get("isModerator");
                    }else {
                        isModerator = (boolean) jsonObject.get("ismoderator");
                    }
                    isOwner = (boolean) jsonObject.get("owner");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ClassCastException e) {
                    String moderator;
                    try {
                        if(jsonObject.has("isModerator")){
                            moderator = String.valueOf(jsonObject.get("isModerator"));
                        }else {
                            moderator = String.valueOf(jsonObject.get("ismoderator"));
                        }
                        if(moderator.equals("1")){
                            isModerator=true;
                        }else {
                            isModerator = false;
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }

                Intent intent = new Intent(getContext(), CCGroupChatActivity.class);
                //intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
                Logger.error(TAG, "chatroomId : " + String.valueOf(chatroomId));
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, String.valueOf(chatroomId));
                intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ISMODERATOR, isModerator);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, isOwner);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_IMAGE_URL)) {
                    intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
                }
                if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_VIDEO_URL)) {
                    intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
                }
                if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_AUDIO_URL)) {
                    intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
                }
                if(progressDialog!=null)
                    progressDialog.dismiss();
                startActivity(intent);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error("JoinChatroom fail  = " + jsonObject);
                String message = "";
                try{
                    if (jsonObject.has("message")) {
                        message = jsonObject.getString("message");
                    } else {
                        if (jsonObject.has("s"))
                            message = jsonObject.getString("s");
                        if (message.contains("BANNED")) {
                            message = "You have been banned from this group";
                        }
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                if(progressDialog!=null)
                    progressDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        if (groupsRecyclerView != null && groupListAdapter != null) {
            searchText = searchText.replaceAll("^\\s+", "");
            if (!searchView.isIconified() && !TextUtils.isEmpty(searchText)) {
                onoSearchText = searchText;
            }
            if (!TextUtils.isEmpty(searchText)) {
                searchUser(searchText, true);
                isSearchStart = true;
                lastSearchisZero = false;
            } else {
                if (isSearchStart) {
                    if (!lastSearchisZero) {
                        lastSearchisZero = true;
                        onoSearchText = searchText;
                        searchUser(searchText, false);
                    }
                }
            }
        }
        return true;
    }


    private void searchUser(String searchText, boolean search) {
        Logger.error(TAG,"Search user called with key "+searchText);
        if (search) {
            isSearching = true;
            Bundle bundle = new Bundle();
            bundle.putString("search_key",searchText);
            if (getLoaderManager().getLoader(GROUPS_SEARCH_LOADER) == null) {
                getLoaderManager().initLoader(GROUPS_SEARCH_LOADER, bundle, this);
            }else {
                getLoaderManager().restartLoader(GROUPS_SEARCH_LOADER, bundle, this);
            }
        } else {
            getLoaderManager().initLoader(GROUPS_LOADER, null, this);
        }
    }

    public void refreshFragment(){
        Logger.error("Refresh Fragment called");
        try {
            if (getLoaderManager().getLoader(GROUPS_LOADER) != null) {
                getLoaderManager().restartLoader(GROUPS_LOADER, null, this);
            } else {
                getLoaderManager().initLoader(GROUPS_LOADER, null, this);
            }
        }catch(Exception e){
            Logger.error(TAG, "Exception : " + e.toString());
        }
    }
}
