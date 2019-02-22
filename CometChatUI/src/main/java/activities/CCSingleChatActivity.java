package activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
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
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import com.inscripts.custom.EmoticonUtils;
import com.inscripts.custom.StickyHeaderDecoration;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.enums.Status;
import com.inscripts.factories.DataCursorLoader;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.CCPermissionHelper;
import com.inscripts.helpers.PopupHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.KeyboardVisibilityEventListener;
import com.inscripts.interfaces.OnAlertDialogButtonClickListener;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.IntentExtraKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.plugins.AudioSharing;
import com.inscripts.plugins.ImageSharing;
import com.inscripts.plugins.Smilies;
import com.inscripts.plugins.Stickers;
import com.inscripts.plugins.VideoSharing;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.KeyboardVisibilityEvent;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import Keys.BroadCastReceiverKeys;
import Keys.JsonParsingKeys;
import Keys.MessageTypeKeys;
import adapters.OneOnOneMessageAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.cometchatcore.coresdk.MessageSDK;
import cometchat.inscripts.com.readyui.R;
import customsviews.ConfirmationWindow;
import helpers.CCAnalyticsHelper;
import helpers.CCMessageHelper;
import helpers.FileSharing;
import helpers.NotificationDataHelper;
import models.Contact;
import models.OneOnOneMessage;
import videochat.CCOutgoingCallActivity;
import videochat.CCVideoChatActivity;

public class CCSingleChatActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        OnAlertDialogButtonClickListener, EmojiGridviewImageAdapter.EmojiClickInterface, OneOnOneMessageAdapter.RetryCallback {

    private static final java.lang.String TAG = CCSingleChatActivity.class.getSimpleName();
    private final int MESSAGE_LOADER = 1;
    private final int PICASSA_IMAGE_PREVIEW_POPUP = 4, IMAGE_SEND_PREVIEW_POPUP = 2, VIDEO_SEND_PREVIEW_POPUP = 3, AUDIO_SEND_PREVIEW_POPUP = 6, BLOCK_USER_POPUP = 7, REPORT_CONVERSATION_POPUP = 8;

    private Toolbar toolbar;
    private RelativeLayout ccContainer, customMenu, chatFooter;
    private int colorPrimaryDark;
    private static int colorPrimary;
    private long contactId;
    private static Contact contact;
    private boolean setBackButton = true;
    private String contactName, picassImageName;
    private static String audioFileNamewithPath;
    private Intent data;
    private EditText messageField;
    private ImageButton sendButton,voiceNotebtn,btnMenu,btnChatMenuKeyBoard,btnChatMenuSharePhoto,btnChatMenuShareVideo, btnChatMenuSmiliey,
            btnCameraButton,btnChatMenuMore, btnSticker,btnAVBroadcast;
    private ImageView capturePhotoImageView,capturevideoImageView,whiteboardImageView, collaborativeDocumentImageView, handwriteMessageImageView;
    private TextView txtViewWiteBoard, txtViewWriteBoard, txtViewHandwrite, txtViewCapturePhoto, txtViewCaptureVideo , txtLoadEarlierMessages;
    private SwipeRefreshLayout refreshLayout;
    private CometChat cometChat;
    private SessionData sessionData;
    private RecyclerView messageRecyclerView;
    private OneOnOneMessageAdapter messageAdapter;
    private List<OneOnOneMessage> messageList;
    private int messageCount = -1;
    private BroadcastReceiver broadcastReceiver;
    private Bitmap picassaBitmap;
    private boolean isVoiceNoteplaying = false;
    private static boolean isRecording;
    private static MediaRecorder voiceRecorder;
    private Runnable timerRunnable;
    private Handler seekHandler = new Handler();
    private View dirtyView;
    private BottomSheetBehavior sheetCameraBehavior, sheetBehavior, sheetBehaviorSelectColor;
    private LinearLayout cameraSheetLayout, chatMenuLayout, bottomSheetlayout, bottomSheetSelectColor,llCustomPopup;
    private RelativeLayout viewShareWiteboard, viewCollaborativeDocument, viewHandwriteMessage;
    private RelativeLayout  viewCapturePhoto, viewCaptureVideo;
    private static Uri fileUri;
    private SmileyKeyBoard smiliKeyBoard;
    private StickerKeyboard stickerKeyboard;
    boolean flag = true;
    private static String mediaFilePath;
    private long firstMessageID =0L;
    private int requestCode = 0;
    private LinearLayoutManager linearLayoutManager;
    private MediaPlayer player;
    private StickyHeaderDecoration decor;
    private TextView toolbarTitle,toolbarSubTitle;
    private Button btnScroll;
    private long newMessageCount;
    private boolean isInitialLoad = true;
    private Cursor cursorData;
    private Animation viewAniamtion,goneAnimation;
    private boolean showIsTyping;
    private boolean handwriteEnabled;
    private boolean whiteBoardEnabled;
    private boolean writeBoardEnabled;
    private boolean avBroadcastEnabled;

