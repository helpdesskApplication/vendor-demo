/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.inscripts.enums.FeatureState;
import com.inscripts.enums.GroupType;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import helpers.CCAnalyticsHelper;
import models.Groups;


public class CCCreateChatroomActivity extends AppCompatActivity {

    private static final java.lang.String TAG = CCCreateChatroomActivity.class.getSimpleName();
    private EditText chatroomNameField;
    private EditText chatroomTypeField;
    private Button chatroomCancle;
    private Button btnCreateChatroom;
    private EditText chatroomPasswordField;
    private RelativeLayout ccContainer;
    /*  private Spinner chatroomTypeSelector;
      private TextView passswordLabel, nameLabel, typeLabel;
      private Button createButton, cancelButton;*/
    private String chatroomName, chatroomType, chatroomPassword;
    private TextInputLayout textInputLayoutChatRoomName, textInputLayoutChatroomType, textInputLayoutPassword;
    //    private Chatrooms chatroomLang;
    private Toolbar toolbar;
    /*  private MobileTheme theme;
      private Css css;*/
    private int colorPrimary, colorPrimaryDark;
    private CometChat cometChat;
    private ProgressBar progressWheel;
    private FeatureState createPrivateGroup;
    private FeatureState createProtectedGroup;
    private FeatureState createPublicGroup;
    private FeatureState createInviteGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_create_chatroom);

        toolbar = (Toolbar) findViewById(R.id.create_chatroom_toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            ccContainer = (RelativeLayout) findViewById(R.id.cc_create_container);
            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Create Group");
        CCAnalyticsHelper.logFeatureEvent("CCCreateChatroom Activity");

        chatroomNameField = (EditText) findViewById(R.id.input_chatroom_name);
        chatroomTypeField = (EditText) findViewById(R.id.input_type);
        chatroomPasswordField = (EditText) findViewById(R.id.input_password);
        chatroomCancle = (Button) findViewById(R.id.btn_text_cancle);
        btnCreateChatroom = (Button) findViewById(R.id.buttonCreateChatroom);
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        textInputLayoutChatRoomName = (TextInputLayout) findViewById(R.id.input_layout_chatrroom_name);
        textInputLayoutChatroomType = (TextInputLayout) findViewById(R.id.input_layout_chatroom_type);
        textInputLayoutPassword = (TextInputLayout) findViewById(R.id.input_layout_password);
        progressWheel = (ProgressBar) findViewById(R.id.progressWheel);

        chatroomTypeField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChatroomTypeDialog();
            }
        });
        chatroomTypeField.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PRIVATE_GROUP)));
        textInputLayoutChatRoomName.setHint((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NAME)));
        textInputLayoutPassword.setHint((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PASSWORD)));
        textInputLayoutChatroomType.setHint((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_TYPE)));
        chatroomCancle.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CANCEL)));
        btnCreateChatroom.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CREATE)));
        cometChat = CometChat.getInstance(this.getApplicationContext());
        chatroomNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textInputLayoutChatRoomName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnCreateChatroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createGroup();
            }
        });

        chatroomCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        setThemeColor();
        initializeFeatureState();
    }

    private void createGroup() {
        CCAnalyticsHelper.logFeatureEvent("Create Group");
        chatroomName = chatroomNameField.getText().toString().trim();
        chatroomPassword = chatroomPasswordField.getText().toString().trim();
        chatroomType = chatroomTypeField.getText().toString().trim();

        if (chatroomName.isEmpty() && chatroomType.isEmpty()) {
            textInputLayoutChatRoomName.setError((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_ENTER_GROUP_NAME)));
            chatroomTypeField.setError("Please select Group Type");
        } else if (chatroomName.isEmpty()) {
            textInputLayoutChatRoomName.setError((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_ENTER_GROUP_NAME)));
        } else if (chatroomType.isEmpty()) {
            chatroomTypeField.setError("Please select Group Type");
        }

        if (chatroomPasswordField.getVisibility() == View.VISIBLE && chatroomPassword.isEmpty()) {
            chatroomPasswordField.setText("");
            textInputLayoutPassword.setError((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_ENTER_PASSWORD)));
        }

        if (!chatroomName.isEmpty() && !chatroomType.isEmpty()) {


            if (chatroomType.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PASSWORD_PROTECTED_GROUP)))) {

                if (!chatroomPassword.isEmpty()) {
                    createChatroom(chatroomName, chatroomPassword, chatroomType);
                } /*else {
                    chatroomPasswordField.setText("");
                    chatroomPasswordField.setError(chatroomLang.get26());
                }*/
            } else {
                createChatroom(chatroomName, chatroomPassword, chatroomType);
            }

        } /*else {
                    chatroomNameField.setText("");
                    assert textInputLayoutChatRoomName!= null;
                    textInputLayoutChatRoomName.setError(chatroomLang.get50());
                    chatroomTypeField.setError("Please select Group Type");
                }*/
    }


    private void initializeFeatureState() {
        createPrivateGroup = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CREATE_PRIVATE_GROUPS_ENABLED));
        createProtectedGroup = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CREATE_PASSWORD_PROTECTED_GROUPS_ENABLED));
        createPublicGroup = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CREATE_PUBLIC_GROUPS_ENABLED));
        createInviteGroup = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CREATE_INVITATION_ONLY_GROUPS_ENABLED));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    void setThemeColor() {
        chatroomCancle.setTextColor(colorPrimary);
        Drawable background = btnCreateChatroom.getBackground();
        background.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        } else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        progressWheel.getIndeterminateDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }


    public static void setInputTextLayoutColor(EditText editText, @ColorInt int color) {
        TextInputLayout til = (TextInputLayout) editText.getParent();
        try {
            Field fDefaultTextColor = TextInputLayout.class.getDeclaredField("mDefaultTextColor");
            fDefaultTextColor.setAccessible(true);
            fDefaultTextColor.set(til, new ColorStateList(new int[][]{{0}}, new int[]{color}));

            Field fFocusedTextColor = TextInputLayout.class.getDeclaredField("mFocusedTextColor");
            fFocusedTextColor.setAccessible(true);
            fFocusedTextColor.set(til, new ColorStateList(new int[][]{{0}}, new int[]{color}));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void showChatroomTypeDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(CCCreateChatroomActivity.this);
        builder.setTitle((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_GROUP_TYPE)));
        View view = getLayoutInflater().inflate(R.layout.cc_custom_create_chatroom_dialog, null);
        builder.setView(view);

        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.create_chatroom_radio_grp);
        RadioButton radioButtonPublic = (RadioButton) view.findViewById(R.id.create_chatroom_radio_btn_public);
        RadioButton radioButtonPassword = (RadioButton) view.findViewById(R.id.create_chatroom_radio_btn_password);
        RadioButton radioButtonPublicinvite = (RadioButton) view.findViewById(R.id.create_chatroom_radio_btn_invite);
        RadioButton radioButtonPrivate = view.findViewById(R.id.create_chatroom_radio_btn_private);
        radioButtonPublicinvite.setVisibility(View.GONE);
        radioButtonPublic.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PUBLIC_GROUP)));
        radioButtonPassword.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PASSWORD_PROTECTED_GROUP)));
        radioButtonPublicinvite.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_INVITATION_ONLY_GROUP)));
        radioButtonPrivate.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PRIVATE_GROUP)));

        ColorStateList colorStateList = new ColorStateList(
                new int[][]{

                        new int[]{-android.R.attr.state_enabled},
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[]{
                        Color.BLACK //disabled
                        , Color.parseColor("#8e8e92")
                        , colorPrimary
                }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            radioButtonPublic.setButtonTintList(colorStateList);
            radioButtonPassword.setButtonTintList(colorStateList);
            radioButtonPublicinvite.setButtonTintList(colorStateList);
        }


        builder.setPositiveButton((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_OK)),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // positive button logic
                        int id = radioGroup.getCheckedRadioButtonId();
                        textInputLayoutChatroomType.setError(null);
                        if (id == R.id.create_chatroom_radio_btn_public) {
                            chatroomTypeField.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PUBLIC_GROUP)));
                            chatroomPasswordField.setVisibility(View.GONE);
                            textInputLayoutPassword.setVisibility(View.GONE);
                        } else if (id == R.id.create_chatroom_radio_btn_password) {
                            chatroomTypeField.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PASSWORD_PROTECTED_GROUP)));
                            chatroomPasswordField.setVisibility(View.VISIBLE);
                            textInputLayoutPassword.setVisibility(View.VISIBLE);
                        } else if (id == R.id.create_chatroom_radio_btn_invite) {
                            chatroomTypeField.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_INVITATION_ONLY_GROUP)));
                            chatroomPasswordField.setVisibility(View.GONE);
                            textInputLayoutPassword.setVisibility(View.GONE);
                        } else if (id == R.id.create_chatroom_radio_btn_private) {
                            chatroomTypeField.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PRIVATE_GROUP)));
                            chatroomPasswordField.setVisibility(View.GONE);
                            textInputLayoutPassword.setVisibility(View.GONE);
                        }
                    }
                });

        builder.setNegativeButton((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CANCEL)),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative button logic

                    }
                });


        String type = chatroomTypeField.getText().toString();
        if (type.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PASSWORD_PROTECTED_GROUP)))) {
            radioButtonPassword.setChecked(true);
        } else if (type.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_INVITATION_ONLY_GROUP)))) {
            radioButtonPublicinvite.setChecked(true);
        } else if (type.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PUBLIC_GROUP)))) {
            radioButtonPublic.setChecked(true);
        } else {
            radioButtonPrivate.setChecked(true);
        }

        final AlertDialog dialog = builder.create();

        // display dialog


        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colorPrimary);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorPrimary);
            }
        });

        dialog.show();
    }

    @SuppressLint("HandlerLeak")
    private void createChatroom(final String chatroomName, final String chatroomPassword, String chatroomType) {
        startWheel();

        if (chatroomType.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PUBLIC_GROUP)))) {  // Public Chatroom
            if (createPublicGroup == FeatureState.INACCESSIBLE) {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(CCCreateChatroomActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                stopWheel();
            } else {
                cometChat.createGroup(chatroomName, chatroomPassword, GroupType.PUBLIC_GROUP, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        stopWheel();
                        try {
                            Logger.error(TAG, ": createChatroom() : " + jsonObject);
                            Logger.error(TAG, "jsonObject.getString(group_id) : " + jsonObject.getString("group_id"));

                            // Creating group instance and handling backward compatibility
                            if (!jsonObject.has("type")) {
                                jsonObject.put("type", 0);
                            }
                            if (!jsonObject.has("createdby")) {
                                jsonObject.put("createdby", SessionData.getInstance().getId());
                            }
                            Groups.insertNewGroup(jsonObject);
                            /*group.groupId = jsonObject.getLong("group_id");
                            group.lastUpdated = System.currentTimeMillis();
                            if (jsonObject.has("groupname")) {
                                group.name = jsonObject.getString("groupname");
                            } else {
                                group.name = jsonObject.getString("chatroomname");
                            }
                            group.memberCount = 1;
                            group.type = GroupType.PUBLIC_GROUP.ordinal();
                            group.password = jsonObject.getString("password");
                            group.createdBy = 1;
                            if (jsonObject.has("owner") && (jsonObject.get("owner") instanceof Boolean)) {
                                group.owner = jsonObject.getBoolean("owner") ? 1 : 0;
                            } else {
                                group.owner = jsonObject.getInt("owner");
                            }
                            group.status = 1;
                            group.save();*/

                            Intent intent = new Intent(CCCreateChatroomActivity.this, CCGroupChatActivity.class);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, jsonObject.getString("group_id"));
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        stopWheel();
                        Logger.error(TAG, "Create public chatroom fail responce = " + jsonObject);
                    }
                });
            }
        } else if (chatroomType.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_PASSWORD_PROTECTED_GROUP)))) { // password protected
            if (createProtectedGroup == FeatureState.INACCESSIBLE) {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(CCCreateChatroomActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                stopWheel();
            } else {
                cometChat.createGroup(chatroomName, chatroomPassword, GroupType.PASSWORD_PROTECTED, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "createGroup: " + jsonObject);
                        stopWheel();
                        try {
                            // Creating group instance and handling backward compatibility
                            if (!jsonObject.has("type")) {
                                jsonObject.put("type", 1);
                            }
                            Logger.error(TAG, "userid: " + SessionData.getInstance().getId());
                            if (!jsonObject.has("createdby")) {
                                jsonObject.put("createdby", SessionData.getInstance().getId());
                            }
                            Groups.insertNewGroup(jsonObject);
                            /*group.groupId = jsonObject.getLong("group_id");
                            group.lastUpdated = System.currentTimeMillis();
                            if (jsonObject.has("groupname")) {
                                group.name = jsonObject.getString("groupname");
                            } else {
                                group.name = jsonObject.getString("chatroomname");
                            }
                            group.memberCount = 1;
                            group.type = GroupType.PASSWORD_PROTECTED.ordinal();
                            group.password = jsonObject.getString("password");
                            group.createdBy = 1;
                            if (jsonObject.has("owner") && (jsonObject.get("owner") instanceof Boolean)) {
                                group.owner = jsonObject.getBoolean("owner") ? 1 : 0;
                            } else {
                                group.owner = jsonObject.getInt("owner");
                            }
                            group.status = 1;*/
                            //                        group.save();

                            Intent intent = new Intent(CCCreateChatroomActivity.this, CCGroupChatActivity.class);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, jsonObject.getString("group_id"));
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        stopWheel();
                        Logger.error(TAG, "Create password protected chatroom error responce = " + jsonObject);
                    }
                });
            }
        } else if (chatroomType.equals((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_INVITATION_ONLY_GROUP)))) { // invite only
            if (createInviteGroup == FeatureState.INACCESSIBLE) {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(CCCreateChatroomActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                stopWheel();
            } else {
                cometChat.createGroup(chatroomName, chatroomPassword, GroupType.INVITE_ONLY, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error("createChatroom : successCallback - " + jsonObject.toString());
                        stopWheel();
                        try {
                            // Creating group instance and handling backward compatibility
                            if (!jsonObject.has("type")) {
                                jsonObject.put("type", 2);
                            }
                            if (!jsonObject.has("createdby")) {
                                jsonObject.put("createdby", SessionData.getInstance().getId());
                            }
                            Groups.insertNewGroup(jsonObject);
                            /*Groups group = new Groups();
                            group.groupId = jsonObject.getLong("group_id");
                            group.lastUpdated = System.currentTimeMillis();
                            if (jsonObject.has("groupname")) {
                                group.name = jsonObject.getString("groupname");
                            } else {
                                group.name = jsonObject.getString("chatroomname");
                            }
                            group.memberCount = 1;
                            group.type = GroupType.INVITE_ONLY.ordinal();
                            group.password = jsonObject.getString("password");
                            group.createdBy = 1;
                            if (jsonObject.has("owner") && (jsonObject.get("owner") instanceof Boolean)) {
                                group.owner = jsonObject.getBoolean("owner") ? 1 : 0;
                            } else {
                                group.owner = jsonObject.getInt("owner");
                            }
                            group.status = 1;
                            group.save();*/

                            //                        Logger.error(TAG,"createChatroom : InviteOnly : Group : " + group.toString());

                            Intent intent = new Intent(CCCreateChatroomActivity.this, CCGroupChatActivity.class);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, jsonObject.getString("group_id"));
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                        } catch (JSONException e) {
                            Logger.error("createChatroom : e - " + jsonObject.toString());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        stopWheel();
                        Logger.error("createChatroom : failCallback - " + jsonObject.toString());
                        Logger.error(TAG, "Create invite chatroom fail responce = " + jsonObject);
                    }
                });
            }
        } else if(chatroomType.equals(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_PRIVATE_GROUP)))){
            if (createPrivateGroup == FeatureState.INACCESSIBLE) {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(CCCreateChatroomActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                stopWheel();
            } else {
                cometChat.createGroup(chatroomName, chatroomPassword, GroupType.PRIVATE, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "successCallback: createGroup: " + jsonObject);
                        stopWheel();
                        try {
                            if (!jsonObject.has("type")) {
                                jsonObject.put("type", 4);
                            }
                            if (!jsonObject.has("createdby")) {
                                jsonObject.put("createdby", SessionData.getInstance().getId());
                            }
                            Groups.insertNewGroup(jsonObject);
                            Intent intent = new Intent(CCCreateChatroomActivity.this, CCGroupChatActivity.class);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, jsonObject.getString("group_id"));
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
                            intent.putExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, true);
                            intent.putExtra("GROUP_TYPE", 4);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {

                    }
                });
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    private void startWheel() {
        progressWheel.setVisibility(View.VISIBLE);
    }

    private void stopWheel() {
        progressWheel.setVisibility(View.INVISIBLE);
    }
}
