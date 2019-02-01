package cometchat.inscripts.com.readyui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.inscripts.custom.CustomAlertDialogHelper;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.EncryptionHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.LaunchCallbacks;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.orm.SugarContext;
import com.inscripts.orm.SugarRecord;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import activities.CometChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatcore.coresdk.MessageSDK;
import fragments.GroupFragment;
import models.Contact;
import models.Conversation;
import models.GroupMessage;
import models.Groups;
import models.OneOnOneMessage;
import models.Status;


public class CCReadyUI implements OnAlertDialogButtonClickListener {

    private static final java.lang.String TAG = CCReadyUI.class.getSimpleName();

    private static Context context = null;

    private static CometChat cometChat;
    private static long chatroomId;
    private String chatroomName, chatroomPassword;
    private static ProgressDialog progressDialog;

    private static Activity activity1;
    private static boolean isFullscreen1, isGroup1;
    private static LaunchCallbacks callbacks1;
    private static String groupUserId1;
    private static CCReadyUI ccReadyUI = null;

    public static void initializeCometChat(Context context1, final Callbacks callbacks) {

        context = context1;

        SugarContext.init(context);
        MessageSDK.initializeCometChat(new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "initializeCometChat successCallback = " + jsonObject.toString());
                callbacks.successCallback(jsonObject);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "initializeCometChat failCallback = " + jsonObject.toString());
                callbacks.failCallback(jsonObject);
            }
        });
    }

    public static void launchCometChat(final Activity context, final boolean isFullscreen, LaunchCallbacks callbacks) {
        SugarContext.init(context);
        String clearData = PreferenceHelper.get(PreferenceKeys.DataKeys.CLEAR_USER_DATA);
        Logger.error(TAG,"Clear data = "+clearData);
        if(!TextUtils.isEmpty(clearData) && clearData.equals("1")){
            clearDataBase();
            PreferenceHelper.save(PreferenceKeys.DataKeys.CLEAR_USER_DATA, "0");
        }
        MessageSDK.launchCometChat(context, CometChatActivity.class, isFullscreen, callbacks);
    }

    public static void launchCometChat(final Activity activity, final boolean isFullscreen,
                                       final String groupUserId, final boolean isGroup, final LaunchCallbacks callbacks) {

        activity1 = activity;
        isFullscreen1 = isFullscreen;
        groupUserId1 = groupUserId;
        isGroup1 = isGroup;
        callbacks1 = callbacks;
        ccReadyUI = new CCReadyUI();
        SugarContext.init(activity);
        if (isGroup) {

            Groups groups = Groups.getGroupDetails(Long.valueOf(groupUserId));

            if (groups != null) {
               /* cometChat = CometChat.getInstance(activity);
                cometChat.joinGroup(String.valueOf(groups.groupId), groups.name, groups.password, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"Join grp success responce = "+jsonObject);
                        MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen, isGroup,
                                callbacks, groupUserId);
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "joinGroup failCallback = " + jsonObject.toString());
                    }
                });*/
                cometChat = CometChat.getInstance(activity);
                chatroomId = Long.parseLong(groupUserId);
                ccReadyUI.initGroupJoin(false);

            } else {
                Logger.error("groups", "null");
                cometChat = CometChat.getInstance(activity);
                cometChat.getGroupInfo(groupUserId, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "getGroupInfo successCallback = " + jsonObject.toString());

                        Groups.insertNewGroup(jsonObject);
                        Groups insertedGroup = Groups.getGroupDetails(groupUserId);
                        chatroomId = insertedGroup.groupId;
                        ccReadyUI.initGroupJoin(false);
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "getGroupInfo failCallback = " + jsonObject.toString());
                        callbacks.failCallback(generateFailresponce(true));
                    }
                });
            }
        } else {

                Contact contact = Contact.getContactDetails(Long.valueOf(groupUserId));
                if (contact != null) {
                    MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen, isGroup,
                            callbacks, groupUserId);
                } else {
                    CometChat cometChat = CometChat.getInstance(activity);
                    cometChat.getUserInfo(groupUserId, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "getUserInfo successCallback = " + jsonObject.toString());

                            Contact.insertNewBuddy(jsonObject);

                            MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen,
                                    isGroup, callbacks, groupUserId);
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            callbacks.failCallback(generateFailresponce(false));
                        }
                    });
                }
            }
    }


    public static void launchCometChat(final Activity activity, final boolean isFullscreen,
                                       final String groupUserId, final boolean isGroup,
                                       final boolean closeWindowEnable, final LaunchCallbacks callbacks) {

        activity1 = activity;
        isFullscreen1 = isFullscreen;
        groupUserId1 = groupUserId;
        isGroup1 = isGroup;
        callbacks1 = callbacks;
        ccReadyUI = new CCReadyUI();
        SugarContext.init(activity);

        if (isGroup) {
            if (SessionData.getInstance().isCometOnDemand()) {
                // for cloud
                cometChat = CometChat.getInstance(activity);
                cometChat.getGroupByGUID(activity, groupUserId, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        try {
                            Logger.error(TAG,"getGroupByGUID() : success : response : "+jsonObject);
                            final String groupId = jsonObject.getString("crid");

                            Groups groups = Groups.getGroupDetails(Long.valueOf(groupId));
                            Logger.error(TAG, "getGroupByGUID() : groups : groupId : " + groupId);
                            if (groups != null) {
                                chatroomId = Long.parseLong(groupId);
                                ccReadyUI.initGroupJoin(closeWindowEnable);
                            } else {
                                Groups.insertNewGroup(jsonObject);
                                Groups insertedGroup = Groups.getGroupDetails(groupId);
                                chatroomId = insertedGroup.groupId;
                                ccReadyUI.initGroupJoin(closeWindowEnable);
                            }

                        } catch (JSONException ex) {
                            Logger.error(TAG, "launchCometChat() : getGroupByGUID() : exception : " + ex.getLocalizedMessage());
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"getGroupByGUID() : failCallback() : response : "+jsonObject);
                    }
                });
            } else {
                // for self-hosted
                Groups groups = Groups.getGroupDetails(Long.valueOf(groupUserId));
                Logger.error(TAG, "groups : " + groups);

                if (groups != null) {
                /*cometChat = CometChat.getInstance(activity);
                cometChat.joinGroup(String.valueOf(groups.groupId), groups.name, groups.password, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "joinGroup successCallback = " + jsonObject.toString());
                        MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen, isGroup,closeWindowEnable,
                                callbacks, groupUserId);
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "joinGroup failCallback = " + jsonObject.toString());
                    }
                });*/
                    cometChat = CometChat.getInstance(activity);
                    chatroomId = Long.parseLong(groupUserId);
                    ccReadyUI.initGroupJoin(closeWindowEnable);
                } else {
                    Logger.error("groups", "null");
                    cometChat = CometChat.getInstance(activity);
                    cometChat.getGroupInfo(groupUserId, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "getGroupInfo successCallback = " + jsonObject.toString());

                            Groups.insertNewGroup(jsonObject);
                            Groups insertedGroup = Groups.getGroupDetails(groupUserId);
                            chatroomId = insertedGroup.groupId;
                            ccReadyUI.initGroupJoin(closeWindowEnable);
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "getGroupInfo failCallback = " + jsonObject.toString());
                            callbacks.failCallback(generateFailresponce(true));
                        }
                    });
                }
            }
        } else {

            if(SessionData.getInstance().isCometOnDemand()){
                cometChat = CometChat.getInstance(activity);
                cometChat.getUserByUID(activity,groupUserId, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {

                        try {

                            Logger.error(TAG,"getUserByUID success = "+jsonObject);
                            final String contactId = jsonObject.getJSONObject("user").getString("cid");


                            Contact contact = Contact.getContactDetails(Long.valueOf(contactId));
                            if (contact != null) {
                                MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen, isGroup,closeWindowEnable,
                                        callbacks, contactId);
                            } else {
                                CometChat cometChat = CometChat.getInstance(activity);
                                cometChat.getUserInfo(contactId, new Callbacks() {
                                    @Override
                                    public void successCallback(JSONObject jsonObject) {
                                        Logger.error(TAG, "getUserInfo successCallback = " + jsonObject.toString());

                                        Contact.insertNewBuddy(jsonObject);

                                        MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen,
                                                isGroup, closeWindowEnable,callbacks, contactId);
                                    }

                                    @Override
                                    public void failCallback(JSONObject jsonObject) {
                                        callbacks.failCallback(generateFailresponce(false));
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG,"GetUserByUID fail = "+jsonObject);
                    }
                });
            }else{
                Contact contact = Contact.getContactDetails(Long.valueOf(groupUserId));
                if (contact != null) {
                    MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen, isGroup,closeWindowEnable,
                            callbacks, groupUserId);
                } else {
                    CometChat cometChat = CometChat.getInstance(activity);
                    cometChat.getUserInfo(groupUserId, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Logger.error(TAG, "getUserInfo successCallback = " + jsonObject.toString());

                            Contact.insertNewBuddy(jsonObject);

                            MessageSDK.launchCometChat(activity, CometChatActivity.class, isFullscreen,
                                    isGroup, closeWindowEnable,callbacks, groupUserId);
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            callbacks.failCallback(generateFailresponce(false));
                        }
                    });
                }
            }
        }
    }


    public void initGroupJoin(boolean closeButtonEnabled) {

        Groups chatroom = Groups.getGroupDetails(chatroomId);

        Logger.error(TAG, "chatroom : " + chatroom);

        final ProgressDialog progressDialog;

        try {
            if (CommonUtils.isConnected()) {

                chatroom.unreadCount = 0;
                chatroom.save();

                chatroomId = chatroom.groupId;
                chatroomPassword = chatroom.password;
                int createdBy = chatroom.createdBy;
                chatroomName = chatroom.name;

                Logger.error(TAG, "groupID = " + chatroomId);
                Logger.error(TAG, "createdBy = " + createdBy);
                Logger.error(TAG, "chatroomName = " + chatroomName);
                Logger.error(TAG, "chatroomPassword = " + chatroomPassword);

                /*if (createdBy == 1 || createdBy == 2) {

                    progressDialog = ProgressDialog.show(activity1, "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                    progressDialog.setCancelable(false);
                    progressDialog.dismiss();
                    joinGroup(closeButtonEnabled);
                } else */
                if (createdBy == 0 || createdBy != SessionData.getInstance().getId()) {
                    switch (chatroom.type) {
                        case CometChatKeys.ChatroomKeys.TypeKeys.PUBLIC:
                            progressDialog = ProgressDialog.show(activity1, "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup(closeButtonEnabled);
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.PASSWORD_PROTECTED:

                            LayoutInflater layoutInflater = LayoutInflater.from(activity1);
                            View dialogview = layoutInflater.inflate(R.layout.cc_custom_dialog, null);
                            new CustomAlertDialogHelper(activity1, "Group Password", dialogview,
                                    (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_OK)),
                                    "", (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CANCEL)),
                                    this, 1, false);
                            break;
                        case CometChatKeys.ChatroomKeys.TypeKeys.INVITE_ONLY:

                            progressDialog = ProgressDialog.show(activity1, "",(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                            progressDialog.setCancelable(false);
                            progressDialog.dismiss();
                            joinGroup(closeButtonEnabled);
                            break;

                        default:
                            break;
                    }
                } else {

                    if(chatroom.type == CometChatKeys.ChatroomKeys.TypeKeys.PASSWORD_PROTECTED && TextUtils.isEmpty(chatroomPassword)) {
                        LayoutInflater layoutInflater = LayoutInflater.from(activity1);
                        View dialogview = layoutInflater.inflate(R.layout.cc_custom_dialog, null);
                        new CustomAlertDialogHelper(activity1, "Group Password", dialogview, (String)
                                cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_OK)),
                                "", (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CANCEL)),
                                this, 1, false);
                    } else {
                        progressDialog = ProgressDialog.show(activity1, "", (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                        progressDialog.setCancelable(false);
                        progressDialog.dismiss();
                        joinGroup(closeButtonEnabled);
                    }
                }
            } else {
                Logger.error(TAG, "CommonUtils.isConnected() false");
            }
        } catch (Exception e) {
            Logger.error(TAG, "initGroupJoin = " + e.toString());
            e.printStackTrace();
        }
    }

    private void joinGroup(final boolean closeButtonEnabled) {
        cometChat.joinGroup(String.valueOf(chatroomId), chatroomName, chatroomPassword, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "joinGroup successCallback = " + jsonObject.toString());
                MessageSDK.launchCometChat(activity1, CometChatActivity.class, isFullscreen1,
                        isGroup1,closeButtonEnabled, callbacks1, String.valueOf(chatroomId));
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "joinGroup failCallback = " + jsonObject.toString());
            }
        });
    }

    @Override
    public void onButtonClick(AlertDialog alertDialog, View view, int which, int i1) {
        final EditText chatroomPasswordInput = (EditText) view.findViewById(R.id.edittextDialogueInput);

        if (which == DialogInterface.BUTTON_NEGATIVE) { // Cancel
            alertDialog.dismiss();
        } else if (which == DialogInterface.BUTTON_POSITIVE) { // Join
            try {
                progressDialog = ProgressDialog.show(context, "",(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_JOINING_GROUP)));
                progressDialog.setCancelable(false);
                chatroomPassword = chatroomPasswordInput.getText().toString();
                if (chatroomPassword.length() == 0) {
                    chatroomPasswordInput.setText("");
                    chatroomPasswordInput.setError("Incorrect password");
                    progressDialog.dismiss();
                } else {
                    try {
                        chatroomPassword = EncryptionHelper.encodeIntoShaOne(chatroomPassword);
                        joinGroup(false);

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

    private static JSONObject generateFailresponce(boolean isGroup){

        JSONObject jsonObject = new JSONObject();
            try {
                if(isGroup){
                    jsonObject.put("Fail","Invalid Group ID");
                }else{
                    jsonObject.put("Fail","Invalid Contact ID");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return  jsonObject;
    }


    private static void getContactAndLaunch(String contactID){

    }

    private static void clearDataBase(){
        SugarRecord.deleteAll(OneOnOneMessage.class);
        SugarRecord.deleteAll(Groups.class);
        SugarRecord.deleteAll(Conversation.class);
        SugarRecord.deleteAll(GroupMessage.class);
        SugarRecord.deleteAll(Status.class);
    }
}
