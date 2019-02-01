package fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.constraint.Group;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
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
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.RecyclerTouchListener;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.DataCursorLoader;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.ClickListener;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.LocalConfig;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import Keys.BroadCastReceiverKeys;
import activities.CCSingleChatActivity;
import adapters.ContactListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Contact;
import models.Conversation;


public class ContactFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> ,SearchView.OnQueryTextListener {
    private static final java.lang.String TAG = ContactFragment.class.getSimpleName();
    private final int CONTACTS_LOADER = 1,CONTACTS_SEARCH_LOADER = 2;
    private ContactListAdapter contactListAdapter;
    private RecyclerView contactRecyclerView;
    private BroadcastReceiver broadcastReceiver;
    private Group grpNoContacts;
    private TextView tvNoContacts;
    private SearchView searchView;
    private boolean isSearchStart = true, lastSearchisZero = false, isSearching = false;
    private static String onoSearchText = "";
    private CometChat cometChat;
    public ContactFragment() {
        // Required empty public constructor
    }

    public static ContactFragment newInstance(String param1, String param2) {
        ContactFragment fragment = new ContactFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.error(TAG,"OnReceive Called");
                Bundle extras = intent.getExtras();
                if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_CONTACT_LIST_KEY)) {
                    if (contactListAdapter !=null && !isSearching) {
                        getLoaderManager().restartLoader(CONTACTS_LOADER, null, ContactFragment.this);
                    }
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        cometChat = CometChat.getInstance(getContext());
        grpNoContacts = view.findViewById(R.id.grpNoContacts);
        tvNoContacts = view.findViewById(R.id.tvNoContacts);
        contactRecyclerView = view.findViewById(R.id.contacts_recycler_view);
        contactRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactRecyclerView.setItemAnimator(new DefaultItemAnimator());
        if (getLoaderManager().getLoader(CONTACTS_LOADER) == null) {
            getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
        }else{
            getLoaderManager().restartLoader(CONTACTS_LOADER, null, this);
        }

        tvNoContacts.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NO_CONTACTS_FOUND)).toString());

        contactRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), contactRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                long contactID = (long) view.getTag(R.string.contact_id);
                String contactname = (String) view.getTag(R.string.contact_name);

                Conversation conversation = Conversation.getConversationByBuddyID(String.valueOf(contactID));
                if(conversation != null) {
                    conversation.unreadCount = 0;
                    conversation.save();
                }

                Contact contact = Contact.getContactDetails(contactID);
                if(contact!=null) {
                    contact.unreadCount = 0;
                    contact.save();
                }

                Intent intent = new Intent(getActivity(), CCSingleChatActivity.class);
                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID,contactID);
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
                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME, contactname);
                SessionData.getInstance().setTopFragment(StaticMembers.TOP_FRAGMENT_ONE_ON_ONE);
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        if(LocalConfig.isApp && !TextUtils.isEmpty((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AD_UNIT_ID)))){
            CommonUtils.setBottomMarginToRecyclerView(contactRecyclerView);
        }

        return view;
    }


    @Override
    public void onDetach() {
        super.onDetach();
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
    public void onStart() {
        super.onStart();
        if(broadcastReceiver!= null){
            getActivity().registerReceiver(broadcastReceiver,
                    new IntentFilter(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST));
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (null != broadcastReceiver) {
            getActivity().unregisterReceiver(broadcastReceiver);
        }
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection;

        switch (id){

            case CONTACTS_LOADER:
                selection = Contact.getAllContactsQuery();
                return new DataCursorLoader(getContext(), selection, null);

            case CONTACTS_SEARCH_LOADER:
                Logger.error(TAG,"Search key = "+args.getString("search_key"));
                selection = Contact.searchContactsQuery(args.getString("search_key"));
                return new DataCursorLoader(getContext(), selection, null);


            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Logger.error(TAG,"on load data = "+ data.getCount());
        if(contactListAdapter == null){
            contactListAdapter = new ContactListAdapter(getContext(),data);
            contactRecyclerView.setAdapter(contactListAdapter);
        }

        if(data.getCount() == 0){
            grpNoContacts.setVisibility(View.VISIBLE);
        }else {
            grpNoContacts.setVisibility(View.GONE);
        }
        contactListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*switch (loader.getId()) {
            case CONTACTS_LOADER:
                if (contactListAdapter != null) {
                    contactListAdapter.swapCursor(null);
                }
                break;
            default:
                break;
        }*/
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        if (contactRecyclerView != null && contactListAdapter != null) {
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
            if (getLoaderManager().getLoader(CONTACTS_SEARCH_LOADER) == null) {
                getLoaderManager().initLoader(CONTACTS_SEARCH_LOADER, bundle, this);
            }else {
                getLoaderManager().restartLoader(CONTACTS_SEARCH_LOADER, bundle, this);
            }
        } else {
            getLoaderManager().restartLoader(CONTACTS_LOADER, null, this);
        }
    }

    public void refreshFragment(){
        Logger.error("Refresh Fragment called");
        try {
            if (getLoaderManager().getLoader(CONTACTS_LOADER) != null) {
                getLoaderManager().restartLoader(CONTACTS_LOADER, null, this);
            } else {
                getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
