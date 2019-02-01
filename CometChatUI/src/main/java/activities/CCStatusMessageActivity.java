package activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.CustomAlertDialogHelper;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONObject;

import java.util.ArrayList;

import adapters.StatusMessageAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Status;

public class CCStatusMessageActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, OnAlertDialogButtonClickListener {

    private static final String TAG = CCStatusMessageActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private TextView edtStatusMessage;
    private ImageView ivEditStatus;
    private ListView lvStatusMessage;
    private StatusMessageAdapter adapter;
    private TextView tvCurrentStatusTitle, tvNewStatusTitle;

    private ArrayList<Status> list;
    private String previousStatus;
    private int colorPrimary,colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_status_message);
        cometChat = CometChat.getInstance(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_status_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar, R.drawable.cc_rounded_corners_colored);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();
        setupFields();
        setupFieldListeners();
        setupFieldValues();

        this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_STATUS_MESSAGE)));

        setCurrentStatus();
        fetchAllStatusMessages();
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
    }

    private void setupFields() {
        edtStatusMessage = (TextView) findViewById(R.id.editTextStatusMessage);
        ivEditStatus = (ImageView) findViewById(R.id.iv_edit_status);
        lvStatusMessage = (ListView) findViewById(R.id.lv_status);
        tvCurrentStatusTitle = (TextView) findViewById(R.id.tv_status_title);
        tvNewStatusTitle = (TextView) findViewById(R.id.tv_status_list_title);
    }

    private void setupFieldListeners() {
        ivEditStatus.setOnClickListener(this);
        lvStatusMessage.setOnItemClickListener(this);
        lvStatusMessage.setOnItemLongClickListener(this);
    }

    private void setupFieldValues() {
        tvCurrentStatusTitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CURRENT_STATUS_TITLE)));

        tvNewStatusTitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SELECT_NEW_STATUS)));

        tvCurrentStatusTitle.setTextColor(colorPrimary);
        tvNewStatusTitle.setTextColor(colorPrimary);
        ivEditStatus.getDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }

    private void setCurrentStatus() {
        previousStatus = SessionData.getInstance().getStatusMessage();
        Logger.error(TAG,"PreviousStatus = "+previousStatus);
        if (TextUtils.isEmpty(previousStatus)) {
            previousStatus = PreferenceHelper.get(PreferenceKeys.UserKeys.STATUS_MESSAGE);
            if(previousStatus == null){
                previousStatus = "I am available";
                PreferenceHelper.save(PreferenceKeys.UserKeys.STATUS_MESSAGE,"I am available");
            }
        }else{
            PreferenceHelper.save(PreferenceKeys.UserKeys.STATUS_MESSAGE,previousStatus);
        }

        if (previousStatus != null) {
            edtStatusMessage.setText(previousStatus);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllStatusMessages();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_edit_status) {
            Intent statusIntent = new Intent(this, CCUpdateStatusMessageActivity.class);
            startActivity(statusIntent);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String newStatusMessage = list.get(position).message.trim();
        if (!TextUtils.isEmpty(newStatusMessage)) {
            cometChat.updateStatusMessage(newStatusMessage, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"updateStatusMessage(): successCallback: "+jsonObject);
                    PreferenceHelper.save(PreferenceKeys.UserKeys.STATUS_MESSAGE, newStatusMessage);
                    edtStatusMessage.setText(newStatusMessage);
                    Toast.makeText(CCStatusMessageActivity.this, "Status Updated", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"updateStatusMessage(): failCallback: "+jsonObject);
                }
            });
        } else {
            edtStatusMessage.setError((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_ENTER_STATUS)));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, "Message : " + list.get(position).message, Toast.LENGTH_SHORT).show();
        showDeletePopup(position);
        return false;
    }

    private void showDeletePopup(int position) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogview = inflater.inflate(R.layout.cc_custom_dialog, null);
        TextView dialogueTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
        EditText dialogueInput = (EditText) dialogview.findViewById(R.id.edittextDialogueInput);
        dialogueInput.setVisibility(View.GONE);
        dialogueTitle.setVisibility(View.GONE);
//        dialogueTitle.setText("Delete status?");
        new CustomAlertDialogHelper(this, "Delete status?", dialogview, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOGOUT)), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL)), this, position,true);
    }

    private void fetchAllStatusMessages() {
        if (null != list) {
            list.clear();
        }
        list = (ArrayList<Status>) Status.getAllStatusMessages();

        adapter = new StatusMessageAdapter(this, list);
        lvStatusMessage.setAdapter(adapter);
        String currentStatus = PreferenceHelper.get(PreferenceKeys.UserKeys.STATUS_MESSAGE);
        edtStatusMessage.setText(currentStatus);
    }

    @Override
    public void onButtonClick(AlertDialog alertDialog, View v, int which, int popupId) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (list != null && list.size() > 0) {
                Status.deleteStatus(list.get(popupId).getId());
                list.remove(popupId);
                adapter.notifyDataSetChanged();
            }
        }
        alertDialog.dismiss();
    }
}
