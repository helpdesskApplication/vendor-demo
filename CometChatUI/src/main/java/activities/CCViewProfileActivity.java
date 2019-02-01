package activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.custom.CustomAlertDialogHelper;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.CometchatCallbacks;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.jsonphp.JsonPhp;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.plugins.ImageSharing;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import services.AvatarService;

import com.inscripts.custom.ProfileRoundedImageView;

public class CCViewProfileActivity extends AppCompatActivity implements View.OnClickListener, OnAlertDialogButtonClickListener {

    private static final String TAG = CCViewProfileActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private static Uri fileUri, tempFileURI;
    private RelativeLayout viewStatusMessage, viewOnlineStatus;
    private ImageView ivEditImage;
    private ImageView imgStatusMessage, imgOnlineStatus, imgEditUserName;
    private ProfileRoundedImageView avatarImage;
    private TextView username;
    private TextView tvOnlineStatusSubtitle;
    private static final int EDIT_USER_NAME = 2;
    private int colorPrimary,colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_view_profile);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_view_profile_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar, R.drawable.cc_rounded_corners_colored);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();
        setupFields();


        this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_VIEW_PROFILE)));

        setupProfile();
        setCurrentStatus();
        setupSetting();
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
        username = (TextView) findViewById(R.id.textViewProfileUserName);
        avatarImage = (ProfileRoundedImageView) findViewById(R.id.imageViewUserProfilePhoto);
        viewStatusMessage = (RelativeLayout) findViewById(R.id.ll_status_message);
        viewOnlineStatus = (RelativeLayout) findViewById(R.id.ll_online_status);
        tvOnlineStatusSubtitle = (TextView) findViewById(R.id.online_status_subtitle);
        imgEditUserName = (ImageView) findViewById(R.id.iv_edit_username);
        ivEditImage = (ImageView) findViewById(R.id.iv_change_profile);

        imgStatusMessage = (ImageView) findViewById(R.id.image_view_status_message);
        imgStatusMessage.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

        imgOnlineStatus = (ImageView) findViewById(R.id.setting_online_status);
        imgOnlineStatus.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        animateStatusViews();
    }

    private void setupProfile() {
        SessionData sessionData = SessionData.getInstance();
        try {

            avatarImage.setBorderWidth(1);
            ivEditImage.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

            if (!TextUtils.isEmpty(sessionData.getName())) {
                if (PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN_AS_GUEST) || PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN_AS_DEMO)) {
                    String guestPrefix = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LOGIN_SETTINGS,SettingSubType.LANG_GUEST_PREFIX));
                    if (TextUtils.isEmpty(guestPrefix)) {
                        username.setText(Html.fromHtml("Guest-" + sessionData.getName()));
                    } else {
                        username.setText(Html.fromHtml(sessionData.getName()));
                    }
                } else {
                    username.setText(Html.fromHtml(sessionData.getName()));
                }
            }

            String url = sessionData.getAvatarLink();
            if (null != url) {
                LocalStorageFactory.loadImageUsingURL(this, url, avatarImage, R.drawable.cc_default_avatar);
            }
            FrameLayout imageContainer = (FrameLayout) findViewById(R.id.relativeLayoutProfilePicContainer);

                if ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.USERNAME_PASSWORD_ENABLED))=="1" || PreferenceHelper.contains(PreferenceKeys.LoginKeys.LOGGED_IN_AS_COD)) {
                    ivEditImage.setVisibility(View.GONE);
                } else {
                    ivEditImage.setOnClickListener(this);
                }
        } catch (Exception e) {
            avatarImage.setImageResource(R.drawable.cc_default_avatar);
            username.setText(sessionData.getName());
            Logger.error(TAG, "Cannot setup profile: setupProfile() : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setCurrentStatus() {
        String status = PreferenceHelper.get(PreferenceKeys.UserKeys.STATUS);
        if (status != null) {
            switch (status) {
                case CometChatKeys.StatusKeys.AVALIABLE:
                    tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_AVAILABLE)));
                    break;

                case CometChatKeys.StatusKeys.BUSY:
                    tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)));
                    break;

                case CometChatKeys.StatusKeys.INVISIBLE:
                    tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVISIBLE)));
                    break;

                case CometChatKeys.StatusKeys.OFFLINE:
                    tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_OFFLINE)));
                    break;
            }
        }
    }

    private void setupSetting() {


            TextView tvStatusMessage = (TextView) viewStatusMessage.findViewById(R.id.setting_edit_status_messgae);
            tvStatusMessage.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_STATUS_MESSAGE)));


        viewStatusMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent statusIntent = new Intent(CCViewProfileActivity.this, CCStatusMessageActivity.class);
                startActivity(statusIntent);
            }
        });


            TextView tvOnlineStatus = (TextView) viewOnlineStatus.findViewById(R.id.online_status_title);
            tvOnlineStatus.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_ONLINE_STATUS)));


        viewOnlineStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStatusTypeDialog();
            }
        });

        if (PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_GUEST) != null && PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_GUEST).equals("1")) {
            imgEditUserName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View dialogview1 = getLayoutInflater().inflate(R.layout.cc_custom_dialog, null);

                    TextView dialogueTitle1 = (TextView) dialogview1.findViewById(R.id.textViewDialogueTitle);
                    dialogueTitle1.setVisibility(View.GONE);

                    EditText dialogueTextInput1 = (EditText) dialogview1.findViewById(R.id.edittextDialogueInput);
                    dialogueTextInput1.setInputType(InputType.TYPE_CLASS_TEXT);
                    String nameTitle = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SET_USERNAME));
                    dialogueTextInput1.setHint(nameTitle);

                    SessionData sessionData = SessionData.getInstance();

                    String name = "";
                    if (!TextUtils.isEmpty(sessionData.getName()) && sessionData.getName().startsWith("Guest-")) {
                        name = sessionData.getName().substring(6);
                    } else {
                        name = sessionData.getName();
                    }
                    dialogueTextInput1.setText(name);
                    dialogueTextInput1.setSelection(name.length());

                    new CustomAlertDialogHelper(CCViewProfileActivity.this, nameTitle, dialogview1, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SET)), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL)), CCViewProfileActivity.this, EDIT_USER_NAME,false);
                }
            });
        } else if (PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_DEMO) != null && PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_DEMO).equals("1")) {
            imgEditUserName.setVisibility(View.VISIBLE);

            imgEditUserName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View dialogview1 = getLayoutInflater().inflate(R.layout.cc_custom_dialog, null);

                    TextView dialogueTitle1 = (TextView) dialogview1.findViewById(R.id.textViewDialogueTitle);
                    dialogueTitle1.setVisibility(View.GONE);

                    EditText dialogueTextInput1 = (EditText) dialogview1.findViewById(R.id.edittextDialogueInput);
                    dialogueTextInput1.setInputType(InputType.TYPE_CLASS_TEXT);
                    String nameTitle = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SET_USERNAME));
                    dialogueTextInput1.setHint(nameTitle);

                    SessionData sessionData = SessionData.getInstance();
                    String name = "";
                    if (!TextUtils.isEmpty(sessionData.getName()) && sessionData.getName().startsWith("Guest-")) {
                        name = sessionData.getName().substring(6);
                    } else {
                        name = sessionData.getName();
                    }
                    dialogueTextInput1.setText(name);
                    dialogueTextInput1.setSelection(name.length());
                    new CustomAlertDialogHelper(CCViewProfileActivity.this, nameTitle, dialogview1, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SET)), "", (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL)), CCViewProfileActivity.this, EDIT_USER_NAME,false);
                }
            });
        } else {
            imgEditUserName.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == 1) {
                    boolean isCamera;
                    isCamera = ((data == null) || (data.hasExtra(MediaStore.EXTRA_OUTPUT)));

                    Intent intent = new Intent("com.android.camera.action.CROP");
                    if (isCamera) {
                        intent.setDataAndType(fileUri, StaticMembers.IMAGE_TYPE);
                    } else {
                        intent.setDataAndType(data.getData(), StaticMembers.IMAGE_TYPE);
                    }

                    intent.putExtra("crop", "true");
                    intent.putExtra("aspectX", 1);
                    intent.putExtra("aspectY", 1);
                    // intent.putExtra("outputX", 150);
                    // intent.putExtra("outputY", 150);
                    intent.putExtra("scale", true);
                    intent.putExtra("noFaceDetection", true);
                    intent.putExtra("return-data", false);
                    tempFileURI = ImageSharing.getOutputMediaFileUri(CCViewProfileActivity.this,StaticMembers.MEDIA_TYPE_IMAGE, false);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileURI);
                    startActivityForResult(intent, 2);
                } else if (requestCode == 2) {
                    Bitmap profilePic = null;
                    String fileName, filePath;
                    Bundle extras = data.getExtras();
                    if (null != extras) {
                        profilePic = extras.getParcelable("data");
                    }
                    if (profilePic == null) {
                        File image = new File(tempFileURI.getPath());
                        fileName = image.getName();
                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                        try {
                            profilePic = BitmapFactory.decodeFile(image.getPath(), bmOptions);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } catch (OutOfMemoryError e) {
                            bmOptions.inSampleSize = 2;
                            profilePic = BitmapFactory.decodeFile(image.getPath(), bmOptions);
                        }
                        avatarImage.setImageBitmap(profilePic);
                    } else {
                        avatarImage.setImageBitmap(profilePic);
                        filePath = LocalStorageFactory.getFilePathFromIntent(data);
                        if (null == filePath) {
                            fileName = "temp.png";
                        } else {
                            fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
                        }
                    }

                    fileName = fileName.replaceAll("-", "").replaceAll(" ", "_").replaceAll("_", "");

                    changeAvatar(profilePic, fileName, new CometchatCallbacks() {

                        @Override
                        public void successCallback() {
                            SessionData.getInstance().setUserInfoHeartBeatFlag("1");
                            Toast.makeText(CCViewProfileActivity.this, getResources().getString(R.string.avatar_changed_success), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void failCallback() {
                            Logger.error("change avatar failed");
                            Toast.makeText(CCViewProfileActivity.this, getResources().getString(R.string.avatar_changed_failure), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if (requestCode == 3) {
                    if (data != null) {
                        fileUri = data.getData();
                        Intent intent = new Intent("com.android.camera.action.CROP");
                        intent.setData(fileUri);
                        intent.putExtra("crop", "true");
                        intent.putExtra("aspectX", 1);
                        intent.putExtra("aspectY", 2);
                        intent.putExtra("scale", true);
                        intent.putExtra("noFaceDetection", true);
                        intent.putExtra("return-data", false);
                        tempFileURI = Uri.fromFile(getOutputCroppedFile(StaticMembers.MEDIA_TYPE_IMAGE));
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileURI);
                        startActivityForResult(intent, 4);

                    }
                } else if (requestCode == 4) {
                    Bitmap profilePic;
                    Bundle extras = data.getExtras();
                    profilePic = extras.getParcelable("data");
                    if (profilePic == null) {
                        File image = new File(tempFileURI.getPath());
                        PreferenceHelper.save(PreferenceKeys.DataKeys.WALLPAPER_FILENAME, String.valueOf(image.getPath()));
                        Toast.makeText(this, "Wallpaper Set", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onButtonClick(final android.app.AlertDialog alertDialog, View v, int which, int popupId) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            switch (popupId) {
                case EDIT_USER_NAME:
                    EditText statusMessageField1 = (EditText) v.findViewById(R.id.edittextDialogueInput);
                    final String newUserName = statusMessageField1.getText().toString().trim();
                    if (!TextUtils.isEmpty(newUserName)) {
                        String url = "";
                        String key = "";

                        if ((PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_GUEST) != null && PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_GUEST).equals("1")) ||
                                (PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_DEMO) != null && PreferenceHelper.get(PreferenceKeys.LoginKeys.LOGGED_IN_AS_DEMO).equals("1"))) {
                            url = URLFactory.getSendOneToOneMessageURL();
                            key = CometChatKeys.AjaxKeys.GUEST_NAME;
                        } else {
                            url = URLFactory.getPhoneRegisterURL();
                            key = CometChatKeys.AjaxKeys.ACTION;
                        }

                        VolleyHelper volley = new VolleyHelper(CCViewProfileActivity.this, url,
                                new VolleyAjaxCallbacks() {

                                    @Override
                                    public void successCallback(String response) {
                                        Logger.error("ViewProfileAcivity : onButtonClick() : success : " + response);
                                        SessionData.getInstance().setUserInfoHeartBeatFlag("1");
                                        SessionData.getInstance().setName(newUserName);
                                        PreferenceHelper.save(PreferenceKeys.UserKeys.USER_NAME, newUserName);

                                        String displayName = "";
                                        if (!TextUtils.isEmpty(newUserName) && !newUserName.startsWith("Guest-")) {
                                            displayName = "Guest-" + newUserName;
                                        }
                                        username.setText(displayName);

                                        alertDialog.dismiss();
                                    }

                                    @Override
                                    public void failCallback(String response, boolean isNoInternet) {
                                        Logger.error("ViewProfileAcivity : onButtonClick() : failure : " + response);
                                        if (isNoInternet) {
                                            Toast.makeText(CCViewProfileActivity.this, StaticMembers.PLEASE_CHECK_YOUR_INTERNET,
                                                    Toast.LENGTH_LONG).show();
                                            alertDialog.dismiss();
                                        }
                                    }
                                });
                        if (key.equalsIgnoreCase(CometChatKeys.AjaxKeys.GUEST_NAME)) {
                            volley.addNameValuePair(key, newUserName);
                        } else {
                            volley.addNameValuePair(key, "change_name");
                        }

                        volley.addNameValuePair(CometChatKeys.AjaxKeys.NAME, newUserName);

                        volley.sendAjax();

                        SessionData.getInstance().setUserInfoHeartBeatFlag("1");
                        SessionData.getInstance().setName(newUserName);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.USER_NAME, newUserName);
                        alertDialog.dismiss();
                    } else {
                        statusMessageField1.setError((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_ENTER_USERNAME)));
                        alertDialog.dismiss();

                    }
                    break;

                default:
                    Logger.error("CCViewProfileActivity : onButtonClick : default case");
                    break;
            }
        } else {
            alertDialog.dismiss();
        }
    }

    private void showStatusTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CCViewProfileActivity.this);
        View view = getLayoutInflater().inflate(R.layout.cc_custom_set_status_dialog, null);
        builder.setView(view);

        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.set_status_radio_grp);
        RadioButton radioButtonAviailable = (RadioButton) view.findViewById(R.id.set_status_radio_btn_available);
        RadioButton radioButtonBussy = (RadioButton) view.findViewById(R.id.set_status_radio_btn_bussy);
        RadioButton radioButtonInvisible = (RadioButton) view.findViewById(R.id.set_status_radio_btn_invisible);
        RadioButton radioButtonOfline = (RadioButton) view.findViewById(R.id.set_status_radio_btn_ofline);

        radioButtonAviailable.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_AVAILABLE)));
        radioButtonBussy.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)));
        radioButtonInvisible.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVISIBLE)));
        radioButtonOfline.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_OFFLINE)));

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
                radioButtonAviailable.setButtonTintList(colorStateList);
                radioButtonBussy.setButtonTintList(colorStateList);
                radioButtonInvisible.setButtonTintList(colorStateList);
                radioButtonOfline.setButtonTintList(colorStateList);
            }


        builder.setTitle((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_SET_STATUS)));

        builder.setPositiveButton((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_OK)),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // positive button logic
                        String newStatus;

                        int id = radioGroup.getCheckedRadioButtonId();

                        if (id == R.id.set_status_radio_btn_available) {
                            newStatus = CometChatKeys.StatusKeys.AVALIABLE;

                        } else if (id == R.id.set_status_radio_btn_bussy) {
                            newStatus = CometChatKeys.StatusKeys.BUSY;

                        } else if (id == R.id.set_status_radio_btn_invisible) {
                            newStatus = CometChatKeys.StatusKeys.INVISIBLE;

                        } else {
                            newStatus = CometChatKeys.StatusKeys.OFFLINE;
                        }
                        final String finalNewStatus = newStatus;
                        cometChat.updateAvailabilityStatus(finalNewStatus, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"updateAvailabilityStatus(): successCallback: "+jsonObject);
                                PreferenceHelper.save(PreferenceKeys.UserKeys.STATUS, finalNewStatus);
                                switch (finalNewStatus) {
                                    case CometChatKeys.StatusKeys.AVALIABLE:
                                        tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_AVAILABLE)));
                                        break;

                                    case CometChatKeys.StatusKeys.BUSY:
                                        tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)));
                                        break;

                                    case CometChatKeys.StatusKeys.INVISIBLE:
                                        tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVISIBLE)));
                                        break;

                                    case CometChatKeys.StatusKeys.OFFLINE:
                                        tvOnlineStatusSubtitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_OFFLINE)));
                                        break;
                                }

                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"updateAvailabilityStatus(): failCallback: "+jsonObject);
                            }
                        });
                        dialog.dismiss();
                    }
                });

        String negativeText = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL));
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative button logic
                    }
                });

        String status = PreferenceHelper.get(PreferenceKeys.UserKeys.STATUS);

        if (status.equals(CometChatKeys.StatusKeys.BUSY)) {
            radioButtonBussy.setChecked(true);
        } else if (status.equals(CometChatKeys.StatusKeys.INVISIBLE)) {
            radioButtonInvisible.setChecked(true);
        } else if (status.equals(CometChatKeys.StatusKeys.OFFLINE)) {
            radioButtonOfline.setChecked(true);
        } else {
            radioButtonAviailable.setChecked(true);
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

    private static File getOutputCroppedFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), PreferenceHelper.getContext()
                .getString(R.string.app_name));

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File mediaFile = null;
        if (type == StaticMembers.MEDIA_TYPE_IMAGE) {
            String imageStoragePath = LocalStorageFactory.getWallpaperStoragePath();
            LocalStorageFactory.createDirectory(imageStoragePath);
            String filename = "IMG" + timeStamp + ".jpg";
            mediaFile = new File(imageStoragePath + filename);
        }
        return mediaFile;
    }

    @Override
    public void onClick(View v) {
//        NewMobile newmobilelangs = JsonPhp.getInstance().getNewMobile();
            if (v.getId() == R.id.iv_change_profile) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = ImageSharing.getOutputMediaFileUri(CCViewProfileActivity.this,StaticMembers.MEDIA_TYPE_IMAGE, false);

                List<Intent> cameraIntents = new ArrayList<Intent>();
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> listCam = packageManager.queryIntentActivities(cameraIntent, 0);
                for (ResolveInfo res : listCam) {
                    String packageName = res.activityInfo.packageName;
                    Intent intent = new Intent(cameraIntent);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                    intent.setPackage(packageName);
                    intent.putExtra("return-data", true);
                    cameraIntents.add(intent);
                }

                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType(StaticMembers.IMAGE_TYPE);

                Intent chooserIntent;
                chooserIntent = Intent.createChooser(galleryIntent, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_COMPLETE_ACTION)));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
                startActivityForResult(chooserIntent, 1);
            } else {
                Logger.error("CCViewProfileActivity : onClick() : default case executed");
            }
    }

    public void changeAvatar(Bitmap bitmap, String filename, final CometchatCallbacks callbacks) {
        try {
            Handler handler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    String response = msg.obj.toString();
                    Logger.error("Response of change avatar =" + response);
                    switch (msg.what) {
                        case 200:
                            try {
                                JSONObject jsonresponse = new JSONObject(response);
                                if (jsonresponse.getString("status").equals("1")) {
                                    callbacks.successCallback();
                                    SessionData.getInstance().setAvatarLink(jsonresponse.getString("avatar"));
                                }
                            } catch (Exception e) {
                                callbacks.failCallback();
                                e.printStackTrace();
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

            AvatarService.setHandler(handler);
            AvatarService.startActionChangeAvatar(this, filename, callbacks, bitmap);
        } catch (Exception e) {
            callbacks.failCallback();
            e.printStackTrace();
        }
    }
    private void animateStatusViews() {
        ObjectAnimator scaleStatusMessageImageX = ObjectAnimator.ofFloat(imgStatusMessage, "scaleX", 0.0f,1.0f);
        ObjectAnimator scaleStatusMessageImageY = ObjectAnimator.ofFloat(imgStatusMessage, "scaleY", 0.0f,1.0f);
        ObjectAnimator scaleOnlineStatusImageX = ObjectAnimator.ofFloat(imgOnlineStatus, "scaleX", 0.0f,1.0f);
        ObjectAnimator scaleOnlineStatusImageY = ObjectAnimator.ofFloat(imgOnlineStatus, "scaleY", 0.0f,1.0f);
        scaleStatusMessageImageX.setDuration(500);
        scaleStatusMessageImageY.setDuration(500);
        scaleOnlineStatusImageX.setDuration(500);
        scaleOnlineStatusImageY.setDuration(500);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleOnlineStatusImageX,scaleOnlineStatusImageY,scaleStatusMessageImageX,scaleStatusMessageImageY);
        animatorSet.start();
    }
}