    private FeatureState avCallState,audioCallState,fileTransferState,voiceNoteState,stickerState,smileyState;
    private FeatureState avBroadcastState;
    private FeatureState writeBoardState;
    private FeatureState whiteBoardState;
    private FeatureState handwriteState;
    private FeatureState clearConversationState;
    private FeatureState reportConversationState;
    private FeatureState blockUserState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_cordinate);
        cometChat = CometChat.getInstance(this);
        sessionData = SessionData.getInstance();
        toolbar = (Toolbar) findViewById(R.id.cometchat_toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        ccContainer = (RelativeLayout) findViewById(R.id.cc_chat_container);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupFields();
        setCCTheme();
        processIntentData(getIntent());
        if(!setBackButton){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationIcon(R.drawable.cc_ic_action_cancel);
        }
        getSupportActionBar().setTitle("");
        toolbarTitle.setText(contactName);
        initializeFeatureState();
        updateLastSeenActivity();
        setFieldListners();
        setupSmileyKeyboars();
        setupStickerKeyboard();
        NotificationManager notificationManager = (NotificationManager) PreferenceHelper.getContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        PreferenceHelper.removeKey(PreferenceKeys.DataKeys.NOTIFICATION_STACK);

        if (getSupportLoaderManager().getLoader(MESSAGE_LOADER) == null) {
            getSupportLoaderManager().initLoader(MESSAGE_LOADER, null, this);
        }else{
            getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, this);
        }

        sendStatusPingMessage();
        newMessageCount = 0;
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Logger.error(TAG,"Keys = "+String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
                }

                if (extras.containsKey(BroadCastReceiverKeys.NEW_MESSAGE)) {
                    if(extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.READ_MESSAGE)){
                        messageList = OneOnOneMessage.getAllMessages(sessionData.getId(), contactId);
                        for (OneOnOneMessage msg : messageList) {
                            if (msg.messagetick != CometChatKeys.MessageTypeKeys.MESSAGE_READ) {
                                msg.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_READ;
                                msg.save();
                            }
                        }
                    }else {
                        newMessageCount++;
                        Logger.error(TAG,"Message count onreceive = "+newMessageCount);
                    }
                   /* if(!extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.READ_MESSAGE)
                            && !extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.DELIVERED_MESSAGE)
                            && !extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.IS_TYPING)
                            && !extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.STOP_TYPING)
                    ){
                        newMessageCount++;
                        Logger.error(TAG,"Message count onreceive = "+newMessageCount);
                    }*/
                    getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                } else if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.IS_TYPING)) {
                    if (Long.parseLong(extras.getString(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID)) == contactId){

                       /* if(btnScroll.getVisibility() == View.VISIBLE){
                            toolbarSubTitle.setVisibility(View.VISIBLE);
                            toolbarSubTitle.setText("typing...");
                        }*/
                        toolbarSubTitle.setVisibility(View.VISIBLE);
                        toolbarSubTitle.setText("typing...");
                       // showIsTyping = false;
                        //getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                    }
                } else if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.STOP_TYPING)) {
                    if (Long.parseLong(extras.getString(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID)) == contactId){
                        toolbarSubTitle.setText("");
                        toolbarSubTitle.setVisibility(View.GONE);
                        //showIsTyping = false;
                        //getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                    }
                    updateLastSeenActivity();
                }else if (extras.containsKey(BroadCastReceiverKeys.IntentExtrasKeys.UPDATE_LAST_SEEN)) {
                    updateLastSeenActivity();
                }
            }
        };
        registerReceiver(broadcastReceiver,
                new IntentFilter(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST));
    }

    private void initializeFeatureState() {
        avCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.VIDEO_CALL_ENABLED));
        audioCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AUDIO_CALL_ENABLED));
        fileTransferState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.FILE_TRANSFER_ENABLED));
        voiceNoteState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.VOICE_NOTE_ENABLED));
        stickerState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.STICKER_ENABLED));
        smileyState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.EMOJI_ENABLED));
        avBroadcastState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AV_BROADCAST_ENABLED));
        handwriteState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.HANDWRITE_ENABLED));
        whiteBoardState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.WHITEBOARD_ENABLED));
        writeBoardState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.WRITEBOARD_ENABLED));
        clearConversationState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.CLEAR_CONVERSATION_ENABLED));
        reportConversationState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.REPORT_CONVERSATION_ENABLED));
        blockUserState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.BLOCKED_USER_ENABLED));
    }

    private void setupFields() {
        messageField = (EditText) findViewById(R.id.editTextChatMessage);
        messageField.setHint(Html.fromHtml("<small>"+(String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_TYPE_YOUR_MESSAGE))+"</small>"));
        sendButton = (ImageButton) findViewById(R.id.buttonSendMessage);
        voiceNotebtn = (ImageButton) findViewById(R.id.buttonSendVoice);
        btnMenu = (ImageButton) findViewById(R.id.img_btn_chat_more);
        btnChatMenuKeyBoard = (ImageButton) findViewById(R.id.btn_chat_menu_keyboard);
        btnChatMenuSharePhoto = (ImageButton) findViewById(R.id.btn_chat_menu_share_image);
        btnChatMenuShareVideo = (ImageButton) findViewById(R.id.btn_chat_menu_share_video);
        btnChatMenuSmiliey = (ImageButton) findViewById(R.id.img_btn_smiley);
        btnCameraButton = (ImageButton) findViewById(R.id.img_btn_camera);
        btnAVBroadcast = (ImageButton) findViewById(R.id.btn_chat_menu_broadcast);
        messageRecyclerView = (RecyclerView) findViewById(R.id.rvChatMessages);
        dirtyView = findViewById(R.id.dirty_view);
        linearLayoutManager = new LinearLayoutManager(this);
        /*linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);*/
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
        sheetBehaviorSelectColor.setPeekHeight(0);
        txtViewWiteBoard = (TextView) findViewById(R.id.textWhiteBoard);
        txtViewWriteBoard = (TextView) findViewById(R.id.textCollaborative);
        txtViewHandwrite = (TextView) findViewById(R.id.textHandwrite);
        txtViewCapturePhoto = (TextView) findViewById(R.id.textCapturePhoto);
        txtViewCaptureVideo = (TextView) findViewById(R.id.textCaptureVideo);
        txtLoadEarlierMessages = (TextView) findViewById(R.id.txt_load_earlier_messages);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        if(isRecording){
            messageField.setEnabled(false);
            btnChatMenuSmiliey.setEnabled(false);
            voiceNotebtn.setBackgroundResource(R.drawable.ic_mic);
            AnimationDrawable animationDrawable = (AnimationDrawable) voiceNotebtn.getBackground();
            voiceNotebtn.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
            animationDrawable.start();
        }else {
            messageField.setEnabled(true);
            btnChatMenuSmiliey.setEnabled(true);
            voiceNotebtn.setBackgroundResource(R.drawable.ic_mic_6);
        }
        toolbarTitle = (TextView) findViewById(R.id.title);
        toolbarSubTitle = (TextView) findViewById(R.id.subTitle);
        toolbarSubTitle.setSelected(true);
        btnScroll = (Button) findViewById(R.id.btn_new_message);
        viewAniamtion = AnimationUtils.loadAnimation(this, R.anim.animate);
        goneAnimation = AnimationUtils.loadAnimation(this, R.anim.gone_animation);
    }


    private void setFieldListners() {
        sendButton.setOnClickListener(this);
        btnMenu.setOnClickListener(this);
        btnChatMenuKeyBoard.setOnClickListener(this);
        btnScroll.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            messageRecyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if(messageCount-linearLayoutManager.findLastVisibleItemPosition() < 5){
                        if(btnScroll.getVisibility() == View.VISIBLE){
                            btnScroll.startAnimation(goneAnimation);
                            btnScroll.setVisibility(View.GONE);
                            toolbarSubTitle.setText("");
                            toolbarSubTitle.setVisibility(View.GONE);
                        }
                        if(!TextUtils.isEmpty(contact.cometid)){
                            if(cursorData != null && cursorData.getCount()>0){
                                cursorData.moveToLast();
                                if(newMessageCount >0){
                                    sendReadReceitMessage(cursorData.getLong(cursorData.getColumnIndex(OneOnOneMessage.COLUMN_REMOTE_ID)));
                                }
                                newMessageCount = 0;
                            }
                        }
                    }else{
                        if(btnScroll.getVisibility()==View.GONE){
                            btnScroll.startAnimation(viewAniamtion);
                            btnScroll.setVisibility(View.VISIBLE);
                        }
                    }

                    if(messageCount-2 == linearLayoutManager.findLastVisibleItemPosition()){
                        newMessageCount = 0;
                        btnScroll.setText("Jump to latest");
                        btnScroll.getBackground().setColorFilter(Color.parseColor("#8e8e92"), PorterDuff.Mode.SRC_ATOP);
                    }
                }
            });
        }
        //btnAVBroadcast.setOnClickListener(this);
        if (fileTransferState == FeatureState.INVISIBLE) {
            sendButton.setVisibility(View.VISIBLE);
            btnChatMenuSharePhoto.setVisibility(View.GONE);
            btnChatMenuShareVideo.setVisibility(View.GONE);
            voiceNotebtn.setVisibility(View.INVISIBLE);
            btnCameraButton.setVisibility(View.GONE);
        } else {
            btnChatMenuSharePhoto.setOnClickListener(this);
            btnChatMenuShareVideo.setOnClickListener(this);
            btnCameraButton.setOnClickListener(this);
            viewCapturePhoto.setOnClickListener(this);
            viewCaptureVideo.setOnClickListener(this);
            txtViewCapturePhoto.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CAPTURE_PHOTO)).toString());
            txtViewCaptureVideo.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CAPTURE_VIDEO)).toString());
            voiceNotebtn.setOnClickListener(this);
            if(voiceNoteState == FeatureState.INVISIBLE){
                sendButton.setVisibility(View.VISIBLE);
                voiceNotebtn.setVisibility(View.INVISIBLE);
            }
        }

        if (stickerState != FeatureState.INVISIBLE) {
            btnSticker.setOnClickListener(this);
        } else {
            btnSticker.setVisibility(View.GONE);
        }

        if (smileyState != FeatureState.INVISIBLE) {
            btnChatMenuSmiliey.setOnClickListener(this);
        } else {
            btnChatMenuSmiliey.setVisibility(View.GONE);
        }

        dirtyView.setOnClickListener(this);



        if (handwriteState != FeatureState.INVISIBLE) {
            viewHandwriteMessage.setOnClickListener(this);
            txtViewHandwrite.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_HANDWRITE)).toString());
        } else {
            viewHandwriteMessage.setVisibility(View.GONE);
        }

        if (whiteBoardState != FeatureState.INVISIBLE) {
            viewShareWiteboard.setOnClickListener(this);
            txtViewWiteBoard.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_WHITEBOARD)).toString());
        } else {
            viewShareWiteboard.setVisibility(View.GONE);
        }

        if (writeBoardState != FeatureState.INVISIBLE) {
            viewCollaborativeDocument.setOnClickListener(this);
            txtViewWriteBoard.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_WRITEBOARD)).toString());
        } else {
            viewCollaborativeDocument.setVisibility(View.GONE);
        }

        if (handwriteState == FeatureState.INVISIBLE && writeBoardState == FeatureState.INVISIBLE && whiteBoardState == FeatureState.INVISIBLE) {
            btnChatMenuMore.setVisibility(View.GONE);
        }
        btnChatMenuMore.setOnClickListener(this);

        KeyboardVisibilityEvent.setEventListener(
                this,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        messageRecyclerView.scrollToPosition(messageCount - 1);
                    }
                });

        flag = true;
        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (fileTransferState != FeatureState.INVISIBLE && voiceNoteState != FeatureState.INVISIBLE){
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
                }else {
                    sendButton.setVisibility(View.VISIBLE);
                    voiceNotebtn.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (flag) {
                    flag = false;
                    cometChat.isTyping(true, contact.cometid, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                            Logger.error(TAG,"Is Typing responce = "+jsonObject);
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG,"Is Typing fail responce = "+jsonObject);
                        }
                    });
                }
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        flag = true;
                        cometChat.isTyping(false, contact.cometid, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"Is Typing responce = "+jsonObject);
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG,"Is Typing fail responce = "+jsonObject);
                            }
                        });
                    }
                }, 3000);
            }
        });
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadChatHistory();
            }
        });

        if(avBroadcastState != FeatureState.INVISIBLE) {
            btnAVBroadcast.setOnClickListener(this);
        } else{
            btnAVBroadcast.setVisibility(View.GONE);
        }
        txtLoadEarlierMessages.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LOAD_EARLIER)));
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

    private void updateLastSeenActivity() {

        contact = Contact.getContactDetails(contactId);
        Logger.error(TAG,"updateLastSeenActivity: lstn: "+contact.lstn+" lastseen: "+contact.lastseen);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.LAST_SEEN_ENABLED))) {
            if (!TextUtils.isEmpty(contact.lastseen) && contact.lstn != 0) {
                toolbarSubTitle.setVisibility(View.VISIBLE);
                final String status = CommonUtils.checkOnlineStatus(Long.parseLong(contact.lastseen));
                toolbarSubTitle.setText(status);
            } else {
                  toolbarSubTitle.setVisibility(View.GONE);
            }
        }
    }

    private void processIntentData(final Intent intent) {
        if (intent.hasExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID)) {
            contactId = intent.getLongExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, 0);
            Logger.error(TAG, "processIntentData: contactId: "+contactId);
            NotificationDataHelper.deleteFromMap((int) contactId);
            contact = Contact.getContactDetails(contactId);
            PreferenceHelper.save(JsonParsingKeys.WINDOW_ID,contactId);
        } else {
            if (null != contact) {
                contactId = contact.contactId;
            }
        }

        Logger.error(TAG,"has CLOSE_WINDOW_ENABLED ? "+intent.hasExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED));
        if(intent.hasExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED)){
            setBackButton = intent.getBooleanExtra(BroadCastReceiverKeys.IntentExtrasKeys.CLOSE_WINDOW_ENABLED,true);
        }

        if (intent.hasExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME)) {
            contactName = intent.getStringExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_NAME);
        } else {
            if (null != contact) {
                contactName = contact.name;
            }
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

        /*if (intent.hasExtra("FileUri")) {
            if (PreferenceHelper.contains(PreferenceKeys.DataKeys.SHARE_FILE_URL)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(ShareConfirm);
                builder.setCancelable(true);
                builder.setPositiveButton(positiveResponse, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String shareFileUri = intent.getStringExtra("FileUri");
                        shareFileUri = shareFileUri.replace("file://", "");
                        Uri uri = Uri.parse(shareFileUri);
                        FileSharing.sendFile(CCSingleChatActivity.this, uri, buddyId, false);
                    }
                });
                builder.setNegativeButton(negativeResponse, null);
                AlertDialog alert = builder.create();
                alert.show();
                PreferenceHelper.removeKey(PreferenceKeys.DataKeys.SHARE_FILE_URL);
            }
        }*/

        String positiveResponse = ((String)cometChat.getCCSetting(new CCSettingMapper(
                SettingType.LANGUAGE,SettingSubType.LANG_YES)));
        if (positiveResponse == null) {
            positiveResponse = "Yes";
        }

        String negativeResponse = ((String)cometChat.getCCSetting(new CCSettingMapper(
                SettingType.LANGUAGE,SettingSubType.LANG_NO)));
        if (negativeResponse == null) {
            negativeResponse = "No";
        }

        String shareConfirm = ((String)cometChat.getCCSetting(new CCSettingMapper(
                SettingType.LANGUAGE,SettingSubType.LANG_FILE_SHARE_CONFIRM)));
        if (shareConfirm == null) {
            shareConfirm = "Do you want to share?";
        }

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
    }

    private void setCCTheme() {
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);

        if (avCallState == FeatureState.INVISIBLE) {
            menu.findItem(R.id.custom_action_video_call).setVisible(false);
        }

        if (audioCallState == FeatureState.INVISIBLE) {
            menu.findItem(R.id.custom_action_audio_call).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
             if(!setBackButton){
                MessageSDK.closeCometChatWindow(CCSingleChatActivity.this, ccContainer);
                cometChat.sendCloseCCWindowResponce();
            }else{
                finish();
            }
        } else if (id == R.id.custom_action_more) {
            final View menuItemView = findViewById(R.id.custom_action_more);

            if (!sheetBehavior.isHideable()) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            showCustomActionBarPopup(menuItemView);
        } else if(id == R.id.custom_action_video_call){
            if(avCallState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                makeVideoCall();
            }
        }
        else if (id == R.id.custom_action_audio_call){
            if(audioCallState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                makeAudioCall();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void makeVideoCall() {
        if (CommonUtils.isConnected()) {
            if (Build.VERSION.SDK_INT >= 16) {
                String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                        CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
                if(CCPermissionHelper.hasPermissions(this, PERMISSIONS)){
                    showCallPopup(false);
                }else{
                    CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_VIDEO_CALL);
                }
            } else {
                Toast.makeText(this, "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
            }
        } else {Toast.makeText(this, "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
    }

    private void makeAudioCall() {
        if (CommonUtils.isConnected()) {
            if (Build.VERSION.SDK_INT >= 16) {
                String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                        CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
                if(CCPermissionHelper.hasPermissions(this, PERMISSIONS)){
                    showCallPopup(true);
                }else{
                    CCPermissionHelper.requestPermissions(this, PERMISSIONS, CCPermissionHelper.PERMISSION_AUDIO_CALL);
                }
            } else {
                Toast.makeText(this, "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
            }
        } else {Toast.makeText(this, "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (broadcastReceiver != null) {
            registerReceiver(broadcastReceiver,
                    new IntentFilter(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST));
            if (PreferenceHelper.contains("WINDOW ID"))
                PreferenceHelper.removeKey("WINDOW ID");
        }
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID, contactId);
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
                        + contactName + "?");
            } else {
                cWindow.setMessage(/*(lang.getAvchat() == null) ? "Call" : lang.getAvchat().get28()+*/ "Call " + contactName
                        + "?");
            }

            cWindow.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private void initiateCall(final boolean isAudioOnlyCall) {
        if(isAudioOnlyCall){
//            FlurryAgent.logEvent("Audio Call");
            CCAnalyticsHelper.logFeatureEvent("Audio Call");
            cometChat.sendAudioChatRequest(String.valueOf(contactId), new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAudioChatRequest Success callback = "+jsonObject);
                    try {
                        Intent intent = new Intent(CCSingleChatActivity.this, CCOutgoingCallActivity.class);
                        intent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(contactId));
                        intent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, isAudioOnlyCall);
                        //intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME,jsonObject.getString("callid"));
                        PreferenceHelper.save("IS_INITIATOR","1");
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"Fail callback = "+jsonObject);
                }
        });
        }else{
//            FlurryAgent.logEvent("Video Call");
            CCAnalyticsHelper.logFeatureEvent("Video Call");
            cometChat.sendAVChatRequest(String.valueOf(contactId), new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAVChatRequest responc = "+jsonObject);
                    try {
                        Intent intent = new Intent(CCSingleChatActivity.this, CCOutgoingCallActivity.class);
                        intent.putExtra(CometChatKeys.AVchatKeys.CALLER_ID, String.valueOf(contactId));
                        intent.putExtra(CometChatKeys.AudiochatKeys.AUDIO_ONLY_CALL, isAudioOnlyCall);
                        intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME,jsonObject.getString("callid"));
                        PreferenceHelper.save("IS_INITIATOR","1");
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"sendAVChatRequest fail responc = "+jsonObject);
                }
            });
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Logger.error(TAG, "onResume: isRecording: "+isRecording);
        if (requestCode == 0) {
            if (getSupportLoaderManager().getLoader(MESSAGE_LOADER) != null) {
                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, this);
            }
        }
        requestCode = 0;
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID, contactId);
    }

    @Override
    public void  onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Logger.error(TAG, "onConfigurationChanged: isRecording: "+isRecording);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceHelper.save(PreferenceKeys.DataKeys.ACTIVE_BUDDY_ID, "0");

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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(broadcastReceiver!=null){
            unregisterReceiver(broadcastReceiver);
        }
        PreferenceHelper.save(JsonParsingKeys.WINDOW_ID,-1);
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
                Logger.error(TAG, "requestpermissionres : requestCode : " + requestCode);
                for (int result : grantResults) {
                    Logger.error(TAG, "requestpermissionres : result : " + result);
                }
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openCaptureMediaBottomSheet();
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
                break;

            case CCPermissionHelper.PERMISSION_AUDIO_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if (audioCallState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
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

            case CCPermissionHelper.PERMISSION_VIDEO_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if (avCallState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
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

            case CCPermissionHelper.PERMISSION_AV_BROADCAST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    startAvBroadcast();
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
            if (requestCode == 1111) {
                this.requestCode = requestCode;
                if (messageAdapter != null) {
                    messageAdapter.notifyDataSetChanged();
                }
            } else {
                if (resultCode == RESULT_OK) {
                    switch (requestCode) {
                        case StaticMembers.CHOOSE_IMAGE_REQUEST_ONE_ON_ONE:
                            checkUserConfirmation(data, false);
                            break;

                        case StaticMembers.CHOOSE_VIDEO_REQUEST_ONE_ON_ONE:
                            Uri imageUri = data.getData();
                            System.out.println("File imageUri : " + imageUri);
                            //String filePath = imageUri.getPath();
                            String filePath1 = getRealPathFromURI(imageUri);
                            System.out.println("File filePath : " + filePath1);
                            System.out.println("File exists : " + new File(filePath1).exists());

                            checkUserConfirmation(data, true);
                            break;

                        case StaticMembers.CAPTURE_PHOTO_REQUEST_ONE_ON_ONE:
                            Logger.error(TAG,"fileUri: "+fileUri);
                            if(fileUri.toString().contains("file://")){
//                                sendImageMessage(fileUri.getPath());
                                Bitmap bitmapToSend = CommonUtils.getOrientationFromExifData(fileUri.getPath());
                                sendBitmap(bitmapToSend,fileUri.getPath());
                            }else{
                                Bitmap bitmapToSend = CommonUtils.getOrientationFromExifData(mediaFilePath);
//                                sendImageMessage(fileUri);
                                sendBitmap(bitmapToSend,mediaFilePath);
                            }

                        /*File imageFile = new File(mediaFilePath);
                        Logger.error(TAG," videoFile exists ? " + imageFile.exists());
                        if(imageFile.exists()){
                            sendImageMessage(mediaFilePath);
                        }*/
                            break;
                        case StaticMembers.CAPTURE_VIDEO_REQUEST_ONE_ON_ONE:
                        /*Logger.error(TAG,"File URI = "+fileUri);
                        Logger.error(TAG,"Video file path "+ fileUri.getPath()+" Video file Exists ? "+new File(fileUri.getPath()).exists());

                        if(fileUri.toString().contains("content://")){
                           // sendVideoMessage(fileUri.toString());
                            File sdDir = Environment.getExternalStorageDirectory();

                        }else{
                            String filePath = LocalStorageFactory.getFilePathFromIntent(data);
                            Logger.error(TAG,"File Path = "+filePath);
                            File file = new File(fileUri.getPath());
                            Logger.error(TAG,"File Exist ? "+file.exists());
                            if(file.exists())
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
            }

        } catch (Exception e) {
            Logger.error("SingleChatActivity.java onActivityResult() : Exception =" + e);
            e.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btn_new_message){
            linearLayoutManager.smoothScrollToPosition(messageRecyclerView,null,messageCount-1);
        }else if (id == R.id.buttonSendMessage) {
            String message = messageField.getText().toString().trim();
            cometChat.isTyping(false, contact.cometid, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"Is Typing responce = "+jsonObject);
                }

                @Override
                public void failCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"Is Typing fail responce = "+jsonObject);
                }
            });
            if (!TextUtils.isEmpty(message)) {
                sendTextMessage(message);
            } else {
                Toast.makeText(CCSingleChatActivity.this, "Message Cannot be Empty", Toast.LENGTH_SHORT).show();
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

            if (Stickers.isEnabled() && stickerKeyboard.isKeyboardVisibile()) {
                stickerKeyboard.showKeyboard(chatFooter);

                if (!inputMethodManager.isActive()) {
                    inputMethodManager.toggleSoftInputFromWindow(ccContainer.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                }

            } else {
                inputMethodManager.toggleSoftInputFromWindow(ccContainer.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            }
        } else if (id == R.id.btn_chat_menu_share_image) {
            Logger.error(TAG, "Share image Clicked");

           if(fileTransferState == FeatureState.INACCESSIBLE){
               AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
               alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                   }
               }).show();
           }else {
               shareImage();
           }
        } else if (id == R.id.btn_chat_menu_share_video) {
            if(fileTransferState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                shareVideo();
            }
        } else if (id == R.id.buttonSendVoice) {
            if(fileTransferState == FeatureState.INACCESSIBLE || voiceNoteState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                shareVoiceNote();
            }
        } else if (id == R.id.img_btn_camera) {
            if(fileTransferState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
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
            if(smileyState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                smiliKeyBoard.showKeyboard(chatFooter);
            }
        } else if (id == R.id.btn_chat_menu_sticker) {
            if(stickerState == FeatureState.INACCESSIBLE){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {
                stickerKeyboard.showKeyboard(chatFooter);
            }
        } else if (id == R.id.ll_handwrite_message) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (handwriteState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                startHandwrite();
            }
        } else if (id == R.id.ll_share_whiteboard) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (whiteBoardState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                shareWhiteBoard();
            }

        } else if (id == R.id.ll_collaborative_document) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (writeBoardState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                shareWriteBoard();
            }
        }else if( id == R.id.btn_chat_menu_broadcast){
            if (avBroadcastState == FeatureState.INACCESSIBLE) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
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

    private void showCustomActionBarPopup(View view) {
        final PopupWindow showPopup = PopupHelper.newBasicPopupWindow(getApplicationContext());
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.cc_custom_single_chat_action_bar_menu, null);
        showPopup.setContentView(popupView);
        LinearLayout llCustomPopup = (LinearLayout) popupView.findViewById(R.id.singleChatCustomPopup);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            llCustomPopup.setPadding(0,0,CommonUtils.dpToPx(30),0);
        }
        RelativeLayout viewProfile = (RelativeLayout) popupView.findViewById(R.id.ll_view_profile);
        final RelativeLayout clearConversation = (RelativeLayout) popupView.findViewById(R.id.ll_clear_conversation);
        RelativeLayout reportConversation = (RelativeLayout) popupView.findViewById(R.id.ll_report_conversation);
        final RelativeLayout blockuser = (RelativeLayout) popupView.findViewById(R.id.ll_block_user);

        ImageView viewProfileImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_view_profile);
        ImageView clearConversationImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_clear_conversation);
        ImageView reportConversarionImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_report_conversation);
        ImageView blockUserImageView = (ImageView) popupView.findViewById(R.id.action_bar_menu_block_user);
        viewProfileImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        clearConversationImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        reportConversarionImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        blockUserImageView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

        TextView tvViewProfile = (TextView) popupView.findViewById(R.id.tv_view_profile);
        TextView tvClearConversation = (TextView) popupView.findViewById(R.id.tv_clear_conversation);
        TextView tvReportConversation = (TextView) popupView.findViewById(R.id.tv_reportConversation);
        TextView tvBlockuser = (TextView) popupView.findViewById(R.id.tv_block_user);

        tvViewProfile.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_VIEW_PROFILE)).toString());
        tvClearConversation.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_CLEAR_CONVERSATION)).toString());
        tvReportConversation.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_REPORT_CONVERSATION)).toString());
        tvBlockuser.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_BLOCK_USER)).toString());

        viewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup.dismiss();
                CCAnalyticsHelper.logFeatureEvent("View Profile");
                Intent i = new Intent(getApplicationContext(), CCViewUserProfileActivity.class);
                i.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, contactId);
                startActivity(i);
            }
        });

        if (clearConversationState != FeatureState.INVISIBLE) {
            clearConversation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup.dismiss();
                    CCAnalyticsHelper.logFeatureEvent("Clear Conversation");
                    if(clearConversationState == FeatureState.INACCESSIBLE){
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    }else {
                        clearOneToOneConversation();
                    }
                }
            });
        } else {
            clearConversation.setVisibility(View.GONE);
        }

        if (reportConversationState != FeatureState.INVISIBLE) {
            reportConversation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup.dismiss();
                    CCAnalyticsHelper.logFeatureEvent("Report Conversation");
                    if (reportConversationState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        reportConversation();
                    }
                }
            });
        }else {
            reportConversation.setVisibility(View.GONE);
        }

        if (blockUserState != FeatureState.INVISIBLE) {
            blockuser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopup.dismiss();
                    CCAnalyticsHelper.logFeatureEvent("Block User");
                    if (blockUserState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CCSingleChatActivity.this);
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        blockUser();
                    }
                }
            });
        } else {
            blockuser.setVisibility(View.GONE);
        }

        showPopup.setWidth(Toolbar.LayoutParams.WRAP_CONTENT);
        showPopup.setHeight(Toolbar.LayoutParams.WRAP_CONTENT);
        showPopup.setAnimationStyle(R.style.Animations_GrowFromTop);
        showPopup.showAsDropDown(view);
    }

    private void clearOneToOneConversation() {
        OneOnOneMessage.clearConversation(String.valueOf(contactId));
        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
        firstMessageID = 0;
    }

    private void openCaptureMediaBottomSheet() {
        dirtyView.setVisibility(View.VISIBLE);
        hideSoftKeyboard(CCSingleChatActivity.this);
        if(smiliKeyBoard.isKeyboardVisibile()){
            smiliKeyBoard.dismissKeyboard();
        }
        sheetCameraBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        AnimateBottomSheetViews();
    }

    private void loadChatHistory(){
        final String noMoreMessages = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_MORE_MESSAGES));
        cometChat.getChatHistory(contactId,firstMessageID, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG,"Get Chat History success = "+jsonObject);
                try {
                    JSONArray history = jsonObject.getJSONArray("history");

                    if(history.length()!=0){

                        for(int i=0;i<history.length();i++){
                            CCMessageHelper.processOneOnOneMessage(history.getJSONObject(i));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(false);
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                            }
                        });

                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(false);
                                Toast.makeText(CCSingleChatActivity.this, noMoreMessages, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }


                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.setRefreshing(false);
                            Toast.makeText(CCSingleChatActivity.this, noMoreMessages, Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(final JSONObject jsonObject) {
                Logger.error(TAG,"Get Chat History fail = "+jsonObject);
                if (jsonObject != null && !TextUtils.isEmpty(jsonObject.toString())) {
                    try {
                        if (jsonObject.has("code") && !TextUtils.isEmpty(jsonObject.getString("code")) && jsonObject.getString("code").equalsIgnoreCase("100")) {
                            final String toastMessage = jsonObject.getString("message");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(CCSingleChatActivity.this, toastMessage , Toast.LENGTH_SHORT).show();
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

    private void startHandwrite() {
//        FlurryAgent.logEvent("Handwrite Message");
        CCAnalyticsHelper.logFeatureEvent("Handwrite Message");
        Intent i = new Intent(getApplicationContext(), CCHandwriteActivity.class);
        i.putExtra(CometChatKeys.AjaxKeys.SENDERNAME, contactName);
        i.putExtra(CometChatKeys.AjaxKeys.BASE_DATA, PreferenceHelper.get(PreferenceKeys.DataKeys.BASE_DATA));
        i.putExtra(CometChatKeys.AjaxKeys.ID, contactId);
        startActivity(i);
    }

    private void startWriteBoard() {
//        FlurryAgent.logEvent("Collaborative Document");
        CCAnalyticsHelper.logFeatureEvent("Collaborative Document");
        cometChat.sendWriteBoardRequest(String.valueOf(contactId),false, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG, "SendWriteBoard success responce = " + jsonObject);
                try {
                    Logger.error(TAG, "SendWhiteBoard success responce = " + jsonObject);
                    Intent i = new Intent(getApplicationContext(), CCWebViewActivity.class);
                    i.putExtra(IntentExtraKeys.WEBSITE_URL, jsonObject.getString("writeboard_url"));
                    i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_WRTITEBOARD_TITLE)));
                    startActivity(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "SendWriteBoard success responce = " + jsonObject);
            }
        });
    }

    private void startAvBroadcast(){
        Logger.error(TAG,"Start AV broadcast called");
//        FlurryAgent.logEvent("AV Broadcast Started");
        CCAnalyticsHelper.logFeatureEvent("AV Broadcast Started");
        cometChat.sendAVBroadcastRequest(String.valueOf(contactId), new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                try {
                    Logger.error(TAG,"Start AV broadcast success = "+jsonObject);
                    Logger.error(TAG,"Room name = "+ jsonObject.getString("callid"));
                    Intent intent = new Intent(CCSingleChatActivity.this, CCVideoChatActivity.class);
                    intent.putExtra(StaticMembers.INTENT_ROOM_NAME, jsonObject.getString("callid"));
                    intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG,true);
                    intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG,true);
                    intent.putExtra("CONTACT_ID",String.valueOf(contactId));
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

    private void startWhiteBoard() {
//        FlurryAgent.logEvent("WhiteBoard");
        CCAnalyticsHelper.logFeatureEvent("WhiteBoard");
        cometChat.sendWhiteBoardRequest(String.valueOf(contactId),false, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {

                try {
                    Logger.error(TAG, "SendWhiteBoard success responce = " + jsonObject);
                    Intent i = new Intent(getApplicationContext(), CCWebViewActivity.class);
                    i.putExtra(IntentExtraKeys.WEBSITE_URL, jsonObject.getString("whiteboard_url"));
                    i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME, (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_WHITEBOARD_TITLE)));
                    startActivity(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "SendWhiteBoard fail responce = " + jsonObject);
            }
        });
    }

    private void blockUser() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogview = inflater.inflate(R.layout.cc_custom_dialog, null);
        TextView dialogueTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
        EditText dialogueInput = (EditText) dialogview.findViewById(R.id.edittextDialogueInput);
        dialogueInput.setVisibility(View.GONE);
        dialogueTitle.setVisibility(View.GONE);
        String yes = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_YES));
        String no = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO));
        new CustomAlertDialogHelper(this, ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BLOCK_USER_WARNING))), dialogview, yes, "", no, this, BLOCK_USER_POPUP,true);
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }


    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = ImageSharing.getOutputMediaFileUri(CCSingleChatActivity.this,StaticMembers.MEDIA_TYPE_IMAGE, false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        mediaFilePath = ImageSharing.getOutputMediaFilePath();
        startActivityForResult(intent, StaticMembers.CAPTURE_PHOTO_REQUEST_ONE_ON_ONE);
    }

    private void videoCapture() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (!CommonUtils.isSamsungWithApi16()) {
            Logger.error(TAG,"fileUri : ");
            fileUri = ImageSharing.getOutputMediaFileUri(CCSingleChatActivity.this,StaticMembers.MEDIA_TYPE_VIDEO, false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        }
        Logger.error(TAG,"fileUri : " + fileUri);

        mediaFilePath = ImageSharing.getOutputMediaFilePath();
        Logger.error(TAG,"mediaFilePath : " + mediaFilePath);

        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 15000000L);
        startActivityForResult(intent, StaticMembers.CAPTURE_VIDEO_REQUEST_ONE_ON_ONE);
    }

    private void reportConversation() {
        if (messageCount > 0) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogview = inflater.inflate(R.layout.cc_custom_dialog, null);
            TextView dialogueTitle = (TextView) dialogview.findViewById(R.id.textViewDialogueTitle);
            EditText dialogueInput = (EditText) dialogview.findViewById(R.id.edittextDialogueInput);
            dialogueTitle.setVisibility(View.GONE);
//            dialogueTitle.setText(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REPORT_CONVERSATION_DIALOG_TITLE)).toString());

            dialogueInput.setHint(cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REASON)).toString());

            dialogueInput.setInputType(InputType.TYPE_CLASS_TEXT);
            String cancelDialogButton = cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL)).toString();
            new CustomAlertDialogHelper(this, cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REPORT_CONVERSATION_DIALOG_TITLE)).toString(), dialogview, (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_OK)), "", cancelDialogButton, this,
                    REPORT_CONVERSATION_POPUP,true);
        } else {
            Toast.makeText(this, cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REPORT_CONVERSATION_EMPTY)).toString(), Toast.LENGTH_LONG).show();
        }
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
                    String cancelDialogButton = (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCEL));
                    final View dialogview = getLayoutInflater().inflate(R.layout.cc_custom_voice_note_preview, null);

                    final ImageView playBtn = (ImageView) dialogview.findViewById(R.id.imageViewPlayIconPreview);
                    final SeekBar seekbar = (SeekBar) dialogview.findViewById(R.id.seek_barPreview);
                    seekbar.getProgressDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        seekbar.getThumb().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                    }
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
//                voiceNotebtn.getDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                voiceNotebtn.setBackgroundResource(R.drawable.ic_mic);
                AnimationDrawable animationDrawable = (AnimationDrawable) voiceNotebtn.getBackground();
                voiceNotebtn.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                animationDrawable.start();
                voiceRecorder = new MediaRecorder();
                Logger.error(TAG,"converting file format to .mp3");
                voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                audioFileNamewithPath = AudioSharing.getOutputMediaFile();
                voiceRecorder.setOutputFile(audioFileNamewithPath);
                voiceRecorder.prepare();
                voiceRecorder.start();
                Toast.makeText(getApplicationContext(), "Recording..", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
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
            Logger.error(TAG,"selectedImageUri: "+selectedImageUri);
            LayoutInflater inflater = getLayoutInflater();
            View dialogview = inflater.inflate(R.layout.cc_custom_image_preview, null);
            ImageView imagePreview = (ImageView) dialogview.findViewById(R.id.imageViewLargePreview);
            ImageView closePopup = (ImageView) dialogview.findViewById(R.id.imageViewClosePreviewPopup);
            closePopup.setVisibility(View.GONE);
            this.data = data;
            String sendDialogButton = "Send", cancelDialogButton = "Cancel";
            Logger.error(TAG,"ACTION_SEND Selected URI = "+selectedImageUri);
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
                         //       || imageUri.toString().startsWith("content://com.android.gallery3d")) {
                            try {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                final InputStream ist = getContentResolver().openInputStream(imageUri);
                                picassaBitmap = BitmapFactory.decodeStream(ist, null, options);
                                ist.close();
                                if (null != picassaBitmap) {
                                    picassImageName = imageUri.toString().substring(
                                            imageUri.toString().lastIndexOf("/") + 1);
                                    imagePreview.setImageBitmap(picassaBitmap);
                                    //new CustomAlertDialogHelper(this, "", dialogview, sendDialogButton, "",
                                     //       cancelDialogButton, this, PICASSA_IMAGE_PREVIEW_POPUP,false);
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
                            String filepath = FileSharing.getImageBitmap(CCSingleChatActivity.this, data, contactId, false);
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
                if (which == DialogInterface.BUTTON_POSITIVE) {
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
                            Logger.error(TAG, "Block user fail responce = " + jsonObject);
                        }
                    });

                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    alertDialog.dismiss();
                }


                break;

            case REPORT_CONVERSATION_POPUP:
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    EditText dialogueInput = (EditText) view.findViewById(R.id.edittextDialogueInput);
                    String reasonForReporting = dialogueInput.getText().toString().trim();

                    if (reasonForReporting.length() > 0) {
                        cometChat.reportConversation(String.valueOf(contactId), reasonForReporting, new Callbacks() {
                            @Override
                            public void successCallback(JSONObject jsonObject) {
                                Logger.error(TAG, "Report conversation responce = " + jsonObject);
                            }

                            @Override
                            public void failCallback(JSONObject jsonObject) {
                                Logger.error(TAG, "Report conversation fail responce = " + jsonObject);
                            }
                        });
                        alertDialog.dismiss();
                    } else {
                        /*if (lang.getReport() != null && lang.getReport().get6() != null){
                            dialogueInput.setError(lang.getReport().get6());
                        }else{*/
                        dialogueInput.setError("Reason must be filled out.");
                        // }
                    }
                }
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    alertDialog.dismiss();
                }
                break;

        }
    }

    private void addMessage(OneOnOneMessage message) {
        message.save();
        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, this);
        CCMessageHelper.addSingleChatConversation(contact, message, false);
    }


    private void sendTextMessage(String message) {

        final OneOnOneMessage mess = new OneOnOneMessage(0L,
                sessionData.getId(), contactId, message, System.currentTimeMillis(), 1, 1, EmoticonUtils.isEmojiMessage(message)?MessageTypeKeys.EMOJI_MESSAGE:
                MessageTypeKeys.NORMAL_MESSAGE, "", 1, CometChatKeys.MessageTypeKeys.MESSAGE_PENDING, 0);

        if(EmoticonUtils.isOnlySmileyMessage(message)){
            mess.imageUrl = MessageTypeKeys.NO_BACKGROUND;
        }
        addMessage(mess);
        messageField.setText("");
        sendTextMessage(mess);
    }

    private void sendTextMessage(final OneOnOneMessage mess) {
//        FlurryAgent.logEvent("Text Message");
        CCAnalyticsHelper.logFeatureEvent("Text Message");
        cometChat.sendMessage(mess.getId(), String.valueOf(contactId), mess.message,false, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error(TAG, "sendResponse Message - "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                    Long timestamp = sendResponse.getLong(CometChatKeys.AjaxKeys.SENT);
                    Logger.error(TAG, "sendResponse : id - "+id);
                    String messageAfterSuccess = sendResponse.getString(CometChatKeys.AjaxKeys.MESSAGE);
                    if (messageAfterSuccess.contains("<img class=\"cometchat_smiley\"")) {
                        messageAfterSuccess = Smilies.convertImageTagToEmoji(messageAfterSuccess);
                        Logger.error(TAG, "emoji message: " + messageAfterSuccess);
                    }
//                    OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                    if (mess != null) {
                        mess.remoteId = id;
                        mess.sentTimestamp = timestamp * 1000;
                        mess.messageStatus = 1;
                        mess.message = String.valueOf(Html.fromHtml(messageAfterSuccess));
                        if(cometChat.isMessageinPendingDeliveredList(String.valueOf(id))){
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                        }else {
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_SENT;
                        }
                        mess.retryCount = 3;
                        mess.save();
                        Logger.error(TAG,"Remote id = "+id);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                            }
                        });
                    }
                } catch (JSONException e) {
                    Logger.error("sendResponse : e - "+e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                try {
                    if(jsonObject.getString("code").equals("900")){
                        OneOnOneMessage mess = OneOnOneMessage.findByLocalId(jsonObject.getString("localmessageid"));
                        if (mess != null) {
                            mess.messageStatus = 2;
                            mess.save();
                            getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                            Toast.makeText(CCSingleChatActivity.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendImageMessage(String imagePath) {
        Logger.error(TAG,"sendImageMessage(): imagePath: "+imagePath);
//        FlurryAgent.logEvent("Image Message");
        CCAnalyticsHelper.logFeatureEvent("Image Message");
        OneOnOneMessage message = createNewOneOnOneMessage(MessageTypeKeys.IMAGE_MESSAGE, imagePath);
        addMessage(message);

        sendImage(message);
    }

    private void sendImage(final OneOnOneMessage message) {
        cometChat.sendImage(message.getId(),new File(message.message),String.valueOf(contactId),false, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error(TAG,"sendImageMessage sendResponse "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
//                    OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        if(cometChat.isMessageinPendingDeliveredList(String.valueOf(id))){
                            message.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                        }else {
                            message.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_SENT;
                        }
                        message.retryCount = 3;
                        message.save();
                        Logger.error(TAG,"sendImageMessage mess "+message);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                            }
                        });

                    }
                } catch (Exception e) {
                    Logger.error(TAG, "successCallback: ex: "+e.getLocalizedMessage());
                    e.printStackTrace();
                }

            }

            @Override
            public void failCallback(JSONObject responce) {
                Logger.error(TAG, "sendImageMessage fail responce = " + responce);
            }
        });
    }

    private void sendImageMessage(Uri imagePath) {

       /* OneOnOneMessage message = createNewOneOnOneMessage(MessageTypeKeys.IMAGE_MESSAGE, imagePath.toString());
        addMessage(message);*/

        try {
            InputStream image_stream = getContentResolver().openInputStream(fileUri);
            Bitmap bitmap= BitmapFactory.decodeStream(image_stream );
            sendBitmap(bitmap,imagePath.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void sendBitmap(Bitmap bitmap,String path) {
        OneOnOneMessage message = createNewOneOnOneMessage(MessageTypeKeys.IMAGE_MESSAGE, path);
        addMessage(message);
        cometChat.sendImage(message.getId(),bitmap,String.valueOf(contactId),false, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error(TAG,"sendResponse Image "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                    OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                    if (mess != null) {
                        mess.remoteId = id;
                        mess.messageStatus = 1;
                        if(cometChat.isMessageinPendingDeliveredList(String.valueOf(id))){
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                        }else {
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_SENT;
                        }
                        mess.save();
                        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
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


    private void sendStickerMessage(String messagedata) {
        OneOnOneMessage message = createNewOneOnOneMessage(MessageTypeKeys.STICKER, messagedata);
        addMessage(message);

        sendSticker(message);

    }

    private void sendSticker(OneOnOneMessage message) {
//        FlurryAgent.logEvent("Sticker");
        CCAnalyticsHelper.logFeatureEvent("Sticker Message");
        cometChat.sendSticker(message.getId(),message.message, String.valueOf(contactId),false, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error(TAG,"sendResponse Sticker "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                    OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                    if (mess != null) {
                        mess.remoteId = id;
                        mess.messageStatus = 1;
                        if(cometChat.isMessageinPendingDeliveredList(String.valueOf(id))){
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                        }else {
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_SENT;
                        }
                        mess.save();
                        getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
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


    private void sendVideoMessage(String videoPath) {
        OneOnOneMessage message = createNewOneOnOneMessage(MessageTypeKeys.VIDEO_MESSAGE, videoPath);
        addMessage(message);
        sendVideo(message);
    }

    private void sendVideo(OneOnOneMessage message) {
//        FlurryAgent.logEvent("Video Message");
        CCAnalyticsHelper.logFeatureEvent("Video Message");
        cometChat.sendVideo(message.getId(),message.message,String.valueOf(contactId),false,new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error(TAG, "sendResponse Video "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
                    OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                    if (mess != null) {
                        mess.remoteId = id;
                        mess.messageStatus = 1;
                        if(cometChat.isMessageinPendingDeliveredList(String.valueOf(id))){
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                        }else {
                            mess.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_SENT;
                        }
                        mess.save();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                            }
                        });

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "Send Video fail responce = " + jsonObject);
            }
        });
    }

    private void sendAudioMessage(String audioPath) {
        File audioFile = new File(audioPath);
        if (audioFile.exists()) {
            OneOnOneMessage message = createNewOneOnOneMessage(MessageTypeKeys.AUDIO_MESSAGE, audioPath);
            addMessage(message);

            sendAudioNote(message);
        }
    }

    private void sendAudioNote(final OneOnOneMessage message) {
        Logger.error(TAG, "sendAudioNote: contactId: "+contactId);
        cometChat.sendAudioFile(message.getId(),new File(message.message),String.valueOf(contactId),false, new Callbacks() {
            @Override
            public void successCallback(JSONObject sendResponse) {
                try {
                    Logger.error("sendResponse Audio "+sendResponse);
                    long localMessId = sendResponse.getLong(JsonParsingKeys.LOCAL_MESSAGE_ID);
                    Long id = sendResponse.getLong(CometChatKeys.AjaxKeys.ID);
//                    OneOnOneMessage mess = OneOnOneMessage.findByLocalId(localMessId);
                    if (message != null) {
                        message.remoteId = id;
                        message.messageStatus = 1;
                        message.retryCount = 3;
                        if(cometChat.isMessageinPendingDeliveredList(String.valueOf(id))){
                            message.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD;
                        }else {
                            message.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_SENT;
                        }
                        message.save();
                        Logger.error(TAG, "successCallback: sendAudioNote message: "+message);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                getSupportLoaderManager().restartLoader(MESSAGE_LOADER, null, CCSingleChatActivity.this);
                            }
                        });
                    }
                } catch (Exception e) {
                    Logger.error(TAG, "successCallback: exception: "+e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG, "send Audio file fail responce = " + jsonObject);
            }
        });
    }

    private void imageUpload() {
        /*Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(StaticMembers.IMAGE_TYPE);
        startActivityForResult(intent, StaticMembers.CHOOSE_IMAGE_REQUEST_ONE_ON_ONE);*/
        Intent imagePickerIntent;
        if (CommonUtils.isSamsung()) {
            imagePickerIntent = new Intent(Intent.ACTION_PICK);
        } else {
            imagePickerIntent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        imagePickerIntent.setType(StaticMembers.IMAGE_TYPE);
        startActivityForResult(imagePickerIntent, StaticMembers.CHOOSE_IMAGE_REQUEST_ONE_ON_ONE);
    }

    private void videoUpload() {
        Intent videoPickerIntent;
        videoPickerIntent = new Intent(Intent.ACTION_PICK);
        videoPickerIntent.setType(StaticMembers.VIDEO_TYPE);
        startActivityForResult(videoPickerIntent, StaticMembers.CHOOSE_VIDEO_REQUEST_ONE_ON_ONE);
    }

    private OneOnOneMessage  createNewOneOnOneMessage(String type, String messagedata) {
        OneOnOneMessage message = new OneOnOneMessage();
        message.remoteId = 0L;
        message.fromId = SessionData.getInstance().getId();
        message.toId = contactId;
        message.message = messagedata;
        message.imageUrl = "";
        message.sentTimestamp = System.currentTimeMillis();
        message.type = type;
        message.read = 1;
        message.self = 1;
        message.insertedBy = 1;
        message.messagetick = CometChatKeys.MessageTypeKeys.MESSAGE_PENDING;
        message.messageStatus = 0;
        return message;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection;
        switch (id) {

            case MESSAGE_LOADER:
                selection = OneOnOneMessage.getAllMessagesQuery(String.valueOf(sessionData.getId()), String.valueOf(contactId));
                return new DataCursorLoader(this, selection, null);

            default:
                return null;

        }
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorData = data;
        messageCount = data.getCount();
        Logger.error(TAG,"Message count = "+messageCount);

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

        if (messageAdapter == null) {
            messageAdapter = new OneOnOneMessageAdapter(CCSingleChatActivity.this, data,contactId);
            messageRecyclerView.setAdapter(messageAdapter);
            decor = new StickyHeaderDecoration(messageAdapter);
            messageRecyclerView.addItemDecoration(decor, 0);
        }

        if(showIsTyping){
            MatrixCursor matrixCursor = new MatrixCursor(new String[] { "remote_id","from_id","to_id","sent","id","from","to","message","self",
                    "image_url","inserted_by","messagetick","messagetype","read","messagestatus"
            });
            matrixCursor.addRow(new Object[] { 0L,contactId,SessionData.getInstance().getId(),
                    System.currentTimeMillis(),0,contactId,SessionData.getInstance().getId(),"Typing",0,"",1,1,100,1,1 });
            MergeCursor mergeCursor = new MergeCursor(new Cursor[] { data, matrixCursor });

            messageAdapter.swapCursor(mergeCursor);
        }else{
            messageAdapter.swapCursor(data);
        }

        if(data.getCount()>0){
            if(!isInitialLoad){
                if( btnScroll.getVisibility() != View.VISIBLE ){

                    if(!showIsTyping){
                        messageRecyclerView.smoothScrollToPosition(data.getCount() - 1);
                    }else{
                        messageRecyclerView.smoothScrollToPosition(data.getCount());
                    }

                    if(!TextUtils.isEmpty(contact.cometid)){
                        data.moveToLast();
                        sendReadReceitMessage(data.getLong(data.getColumnIndex(OneOnOneMessage.COLUMN_REMOTE_ID)));
                    }
                }
                else{
                    if(newMessageCount>0){
                        btnScroll.setText(newMessageCount+" New Message ");
                        btnScroll.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
                    }
                }
            }else{
                messageRecyclerView.scrollToPosition(data.getCount() - 1);
                isInitialLoad = false;
                data.moveToLast();
                sendReadReceitMessage(data.getLong(data.getColumnIndex(OneOnOneMessage.COLUMN_REMOTE_ID)));
            }

            data.moveToFirst();
            long firstMessageId = data.getLong(data.getColumnIndex(OneOnOneMessage.COLUMN_REMOTE_ID));
            firstMessageID = firstMessageId;
        }else{
            btnScroll.setVisibility(View.GONE);
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
    public void onBackPressed() {
        if(smiliKeyBoard.isKeyboardVisibile()){
            smiliKeyBoard.dismissKeyboard();
        }else if(!setBackButton) {
            cometChat.sendCloseCCWindowResponce();
            MessageSDK.closeCometChatWindow(CCSingleChatActivity.this, ccContainer);
        }else{
            super.onBackPressed();
        }
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

    private void animateCustomMenu() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleCustomMenu = ObjectAnimator.ofFloat(customMenu, "translationY", 100.0f,0.0f).setDuration(250);
        animatorSet.play(scaleCustomMenu);
        animatorSet.start();
    }

    private void sendReadReceitMessage(long remoteId){
        cometChat.sendReadReceipt(String.valueOf(remoteId),contact.cometid, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG,"sendReadReceipt success = "+jsonObject);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"sendReadReceipt fail = "+jsonObject);
            }
        });
    }

    @Override
    public void onRetryClicked(long localMessageId) {
        Logger.error(TAG, "onRetryClicked: "+localMessageId );
        OneOnOneMessage oneOnOneMessage = OneOnOneMessage.findByLocalId(String.valueOf(localMessageId));
        Logger.error(TAG, "onRetryClicked: message: "+oneOnOneMessage);
        if (oneOnOneMessage != null) {
            switch (oneOnOneMessage.type){
                case MessageTypeKeys.EMOJI_MESSAGE:
                case MessageTypeKeys.NORMAL_MESSAGE:
                    sendTextMessage(oneOnOneMessage);
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    sendImage(oneOnOneMessage);
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    sendVideo(oneOnOneMessage);
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    sendAudioNote(oneOnOneMessage);
                    break;
                case MessageTypeKeys.STICKER:
                    sendSticker(oneOnOneMessage);
                    break;
                default:
                    Logger.error(TAG, "onRetryClicked: incorrect message type");
                    break;
            }
        }else {
            Logger.error(TAG, "onRetryClicked: message not present in database");
        }
    }


    private void sendStatusPingMessage(){
        cometChat.sendStatusPing(Status.online,contact.cometid, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG,"Statusn ping success = "+jsonObject);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"Statusn ping fail = "+jsonObject);
            }
        });
    }
}
