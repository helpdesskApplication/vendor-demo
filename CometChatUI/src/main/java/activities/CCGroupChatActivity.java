package activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.Keyboards.SmileyKeyBoard;
import com.inscripts.Keyboards.StickerKeyboard;
import com.inscripts.Keyboards.adapter.EmojiGridviewImageAdapter;
import com.inscripts.Keyboards.adapter.StickerGridviewImageAdapter;
import com.inscripts.activities.CCHandwriteActivity;
import com.inscripts.activities.CCWebViewActivity;
import com.inscripts.custom.CustomAlertDialogHelper;
import com.inscripts.custom.StickyHeaderDecoration;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.DataCursorLoader;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.PopupHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.CometchatCallbacks;
import com.inscripts.interfaces.KeyboardVisibilityEventListener;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.IntentExtraKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.plugins.AudioSharing;
import com.inscripts.plugins.ChatroomManager;
import com.inscripts.plugins.ImageSharing;
import com.inscripts.plugins.Smilies;
import com.inscripts.plugins.VideoSharing;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.KeyboardVisibilityEvent;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;
import com.inscripts.helpers.CCPermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import Keys.BroadCastReceiverKeys;
import Keys.JsonParsingKeys;
import Keys.MessageTypeKeys;
import adapters.GroupMessageAdapter;
import adapters.SelectColorGridAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatcore.coresdk.MessageSDK;
import cometchat.inscripts.com.readyui.R;
import customsviews.ConfirmationWindow;
import helpers.CCMessageHelper;
import helpers.FileSharing;
import helpers.NotificationDataHelper;
import models.GroupMessage;
import models.Groups;
import models.OneOnOneMessage;
import pojo.ColorPojo;
import videochat.CCVideoChatActivity;

import static activities.CCSingleChatActivity.hideSoftKeyboard;

public class CCGroupChatActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>
        , OnAlertDialogButtonClickListener, EmojiGridviewImageAdapter.EmojiClickInterface, GroupMessageAdapter.RetryCallback {
    private static final java.lang.String TAG = CCGroupChatActivity.class.getSimpleName();
    private SessionData sessionData;
    private Toolbar toolbar;
    private RelativeLayout ccContainer, customMenu, chatFooter;
    private int colorPrimary, colorPrimaryDark, picassImageName;
    private String chatroomName, shareImageUri, shareVideoUri, shareAudioUri, audioFileNamewithPath;
    private long firstMessageID = 0L;
    private long chatroomId;
    private Intent data;
    private EditText messageField;
    private ImageButton sendButton, voiceNotebtn, btnMenu, btnChatMenuKeyBoard, btnChatMenuSharePhoto, btnChatMenuShareVideo, btnChatMenuSmiliey,
            btnCameraButton, btnChatMenuMore, btnSticker,btnAVBroadcast;
    private RecyclerView messageRecyclerView;
    private View dirtyView;
    private BottomSheetBehavior sheetCameraBehavior, sheetBehavior, sheetBehaviorSelectColor;
    private LinearLayout cameraSheetLayout, chatMenuLayout, bottomSheetlayout, bottomSheetSelectColor;
    private RelativeLayout viewShareWiteboard, viewCollaborativeDocument, viewHandwriteMessage;
    private RelativeLayout  viewCapturePhoto, viewCaptureVideo;
    private ImageView capturePhotoImageView, capturevideoImageView, whiteboardImageView, collaborativeDocumentImageView, handwriteMessageImageView;
    private CometChat cometChat;
    private final int PICASSA_IMAGE_PREVIEW_POPUP = 4, IMAGE_SEND_PREVIEW_POPUP = 2, VIDEO_SEND_PREVIEW_POPUP = 3, AUDIO_SEND_PREVIEW_POPUP = 6, BLOCK_USER_POPUP = 7, REPORT_CONVERSATION_POPUP = 8;
    private final int MESSAGE_LOADER = 1;
    private int messageCount;
    private GroupMessageAdapter groupMessageAdapter;
    private Bitmap picassaBitmap;
    private BroadcastReceiver broadcastReceiver;
    private boolean isRecording = false, isVoiceNoteplaying = false;
    private MediaRecorder voiceRecorder;
    private Runnable timerRunnable;
    private Handler seekHandler = new Handler();
    private static Uri fileUri;
    private String selectedColor;
    private SelectColorGridAdapter gridAdapter;
    private ArrayList<ColorPojo> colorList;
    private SmileyKeyBoard smiliKeyBoard;
    private StickerKeyboard stickerKeyboard;
    private TextView tvHandwriteMessage,tvCollaborativeDoc,tvWhiteBoard,tvCapturePhoto,tvCaptureVideo,txtLoadEarlierMessages;
    private SwipeRefreshLayout refreshLayout;
    //private CometChat cometChat;

    private static String mediaFilePath;
    private StickyHeaderDecoration decor;

    private boolean isModerator = false;
    private boolean isOwner = false;
    private int requestCode=0;
    private MediaPlayer player;
    private BroadcastReceiver finishActivityReceiver;
    private TextView toolbarTitle,toolbarSubTitle;
    private LinearLayoutManager linearLayoutManager;
    private boolean isInitialLoad = true;
    private Button btnScroll;
    private long newMessageCount;
    private boolean isCloseWindowEnabled;
    private FeatureState grpAvCallState,grpAudioCallState,grpFileTransferState,grpVoicenoteState;
    private FeatureState grpStickerState,grpSmileyState,grpAVBroadcastState;
    private FeatureState grpHandwriteState;
    private FeatureState grpWhiteBoardState;
    private FeatureState grpWriteBoardState;
    private FeatureState grpClearConversationState;
    private FeatureState colorYourTextState;
    private boolean isPrivateGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_cordinate);
        cometChat = CometChat.getInstance(this);
        sessionData = SessionData.getInstance();
        toolbar = (Toolbar) findViewById(R.id.cometchat_toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        Logger.error(TAG,"id: "+SessionData.getInstance().getId());
        NotificationManager notificationManager = (NotificationManager) PreferenceHelper.getContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        PreferenceHelper.removeKey(PreferenceKeys.DataKeys.NOTIFICATION_STACK);
        ccContainer = (RelativeLayout) findViewById(R.id.cc_chat_container);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){

            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupFields();
        setCCTheme();
        setFieldListners();
        initializeFeatureState();
        processIntentData(getIntent());
        if(isCloseWindowEnabled){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationIcon(R.drawable.cc_ic_action_cancel);
        }
        getSupportActionBar().setTitle("");
        toolbarTitle.setText(chatroomName);
        if (getSupportLoaderManager().getLoader(MESSAGE_LOADER) == null) {
            Logger.error(TAG, "initLoader");
            getSupportLoaderManager().initLoader(MESSAGE_LOADER, null, this);
        }else {
            Logger.error(TAG, "restartLoader");
            getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, this);
        }

        setupSmileyKeyboars();
        setupStickerKeyboard();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras.containsKey(BroadCastReceiverKeys.NEW_MESSAGE)) {
                    newMessageCount++;
                    getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                    Logger.error(TAG,"NEW_MESSAGE BroadCast grp");
                }
            }
        };
        finishActivityReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.error(TAG, "onReceive: CCGroupChatActivity called");
                Bundle extras = intent.getExtras();
                Logger.error(TAG, "onReceive: contains group id:"+ extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID)+" groupId : "+intent.getStringExtra(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID));
                if (extras != null && extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID)) {
                    if (intent.getStringExtra(BroadCastReceiverKeys.IntentExtrasKeys.GROUP_ID).equals(String.valueOf(chatroomId))) {
                        Groups groups = Groups.getGroupDetails(chatroomId);
                        groups.status = 0;
                        groups.save();
                        finish();
                        /*if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.KICKED)) {
                            Toast.makeText(getBaseContext(), "You have been kicked from " + chatroomName, Toast.LENGTH_SHORT).show();
                        } else if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.BANNED)) {
                            Toast.makeText(getBaseContext(), "You have been banned from " + chatroomName, Toast.LENGTH_SHORT).show();
                        }*/
                    }
                }
            }
        };
