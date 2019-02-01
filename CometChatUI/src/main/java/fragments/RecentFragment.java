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
import android.support.constraint.Group;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
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
import activities.CCSingleChatActivity;
import adapters.RecentListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;
import models.Conversation;
import models.Groups;

public class RecentFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener , OnAlertDialogButtonClickListener {

    private static final String TAG = RecentFragment.class.getSimpleName();
    private final int CHATS_LOADER = 1;
    private final int CHATS_SEARCH_LOADER =2;
    private RecyclerView recentRecyclerView;
    private RecentListAdapter recentListAdapter;
    private BroadcastReceiver customReceiver;
    private SearchView searchView;
    private Group grpNoRecent;
    private TextView tvNoRecent;
    private boolean isSearching = false,isSearchStart = true,lastSearchisZero = false;
    private static String onoSearchText = "";
    private CometChat cometChat;
    //private CometChatroom cometChatroom;
    String chatroomPassword;
    String chatroomName;
    private ProgressDialog progressDialog;
    private String chatroomId;
    boolean isModerator;
    boolean isOwner;
    private FeatureState groupState;

    public RecentFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static RecentFragment newInstance(String param1, String param2) {
        RecentFragment fragment = new RecentFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        PreferenceHelper.initialize(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cc_fragment_recent, container, false);

        grpNoRecent = view.findViewById(R.id.grpNoRecent);
        tvNoRecent = view.findViewById(R.id.tvNoRecent);
        recentRecyclerView = view.findViewById(R.id.recent_recyler_view);
        recentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentRecyclerView.setAdapter(recentListAdapter);
        cometChat = CometChat.getInstance(getContext());
        if (getLoaderManager().getLoader(CHATS_LOADER) == null) {
            getLoaderManager().initLoader(CHATS_LOADER, null, this);
        }else {
            getLoaderManager().restartLoader(CHATS_LOADER, null, this);
        }
        initializeFeatureState();
        tvNoRecent.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NO_CONVERSATION_AVAILABLE)).toString());
        customReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    Logger.error(TAG, "Refresh recent");
                    Bundle extras = intent.getExtras();
                    if(extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY)
                            && extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.IS_TYPING)){
                        Logger.error(TAG,"Recent tab is typing");
                        if(recentListAdapter!= null){
                            recentListAdapter.setIsTyping(true,extras.getString(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID));
                            refreshFragment();
                        }
                    }if(extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY)
                            && extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.STOP_TYPING)){
                        Logger.error(TAG,"Recent tab is typing");
                        if(recentListAdapter!= null){
                            recentListAdapter.setIsTyping(false,extras.getString(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID));
                            refreshFragment();
                        }
                    }else  if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_RECENT_LIST_KEY)) {
                        Logger.error(TAG,"Recent tab");
                        if(recentListAdapter != null) {
                            refreshFragment();
                        }
                    }
                } catch (Exception e) {
                    Logger.error(TAG, "customReceiver e : "+e.toString());
                    e.printStackTrace();
                }
            }
        };

        recentRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recentRecyclerView, new ClickListener() {

            @Override
            public void onClick(View view, int i) {
                chatroomId = (String) view.getTag(R.string.group_id);
                final String contactId = (String) view.getTag(R.string.contact_id);
                Logger.error(TAG,"Chatroom id = "+chatroomId);
                Logger.error(TAG,"Contact id = "+contactId);
                if(chatroomId.equals("0")){ // Is buddy conversation
                    PreferenceHelper.save("WINDOW ID",contactId);
                    final Contact buddy = Contact.getContactDetails(contactId);
                    if(buddy == null){
                        cometChat.getUserInfo(String.valueOf(chatroomId), new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                openChatActivity(buddy);
                            }
                            @Override
                            public void failCallback(JSONObject jsonObject) {

                            }
                        });
                    }else {
                        openChatActivity(buddy);
                    }
                }else{  // Is Chatroom Conversation
                    if(groupState == FeatureState.INACCESSIBLE){
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    }else {
                        enterInGroup();
                    }
                }

            }

            @Override
            public void onLongClick(View view, int i) {

            }
        }));

        if(LocalConfig.isApp && !TextUtils.isEmpty((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AD_UNIT_ID)))){
            CommonUtils.setBottomMarginToRecyclerView(recentRecyclerView);
        }

        return view;
    }

    private void initializeFeatureState() {
      groupState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.CREATE_GROUPS_ENABLED));
    }

    private void enterInGroup() {
        PreferenceHelper.save("WINDOW ID",chatroomId);
        Groups chatroom = Groups.getGroupDetails(chatroomId);

        chatroomName = chatroom.name;
        Conversation conversation = Conversation.getConversationByChatroomID(chatroomId);
        if(conversation != null) {
            conversation.unreadCount = 0;
            conversation.save();
        }
        getLoaderManager().restartLoader(CHATS_LOADER, null, RecentFragment.this);

        final ProgressDialog progressDialog;

        try {
            if (CommonUtils.isConnected()) {

                chatroom.unreadCount = 0;
                chatroom.save();
                //getLoaderManager().restartLoader(GROUPS_LOADER, null, GroupFragment.this);

                            chatroomId = String.valueOf(chatroom.groupId);
                            chatroomPassword = chatroom.password;
                            int createdBy = chatroom.createdBy;
                            chatroomName = chatroom.name;
                            /*if (createdBy == 1 || createdBy == 2) {

                    progressDialog = ProgressDialog.show(getContext(), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                    progressDialog.setCancelable(false);
                    progressDialog.dismiss();
                    joinGroup(chatroomId);
                } else*/ if (createdBy == 0 || createdBy != SessionData.getInstance().getId()) {
                    switch (chatroom.type) {
                        case CometChatKeys.ChatroomKeys.TypeKeys.PUBLIC:
                            progressDialog = ProgressDialog.show(getContext(), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup(chatroomId);
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.PASSWORD_PROTECTED:

                            View dialogview = getActivity().getLayoutInflater().inflate(R.layout.cc_custom_dialog, null);
                            TextView tvTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
                            tvTitle.setText("");
                            new CustomAlertDialogHelper(getContext(), "Group Password", dialogview, "OK",
                                    "", "Cancel", RecentFragment.this, 1,false);
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.INVITE_ONLY:
                            progressDialog = ProgressDialog.show(getContext(), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup(chatroomId);
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.PRIVATE:
                            progressDialog = ProgressDialog.show(getContext(), "",(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup(chatroomId);
                        default:
                            break;
                    }
                }else {
                                if(chatroom.type == CometChatKeys.ChatroomKeys.TypeKeys.PASSWORD_PROTECTED && TextUtils.isEmpty(chatroomPassword)) {
                                    View dialogview = getActivity().getLayoutInflater().inflate(R.layout.cc_custom_dialog, null);
                                    TextView tvTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
                                    tvTitle.setText("");
                                    new CustomAlertDialogHelper(getContext(), "Group Password", dialogview, "OK",
                                            "", "Cancel", RecentFragment.this, 1,false);
                                } else {
                                    progressDialog = ProgressDialog.show(getContext(), "",
                                            (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                                    progressDialog.setCancelable(false);
                                    progressDialog.dismiss();
                                    joinGroup(chatroomId);
                                }
                            }
                //}
            } else {
                //Toast.makeText(getContext(), language.getMobile().get24(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.error(TAG, "onResume");
        refreshFragment();
    }

    private void openChatActivity(Contact contact){

        Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(contact.contactId));
        conversation.unreadCount = 0;
        conversation.save();
        getLoaderManager().restartLoader(CHATS_LOADER, null, this);

        Intent intent = new Intent(getActivity(), CCSingleChatActivity.class);
        intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID,contact.contactId);
        if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL).isEmpty()) {
            intent.putExtra("ImageUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_IMAGE_URL));
        }
        if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL).isEmpty()) {
            intent.putExtra("VideoUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_VIDEO_URL));
        }
        if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL).isEmpty()) {
            intent.putExtra("AudioUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_AUDIO_URL));
        }
        if (PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL) != null && !PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL).isEmpty()) {
            intent.putExtra("FileUri", PreferenceHelper.get(PreferenceKeys.DataKeys.SHARE_FILE_URL));
        }
        intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, contact.name);
        SessionData.getInstance().setTopFragment(StaticMembers.TOP_FRAGMENT_ONE_ON_ONE);
        startActivity(intent);
    }

    private void joinGroup(final String chatroomId){
        progressDialog = ProgressDialog.show(getActivity(), "", (CharSequence) cometChat.getCCSetting(
                new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_JOINING_GROUP)));
        progressDialog.setCancelable(false);
        if(cometChat.getCometChatServerVersion() <= 6919){
            cometChat.joinGroup(chatroomId, chatroomName, chatroomPassword, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error("JoinChatroom success = "+jsonObject);

                Conversation conversation = Conversation.getConversationByChatroomID(chatroomId);
                if(conversation != null) {
                    conversation.unreadCount = 0;
                    conversation.save();
                }

                try {
                    isModerator = (boolean) jsonObject.get("ismoderator");
                    isOwner = (boolean) jsonObject.get("owner");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ClassCastException e) {
                    String moderator;
                    try {
                        moderator = String.valueOf(jsonObject.get("ismoderator"));
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
                //intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, Long.parseLong(chatroomId));
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ISMODERATOR, isModerator);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, isOwner);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
                getContext().startActivity(intent);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error("JoinChatroom fail  = " + jsonObject);
                String message = "";
                try{
                    message = jsonObject.getString("message");
                }catch (JSONException e){
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                if(progressDialog!=null)
                    progressDialog.dismiss();
            }
        });
        }else{
            Groups grp = Groups.getGroupDetails(chatroomId);
            int isOwnerValue = grp.owner;
            if(isOwnerValue == 1){
                isOwner = true;
            }else isOwner = false;
            Intent intent = new Intent(getContext(), CCGroupChatActivity.class);
            //intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, Long.parseLong(chatroomId));
            intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
            intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
            intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, isOwner);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
            getContext().startActivity(intent);

        }

        Conversation conversation = Conversation.getConversationByChatroomID(chatroomId);
        if(conversation != null) {
            conversation.unreadCount = 0;
            conversation.save();
        }

        /*boolean isModerator = false;
        try {
            isModerator = (boolean) jsonObject.get("ismoderator");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            String moderator;
            try {
                moderator = String.valueOf(jsonObject.get("ismoderator"));
                if(moderator.equals("1")){
                    isModerator=true;
                }else {
                    isModerator = false;
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }*/
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        try {
            MenuItem searchMenuItem = menu.findItem(R.id.custom_action_search);
            searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            searchView.setOnQueryTextListener(this);

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
            menu.findItem(R.id.custom_action_search).setVisible(true);
            menu.findItem(R.id.custom_setting).setVisible(true);
            //
        } catch (Exception e) {
            Logger.error("onCreateOptionsMenu in chatroom.java Exception = " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    public void refreshFragment(){
        Logger.error(TAG,"refreshFragment called");
        try {
            if (getLoaderManager().getLoader(CHATS_LOADER) != null) {
                Logger.error(TAG, "Refreshing list");
                getLoaderManager().restartLoader(CHATS_LOADER, null, this);
            } else {
                getLoaderManager().initLoader(CHATS_LOADER, null, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        String selection;
        String[] selectionArgs;

        switch (id) {
            case CHATS_LOADER:
                selection = Conversation.getAllConversationQuery();
                return new DataCursorLoader(getContext(), selection, null);

            case CHATS_SEARCH_LOADER:
                String searchText = bundle.getString("search_key");
                Logger.error(TAG,"SearchText value = "+searchText);
                selection = Conversation.getSearchConversationQuery(searchText);
                return new DataCursorLoader(getContext(), selection, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Logger.error(TAG,"Recent on load data = "+ data.getCount());

        if(data.getCount() == 0){
            grpNoRecent.setVisibility(View.VISIBLE);
        }else {
            grpNoRecent.setVisibility(View.GONE);
        }

        if(recentListAdapter == null){
            recentListAdapter = new RecentListAdapter(getActivity(),this,data);
            recentRecyclerView.setAdapter(recentListAdapter);
        }else {
            recentListAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    @Override
    public void onStart() {
        super.onStart();
        if (customReceiver != null) {
            getActivity().registerReceiver(customReceiver,
                    new IntentFilter(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST));
        }
        refreshFragment();
    }

    @Override
    public void onStop() {
        try {
            super.onStop();
            if (null != customReceiver) {
                getActivity().unregisterReceiver(customReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        searchText = searchText.replaceAll("^\\s+", "");
        if (!searchView.isIconified() && !TextUtils.isEmpty(searchText)) {
            onoSearchText = searchText;
        }
        if (!TextUtils.isEmpty(searchText)) {
            searchUser(searchText, true);
            isSearchStart = true;
            lastSearchisZero = false;
        } else {

                /* * If view is loaded again because of tab switching then dont
				 * call this method*/


            if (isSearchStart) {
                if (!lastSearchisZero) {
                    lastSearchisZero = true;
                    onoSearchText = searchText;
                    searchUser(searchText, false);
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
            if (getLoaderManager().getLoader(CHATS_SEARCH_LOADER) == null) {
                getLoaderManager().initLoader(CHATS_SEARCH_LOADER, bundle, this);
            }else {
                getLoaderManager().restartLoader(CHATS_SEARCH_LOADER, bundle, this);
            }

        } else {
            getLoaderManager().restartLoader(CHATS_LOADER, null, this);
        }

    }

    @Override
    public void onButtonClick(AlertDialog dialog, View v, int which, int popupId) {
        final EditText chatroomPasswordInput = (EditText) v.findViewById(R.id.edittextDialogueInput);

        if (which == DialogInterface.BUTTON_NEGATIVE) { // Cancel
            dialog.dismiss();
        } else if (which == DialogInterface.BUTTON_POSITIVE) { // Join
            try {
                progressDialog = ProgressDialog.show(getContext(), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                progressDialog.setCancelable(false);
                chatroomPassword = chatroomPasswordInput.getText().toString();
                if (chatroomPassword.length() == 0) {
                    chatroomPasswordInput.setText("");
                    chatroomPasswordInput.setError("Incorrect password");
                    progressDialog.dismiss();
                } else {
                    try {
                        chatroomPassword = EncryptionHelper.encodeIntoShaOne(chatroomPassword);
                        if(!chatroomId.equalsIgnoreCase("0")){
                            dialog.dismiss();
                            joinGroup(chatroomId);
                        }

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
}