//        registerReceiver(finishActivityReceiver, new IntentFilter(BroadCastReceiverKeys.FINISH_GROUP_ACTIVITY));
        registerReceiver(broadcastReceiver, new IntentFilter(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST));
        if(PreferenceHelper.get(PreferenceKeys.Colors.COLOR_CHATROOM_CHAT) != null){
            selectedColor = PreferenceHelper.get(PreferenceKeys.Colors.COLOR_CHATROOM_CHAT);
        }else{
            selectedColor = "#FFF";
        }
        setUpColorPicker();
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_CHATROOM_ID, chatroomId);
        if(isPrivateGroup){
            inviteUsers();
        }
    }

    private void initializeFeatureState() {
        grpAvCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_VIDEO_CALL_ENABLED));
        grpAudioCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_AUDIO_CALL_ENABLED));
        grpFileTransferState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_FILE_TRANSFER_ENABLED));
        grpVoicenoteState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_VOICE_NOTE_ENABLED));
        grpStickerState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_STICKER_ENABLED));
        grpSmileyState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_EMOJI_ENABLED));
        grpAVBroadcastState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_AV_BROADCAST_ENABLED));
        grpHandwriteState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GRP_HANDWRITE_ENABLED));
        grpWhiteBoardState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GRP_WHITEBOARD_ENABLED));
        grpWriteBoardState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GRP_WRITEBOARD_ENABLED));
        grpClearConversationState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GRP_CLEAR_CONVERSATION_ENABLED));
        colorYourTextState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.GRP_COLOR_YOUR_TEXT));
        Logger.error(TAG, "initializeFeatureState: grpVoicenoteState: "+grpVoicenoteState.name());
        Logger.error(TAG, "initializeFeatureState: grpFileTransferState: "+grpFileTransferState.name());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.error(TAG, "onResume: called" );
        if(requestCode == 0){
            if (getSupportLoaderManager().getLoader(MESSAGE_LOADER) != null) {
                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, this);
            }
        }
        requestCode = 0;
        Logger.error(TAG, "onResume: chatroomId: "+chatroomId);
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_CHATROOM_ID, chatroomId);
    }

    private void setCCTheme(){
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        String hexColor = String.format("#%06X", (0xFFFFFF & colorPrimary));
        Logger.error(TAG,"colorPrimary: "+hexColor);
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this, colorPrimaryDark);
        capturePhotoImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        capturevideoImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        collaborativeDocumentImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        whiteboardImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        handwriteMessageImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        txtLoadEarlierMessages.setTextColor(colorPrimary);
        refreshLayout.setColorSchemeColors(colorPrimary);
        btnScroll.getBackground().setColorFilter(Color.parseColor("#8e8e92"), PorterDuff.Mode.SRC_ATOP);
    }


    private void setupFields() {
        messageField = (EditText) findViewById(R.id.editTextChatMessage);
        sendButton = (ImageButton) findViewById(R.id.buttonSendMessage);
        voiceNotebtn = (ImageButton) findViewById(R.id.buttonSendVoice);
        btnMenu = (ImageButton) findViewById(R.id.img_btn_chat_more);
        btnChatMenuKeyBoard = (ImageButton) findViewById(R.id.btn_chat_menu_keyboard);
        btnChatMenuSharePhoto = (ImageButton) findViewById(R.id.btn_chat_menu_share_image);
        btnChatMenuShareVideo = (ImageButton) findViewById(R.id.btn_chat_menu_share_video);
        btnChatMenuSmiliey = (ImageButton) findViewById(R.id.img_btn_smiley);
        btnCameraButton = (ImageButton) findViewById(R.id.img_btn_camera);
        messageRecyclerView = (RecyclerView) findViewById(R.id.rvChatMessages);
        dirtyView = findViewById(R.id.dirty_view);
        linearLayoutManager = new LinearLayoutManager(this);
        messageRecyclerView.setLayoutManager(linearLayoutManager);
        customMenu = (RelativeLayout) findViewById(R.id.relativeLayoutMenu1);
        cameraSheetLayout = (LinearLayout) findViewById(R.id.bottom_sheet_camera);
        sheetCameraBehavior = BottomSheetBehavior.from(cameraSheetLayout);
        sheetCameraBehavior.setPeekHeight(0);
        btnChatMenuMore = (ImageButton) findViewById(R.id.btn_chat_menu_more);
        chatMenuLayout = (LinearLayout) findViewById(R.id.custom_bottom_sheet_menu);
        bottomSheetlayout = (LinearLayout) findViewById(R.id.bottom_sheet_plugin);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetlayout);
        sheetBehavior.setPeekHeight(0);
        viewCapturePhoto = (RelativeLayout) findViewById(R.id.ll_capture_photo);
        viewCaptureVideo = (RelativeLayout) findViewById(R.id.ll_capture_video);
        chatFooter = (RelativeLayout) findViewById(R.id.relativeLayoutControlsHolder);
        btnSticker = (ImageButton) findViewById(R.id.btn_chat_menu_sticker);
        capturePhotoImageView = (ImageView) findViewById(R.id.camera_menu_capture_photo);
        capturevideoImageView = (ImageView) findViewById(R.id.camera_menu_capture_video);
        viewHandwriteMessage = (RelativeLayout) findViewById(R.id.ll_handwrite_message);
        viewShareWiteboard = (RelativeLayout) findViewById(R.id.ll_share_whiteboard);
        viewCollaborativeDocument = (RelativeLayout) findViewById(R.id.ll_collaborative_document);
        whiteboardImageView = (ImageView) findViewById(R.id.action_bar_menu_share_whiteboard);
        collaborativeDocumentImageView = (ImageView) findViewById(R.id.action_bar_menu_collaborative_document);
        handwriteMessageImageView = (ImageView) findViewById(R.id.action_bar_menu_handwrite_message);
        bottomSheetSelectColor = (LinearLayout) findViewById(R.id.bottom_sheet_select_color);
        sheetBehaviorSelectColor = BottomSheetBehavior.from(bottomSheetSelectColor);
        btnAVBroadcast = (ImageButton) findViewById(R.id.btn_chat_menu_broadcast);
        sheetBehaviorSelectColor.setPeekHeight(0);
        tvHandwriteMessage = (TextView) findViewById(R.id.textHandwrite);
        tvCollaborativeDoc = (TextView) findViewById(R.id.textCollaborative);
        tvWhiteBoard = (TextView) findViewById(R.id.textWhiteBoard);
        tvCapturePhoto = (TextView) findViewById(R.id.textCapturePhoto);
        tvCaptureVideo = (TextView) findViewById(R.id.textCaptureVideo);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        toolbarTitle = (TextView) findViewById(R.id.title);
        toolbarSubTitle = (TextView) findViewById(R.id.subTitle);
        toolbarSubTitle.setVisibility(View.GONE);
        voiceNotebtn.setBackgroundResource(R.drawable.ic_mic_6);
        tvHandwriteMessage.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_HANDWRITE)));
        tvCollaborativeDoc.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_WRITEBOARD)));
        tvWhiteBoard.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_WHITEBOARD)));
        tvCapturePhoto.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CAPTURE_PHOTO)));
        tvCaptureVideo.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CAPTURE_VIDEO)));
        messageField.setHint(Html.fromHtml("<small>"+(String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_TYPE_YOUR_MESSAGE))+"</small>"));
        txtLoadEarlierMessages = (TextView) findViewById(R.id.txt_load_earlier_messages);
        btnScroll = (Button) findViewById(R.id.btn_new_message);
    }

    private void setFieldListners() {
        sendButton.setOnClickListener(this);
        btnMenu.setOnClickListener(this);
        btnAVBroadcast.setOnClickListener(this);
        txtLoadEarlierMessages.setVisibility(View.VISIBLE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            messageRecyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if(messageCount-linearLayoutManager.findLastVisibleItemPosition() < 5){
                        btnScroll.setVisibility(View.GONE);
                    }else{
                        btnScroll.setVisibility(View.VISIBLE);
                    }

                    if(messageCount-2 == linearLayoutManager.findLastVisibleItemPosition()){
                        newMessageCount = 0;
                        btnScroll.setText("Jump to latest");
                        btnScroll.getBackground().setColorFilter(Color.parseColor("#8e8e92"), PorterDuff.Mode.SRC_ATOP);
                    }
                }
            });
        }


        btnScroll.setOnClickListener(this);

        if(grpFileTransferState != FeatureState.INVISIBLE){
            btnChatMenuSharePhoto.setOnClickListener(this);
            btnChatMenuShareVideo.setOnClickListener(this);
            btnCameraButton.setOnClickListener(this);
            viewCapturePhoto.setOnClickListener(this);
            viewCaptureVideo.setOnClickListener(this);
            voiceNotebtn.setOnClickListener(this);
            if(grpVoicenoteState != FeatureState.INVISIBLE){
                sendButton.setVisibility(View.INVISIBLE);
                voiceNotebtn.setVisibility(View.VISIBLE);
            }
        } else {
            sendButton.setVisibility(View.VISIBLE);
            btnChatMenuSharePhoto.setVisibility(View.GONE);
            btnChatMenuShareVideo.setVisibility(View.GONE);
            btnCameraButton.setVisibility(View.GONE);
            viewCapturePhoto.setVisibility(View.GONE);
            viewCaptureVideo.setVisibility(View.GONE);
            voiceNotebtn.setVisibility(View.INVISIBLE);
        }
        if(grpSmileyState != FeatureState.INVISIBLE){
            btnChatMenuSmiliey.setOnClickListener(this);
        } else {
            btnChatMenuSmiliey.setVisibility(View.GONE);
        }

        if(grpStickerState != FeatureState.INVISIBLE){
            btnSticker.setOnClickListener(this);
        } else {
            btnSticker.setVisibility(View.GONE);
        }

        btnChatMenuKeyBoard.setOnClickListener(this);
        btnChatMenuMore.setOnClickListener(this);
        dirtyView.setOnClickListener(this);



        if (grpHandwriteState != FeatureState.INVISIBLE) {
            viewHandwriteMessage.setOnClickListener(this);
        } else {
            viewHandwriteMessage.setVisibility(View.GONE);
        }

        if (grpWhiteBoardState != FeatureState.INVISIBLE) {
            viewShareWiteboard.setOnClickListener(this);
        } else {
            viewShareWiteboard.setVisibility(View.GONE);
        }

        if (grpWriteBoardState != FeatureState.INVISIBLE) {
            viewCollaborativeDocument.setOnClickListener(this);
        } else {
            viewCollaborativeDocument.setVisibility(View.GONE);
        }


        if (grpHandwriteState == FeatureState.INVISIBLE && grpWhiteBoardState == FeatureState.INVISIBLE && grpWriteBoardState == FeatureState.INVISIBLE) {
            btnChatMenuMore.setVisibility(View.GONE);
        }

        KeyboardVisibilityEvent.setEventListener(
                this,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        messageRecyclerView.scrollToPosition(messageCount - 1);
                    }
                });

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (grpFileTransferState != FeatureState.INVISIBLE && grpVoicenoteState != FeatureState.INVISIBLE) {
                    int len = messageField.getText().toString().length();
                    if (len > 0) {
//                    voiceNotebtn.setVisibility(View.INVISIBLE);
//                    sendButton.setVisibility(View.VISIBLE);
                        makeSendButtonVisible();
                    } else {
//                        voiceNotebtn.setVisibility(View.VISIBLE);
//                        sendButton.setVisibility(View.INVISIBLE);
                        makeVoiceNoteButtonVisible();
                    }
                } else {
                    sendButton.setVisibility(View.VISIBLE);
                    voiceNotebtn.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        if(grpAVBroadcastState != FeatureState.INVISIBLE) {
            btnAVBroadcast.setOnClickListener(this);
        } else{
            btnAVBroadcast.setVisibility(View.GONE);
        }

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadChatHistory();
            }
        });
    }

    private void makeVoiceNoteButtonVisible() {
        sendButton.setVisibility(View.INVISIBLE);
        voiceNotebtn.setVisibility(View.VISIBLE);
        ObjectAnimator scalevoiceNotebtnX = ObjectAnimator.ofFloat(voiceNotebtn, "scaleX", 0.0f, 1.0f);
        ObjectAnimator scalevoiceNotebtnY = ObjectAnimator.ofFloat(voiceNotebtn, "scaleY", 0.0f, 1.0f);
        scalevoiceNotebtnX.setDuration(300);
        scalevoiceNotebtnY.setDuration(300);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scalevoiceNotebtnX,scalevoiceNotebtnY);
        animatorSet.start();
    }

    private void makeSendButtonVisible() {
        voiceNotebtn.setVisibility(View.INVISIBLE);
        if(sendButton.getVisibility() == View.INVISIBLE){
            ObjectAnimator scaleSendButtonX = ObjectAnimator.ofFloat(sendButton, "scaleX", 0.0f, 1.0f);
            ObjectAnimator scaleSendButtonY = ObjectAnimator.ofFloat(sendButton, "scaleY", 0.0f, 1.0f);
            scaleSendButtonX.setDuration(300);
            scaleSendButtonY.setDuration(300);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleSendButtonX,scaleSendButtonY);
            animatorSet.start();
        }
        sendButton.setVisibility(View.VISIBLE);
    }

    private void setUpColorPicker() {

//        Config config = JsonPhp.getInstance().getConfig();

        /*if (null == config.getCrstyles()) {
            return;
        }*/
        //Logger.error("color list size  = "+ config.getCrstyles().getTextcolor().size());

        ArrayList textcolorList = (ArrayList) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS,SettingSubType.COLOR_CR_TEXT_COLOR));
        colorList = new ArrayList();

        if(textcolorList != null){
            for (Iterator iterator = textcolorList.iterator(); iterator.hasNext(); ) {

                String color = (String) iterator.next();

                if (selectedColor != null && selectedColor.equals("#" + color))
                    colorList.add(new ColorPojo("#" + color, true));
                else
                    colorList.add(new ColorPojo("#" + color, false));
            }
        }

        final GridView gridView = (GridView) findViewById(R.id.select_color_grid_view);
        gridAdapter = new SelectColorGridAdapter(CCGroupChatActivity.this, colorList);
        gridView.setAdapter(gridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logger.error("Positon = " + position);
                for (int i = 0; i < colorList.size(); i++) {
                    if (i == position) {
                        colorList.get(i).setSelected(true);
                        selectedColor = colorList.get(i).getColor();
                        PreferenceHelper.save(PreferenceKeys.Colors.COLOR_CHATROOM_CHAT, selectedColor);
                    } else
                        colorList.get(i).setSelected(false);
                }
                sheetBehaviorSelectColor.setState(BottomSheetBehavior.STATE_COLLAPSED);
                gridAdapter.notifyDataSetChanged();
                //viewChatMenu.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupSmileyKeyboars() {
        smiliKeyBoard = new SmileyKeyBoard();
        smiliKeyBoard.enable(this, this, R.id.footer_for_emoticons, messageField);
        smiliKeyBoard.checkKeyboardHeight(chatFooter);
        smiliKeyBoard.enableFooterView(messageField);
    }

    private void setupStickerKeyboard() {
        stickerKeyboard = new StickerKeyboard();
        stickerKeyboard.enable(this, new StickerGridviewImageAdapter.StickerClickInterface() {
            @Override
            public void getClickedSticker(int gridviewItemPosition) {
                String data = stickerKeyboard.getClickedSticker(gridviewItemPosition);
                sendStickerMessage(data);
            }
        }, R.id.footer_for_emoticons, messageField);
        stickerKeyboard.checkKeyboardHeight(chatFooter);
        stickerKeyboard.enableFooterView(messageField);
    }


    private void processIntentData(final Intent intent) {

        if (intent.hasExtra(StaticMembers.INTENT_CHATROOM_ID)) {
            //chatroomId = intent.getLongExtra(StaticMembers.INTENT_CHATROOM_ID, 0);
            String chatRoomId1 = intent.getStringExtra(StaticMembers.INTENT_CHATROOM_ID);
            Logger.error(TAG,"chatRoomId1 : "+chatRoomId1);
            chatroomId = Long.parseLong(chatRoomId1);
        } else {
            chatroomId = sessionData.getCurrentChatroom();
        }
        Logger.error(TAG, "processIntentData: groupId: "+chatroomId);
        PreferenceHelper.save(JsonParsingKeys.GRP_WINDOW_ID,chatroomId);
        NotificationDataHelper.deleteFromMap((int) chatroomId);

        if(intent.hasExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED)){
            isCloseWindowEnabled = intent.getBooleanExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED,false);
        }
        /*if (intent.hasExtra("ImageUri")) {
            shareImageUri = intent.getStringExtra("ImageUri");
            //checkUserConfirmation(shareImageUri, false);
        }
        if (intent.hasExtra("VideoUri")) {
            shareVideoUri = intent.getStringExtra("VideoUri");
            //checkUserConfirmation(shareVideoUri, true);
        }
        if (intent.hasExtra("AudioUri")) {
            shareAudioUri = intent.getStringExtra("AudioUri");
            Uri uri = Uri.parse(shareAudioUri);
            //String AudioUri = AudioSharing.getRealPathFromURI(this, uri);
            //AudioSharing.sendAudio(this, uri, chatroomId, true);
            PreferenceHelper.removeKey(PreferenceKeys.DataKeys.SHARE_AUDIO_URL);
        }*/

        if (intent.hasExtra(StaticMembers.INTENT_CHATROOM_NAME)) {
            chatroomName = intent.getStringExtra(StaticMembers.INTENT_CHATROOM_NAME);
            if (TextUtils.isEmpty(sessionData.getCurrentChatroomName())) {
                sessionData.setCurrentChatroomName(chatroomName);
            }
        } else {
            chatroomName = sessionData.getCurrentChatroomName();
        }
        if (intent.hasExtra(StaticMembers.INTENT_CHATROOM_ISMODERATOR)) {
            isModerator = intent.getBooleanExtra(StaticMembers.INTENT_CHATROOM_ISMODERATOR, false);
        }

        if (intent.hasExtra(StaticMembers.INTENT_CHATROOM_ISOWNER)) {
            isOwner = intent.getBooleanExtra(StaticMembers.INTENT_CHATROOM_ISOWNER, false);
        }


        if (intent.hasExtra("ImageUri")) {
            Logger.error(TAG, "ACTION_SEND intent.hasExtra(ImageUri)");
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_IMAGE_URL)) {
                String shareImageUri = intent.getStringExtra("ImageUri");
                Logger.error(TAG, "ACTION_SEND shareImageUri : " + shareImageUri);
                checkUserConfirmation(shareImageUri, false);
            }
        }

        if (intent.hasExtra("VideoUri")) {
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_VIDEO_URL)) {
                String shareVideoUri = intent.getStringExtra("VideoUri");
                checkUserConfirmation(shareVideoUri, true);
            }
        }

        String positiveResponse = ((String)cometChat.getCCSetting(new CCSettingMapper(
                SettingType.LANGUAGE,SettingSubType.LANG_YES)));

        String negativeResponse = ((String)cometChat.getCCSetting(new CCSettingMapper(
                SettingType.LANGUAGE,SettingSubType.LANG_NO)));

        String shareConfirm = ((String)cometChat.getCCSetting(new CCSettingMapper(
                SettingType.LANGUAGE,SettingSubType.LANG_FILE_SHARE_CONFIRM)));


        if (intent.hasExtra("AudioUri")) {
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_AUDIO_URL)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(shareConfirm);
                builder.setCancelable(true);
                builder.setPositiveButton(positiveResponse, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri shareAudioUri = Uri.parse(intent.getStringExtra("AudioUri"));
                        Logger.error(TAG, "ACTION_SEND shareAudioUri : " + shareAudioUri);
                        //String filepath = LocalStorageFactory.getFilePathFromIntent(intent);
                        String filepath = LocalStorageFactory.getFilePathFromIntent(getApplicationContext(), shareAudioUri);
                        Logger.error(TAG, "ACTION_SEND filepath : " + filepath);
                        if(filepath == null) {
                            filepath = shareAudioUri.toString().replace("file://", "").replace("%20", " ");
                        }

                        Logger.error(TAG, "ACTION_SEND audioPath : " + filepath);
                        Logger.error(TAG, "ACTION_SEND audioPath File(audioPath).exists() : " + new File(filepath).exists());
                        sendAudioMessage(filepath);
                    }
                });
                builder.setNegativeButton(negativeResponse, null);
                AlertDialog alert = builder.create();
                alert.show();
                PreferenceHelper.removeKey(PreferenceKeys.DataKeys.SHARE_AUDIO_URL);
            }
        }

        if(intent.hasExtra("GROUP_TYPE")){
            isPrivateGroup = 4 == intent.getIntExtra("GROUP_TYPE", 0);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CCPermissionHelper.PERMISSION_IMAGE_UPLOAD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    imageUpload();
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_VIDEO_UPLOAD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    videoUpload();
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_AUDIO_UPLOAD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    toggleRecording();
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_CAPTURE_MEDIA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openCaptureMediaBottomSheet();
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_VIDEO_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if (grpAvCallState != FeatureState.ACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        showCallPopup(false);
                    }
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_AUDIO_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if (grpAudioCallState != FeatureState.ACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        showCallPopup(true);
                    }
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_AV_BROADCAST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    startAvBroadcast();
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_GROUP_AV_CONFERENCE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if(PreferenceHelper.contains("CALL_ID")){
                        String callID = PreferenceHelper.get("CALL_ID");
                        Intent intent = new Intent(this, CCVideoChatActivity.class);
                        intent.putExtra(StaticMembers.INTENT_ROOM_NAME,callID);
                        intent.putExtra(StaticMembers.INTENT_CR_AUDIO_CONFERENCE_FLAG, false);
                        intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);
                        intent.putExtra("GRP_ID", String.valueOf(chatroomId));
                        intent.putExtra("CONTACT_ID", ""+SessionData.getInstance().getId());
                        intent.putExtra("VIDEO", true);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_GROUP_AUDIO_CONFERENCE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if(PreferenceHelper.contains("CALL_ID")){
                        String callID = PreferenceHelper.get("CALL_ID");
                        Intent intent = new Intent(this, CCVideoChatActivity.class);
                        intent.putExtra(StaticMembers.INTENT_ROOM_NAME,callID);
                        intent.putExtra(StaticMembers.INTENT_CR_AUDIO_CONFERENCE_FLAG, false);
                        intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, false);
                        intent.putExtra("GRP_ID", String.valueOf(chatroomId));
                        intent.putExtra("CONTACT_ID", ""+SessionData.getInstance().getId());
                        intent.putExtra("VIDEO", false);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;
            case CCPermissionHelper.PERMISSION_AVBROADCAST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if(PreferenceHelper.contains("CALL_ID")){
                        String callID = PreferenceHelper.get("CALL_ID");
                        Intent intent = new Intent(this, CCVideoChatActivity.class);
                        intent.putExtra(StaticMembers.INTENT_ROOM_NAME, callID);
                        intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG,true);
                        intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG,false);
                        intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);
                        intent.putExtra("GRP_ID",String.valueOf(chatroomId));
                        intent.putExtra("CONTACT_ID", ""+SessionData.getInstance().getId());
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "PERMISSION NOT GRANTED", Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if(requestCode == 1113){
                    this.requestCode = requestCode;
                    if (groupMessageAdapter != null) {
                        groupMessageAdapter.notifyDataSetChanged();
                    }
        }
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case StaticMembers.CHOOSE_IMAGE_REQUEST_CHATROOM:
                        checkUserConfirmation(data, false);
                        break;

                    case StaticMembers.CHOOSE_VIDEO_REQUEST_CHATROOM:
                        checkUserConfirmation(data, true);
                        break;

                    case StaticMembers.CAPTURE_PHOTO_REQUEST_CHATROOM:
                        //sendImageMessage(fileUri.getPath());
                        Logger.error(TAG,"fileUri: "+fileUri);
                        if(fileUri.toString().contains("file://")){
//                                sendImageMessage(fileUri.getPath());
                            Bitmap bitmapToSend = CommonUtils.getOrientationFromExifData(fileUri.getPath());
                            sendBitmap(bitmapToSend,fileUri.getPath());
                        }else{
                            /*File imagefile = new File(mediaFilePath);
                            if(imagefile.exists()){
                                sendImageMessage(mediaFilePath);
                            }*/
                            Bitmap bitmapToSend = CommonUtils.getOrientationFromExifData(mediaFilePath);
                            sendBitmap(bitmapToSend,mediaFilePath);
                        }
                        /*File imageFile = new File(mediaFilePath);
                        Logger.error(TAG," videoFile exists ? " + imageFile.exists());
                        if(imageFile.exists()){
                            sendImageMessage(mediaFilePath);
                        }*/

                        break;
                    case StaticMembers.CAPTURE_VIDEO_REQUEST_CHATROOM:
                        /*if (CommonUtils.isSamsungWithApi16()) {
                            sendVideoMessage(LocalStorageFactory.getFilePathFromIntent(data));
                        } else {
                            sendVideoMessage(fileUri.getPath());
                        }*/

                        File videoFile = new File(mediaFilePath);
                        Logger.error(TAG," videoFile exists ? " + videoFile.exists());
                        if(videoFile.exists()){
                            sendVideoMessage(mediaFilePath);
                        }

                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            Logger.error("SingleChatActivity.java onActivityResult() : Exception =" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void checkUserConfirmation(String selectedString, boolean isVideo) {
        if (!selectedString.isEmpty()) {
            try {
                data = Intent.getIntent(selectedString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (data != null) checkUserConfirmation(data, isVideo);
        }
    }

    private void checkUserConfirmation(Intent data, boolean isVideo) {

        if (data != null) {
            Uri selectedImageUri = data.getData();
            LayoutInflater inflater = getLayoutInflater();
            View dialogview = inflater.inflate(R.layout.cc_custom_image_preview, null);
            ImageView imagePreview = (ImageView) dialogview.findViewById(R.id.imageViewLargePreview);
            ImageView closePopup = (ImageView) dialogview.findViewById(R.id.imageViewClosePreviewPopup);
            closePopup.setVisibility(View.GONE);
            this.data = data;
            String sendDialogButton = "Send", cancelDialogButton = "Cancel";
            Logger.error(TAG,"ACTION_SEND Selected URI = " + selectedImageUri);

            if (isVideo) {
                Bitmap bitmap = null;
                if (CommonUtils.isSamsung()) {
                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(),
                            Long.parseLong(selectedImageUri.getLastPathSegment()), MediaStore.Video.Thumbnails.MINI_KIND,
                            (BitmapFactory.Options) null);
                } else if (CommonUtils.isXiaomi()) {
                    try {
                        bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(),
                                Long.parseLong(selectedImageUri.getLastPathSegment()), MediaStore.Video.Thumbnails.MINI_KIND,
                                (BitmapFactory.Options) null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        bitmap = null;
                    }
                    if (bitmap == null) {
                        bitmap = VideoSharing.getVideoBitmap(selectedImageUri, this);
                    }
                } else if (Build.VERSION.SDK_INT > 19) {
                    try {
                        String wholeID = DocumentsContract.getDocumentId(selectedImageUri);
                        String id = wholeID.split(":")[1];
                        bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(),
                                Long.parseLong(id), MediaStore.Video.Thumbnails.MINI_KIND,
                                (BitmapFactory.Options) null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        bitmap = null;
                    }
                    if (bitmap == null) {
                        bitmap = VideoSharing.getVideoBitmap(selectedImageUri, this);
                    }
                } else {
                    InputStream is = null;
                    try {
                        is = getContentResolver().openInputStream(selectedImageUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    bitmap = BitmapFactory.decodeStream(is);
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                imagePreview.setImageBitmap(bitmap);
                ImageView playButtonImage = (ImageView) dialogview
                        .findViewById(R.id.imageViewVideoPlayButtonForPreview);
                playButtonImage.setVisibility(View.VISIBLE);

                new CustomAlertDialogHelper(this, "", dialogview, sendDialogButton, "", cancelDialogButton, this,
                        VIDEO_SEND_PREVIEW_POPUP,false);
            } else {
                Bitmap bitmap = null;
                if (CommonUtils.isSamsung()) {
                    bitmap = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),
                            Long.parseLong(selectedImageUri.getLastPathSegment()), MediaStore.Images.Thumbnails.MINI_KIND,
                            (BitmapFactory.Options) null);
                    if (bitmap == null) {
                        bitmap = ImageSharing.getImageBitmap(selectedImageUri, this);
                    }
                } else if (CommonUtils.isXiaomi()) {
                    try {
                        bitmap = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),
                                Long.parseLong(selectedImageUri.getLastPathSegment()), MediaStore.Images.Thumbnails.MINI_KIND,
                                (BitmapFactory.Options) null);
                        if (bitmap == null) {
                            bitmap = ImageSharing.getImageBitmap(selectedImageUri, this);
                        }
                    } catch (Exception e) {
                        try {
                            InputStream is = getContentResolver().openInputStream(selectedImageUri);
                            bitmap = BitmapFactory.decodeStream(is);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }

                } else if (Build.VERSION.SDK_INT > 19) {
                    try {
                        String wholeID = DocumentsContract.getDocumentId(selectedImageUri);
                        String id = wholeID.split(":")[1];
                        bitmap = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),
                                Long.parseLong(id), MediaStore.Images.Thumbnails.MINI_KIND,
                                (BitmapFactory.Options) null);
                    } catch (Exception e) {
                        Logger.error(TAG,"ACTION_SEND e = " + e.toString());
                        e.printStackTrace();
                        InputStream is = null;
                        String myPath;
                        if (selectedImageUri.getAuthority() != null) {
                            try {
                                is = getContentResolver().openInputStream(selectedImageUri);
                                Bitmap bmp = BitmapFactory.decodeStream(is);
                                String path = ImageSharing.writeToTempImageAndGetPathUri(this, bmp).toString();
                                Uri pathUri = Uri.parse(path);
                                if (pathUri != null) {
                                    String[] filePathColumn = {MediaStore.MediaColumns.DATA};
                                    Cursor cursor = PreferenceHelper.getContext().getContentResolver()
                                            .query(pathUri, filePathColumn, null, null, null);
                                    cursor.moveToFirst();
                                    myPath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
                                    Logger.error(TAG,"ACTION_SEND myPath = " + myPath);
                                    File imgFile = new File(myPath);
                                    Logger.error(TAG,"ACTION_SEND imgFile.exists() = " + imgFile.exists());
                                    if (imgFile.exists()) {
                                        bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                    }
                                    cursor.close();
                                } else {
                                    myPath = null;
                                }
                            } catch (FileNotFoundException e2) {
                                Logger.error(TAG,"ACTION_SEND e2 = " + e2.toString());
                                Toast.makeText(getApplicationContext(), "Cant Send Empty File ", Toast.LENGTH_SHORT).show();
                                e2.printStackTrace();
                            } finally {
                                try {
                                    if (is != null)
                                        is.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                }
                            }
                        }
                    }

                } else {
                    InputStream is = null;
                    try {
                        is = getContentResolver().openInputStream(selectedImageUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    bitmap = BitmapFactory.decodeStream(is);
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (null == bitmap) {
                    Logger.error(TAG,"ACTION_SEND bitmap null");
                    Uri imageUri = data.getData();

                    Logger.error(TAG,"ACTION_SEND imageUri = " + imageUri);
                    if (null != imageUri) {
                        /* Check weather picasa image is selected from gallery */
                        //if (imageUri.toString().startsWith("content://com.sec.android.gallery3d")
                          //      || imageUri.toString().startsWith("content://com.android.gallery3d")) {
                            try {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                final InputStream ist = getContentResolver().openInputStream(imageUri);
                                picassaBitmap = BitmapFactory.decodeStream(ist, null, options);
                                ist.close();
                                if (null != picassaBitmap) {
                                  /*  picassImageName = imageUri.toString().substring(
                                            imageUri.toString().lastIndexOf("/") + 1);*/
                                    imagePreview.setImageBitmap(picassaBitmap);
                                    //new CustomAlertDialogHelper(this, "", dialogview, sendDialogButton, "",
                                      //      cancelDialogButton, this, PICASSA_IMAGE_PREVIEW_POPUP,false);
                                    new CustomAlertDialogHelper(this, "", dialogview, sendDialogButton, "",
                                            cancelDialogButton, this, IMAGE_SEND_PREVIEW_POPUP,false);
                                }
                            } catch (Exception e) {
                                Logger.error(TAG,"ACTION_SEND e3 = " + e.toString());
                                e.printStackTrace();
                            }
                        //}
                    }
                } else {
                    imagePreview.setImageBitmap(bitmap);
                    new CustomAlertDialogHelper(this, "", dialogview, sendDialogButton, "", cancelDialogButton, this,
                            IMAGE_SEND_PREVIEW_POPUP,false);
                }
            }
        }
    }

    @Override
    public void onButtonClick(final AlertDialog alertDialog, View view, int which, int popupId) {
        switch (popupId) {
            case IMAGE_SEND_PREVIEW_POPUP:
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            String filepath = FileSharing.getImageBitmap(CCGroupChatActivity.this, data, chatroomId, false);
                            File imagefile = null;
                            if (filepath != null) {
                                imagefile = new File(filepath);
                                Logger.error(TAG, "ACTION_SEND imagefile.exists() ? = " + imagefile.exists() + " filepath : " + filepath);
                                if (imagefile.exists()) {
                                    sendImageMessage(filepath);
                                }
                            } else {
                                filepath = data.getData().toString().replace("file://", "").replace("%20", " ");
                                imagefile = new File(filepath);
                                Logger.error(TAG, "ACTION_SEND imagefile.exists() 2 ? = " + imagefile.exists() + " filepath : " + filepath);
                                if (imagefile.exists()) {
                                    sendImageMessage(filepath);
                                }
                            }
                        }
                    }
                }
                alertDialog.dismiss();
                data = null;
                PreferenceHelper.removeKey(PreferenceKeys.DataKeys.SHARE_IMAGE_URL);
                break;
            case VIDEO_SEND_PREVIEW_POPUP:
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (data != null) {
                        String filepath = LocalStorageFactory.getFilePathFromIntent(data);
                        File videofile = null;
                        Logger.error(TAG, "ACTION_SEND Video file path value = " + filepath);
                        if (filepath != null) {
                            videofile = new File(filepath);
                            Logger.error(TAG, "ACTION_SEND Video file exist ? = " + videofile.exists());
                            if (videofile.exists()) {
                                sendVideoMessage(filepath);
                            }
                        } else {
                            filepath = data.getData().toString().replace("file://", "").replace("%20", " ");
                            videofile = new File(filepath);
                            Logger.error(TAG, "ACTION_SEND Video file exist 2 ? = " + videofile.exists() + " filepath : " + filepath);
                            if (videofile.exists()) {
                                sendVideoMessage(filepath);
                            }
                        }
                    }
                }
                alertDialog.dismiss();
                PreferenceHelper.removeKey(PreferenceKeys.DataKeys.SHARE_VIDEO_URL);
                data = null;
                break;
            case AUDIO_SEND_PREVIEW_POPUP:
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    sendAudioMessage(audioFileNamewithPath);
                    alertDialog.dismiss();
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    try {
                        if (player.isPlaying()) {
                            try {
                                player.stop();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        new File(audioFileNamewithPath).delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    alertDialog.dismiss();
                }
                break;

            case BLOCK_USER_POPUP:
                /*if (which == DialogInterface.BUTTON_POSITIVE) {
                    cometChat.blockUser(String.valueOf(contactId), new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            try {
                                if (contact == null) {
                                    contact = Contact.getContactDetails(contactId);
                                }
                                contact.showuser = 0;
                                contact.save();

                                Intent iintent = new Intent(BroadCastReceiverKeys.LIST_DATA_UPDATED_BROADCAST);
                                iintent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.REFRESH_CONTACT_LIST_KEY, 1);
                                PreferenceHelper.getContext().sendBroadcast(iintent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            alertDialog.dismiss();
                            finish();
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG,"Block user fail responce = "+jsonObject);
                        }
                    });

                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    alertDialog.dismiss();
                }*/


                break;

            case REPORT_CONVERSATION_POPUP:
               /* if (which == DialogInterface.BUTTON_POSITIVE) {
                    EditText dialogueInput = (EditText) view.findViewById(R.id.edittextDialogueInput);
                    String reasonForReporting = dialogueInput.getText().toString().trim();

                    if (reasonForReporting.length() > 0) {
                        cometChat.reportConversation(String.valueOf(contactId), reasonForReporting, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"Report conversation responce = "+jsonObject);
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"Report conversation fail responce = "+jsonObject);
                            }
                        });
                        alertDialog.dismiss();
                    } else {
                        *//*if (lang.getReport() != null && lang.getReport().get6() != null){
                            dialogueInput.setError(lang.getReport().get6());
                        }else{*//*
                        dialogueInput.setError("Reason must be filled out.");
                        // }
                    }
                }
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    alertDialog.dismiss();
                }*/
                break;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cc_chatroom_menu, menu);

        if (grpAudioCallState == FeatureState.INVISIBLE) {
            menu.findItem(R.id.custom_action_audio_call).setVisible(false);
        }

        if (grpAvCallState == FeatureState.INVISIBLE) {
            menu.findItem(R.id.custom_action_video_call).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int i = item.getItemId();
        if (i == R.id.custom_action_more) {
            final View menuItemView = findViewById(R.id.custom_action_more);
            // sheetBehaviorSelectColor.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showCustomActionBarPopup(menuItemView);

        } else if (i == android.R.id.home) {
            if(isCloseWindowEnabled){
                MessageSDK.closeCometChatWindow(CCGroupChatActivity.this, ccContainer);
                cometChat.sendCloseCCWindowResponce();
            }else{
                finish();
            }
        } else if(i == R.id.custom_action_audio_call){
            if (CommonUtils.isConnected()) {
                if (Build.VERSION.SDK_INT >= 16) {
                    String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                            CCPermissionHelper.REQUEST_PERMISSION_CAMERA,CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO};
                    if(CCPermissionHelper.hasPermissions(this, PERMISSIONS)){
                        if (grpAudioCallState != FeatureState.ACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            showCallPopup(true);
                        }
                    }else{
                        CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_AUDIO_CALL);
                    }
                } else {
                    Toast.makeText(this, "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
            }
        }else if (i == R.id.custom_action_video_call) {
            if (CommonUtils.isConnected()) {
                if (Build.VERSION.SDK_INT >= 16) {
                    String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                            CCPermissionHelper.REQUEST_PERMISSION_CAMERA, CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO};
                    if(CCPermissionHelper.hasPermissions(this, PERMISSIONS)){
                        if (grpAvCallState != FeatureState.ACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            showCallPopup(false);
                        }
                    }else{
                        CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_VIDEO_CALL);
                    }
                } else {
                    Toast.makeText(this, "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
            }

        }
        return super.onOptionsItemSelected(item);
    }


    private void showCallPopup(final boolean isAudioOnlyCall) {
        try {
            String yes = StaticMembers.POSITIVE_TITLE, no = StaticMembers.NEGATIVE_TITLE;
            ConfirmationWindow cWindow = new ConfirmationWindow(this, yes, no) {

                @Override
                protected void setNegativeResponse() {
                    super.setNegativeResponse();
                }

                @Override
                protected void setPositiveResponse() {
                    super.setPositiveResponse();
                    initiateCall(isAudioOnlyCall);
                }
            };
            if (isAudioOnlyCall) {
                cWindow.setMessage(/*(lang.getAudiochat() == null) ? "Call" : lang.getAudiochat().get28() +*/ "Call "
                        + chatroomName + "?");
            } else {
                cWindow.setMessage(/*(lang.getAvchat() == null) ? "Call" : lang.getAvchat().get28()+*/ "Call " + chatroomName
                        + "?");
            }

            cWindow.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("HandlerLeak")
    private void initiateCall(final boolean isAudioOnlyCall) {

            Logger.error(TAG,"isAudioOnly Call ? "+chatroomId);
            cometChat.sendConferenceRequest(String.valueOf(chatroomId),isAudioOnlyCall,new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error("sendConferenceRequest  success =  "+jsonObject);
                    try {
                        Intent intent = new Intent(CCGroupChatActivity.this, CCVideoChatActivity.class);
                        intent.putExtra(StaticMembers.INTENT_ROOM_NAME, jsonObject.getString("roomName"));
                        intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);

                        intent.putExtra("GRP_ID",String.valueOf(chatroomId));
                        intent.putExtra("CONTACT_ID", ""+SessionData.getInstance().getId());
                        intent.putExtra("VIDEO", !isAudioOnlyCall);
                        PreferenceHelper.save("IS_INITIATOR","1");
                        startActivity(intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error("sendConferenceRequest  fail =  "+jsonObject);
                }
            });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(isCloseWindowEnabled){
            MessageSDK.closeCometChatWindow(CCGroupChatActivity.this, ccContainer);
            cometChat.sendCloseCCWindowResponce();
        }else{
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btn_new_message){
            linearLayoutManager.smoothScrollToPosition(messageRecyclerView,null,messageCount-1);
        }else if (id == R.id.buttonSendMessage) {
            String message = messageField.getText().toString().trim();

            if (!TextUtils.isEmpty(message)) {
                sendTextMessage(message);
            } else {
                Toast.makeText(CCGroupChatActivity.this, "Message Cannot be Empty", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.img_btn_chat_more) {
            if (customMenu.getVisibility() == View.GONE) {
                customMenu.setVisibility(View.VISIBLE);
                btnMenu.setRotation(btnMenu.getRotation() + 180);
//                animateCustomMenu();
            } else {

                customMenu.setVisibility(View.GONE);
                btnMenu.setRotation(btnMenu.getRotation() + 180);
            }
        } else if (id == R.id.btn_chat_menu_keyboard) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(ccContainer.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
               /* inputMethodManager.toggleSoftInputFromWindow(chatLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);

            if (Stickers.isEnabled() && stickerKeyboard.isKeyboardVisibile()) {
                stickerKeyboard.showKeyboard(chatFooter);

                if (!inputMethodManager.isActive()) {
                    inputMethodManager.toggleSoftInputFromWindow(ccContainer.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                }

            } else {
                inputMethodManager.toggleSoftInputFromWindow(ccContainer.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            }*/
        } else if (id == R.id.btn_chat_menu_share_image) {
            Logger.error(TAG, "Share image Clicked");
            if(grpFileTransferState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                shareImage();
            }
        } else if (id == R.id.btn_chat_menu_share_video) {
            if(grpFileTransferState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                shareVideo();
            }
        } else if (id == R.id.buttonSendVoice) {
            if(grpFileTransferState == FeatureState.INACCESSIBLE || grpVoicenoteState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                shareVoiceNote();
            }
        } else if (id == R.id.img_btn_camera) {
            if (grpFileTransferState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                showCameraOptions();
            }
        } else if (id == R.id.btn_chat_menu_more) {
            SmileyKeyBoard.dismissKeyboard();
            if(stickerKeyboard.isKeyboardVisibile()){
                stickerKeyboard.dismissKeyboard();
            }
            dirtyView.setVisibility(View.VISIBLE);
            chatMenuLayout.setVisibility(View.VISIBLE);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            AnimateHandwriteBottomSheetViews();
        } else if (id == R.id.dirty_view) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            sheetCameraBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            dirtyView.setVisibility(View.GONE);
        } else if (id == R.id.ll_capture_photo) {
            sheetCameraBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            capturePhoto();
        } else if (id == R.id.ll_capture_video) {
            sheetCameraBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            videoCapture();
        } else if (id == R.id.img_btn_smiley) {
           if(grpSmileyState == FeatureState.INACCESSIBLE){
               AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
               alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                   }
               }).show();
           }else {
               smiliKeyBoard.showKeyboard(chatFooter);
           }
        } else if (id == R.id.btn_chat_menu_sticker) {
            if (grpStickerState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                stickerKeyboard.showKeyboard(chatFooter);
            }
        } else if (id == R.id.ll_handwrite_message) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (grpHandwriteState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                startHandwrite();
            }
        } else if (id == R.id.ll_share_whiteboard) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (grpWhiteBoardState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                shareWhiteBoard();
            }
        } else if (id == R.id.ll_collaborative_document) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (grpWriteBoardState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                shareWriteBoard();
            }
        }else if (id == R.id.btn_chat_menu_broadcast){
            if (grpAVBroadcastState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                avBroadcast();
            }
        }
    }

    private void shareWriteBoard() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        startWriteBoard();
    }

    private void shareWhiteBoard() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        startWhiteBoard();
    }

    private void shareVoiceNote() {
        String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE, CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO};
        if (CCPermissionHelper.hasPermissions(this, PERMISSIONS)) {
            toggleRecording();
        } else {
            CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_AUDIO_UPLOAD);
        }
    }

    private void shareImage() {
        String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
        if (CCPermissionHelper.hasPermissions(this, PERMISSIONS)) {
            imageUpload();
        } else {
            CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_IMAGE_UPLOAD);
        }
    }

    private void shareVideo() {
        String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
        if (CCPermissionHelper.hasPermissions(this, PERMISSIONS)) {
            videoUpload();
        } else {
            CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_VIDEO_UPLOAD);
        }
    }

    private void avBroadcast() {
        String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
        if(CCPermissionHelper.hasPermissions(this, PERMISSIONS)){
            startAvBroadcast();
        }else{
            CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_AV_BROADCAST);
        }
    }

    private void showCameraOptions() {
        String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
        if (CCPermissionHelper.hasPermissions(this, PERMISSIONS)) {
            openCaptureMediaBottomSheet();
        } else {
            CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_CAPTURE_MEDIA);
        }
    }

    private void addMessage(GroupMessage message) {
        message.save();
        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, this);
        Logger.error(TAG, "Chatroomid = " + chatroomId);
        CCMessageHelper.addGroupConversation(Groups.getGroupDetails(chatroomId), message);
    }

    private void imageUpload() {
       /* Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(StaticMembers.IMAGE_TYPE);
        startActivityForResult(intent, StaticMembers.CHOOSE_IMAGE_REQUEST_CHATROOM);*/
        Intent imagePickerIntent;
        if (CommonUtils.isSamsung()) {
            imagePickerIntent = new Intent(Intent.ACTION_PICK);
        } else {
            imagePickerIntent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        imagePickerIntent.setType(StaticMembers.IMAGE_TYPE);
        startActivityForResult(imagePickerIntent, StaticMembers.CHOOSE_IMAGE_REQUEST_CHATROOM);
    }

    private void videoUpload() {
       /* Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType(StaticMembers.VIDEO_TYPE);
        startActivityForResult(intent, StaticMembers.CHOOSE_VIDEO_REQUEST_CHATROOM);*/

        Intent videoPickerIntent;
        videoPickerIntent = new Intent(Intent.ACTION_PICK);
        videoPickerIntent.setType(StaticMembers.VIDEO_TYPE);
        startActivityForResult(videoPickerIntent, StaticMembers.CHOOSE_VIDEO_REQUEST_CHATROOM);
    }

    private void openCaptureMediaBottomSheet() {
        dirtyView.setVisibility(View.VISIBLE);
        hideSoftKeyboard(CCGroupChatActivity.this);
        sheetCameraBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        AnimateBottomSheetViews();
    }


    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = ImageSharing.getOutputMediaFileUri(CCGroupChatActivity.this,StaticMembers.MEDIA_TYPE_IMAGE, false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        mediaFilePath = ImageSharing.getOutputMediaFilePath();
        Logger.error(TAG,"mediaFilePath : " + mediaFilePath);

        startActivityForResult(intent, StaticMembers.CAPTURE_PHOTO_REQUEST_CHATROOM);
    }

    private void videoCapture() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (!CommonUtils.isSamsungWithApi16()) {
            fileUri = ImageSharing.getOutputMediaFileUri(CCGroupChatActivity.this,StaticMembers.MEDIA_TYPE_VIDEO, false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        }
        Logger.error(TAG,"fileUri : " + fileUri);

        mediaFilePath = ImageSharing.getOutputMediaFilePath();
        Logger.error(TAG,"mediaFilePath : " + mediaFilePath);

        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 15000000L);
        startActivityForResult(intent, StaticMembers.CAPTURE_VIDEO_REQUEST_CHATROOM);
    }

    private void startHandwrite() {
        Intent i = new Intent(getApplicationContext(), CCHandwriteActivity.class);
        i.putExtra(CometChatKeys.AjaxKeys.SENDERNAME, chatroomName);
        i.putExtra(CometChatKeys.AjaxKeys.BASE_DATA, PreferenceHelper.get(PreferenceKeys.DataKeys.BASE_DATA));
        i.putExtra("ischatroom", "1");
        i.putExtra(CometChatKeys.AjaxKeys.ID, chatroomId);
        startActivity(i);
    }

    private void startWriteBoard() {
        cometChat.sendWriteBoardRequest(String.valueOf(chatroomId),true, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "SendWriteBoard success responce = " + jsonObject);
                try {
                    Logger.error(TAG, "SendWhiteBoard success responce = " + jsonObject);
                    Intent i = new Intent(getApplicationContext(), CCWebViewActivity.class);
                    i.putExtra(IntentExtraKeys.WEBSITE_URL, jsonObject.getString("writeboard_url"));
                    i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME, "Collaborative Document");
                    startActivity(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "SendWriteBoard failed responce = " + jsonObject);
            }
        });
    }

    private void startWhiteBoard() {
        cometChat.sendWhiteBoardRequest(String.valueOf(chatroomId), true, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {

                try {
                    Logger.error(TAG,"SendWhiteBoard success responce = "+jsonObject.toString());
                    Intent i = new Intent(getApplicationContext(), CCWebViewActivity.class);
                    i.putExtra(IntentExtraKeys.WEBSITE_URL,jsonObject.getString("whiteboard_url"));
                    i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME,"White Board");
                    startActivity(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"SendWhiteBoard fail responce = "+jsonObject);
            }
        });
    }

    private void startAvBroadcast(){
        Logger.error(TAG,"Start AV broadcast called");

        cometChat.sendGrpAVBroadCastRequest(String.valueOf(chatroomId), new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                try {
                    Logger.error(TAG,"Start GRP AV broadcast success = "+jsonObject);
                    Logger.error(TAG,"Room name = "+ jsonObject.getString("callid"));
                    Intent intent = new Intent(CCGroupChatActivity.this, CCVideoChatActivity.class);
                    intent.putExtra(StaticMembers.INTENT_ROOM_NAME, jsonObject.getString("callid"));
                    intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG,true);
                    intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG,true);
                    intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);
                    intent.putExtra("GRP_ID",String.valueOf(chatroomId));
                    intent.putExtra("CONTACT_ID", ""+SessionData.getInstance().getId());
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"Start AV broadcast fail = "+jsonObject);
            }
        });
    }

    private void toggleRecording() {
        try {
            if (isRecording) {
                isRecording = false;
                voiceNotebtn.setBackgroundResource(R.drawable.ic_mic_6);
                messageField.setEnabled(true);
                messageField.setAlpha(1F);
                btnChatMenuSmiliey.setEnabled(true);
                btnChatMenuSmiliey.setAlpha(1F);

                if (voiceRecorder != null) {
                    voiceRecorder.stop();
                    voiceRecorder.release();
                    voiceRecorder = null;
                    String sendDialogButton = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SEND));
                    String cancelDialogButton = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL));
                    final View dialogview = getLayoutInflater().inflate(R.layout.cc_custom_voice_note_preview, null);
                    final ImageView playBtn = (ImageView) dialogview.findViewById(R.id.imageViewPlayIconPreview);
                    final SeekBar seekbar = (SeekBar) dialogview.findViewById(R.id.seek_barPreview);
                    seekbar.getProgressDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                    seekbar.getThumb().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                    final TextView textView1 = (TextView) dialogview.findViewById(R.id.textViewTimePreview);
                    player = CommonUtils.getPlayerInstance();
                    playBtn.getBackground().setColorFilter(new LightingColorFilter(Color.WHITE, Color.parseColor("#000000")));

                    player.reset();
                    try {
                        player.setDataSource(audioFileNamewithPath);
                        player.prepare();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    final int audioTimetest = player.getDuration();
                    textView1.setText(CommonUtils.convertTimeStampToDurationTime(audioTimetest));
                    seekbar.setMax(audioTimetest);
                    seekbar.setProgress(player.getCurrentPosition());


                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isVoiceNoteplaying) {
                                isVoiceNoteplaying = false;
                                playBtn.setBackgroundResource(R.drawable.ic_play_arrow);
                                if (player.isPlaying()) {
                                    try {
                                        player.stop();
                                        seekbar.setProgress(0);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                isVoiceNoteplaying = true;
                                playBtn.getBackground().setColorFilter(new LightingColorFilter(Color.WHITE, Color.parseColor("#000000")));
                                playBtn.setBackgroundResource(R.drawable.ic_pause);
                                try {
                                    player.reset();
                                    player.setDataSource(audioFileNamewithPath);
                                    player.prepare();
                                    player.start();


                                    timerRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (player != null) {
                                                seekbar.setProgress(player
                                                        .getCurrentPosition());
                                                if (player.isPlaying()
                                                        && player.getCurrentPosition() < audioTimetest) {

                                                    textView1.setText(CommonUtils.convertTimeStampToDurationTime(player.getCurrentPosition()));
                                                    seekHandler.postDelayed(this, 500);
                                                } else {
                                                    seekHandler
                                                            .removeCallbacks(timerRunnable);
                                                    //seekHandler.removeCallbacks(this);
                                                }
                                            }
                                        }
                                    };

                                    seekHandler.postDelayed(timerRunnable, 100);
                                    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mp) {
                                            playBtn.setBackgroundResource(R.drawable.ic_play_arrow);
                                            seekHandler
                                                    .removeCallbacks(timerRunnable);
                                            //seekHandler.removeCallbacks(this);
                                            mp.stop();
                                            isVoiceNoteplaying = false;
                                            seekbar.setProgress(0);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    new CustomAlertDialogHelper(this, "", dialogview, sendDialogButton, "", cancelDialogButton, this,
                            AUDIO_SEND_PREVIEW_POPUP,false);
                }
            } else {
                messageField.setEnabled(false);
                messageField.setAlpha(0.6F);
                btnChatMenuSmiliey.setEnabled(false);
                btnChatMenuSmiliey.setAlpha(0.5F);

                isRecording = true;
                voiceNotebtn.setBackgroundResource(R.drawable.ic_mic);
                AnimationDrawable animationDrawable = (AnimationDrawable) voiceNotebtn.getBackground();
                voiceNotebtn.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                animationDrawable.start();
//                voiceNotebtn.getDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                voiceRecorder = new MediaRecorder();
                voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                audioFileNamewithPath = AudioSharing.getOutputMediaFile();
                voiceRecorder.setOutputFile(audioFileNamewithPath);


                voiceRecorder.prepare();
                voiceRecorder.start();
                Toast.makeText(getApplicationContext(), (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_RECORDING)), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void viewChatroomUsers() {
        Intent intent = new Intent(this, CCShowChatroomUsersActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("GroupID", chatroomId);
        Logger.error(TAG,"isModerator: "+isModerator);
        intent.putExtra("ismoderator", isModerator);
        intent.putExtra("isOwner", isOwner);
        if(isOwner){
         intent.putExtra("user_id",SessionData.getInstance().getId());
        }else{
            intent.putExtra("user_id",0);
        }
        startActivity(intent);
    }

    private void viewBannedUsers() {
        Intent intent = new Intent(this, UnbanChatroomUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("GroupID", chatroomId);
        startActivity(intent);
    }


    private void showCustomActionBarPopup(View view) {
        hideSoftKeyboard(CCGroupChatActivity.this);
        final PopupWindow showPopup = PopupHelper.newBasicPopupWindow(getApplicationContext());
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.cc_custom_chat_room_action_bar_menu, null);
        showPopup.setContentView(popupView);
        LinearLayout llCustomPopup = (LinearLayout) popupView.findViewById(R.id.chatRoomPopup);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            llCustomPopup.setPadding(0,0,CommonUtils.dpToPx(30),0);
        }
        RelativeLayout viewMember = (RelativeLayout) popupView.findViewById(R.id.ll_view_member);
        RelativeLayout clearConversation = (RelativeLayout) popupView.findViewById(R.id.ll_chatroom_clear_conversation);
        RelativeLayout inviteUser = (RelativeLayout) popupView.findViewById(R.id.ll_invite_user);
        RelativeLayout leaveChatroom = (RelativeLayout) popupView.findViewById(R.id.ll_leave_chatroom);
        final RelativeLayout pickColor = (RelativeLayout) popupView.findViewById(R.id.ll_pick_color);
        RelativeLayout llDeleteChatroom = (RelativeLayout) popupView.findViewById(R.id.ll_delete_chatroom);
        RelativeLayout llRenameChatroom = (RelativeLayout) popupView.findViewById(R.id.ll_rename_chatroom);
        RelativeLayout llUnbanUser = (RelativeLayout) popupView.findViewById(R.id.ll_unban_user);


        ImageView viewProfileImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_view_profile);
        ImageView clearConversationImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_clear_conversation);
        ImageView inviteUsersImageView = (ImageView) popupView.findViewById(R.id.image_view_menu_invite_users);
        ImageView blockUserImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_leave_chatroom);
        ImageView pickcolorImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_pick_color);
        ImageView deleteChatroomImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_delete_chatroom);
        ImageView renameChatroomImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_rename_chatroom);
        ImageView unbanChatroomImageView = (ImageView) popupView.findViewById(R.id.image_view_menu_unban_users);

        viewProfileImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        clearConversationImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        inviteUsersImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        blockUserImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        pickcolorImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        deleteChatroomImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        renameChatroomImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        unbanChatroomImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);


        TextView tvViewMenber = (TextView) popupView.findViewById(R.id.textViewMember);
        TextView tvClearConversation = (TextView) popupView.findViewById(R.id.textClearConversation);
        TextView tvInviteUser = (TextView) popupView.findViewById(R.id.textInviteUsers);
        TextView tvLeaveChatroom = (TextView) popupView.findViewById(R.id.textLeaveChatroom);
        TextView tvColorYourText= (TextView) popupView.findViewById(R.id.colorYourText);
        TextView tvDeleteChatroom= (TextView) popupView.findViewById(R.id.textDeleteChatroom);
        TextView tvRenameChatroom= (TextView) popupView.findViewById(R.id.textRenameChatroom);
        TextView tvUnbanChatroom= (TextView) popupView.findViewById(R.id.textUnbanUsers);

        tvViewMenber.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_VIEW_MEMBER)));
        tvClearConversation.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CLEAR_CONVERSATION)));
        tvInviteUser.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_INVITE_USERS)));
        tvLeaveChatroom.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LEAVE_ROOM)));
        tvColorYourText.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_COLOR_YOUR_TEXT)));
        tvDeleteChatroom.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_DELETE_ROOM)));
        tvRenameChatroom.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_RENAME_ROOM)));
        tvUnbanChatroom.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_UNBAN_CHATROOM_TITLE)));

        if(isModerator || isOwner) {
            llUnbanUser.setVisibility(View.VISIBLE);
        } else{
            llUnbanUser.setVisibility(View.GONE);
        }

        viewMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup.dismiss();


                viewChatroomUsers();
            }
        });

        if(grpClearConversationState != FeatureState.INVISIBLE){
            clearConversation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup.dismiss();
                    if (grpClearConversationState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        clearGroupConversation();
                    }
                }
            });
        } else {
            clearConversation.setVisibility(View.GONE);
        }
        Logger.error(TAG, "showCustomActionBarPopup() : ChatroomId : " + chatroomId);
        Logger.error(TAG, "createdBy : "+Groups.getGroupDetails(chatroomId).createdBy);
        Logger.error(TAG,"owner: "+Groups.getGroupDetails(chatroomId).owner);
        Logger.error(TAG,"isOwner : "+isOwner +" ---"+isModerator);
        Logger.error(TAG, "VERSION_CODE : " + PreferenceHelper.get(PreferenceKeys.LoginKeys.VERSION_CODE));


        if (!TextUtils.isEmpty(PreferenceHelper.get(PreferenceKeys.LoginKeys.VERSION_CODE))){
            if(cometChat.getCometChatServerVersion() <= 6911){
                if(Groups.getGroupDetails(chatroomId).owner == 1 || (Groups.getGroupDetails(chatroomId).createdBy == 1)
                        || (Groups.getGroupDetails(chatroomId).createdBy == SessionData.getInstance().getId())){
                    llDeleteChatroom.setVisibility(View.VISIBLE);
                    llRenameChatroom.setVisibility(View.VISIBLE);
                } else {
                    llDeleteChatroom.setVisibility(View.GONE);
                    llRenameChatroom.setVisibility(View.GONE);
                }
            }else {
                if(Groups.getGroupDetails(chatroomId).owner == 1 || isOwner){
                    llDeleteChatroom.setVisibility(View.VISIBLE);
                    llRenameChatroom.setVisibility(View.VISIBLE);
                } else {
                    llDeleteChatroom.setVisibility(View.GONE);
                    llRenameChatroom.setVisibility(View.GONE);
                }
            }
        }


        inviteUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup.dismiss();
                inviteUsers();
            }
        });

        leaveChatroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup.dismiss();
                cometChat.leaveGroup(chatroomId, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "Leave chatroom success responce = " + jsonObject);

                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "Leave chatroom fail responce = " + jsonObject);
                    }
                });
                Groups grp = Groups.getGroupDetails(chatroomId);
                if(grp!=null){
                    grp.status = 0;
                    grp.save();
                }
                finish();

            }
        });

        if(colorYourTextState != FeatureState.INVISIBLE){
            pickColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopup.dismiss();
                    if (colorYourTextState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCGroupChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        pickColor();
                    }
                }
            });
        } else {
            pickColor.setVisibility(View.GONE);
        }

        showPopup.setWidth(Toolbar.LayoutParams.WRAP_CONTENT);
        showPopup.setHeight(Toolbar.LayoutParams.WRAP_CONTENT);
        showPopup.setAnimationStyle(R.style.Animations_GrowFromTop);
        showPopup.showAsDropDown(view);

        llDeleteChatroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatroomManager.deleteChatroom(CCGroupChatActivity.this, chatroomId, new CometchatCallbacks() {
                    @Override
                    public void successCallback() {
                        Logger.error(TAG, "deleteChatroom successCallback chatroomId : "+chatroomId);
                        Groups.deleteGroup(chatroomId);
                        showPopup.dismiss();

                        /*Intent returnIntent = new Intent();
                        returnIntent.putExtra("delete_group", "123");
                        setResult(Activity.RESULT_OK, returnIntent);*/

                        CCGroupChatActivity.this.finish();
                    }

                    @Override
                    public void failCallback() {
                        Logger.error(TAG, "deleteChatroom failCallback");
                    }
                });
            }
        });

        llRenameChatroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatroomManager.renameGroup(CCGroupChatActivity.this, chatroomId, chatroomName, new Callbacks() {
                    @Override
                    public void successCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "renameGroup successCallback : "+jsonObject.toString());
                        try {
                            final String newGroupName = jsonObject.getString("group_name");
                            Groups.renameGroup(chatroomId, newGroupName);
                            chatroomName = newGroupName;
                            showPopup.dismiss();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    toolbarTitle.setText(newGroupName);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failCallback(JSONObject jsonObject) {
                        Logger.error(TAG, "renameGroup failCallback : "+jsonObject.toString());
                    }
                });
            }
        });

        llUnbanUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup.dismiss();
                viewBannedUsers();
            }
        });
    }

    private void pickColor() {
        dirtyView.setVisibility(View.VISIBLE);
        sheetBehaviorSelectColor.setState(BottomSheetBehavior.STATE_EXPANDED);
        if (selectedColor != null) {
            gridAdapter.notifyDataSetChanged();
        }
    }

    private void clearGroupConversation() {
        GroupMessage.clearConversation(chatroomId);
        firstMessageID = 0l;
        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
    }


    private void sendTextMessage(String message) {
        Logger.error(TAG, "Selected Text Color = " + selectedColor);

        /*final GroupMessage messagePojo = new GroupMessage(0, sessionData.getId(), chatroomId, message,
                System.currentTimeMillis(), "", MessageTypeKeys.NORMAL_MESSAGE, "",
                selectedColor, 1);*/
        final GroupMessage messagePojo = new GroupMessage(0,
                sessionData.getId(), chatroomId, message, System.currentTimeMillis(), "",
                MessageTypeKeys.NORMAL_MESSAGE, "", selectedColor, 1, 0);
        addMessage(messagePojo);
        messageField.setText("");
        sendTextMessage(messagePojo);
    }

    private void sendTextMessage(final GroupMessage message) {
        cometChat.sendMessage(message.getId(), String.valueOf(chatroomId), message.message, selectedColor,true, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {

                    Logger.error(TAG, "successCallback: sendTextMessage: "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                    String messageAfterSuccess = sendResponse.getString(CometChatKeys.AjaxKeys.MESSAGE);
                    if (messageAfterSuccess.contains("<img class=\"cometchat_smiley\"")) {
                        messageAfterSuccess = Smilies.convertImageTagToEmoji(messageAfterSuccess);
                        Logger.error(TAG, "emoji message: " + messageAfterSuccess);
                    }
//                    GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                    Logger.error(TAG, "send Response mess : " + message);
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        message.retryCount = 3;
                        //mess.message = String.valueOf(Html.fromHtml(messageAfterSuccess));
                        message.save();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "Send Message fail responce = " + jsonObject);
                try {
                    if(jsonObject.getString("code").equals("900")){
                        GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(jsonObject.getString("localmessageid")));
                        if (mess != null) {
                            mess.messageStatus = 2;
                            mess.save();
                            getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            Toast.makeText(CCGroupChatActivity.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendImageMessage(String imagePath) {
        /*final GroupMessage message = new GroupMessage(0, sessionData.getId(), chatroomId, imagePath,
                System.currentTimeMillis(), "", MessageTypeKeys.IMAGE_MESSAGE, "",
                "#000", 1);*/

        final GroupMessage message = new GroupMessage(0L,
                sessionData.getId(), chatroomId, imagePath, System.currentTimeMillis(), "",
                MessageTypeKeys.IMAGE_MESSAGE, "", "#000", 1, 0);
        addMessage(message);

        sendImage(message);
    }

    private void sendImage(final GroupMessage message) {
        cometChat.sendImage(message.getId(), new File(message.message), chatroomId+"",true, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                Logger.error("send Response Image cometGroup : "+sendResponse);
                try {
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
//                    GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        message.retryCount = 3;
                        message.save();
                        Logger.error(TAG, "successCallback: sendImage message: "+message );
                        runOnUiThread(new Runnable() {
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void failCallback(JSONObject responce) {
                Logger.error(TAG, "Send Image fail responce = " + responce);
            }
        });
    }

    private void inviteUsers() {
        Intent intent = new Intent(CCGroupChatActivity.this, CCInviteUserActivity.class);
        intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, chatroomId);
        intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, chatroomName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }


    private void sendAudioMessage(String audioPath) {
        File audioFile = new File(audioPath);
        if (audioFile.exists()) {
            /*final GroupMessage message = new GroupMessage(0, sessionData.getId(), chatroomId, audioPath,
                    System.currentTimeMillis(), "", MessageTypeKeys.AUDIO_MESSAGE, "",
                    "#000", 1);*/
            final GroupMessage message = new GroupMessage(0L,
                    sessionData.getId(), chatroomId, audioPath, System.currentTimeMillis(), "",
                    MessageTypeKeys.AUDIO_MESSAGE, "", "#000", 1, 0);
            addMessage(message);

            sendAudioNote(message);
        }
    }

    private void sendAudioNote(final GroupMessage message) {
        cometChat.sendAudioFile(message.getId(), new File(message.message), chatroomId+"",true, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                Logger.error("send Response Audio cometGroup : "+sendResponse);
                try {
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
//                    GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        message.retryCount = 3;
                        message.save();
                        CCGroupChatActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "send Audio file fail responce = " + jsonObject);
            }
        });
    }


    private void sendVideoMessage(String videoPath) {
        /*final GroupMessage message = new GroupMessage(0, sessionData.getId(), chatroomId, videoPath,
                System.currentTimeMillis(), "", MessageTypeKeys.VIDEO_MESSAGE, "",
                "#000", 1);*/
        final GroupMessage message = new GroupMessage(0L,
                sessionData.getId(), chatroomId, videoPath, System.currentTimeMillis(), "",
                MessageTypeKeys.VIDEO_MESSAGE, "", "#000", 1, 0);
        addMessage(message);

        sendVideo(message);
    }

    private void sendVideo(final GroupMessage message) {
        cometChat.sendVideo(message.getId(), new File(message.message), chatroomId+"",true, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                Logger.error("send Response Video cometGroup : "+sendResponse);
                try {
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
//                    GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        message.retryCount = 3;
                        message.save();
                        runOnUiThread( new Runnable() {
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void failCallback(JSONObject responce) {
                Logger.error(TAG, "Send Video fail responce = " + responce);
            }
        });
    }

    private void sendStickerMessage(String messagedata) {
        /*final GroupMessage message = new GroupMessage(0, sessionData.getId(), chatroomId, messagedata,
                System.currentTimeMillis(), "", MessageTypeKeys.STICKER, "",
                "#000", 1);*/
        final GroupMessage message = new GroupMessage(0L,
                sessionData.getId(), chatroomId, messagedata, System.currentTimeMillis(), "",
                MessageTypeKeys.STICKER, "", "#000", 1, 0);
        addMessage(message);

        sendSticker(message);
    }

    private void sendSticker(final GroupMessage message) {
        cometChat.sendSticker(message.getId(), message.message, chatroomId+"",true, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error("send Response Sticker cometGroup : "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
//                    GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        message.retryCount = 3;
                        message.save();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "send Sticker responce fail = " + jsonObject);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (broadcastReceiver != null) {
            registerReceiver(broadcastReceiver, new IntentFilter(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST));
        }
        if(finishActivityReceiver != null){
            registerReceiver(finishActivityReceiver, new IntentFilter(BroadCastReceiverKeys.FINISH_GROUP_ACTIVITY));
        }
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_CHATROOM_ID, chatroomId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player == null) {
            player = CommonUtils.getPlayerInstance();
            if (player.isPlaying()) {
                try {
                    player.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        PreferenceHelper.save(JsonParsingKeys.GRP_WINDOW_ID,-1);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection;
        switch (id) {
            case MESSAGE_LOADER:
                selection = GroupMessage.getAllMessagesQuery(chatroomId);
                Logger.error(TAG,"Selection = "+selection);
                return new DataCursorLoader(this, selection, null);
            default:
                return null;

        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        messageCount = data.getCount();
        Logger.error(TAG,"Message count = "+messageCount);
        data.moveToFirst();
        while (!data.isAfterLast()) {
            data.moveToNext();
        }

        if(messageCount == 0){
            txtLoadEarlierMessages.setVisibility(View.VISIBLE);
            txtLoadEarlierMessages.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadChatHistory();
                }
            });
        }else{
            txtLoadEarlierMessages.setVisibility(View.GONE);
        }

        if (groupMessageAdapter == null) {
            groupMessageAdapter = new GroupMessageAdapter(this, data,String.valueOf(chatroomId));
            messageRecyclerView.setAdapter(groupMessageAdapter);
            decor = new StickyHeaderDecoration(groupMessageAdapter);
            messageRecyclerView.addItemDecoration(decor, 0);
        }

        if(messageCount>0){
            if(!isInitialLoad){
                if( btnScroll.getVisibility() != View.VISIBLE ){
                    messageRecyclerView.smoothScrollToPosition(data.getCount() - 1);
                }
                else{
                    btnScroll.setText(newMessageCount+" New Message ");
                    btnScroll.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                }
            }else{
                messageRecyclerView.scrollToPosition(data.getCount() - 1);
                isInitialLoad = false;
            }
        }else{
            btnScroll.setVisibility(View.GONE);
        }


        groupMessageAdapter.swapCursor(data);

        if(messageCount>0){
            data.moveToFirst();
            firstMessageID = data.getLong(data.getColumnIndex(GroupMessage.COLUMN_REMOTE_ID));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void getClickedEmoji(int i) {
        smiliKeyBoard.getClickedEmoji(i);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
        if(finishActivityReceiver != null){
            unregisterReceiver(finishActivityReceiver);
        }
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_CHATROOM_ID, "0");
    }

    private void AnimateBottomSheetViews() {
        ObjectAnimator scalePhotoImageViewX = ObjectAnimator.ofFloat(capturePhotoImageView, "scaleX", 0.0f,1.0f);
        ObjectAnimator scalePhotoImageViewY = ObjectAnimator.ofFloat(capturePhotoImageView, "scaleY", 0.0f,1.0f);
        ObjectAnimator scaleVideoImageViewX = ObjectAnimator.ofFloat(capturevideoImageView, "scaleX", 0.0f,1.0f);
        ObjectAnimator scaleVideoImageViewY = ObjectAnimator.ofFloat(capturevideoImageView, "scaleY", 0.0f,1.0f);
        scalePhotoImageViewX.setDuration(350);
        scalePhotoImageViewY.setDuration(350);
        scaleVideoImageViewX.setDuration(350);
        scaleVideoImageViewY.setDuration(350);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scalePhotoImageViewX,scalePhotoImageViewY,scaleVideoImageViewX,scaleVideoImageViewY);
        animatorSet.start();
    }
    private void AnimateHandwriteBottomSheetViews() {
        ObjectAnimator scalehandwriteMessageImageViewX = ObjectAnimator.ofFloat(handwriteMessageImageView, "scaleX", 0.0f,1.0f);
        ObjectAnimator scalehandwriteMessageImageViewY = ObjectAnimator.ofFloat(handwriteMessageImageView, "scaleY", 0.0f,1.0f);
        ObjectAnimator scaleCollaborativeDocumentImageViewX = ObjectAnimator.ofFloat(collaborativeDocumentImageView, "scaleX", 0.0f,1.0f);
        ObjectAnimator scaleCollaborativeDocumentImageViewY = ObjectAnimator.ofFloat(collaborativeDocumentImageView, "scaleY", 0.0f,1.0f);
        ObjectAnimator scalewhiteboardImageViewX = ObjectAnimator.ofFloat(whiteboardImageView, "scaleX", 0.0f,1.0f);
        ObjectAnimator scalewhiteboardImageViewY = ObjectAnimator.ofFloat(whiteboardImageView, "scaleY", 0.0f,1.0f);
        scalehandwriteMessageImageViewX.setDuration(350);
        scalehandwriteMessageImageViewY.setDuration(350);
        scalewhiteboardImageViewX.setDuration(350);
        scalewhiteboardImageViewY.setDuration(350);
        scaleCollaborativeDocumentImageViewX.setDuration(350);
        scaleCollaborativeDocumentImageViewY.setDuration(350);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleCollaborativeDocumentImageViewX,scaleCollaborativeDocumentImageViewY,scalehandwriteMessageImageViewX,scalehandwriteMessageImageViewY,scalewhiteboardImageViewX,scalewhiteboardImageViewY);
        animatorSet.start();
    }


    private void loadChatHistory(){
        Logger.error(TAG,"LoadChatHistory called");
        Logger.error(TAG,"LoadChatHistory ChatroomID = "+chatroomId);
        Logger.error(TAG,"LoadChatHistory firstMessageID = "+firstMessageID);
        cometChat.getGroupChatHistory(chatroomId, firstMessageID, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG,"Get group chat history success = "+jsonObject);
                try {
                    JSONArray history = jsonObject.getJSONArray("history");

                    if(history.length()!=0){

                        for(int i=0;i<history.length();i++){
                            CCMessageHelper.processGroupMessage(history.getJSONObject(i));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                                refreshLayout.setRefreshing(false);
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(false);
                                Toast.makeText(CCGroupChatActivity.this, "No More Messages", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }


                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.setRefreshing(false);
                            Toast.makeText(CCGroupChatActivity.this, "No More Messages", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"Get group chat history fail = "+jsonObject);
                if (jsonObject != null && !TextUtils.isEmpty(jsonObject.toString())) {
                    try {
                        if (jsonObject.has("code") && !TextUtils.isEmpty(jsonObject.getString("code")) && jsonObject.getString("code").equalsIgnoreCase("100")) {
                            final String toastMessage = jsonObject.getString("message");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(CCGroupChatActivity.this, toastMessage , Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (JSONException ex) {
                        Logger.error(TAG, "loadChatHistory() : failCallback()");
                        ex.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void sendBitmap(Bitmap bitmap,String path) {
        final GroupMessage message = new GroupMessage(0L,
                sessionData.getId(), chatroomId, path, System.currentTimeMillis(), "",
                MessageTypeKeys.IMAGE_MESSAGE, "", "#000", 1, 0);
        addMessage(message);
        cometChat.sendImage(message.getId(),bitmap,String.valueOf(chatroomId),true, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error(TAG,"sendResponse Image "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                    GroupMessage mess = GroupMessage.findByLocalId(String.valueOf(localMessId));
                    if (mess != null) {
                        mess.remoteId = id;
                        mess.messageStatus = 1;
                        mess.save();
                        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCGroupChatActivity.this);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void failCallback(JSONObject responce) {
                Logger.error(TAG, "Send Image fail responce = " + responce);
            }
        });
    }

    private void animateCustomMenu() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateCustomMenu = ObjectAnimator.ofFloat(customMenu, "translationY", customMenu.getHeight(),0.0f).setDuration(100);
        translateCustomMenu.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                customMenu.setBackgroundColor(Color.parseColor("#EEEEEE"));
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.play(translateCustomMenu);
        animatorSet.start();
    }

    @Override
    public void onRetryClicked(long localMessageId) {
        Logger.error(TAG, "onRetryClicked: localMessageId: "+localMessageId );
        GroupMessage groupMessage = GroupMessage.findByLocalId(String.valueOf(localMessageId));
        Logger.error(TAG, "onRetryClicked: GroupMessage "+groupMessage );
        if (groupMessage != null) {
            switch (groupMessage.type){
                case MessageTypeKeys.NORMAL_MESSAGE:
                case MessageTypeKeys.EMOJI_MESSAGE:
                    sendTextMessage(groupMessage);
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    sendImage(groupMessage);
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    sendVideo(groupMessage);
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    sendAudioNote(groupMessage);
                    break;
                case MessageTypeKeys.STICKER:
                    sendSticker(groupMessage);
                    break;
                default:
                    Logger.error(TAG, "onRetryClicked: incorrect message type" );
                    break;
            }
        }else {
            Logger.error(TAG, "onRetryClicked: message is not present in local database"  );
        }
    }
}
