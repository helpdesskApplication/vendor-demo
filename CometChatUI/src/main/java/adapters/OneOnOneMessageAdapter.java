package adapters;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.activities.CCWebViewActivity;
import com.inscripts.custom.RoundedImageView;
import com.inscripts.custom.StickyHeaderAdapter;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.RecyclerViewCursorAdapter;
import com.inscripts.glide.Glide;
import com.inscripts.glide.load.DataSource;
import com.inscripts.glide.load.engine.GlideException;
import com.inscripts.glide.request.RequestListener;
import com.inscripts.glide.request.RequestOptions;
import com.inscripts.glide.request.target.Target;
import com.inscripts.helpers.FileOpenIntentHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.IntentExtraKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.plugins.Stickers;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;
import com.inscripts.custom.CCChromeTabs;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Keys.BroadCastReceiverKeys;
import Keys.MessageTypeKeys;
import activities.CCGroupChatActivity;
import activities.CCImagePreviewActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import helpers.FileDownloadHelper;
import models.Bot;
import models.Contact;
import models.Groups;
import models.OneOnOneMessage;
import videochat.CCVideoChatActivity;
import viewHolders.CallViewHolder;
import viewHolders.LeftAudioViewHolder;
import viewHolders.LeftImageVideoViewHolder;
import viewHolders.LeftMessageViewHolder;
import viewHolders.RightAudioViewHolder;
import viewHolders.RightImageVideoViewHolder;
import viewHolders.RightMessageViewHolder;

public class OneOnOneMessageAdapter extends RecyclerViewCursorAdapter<RecyclerView.ViewHolder> implements
        StickyHeaderAdapter<OneOnOneMessageAdapter.DateItemHolder> {

    private static final java.lang.String TAG = OneOnOneMessageAdapter.class.getSimpleName();
    private static Context context;
    private Cursor cursor;
//    private static final int TYPE_LEFT = 0;
//    private static final int TYPE_RIGHT = 1;
    private static final int TYPE_FOOTER = 2;
    private static final int RIGHT_TEXT_MESSAGE = 334;
    private static final int LEFT_TEXT_MESSAGE = 734;
    private static final int LEFT_EMOJI_MESSAGE = 240;
    private static final int RIGHT_EMOJI_MESSAGE = 576;
    private static final int RIGHT_IMAGE_DOWNLOADING_MESSAGE = 693;
    private static final int LEFT_IMAGE_DOWNLOADING_MESSAGE = 272;
    private static final int LEFT_IMAGE_MESSAGE = 528;
    private static final int RIGHT_IMAGE_MESSAGE = 834;
    private static final int LEFT_VIDEO_MESSAGE = 580;
    private static final int RIGHT_VIDEO_MESSAGE = 797;
    private static final int LEFT_VIDEO_DOWNLOADING_MESSAGE = 583;
    private static final int RIGHT_VIDEO_DOWNLOADING_MESSAGE = 179;
    private static final int RIGHT_HANDWRITE_DOWNLOADING_MESSAGE = 62;
    private static final int LEFT_HANDWRITE_DOWNLOADING_MESSAGE = 427;
    private static final int RIGHT_HANDWRITE_MESSAGE = 764;
    private static final int LEFT_HANDWRITE_MESSAGE = 473;
    private static final int LEFT_AUDIO_DOWNLOADING_MESSAGE = 351;
    private static final int RIGHT_AUDIO_DOWNLOADING_MESSAGE = 208;
    private static final int LEFT_AUDIO_MESSAGE = 416;
    private static final int RIGHT_AUDIO_MESSAGE = 756;
    private static final int LEFT_STICKER_MESSAGE = 173;
    private static final int RIGHT_STICKER_MESSAGE = 521;
    private static final int RIGHT_WHITEBOARD_MESSAGE = 788;
    private static final int LEFT_WHITEBOARD_MESSAGE = 597;
    private static final int WRITEBOARD_MESSAGE = 29;
    private static final int GROUP_INVITE_MESSAGE = 389;
    private static final int LEFT_SCREENSHARE_MESSAGE = 846;
    private static final int RIGHT_SCREENSHARE_MESSAGE = 307;
    private static final int BOT_MESSAGE = 183;
    private static final int AVBROADCAST_REQUEST = 730;
    private static final int RIGHT_AVBROADCAST_REQUEST = 677;
    private static final int AVBROADCAST_EXPIRED = 898;
    private static final int AVCHAT_INCOMING_CALL = 568;
    private static final int AVCHAT_CALL_ACCEPTED = 6;
    private static final int AVCHAT_INCOMING_CALL_END = 301;
    private static final int AVCHAT_BUSY_CALL = 590;
    private static final int LEFT_FILE_MESSAGE = 320;
    private static final int LEFT_FILE_MESSAGE_DOWNLOADING = 659;
    private static final int LEFT_FILE_MESSAGE_DOWNLOADED = 731;
    private static final int RIGHT_FILE_MESSAGE = 340;
    private static final int RIGHT_FILE_MESSAGE_DOWNLOADING = 560;
    private static final int RIGHT_FILE_MESSAGE_DOWNLOADED = 114;
    private static LongSparseArray<Bitmap> videoThumbnails;
    private static LongSparseArray<Integer> audioDurations;
    private Long currentlyPlayingId = 0l;
    private MediaPlayer player;
    private String currentPlayingSong = "";
    private Runnable timerRunnable;
    private Handler seekHandler = new Handler();
    private CometChat cometChat;
    private long conatctID ;
    private String channel;
    private String avatarUrl="";
    private int color;
    private FeatureState writeBoardState;
    private FeatureState whiteBoardState;
    private FeatureState groupState;
    private FeatureState screenshareState;
    private FeatureState avBroadcaseState;
//    private RightMessageViewHolder rightMessageViewHolder;

    public OneOnOneMessageAdapter(Context context, Cursor c , long contactID) {
        super(c);
        this.context = context;
        this.cursor = c;
        videoThumbnails = new LongSparseArray<>();
        audioDurations = new LongSparseArray<>();
        player = CommonUtils.getPlayerInstance();
        cometChat = CometChat.getInstance(context);
        color = (int) CometChat.getInstance(context).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        Contact contact = Contact.getContactDetails(contactID);
        if (contact != null) {
            avatarUrl = contact.avatarURL;
        }
        initializeFeatureState();
    }

    private void initializeFeatureState() {
        whiteBoardState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.WHITEBOARD_ENABLED));
        writeBoardState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.WRITEBOARD_ENABLED));
        groupState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GROUP_CHAT_ENABLED));
        screenshareState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.SCREENSHARE_ENABLED));
        avBroadcaseState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.AV_BROADCAST_ENABLED));
    }


    @Override
    public void swapCursor(Cursor newCursor) {
        super.swapCursor(newCursor);
        this.cursor = newCursor;
    }

    @Override
    public int getItemViewType(int position) {
        OneOnOneMessage message = getMessageByPosition(position);
        Logger.error(TAG, "getItemViewType: messageType: "+cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TYPE)));
        Logger.error(TAG, "getItemViewType: isSelfMessage: "+message.self);
        int viewType = 0;
        if(message.type.equals("100")){
            viewType = TYPE_FOOTER;
        }
        else if (0 == message.self) { //For Received Messages
            switch (cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TYPE))){
                case MessageTypeKeys.NORMAL_MESSAGE:
                    viewType = LEFT_TEXT_MESSAGE;
                    break;
                case MessageTypeKeys.EMOJI_MESSAGE:
                    viewType = LEFT_EMOJI_MESSAGE;
                    break;
                case MessageTypeKeys.IMAGE_DOWNLOADING:
                    viewType = LEFT_IMAGE_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    viewType = LEFT_IMAGE_MESSAGE;
                    break;
                case MessageTypeKeys.VIDEO_DOWNLOADING:
                    viewType = LEFT_VIDEO_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    viewType = LEFT_VIDEO_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_DOWNLOADING:
                    viewType = LEFT_HANDWRITE_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_MESSAGE:
                    viewType = LEFT_HANDWRITE_MESSAGE;
                    break;
                case MessageTypeKeys.AUDIO_DOWNLOADING:
                    viewType = LEFT_AUDIO_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    viewType = LEFT_AUDIO_MESSAGE;
                    break;
                case MessageTypeKeys.STICKER:
                    viewType = LEFT_STICKER_MESSAGE;
                    break;
                case MessageTypeKeys.WHITEBOARD_MESSAGE:
                    viewType = LEFT_WHITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.WRITEBOARD_MESSAGE:
                    viewType = WRITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.GROUP_INVITE:
                    viewType = GROUP_INVITE_MESSAGE;
                    break;
                case MessageTypeKeys.SCREENSHARE_MESSAGE:
                    viewType = LEFT_SCREENSHARE_MESSAGE;
                    break;
                case MessageTypeKeys.BOT_RESPONSE:
                    viewType = BOT_MESSAGE;
                    break;
                case MessageTypeKeys.AVBROADCAST_REQUEST:
                    viewType = AVBROADCAST_REQUEST;
                    break;
                case MessageTypeKeys.AVBROADCAST_EXPIRED:
                    viewType = AVBROADCAST_EXPIRED;
                    break;
                case MessageTypeKeys.AVCHAT_INCOMING_CALL:
                    viewType = AVCHAT_INCOMING_CALL;
                    break;
                case MessageTypeKeys.AVCHAT_CALL_ACCEPTED:
                    viewType = AVCHAT_CALL_ACCEPTED;
                    break;
                case MessageTypeKeys.AVCHAT_INCOMING_CALL_END:
                    viewType = AVCHAT_INCOMING_CALL_END;
                    break;
                case MessageTypeKeys.AVCHAT_BUSY_CALL:
                    viewType = AVCHAT_BUSY_CALL;
                    break;
                case MessageTypeKeys.FILE_MESSAGE:
                    viewType = LEFT_FILE_MESSAGE;
                    break;
                case MessageTypeKeys.FILE_DOWNLOADED:
                    viewType = LEFT_FILE_MESSAGE_DOWNLOADED;
                    break;
            }
        }else { // For Sent Messages
            switch (cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TYPE))){
                case MessageTypeKeys.NORMAL_MESSAGE:
                    viewType = RIGHT_TEXT_MESSAGE;
                    break;
                case MessageTypeKeys.EMOJI_MESSAGE:
                    viewType = RIGHT_EMOJI_MESSAGE;
                    break;
                case MessageTypeKeys.IMAGE_DOWNLOADING:
                    viewType = RIGHT_IMAGE_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.IMAGE_MESSAGE:
                    viewType = RIGHT_IMAGE_MESSAGE;
                    break;
                case MessageTypeKeys.VIDEO_DOWNLOADING:
                    viewType = RIGHT_VIDEO_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.VIDEO_MESSAGE:
                    viewType = RIGHT_VIDEO_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_DOWNLOADING:
                    viewType = RIGHT_HANDWRITE_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_MESSAGE:
                    viewType = RIGHT_HANDWRITE_MESSAGE;
                    break;
                case MessageTypeKeys.AUDIO_DOWNLOADING:
                    viewType = RIGHT_AUDIO_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    viewType = RIGHT_AUDIO_MESSAGE;
                    break;
                case MessageTypeKeys.STICKER:
                    viewType = RIGHT_STICKER_MESSAGE;
                    break;
                case MessageTypeKeys.WHITEBOARD_MESSAGE:
                    viewType = RIGHT_WHITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.AVCHAT_INCOMING_CALL_END:
                    viewType = AVCHAT_INCOMING_CALL_END;
                    break;
                case MessageTypeKeys.SCREENSHARE_MESSAGE:
                    viewType = RIGHT_SCREENSHARE_MESSAGE;
                    break;

                case MessageTypeKeys.GROUP_INVITE:
                    viewType = GROUP_INVITE_MESSAGE;
                    break;

            }
        }
        Logger.error(TAG, "getItemViewType: viewType: "+viewType);
        return viewType;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder messageViewHolder = null;
         switch (viewType){
             case TYPE_FOOTER:
                 final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_chat_message_one_on_one_footer, parent, false);
                 messageViewHolder = new TypingViewHolder(view);
                 break;
             case RIGHT_TEXT_MESSAGE:
                 View rightTextMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                 messageViewHolder = new RightMessageViewHolder(rightTextMessageView);
                 break;
             case LEFT_TEXT_MESSAGE:
                 View leftTextMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(leftTextMessageView);
                 break;
             case RIGHT_EMOJI_MESSAGE:
                 View rightEmojiMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right,parent,false);
                 messageViewHolder = new RightMessageViewHolder(rightEmojiMessageView);
                 break;
             case LEFT_EMOJI_MESSAGE:
                 View leftEmojiMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(leftEmojiMessage);
                 break;
             case LEFT_IMAGE_DOWNLOADING_MESSAGE:
                 View leftImageDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,leftImageDownloadingMessageView);
                 break;
             case RIGHT_IMAGE_DOWNLOADING_MESSAGE:
                 View rightImageDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                 messageViewHolder = new RightImageVideoViewHolder(context,rightImageDownloadingMessageView);
                 break;
             case LEFT_IMAGE_MESSAGE:
                 View leftImageMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,leftImageMessageView);
                 break;
             case RIGHT_IMAGE_MESSAGE:
                 View rightImageMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                 messageViewHolder = new RightImageVideoViewHolder(context,rightImageMessageView);
                 break;
             case RIGHT_VIDEO_DOWNLOADING_MESSAGE:
                 View rightVideoDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                 messageViewHolder = new RightImageVideoViewHolder(context,rightVideoDownloadingMessageView);
                 break;
             case RIGHT_VIDEO_MESSAGE:
                 View rightVideoMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                 messageViewHolder = new RightImageVideoViewHolder(context,rightVideoMessageView);
                 break;
             case LEFT_VIDEO_DOWNLOADING_MESSAGE:
                 View leftVideoDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,leftVideoDownloadingMessageView);
                 break;
             case LEFT_VIDEO_MESSAGE:
                 View leftVideoMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,leftVideoMessageView);
                 break;
             case LEFT_HANDWRITE_DOWNLOADING_MESSAGE:
                 View leftHandwriteDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,leftHandwriteDownloadingMessageView);
                 break;
             case LEFT_HANDWRITE_MESSAGE:
                 View leftHandwriteMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,leftHandwriteMessageView);
                 break;
             case RIGHT_HANDWRITE_DOWNLOADING_MESSAGE:
                 View rightHandwriteDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                 messageViewHolder = new RightImageVideoViewHolder(context,rightHandwriteDownloadingMessageView);
                 break;
             case RIGHT_HANDWRITE_MESSAGE:
                 View rightHandwriteMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                 messageViewHolder = new RightImageVideoViewHolder(context,rightHandwriteMessageView);
                 break;
             case LEFT_AUDIO_DOWNLOADING_MESSAGE:
                 View leftAudioDownloadingMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_left, parent, false);
                 messageViewHolder = new LeftAudioViewHolder(context,leftAudioDownloadingMessageView);
                 break;
             case LEFT_AUDIO_MESSAGE:
                 View leftAudioMessageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_left, parent, false);
                 messageViewHolder = new LeftAudioViewHolder(context,leftAudioMessageView);
                 break;
             case RIGHT_AUDIO_DOWNLOADING_MESSAGE:
                 View rightAudioDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_right, parent, false);
                 messageViewHolder = new RightAudioViewHolder(context,rightAudioDownloadingMessage);
                 break;
             case RIGHT_AUDIO_MESSAGE:
                 View rightAudioMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_right, parent, false);
                 messageViewHolder = new RightAudioViewHolder(context,rightAudioMessage);
                 break;
             case RIGHT_STICKER_MESSAGE:
                 View rightStickerMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                 messageViewHolder = new RightMessageViewHolder(rightStickerMessage);
                 break;
             case LEFT_STICKER_MESSAGE:
                 View leftStickerMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(leftStickerMessage);
                 break;
             case LEFT_WHITEBOARD_MESSAGE:
                 View leftWhiteBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(leftWhiteBoardMessage);
                 break;
             case RIGHT_WHITEBOARD_MESSAGE:
                 View rightWhiteBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                 messageViewHolder = new RightMessageViewHolder(rightWhiteBoardMessage);
                 break;
             case WRITEBOARD_MESSAGE:
                 View writeBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(writeBoardMessage);
                 break;
             case GROUP_INVITE_MESSAGE:
                 View groupInviteMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(groupInviteMessage);
                 break;
             case LEFT_SCREENSHARE_MESSAGE:
                 View leftScreenShareMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(leftScreenShareMessage);
                 break;
             case RIGHT_SCREENSHARE_MESSAGE:
                 View rightScreenShareMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                 messageViewHolder = new RightMessageViewHolder(rightScreenShareMessage);
                 break;
             case BOT_MESSAGE:
                 View botMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                 messageViewHolder = new LeftImageVideoViewHolder(context,botMessage);
                 break;
             case AVBROADCAST_REQUEST:
                 View avBroadcastInviteMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(avBroadcastInviteMessage);
                 break;
             case AVBROADCAST_EXPIRED:
                 View avBroadcastExpiredMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(avBroadcastExpiredMessage);
                 break;
             case AVCHAT_INCOMING_CALL:
                 View avChatIncomingCall = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_call_layout, parent, false);
                 messageViewHolder = new CallViewHolder(avChatIncomingCall);
                 break;
             case AVCHAT_CALL_ACCEPTED:
                 View avChatCallAccepted = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_call_layout, parent, false);
                 messageViewHolder = new CallViewHolder(avChatCallAccepted);
                 break;
             case AVCHAT_INCOMING_CALL_END:
                 View avChatIncomingCallEnd = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_call_layout, parent, false);
                 messageViewHolder = new CallViewHolder(avChatIncomingCallEnd);
                 break;
             case AVCHAT_BUSY_CALL:
                 View avChatBusyCall = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_call_layout, parent, false);
                 messageViewHolder = new CallViewHolder(avChatBusyCall);
                 break;
             case LEFT_FILE_MESSAGE:
                 View fileMessageHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(fileMessageHolder);
                 break;
             case LEFT_FILE_MESSAGE_DOWNLOADED:
                 View fileDownloadedHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                 messageViewHolder = new LeftMessageViewHolder(fileDownloadedHolder);
                 break;

             default:
                 break;
         }

        return messageViewHolder;
    }
//  ViewHolder classes
    class TypingViewHolder extends RecyclerView.ViewHolder{
        RoundedImageView avatar;
        TypingViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.imageViewUserAvatar);
        }
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final Cursor cursor) {
        int self = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SELF));
        long fromId = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_FROM_ID));
        int insertedBy = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SELF));
        final String message = cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE));
        final long sentTimestamp = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SENT_TIMESTAMP));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_IMAGE_URL));
        final int messagetick = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TICK));
        boolean showticks = (boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.SHOW_TICKS));
        final int messageStatus = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_STATUS));
        long remoteId = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_REMOTE_ID));
        final long localId = cursor.getLong(cursor.getColumnIndex("ID"));
        Logger.error(TAG, "onBindViewHolder: imageUrl: "+imageUrl);
        conatctID = fromId;
        channel = imageUrl;
        Logger.error(TAG, "onBindViewHolder: itemType: "+holder.getItemViewType());
        /*switch (holder.getItemViewType()){
            case RIGHT_TEXT_MESSAGE:
                rightMessageViewHolder = (RightMessageViewHolder) holder;
                rightMessageViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Logger.error(TAG, "onClick: retry");
                    }
                });
        }*/
        switch (holder.getItemViewType()){
            case TYPE_FOOTER:
                TypingViewHolder typingViewHolder = (TypingViewHolder) holder;
                if(!TextUtils.isEmpty(avatarUrl)){
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,typingViewHolder.avatar,R.drawable.cc_default_avatar);
                }
                break;
            case RIGHT_TEXT_MESSAGE:
                final RightMessageViewHolder rightMessageViewHolder = (RightMessageViewHolder) holder;
                rightMessageViewHolder.textMessage.setText(message);
                rightMessageViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightMessageViewHolder.rightArrow.setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightMessageViewHolder.textMessage.getBackground().setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                Logger.error(TAG, "onBindViewHolder: RIGHT_TEXT_MESSAGE: messageStatus: "+messageStatus );
                Logger.error(TAG, "onBindViewHolder: RIGHT_TEXT_MESSAGE: messageTick: "+messagetick );
                rightMessageViewHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightMessageViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightMessageViewHolder.retry.setVisibility(View.VISIBLE);
                    rightMessageViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback)context).onRetryClicked(localId);
                            }
                            rotateRetry(rightMessageViewHolder.retry,1);
                        }
                    });
                    break;
                }else if(messageStatus != 0){
                    rightMessageViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }

                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightMessageViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightMessageViewHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightMessageViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightMessageViewHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightMessageViewHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightMessageViewHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                break;
            case LEFT_TEXT_MESSAGE:
                LeftMessageViewHolder leftMessageViewHolder = (LeftMessageViewHolder) holder;
                leftMessageViewHolder.textMessage.setText(message);
                leftMessageViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftMessageViewHolder.senderName.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftMessageViewHolder.avatar,R.drawable.cc_default_avatar);
                }
                break;

            case RIGHT_EMOJI_MESSAGE:
                final RightMessageViewHolder rightEmojiMessageViewHolder = (RightMessageViewHolder) holder;
                rightEmojiMessageViewHolder.rightArrow.setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightEmojiMessageViewHolder.textMessage.getBackground().setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightEmojiMessageViewHolder.textMessage.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                rightEmojiMessageViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                Logger.error(TAG, "onBindViewHolder: RIGHT_EMOJI_MESSAGE: messageStatus: "+messageStatus);
                rightEmojiMessageViewHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightEmojiMessageViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightEmojiMessageViewHolder.retry.setVisibility(View.VISIBLE);
                    rightEmojiMessageViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback)context).onRetryClicked(localId);
                            }
                            rotateRetry(rightEmojiMessageViewHolder.retry,1);
                        }
                    });
                    break;
                }else if(messageStatus != 0){
                    rightEmojiMessageViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightEmojiMessageViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightEmojiMessageViewHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightEmojiMessageViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightEmojiMessageViewHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightEmojiMessageViewHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightEmojiMessageViewHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                break;

            case LEFT_EMOJI_MESSAGE:
                LeftMessageViewHolder leftEmojiMessageViewHolder = (LeftMessageViewHolder) holder;
                leftEmojiMessageViewHolder.senderName.setVisibility(View.GONE);
                leftEmojiMessageViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftEmojiMessageViewHolder.textMessage.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftEmojiMessageViewHolder.avatar,R.drawable.cc_default_avatar);
                }
                break;

            case LEFT_IMAGE_DOWNLOADING_MESSAGE:
                final LeftImageVideoViewHolder leftImageDownloadingViewHolder = (LeftImageVideoViewHolder) holder;
                leftImageDownloadingViewHolder.senderName.setVisibility(View.GONE);
                leftImageDownloadingViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftImageDownloadingViewHolder.imageTitle.setVisibility(View.GONE);
                leftImageDownloadingViewHolder.btnPlayVideo.setVisibility(View.GONE);
                if(!TextUtils.isEmpty(avatarUrl)){
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftImageDownloadingViewHolder.avatar,R.drawable.cc_default_avatar);
                }

                RequestOptions leftImageDownloadingOptions = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.ic_broken_image);
                Logger.error(TAG, "LEFT_IMAGE_DOWNLOADING_MESSAGE : imageUrl: "+imageUrl);
                Glide.with(context)
                        .load(imageUrl)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object o, Target<Drawable> target, boolean b) {
                                Logger.error(TAG, "Glide onLoadFailed: " + e);
                                leftImageDownloadingViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target, DataSource dataSource, boolean b) {
                                Logger.error(TAG, "Glide onResourceReady: called");
                                leftImageDownloadingViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }
                        }).apply(leftImageDownloadingOptions).into(leftImageDownloadingViewHolder.imageMessage);

                final String leftImageDownloadingMessage = message;
                Logger.error(TAG, "onBindViewHolder: LEFT_IMAGE_DOWNLOADING_MESSAGE : leftImageMessage: "+leftImageDownloadingMessage);
                leftImageDownloadingViewHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
//                            leftImageDownloadingViewHolder.imageMessage.setEnabled(false);
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            Logger.error(TAG, "Adapter : clicked_position : " + cursor.getPosition());
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, leftImageDownloadingMessage);
                            ((Activity) context).startActivityForResult(intent, 1111);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                break;

            case RIGHT_IMAGE_DOWNLOADING_MESSAGE:
                final RightImageVideoViewHolder rightImageDownloadingViewHolder = (RightImageVideoViewHolder) holder;
                rightImageDownloadingViewHolder.rightArrow.setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightImageDownloadingViewHolder.imageContainer.getBackground().setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightImageDownloadingViewHolder.btnPlayVideo.setVisibility(View.GONE);
                rightImageDownloadingViewHolder.imageTitle.setVisibility(View.GONE);
                rightImageDownloadingViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if(messageStatus != 0){
                    rightImageDownloadingViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightImageDownloadingViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightImageDownloadingViewHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightImageDownloadingViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightImageDownloadingViewHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightImageDownloadingViewHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightImageDownloadingViewHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }

                RequestOptions rightImageDownloadingOptions = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.ic_broken_image);
                Logger.error(TAG, "RIGHT_IMAGE_DOWNLOADING_MESSAGE : imageUrl: "+imageUrl);
                Glide.with(context)
                        .load(imageUrl)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object o, Target<Drawable> target, boolean b) {
                                Logger.error(TAG, "Glide onLoadFailed: " + e);
                                rightImageDownloadingViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target, DataSource dataSource, boolean b) {
                                Logger.error(TAG, "Glide onResourceReady: called");
                                rightImageDownloadingViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }
                        }).apply(rightImageDownloadingOptions).into(rightImageDownloadingViewHolder.imageMessage);

                final String rightImageDownloadingMessage = message;
                Logger.error(TAG, "onBindViewHolder: RIGHT_IMAGE_DOWNLOADING_MESSAGE : rightImageDownloadingMessage: "+rightImageDownloadingMessage);
                rightImageDownloadingViewHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
//                            rightImageDownloadingViewHolder.imageMessage.setEnabled(false);
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            Logger.error(TAG, "Adapter : clicked_position : " + cursor.getPosition());
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, rightImageDownloadingMessage);
                            ((Activity) context).startActivityForResult(intent, 1111);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                break;

            case LEFT_IMAGE_MESSAGE:
                final LeftImageVideoViewHolder leftImageViewHolder = (LeftImageVideoViewHolder) holder;
                leftImageViewHolder.senderName.setVisibility(View.GONE);
                leftImageViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftImageViewHolder.btnPlayVideo.setVisibility(View.GONE);
                leftImageViewHolder.imageTitle.setVisibility(View.GONE);
                leftImageViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                if(!TextUtils.isEmpty(avatarUrl)){
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftImageViewHolder.avatar,R.drawable.cc_default_avatar);
                }
                LocalStorageFactory.loadImage(context,message,leftImageViewHolder.imageMessage,R.drawable.ic_broken_image);

                leftImageViewHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
//                            leftImageViewHolder.imageMessage.setEnabled(false);
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            Logger.error(TAG, "Adapter : clicked_position : " + cursor.getPosition());
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                            ((Activity)context).startActivityForResult(intent, 1111);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                break;

            case RIGHT_IMAGE_MESSAGE:
                final RightImageVideoViewHolder rightImageViewHolder = (RightImageVideoViewHolder) holder;
                rightImageViewHolder.rightArrow.setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightImageViewHolder.imageContainer.getBackground().setColorFilter(color,PorterDuff.Mode.SRC_ATOP);
                rightImageViewHolder.btnPlayVideo.setVisibility(View.GONE);
                rightImageViewHolder.imageTitle.setVisibility(View.GONE);
                rightImageViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                rightImageViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightImageViewHolder.retry.setVisibility(View.INVISIBLE);
                Logger.error(TAG, "onBindViewHolder: RIGHT_IMAGE_MESSAGE: messageStatus: "+messageStatus );
                if (messageStatus == 2){
                    rightImageViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightImageViewHolder.retry.setVisibility(View.VISIBLE);
                    rightImageViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback)context).onRetryClicked(localId);
                            }
                            rotateRetry(rightImageViewHolder.retry,5);
                        }
                    });
                    break;
                }else if(messageStatus != 0){
                    rightImageViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    rightImageViewHolder.retry.setVisibility(View.GONE);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightImageViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightImageViewHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightImageViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightImageViewHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightImageViewHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightImageViewHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                LocalStorageFactory.loadImage(context,message,rightImageViewHolder.imageMessage,R.drawable.ic_broken_image);

                rightImageViewHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
//                            rightImageViewHolder.imageMessage.setEnabled(false);
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            Logger.error(TAG, "Adapter : clicked_position : " + cursor.getPosition());
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                            ((Activity)context).startActivityForResult(intent, 1111);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case RIGHT_VIDEO_DOWNLOADING_MESSAGE:
                RightImageVideoViewHolder rightVideoDownloadingMessageHolder = (RightImageVideoViewHolder) holder;
                rightVideoDownloadingMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightVideoDownloadingMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoDownloadingMessageHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoDownloadingMessageHolder.imageTitle.setVisibility(View.GONE);
                LocalStorageFactory.loadImage(context, message, rightVideoDownloadingMessageHolder.imageMessage, R.drawable.ic_broken_image);
                if(messageStatus != 0){
                    rightVideoDownloadingMessageHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightVideoDownloadingMessageHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightVideoDownloadingMessageHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightVideoDownloadingMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightVideoDownloadingMessageHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightVideoDownloadingMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightVideoDownloadingMessageHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                break;
            case LEFT_VIDEO_DOWNLOADING_MESSAGE:
                LeftImageVideoViewHolder leftVideoDownloadingHolder = (LeftImageVideoViewHolder) holder;
                leftVideoDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftVideoDownloadingHolder.senderName.setVisibility(View.GONE);
                leftVideoDownloadingHolder.imageTitle.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftVideoDownloadingHolder.avatar, R.drawable.cc_default_avatar);
                }
                LocalStorageFactory.loadImage(context, message, leftVideoDownloadingHolder.imageMessage, R.drawable.ic_broken_image);
                break;
            case RIGHT_VIDEO_MESSAGE:
                final RightImageVideoViewHolder rightVideoViewHolder = (RightImageVideoViewHolder) holder;
                rightVideoViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightVideoViewHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoViewHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoViewHolder.imageTitle.setVisibility(View.GONE);
                rightVideoViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                rightVideoViewHolder.retry.setVisibility(View.INVISIBLE);
                Logger.error(TAG, "onBindViewHolder: RIGHT_IMAGE_MESSAGE: messageStatus: "+messageStatus );
                if (messageStatus == 2){
                    rightVideoViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightVideoViewHolder.retry.setVisibility(View.VISIBLE);
                    rightVideoViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback)context).onRetryClicked(localId);
                            }
                            rotateRetry(rightVideoViewHolder.retry,8);
                        }
                    });
                    break;
                }else if(messageStatus != 0){
                    rightVideoViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    rightVideoViewHolder.retry.setVisibility(View.GONE);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightVideoViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightVideoViewHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightVideoViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightVideoViewHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightVideoViewHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightVideoViewHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                Logger.error(TAG, "onBindViewHolder: RIGHT_VIDEO_MESSAGE: message: "+message);
                if(message.contains("content://")){
                    try {
                       InputStream image_stream = context.getContentResolver().openInputStream(Uri.parse(message));
                        Bitmap bitmap= BitmapFactory.decodeStream(image_stream);
                        rightVideoViewHolder.imageMessage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        Logger.error(TAG,"Exception = "+e);
                        e.printStackTrace();
                    }
                }else if(new File(message).exists()){
                    if (videoThumbnails.get(sentTimestamp) == null) {
                        Cursor c = null;
                        try {
                            BitmapFactory.Options videoOptions = new BitmapFactory.Options();
                            videoOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                            videoOptions.inSampleSize = 2;
                            Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            String[] requierddata = {BaseColumns._ID};
                            c = context.getContentResolver().query(videoUri, requierddata,
                                    MediaStore.MediaColumns.DATA + " like  \"" + message + "\"", null, null);
                            c.moveToFirst();
                            if (c != null && c.getCount() != 0) {
                                Bitmap bmp = MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                                        c.getLong(0), MediaStore.Video.Thumbnails.MINI_KIND, videoOptions);
                                rightVideoViewHolder.imageMessage.setImageBitmap(bmp);
                                videoThumbnails.put(sentTimestamp, bmp);
                            } else {
                                Bitmap bmp = ThumbnailUtils.createVideoThumbnail(message,
                                        MediaStore.Video.Thumbnails.MINI_KIND);
                                rightVideoViewHolder.imageMessage.setImageBitmap(bmp);
                                videoThumbnails.put(sentTimestamp, bmp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                    }else {
                        rightVideoViewHolder.imageMessage.setImageBitmap(videoThumbnails.get(sentTimestamp));
                    }

                }else {
                    RequestOptions requestOptions = new RequestOptions()
                            .fitCenter()
                            .placeholder(R.drawable.ic_broken_image);
                    Glide.with(context)
                            .load("")
                            .apply(requestOptions)
                            .into(rightVideoViewHolder.imageMessage);
                }
                rightVideoViewHolder.btnPlayVideo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            //FileOpenIntentHelper.openFile(v.getContext(), message);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                            intent.setDataAndType(Uri.parse(message), "video/mp4");
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case LEFT_VIDEO_MESSAGE:
                LeftImageVideoViewHolder leftVideoViewHolder = (LeftImageVideoViewHolder) holder;
                leftVideoViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftVideoViewHolder.imageTitle.setVisibility(View.GONE);
                leftVideoViewHolder.senderName.setVisibility(View.GONE);
                leftVideoViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                Logger.error(TAG, "onBindViewHolder: LFFT_VIDEO_MESSAGE: message: "+message);
                if(message.contains("content://")){
                    try {
                        InputStream image_stream = context.getContentResolver().openInputStream(Uri.parse(message));
                        Bitmap bitmap= BitmapFactory.decodeStream(image_stream);
                        leftVideoViewHolder.imageMessage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        Logger.error(TAG,"Exception = "+e);
                        e.printStackTrace();
                    }
                }else if(new File(message).exists()){
                    if (videoThumbnails.get(sentTimestamp) == null) {
                        Cursor c = null;
                        try {
                            BitmapFactory.Options videoOptions = new BitmapFactory.Options();
                            videoOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                            videoOptions.inSampleSize = 2;
                            Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            String[] requierddata = {BaseColumns._ID};
                            c = context.getContentResolver().query(videoUri, requierddata,
                                    MediaStore.MediaColumns.DATA + " like  \"" + message + "\"", null, null);
                            c.moveToFirst();
                            if (c != null && c.getCount() != 0) {
                                Bitmap bmp = MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                                        c.getLong(0), MediaStore.Video.Thumbnails.MINI_KIND, videoOptions);
                                leftVideoViewHolder.imageMessage.setImageBitmap(bmp);
                                videoThumbnails.put(sentTimestamp, bmp);
                            } else {
                                Bitmap bmp = ThumbnailUtils.createVideoThumbnail(message,
                                        MediaStore.Video.Thumbnails.MINI_KIND);
                                leftVideoViewHolder.imageMessage.setImageBitmap(bmp);
                                videoThumbnails.put(sentTimestamp, bmp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                    }else {
                        leftVideoViewHolder.imageMessage.setImageBitmap(videoThumbnails.get(sentTimestamp));
                    }
                }else {
                    RequestOptions requestOptions = new RequestOptions()
                            .fitCenter()
                            .placeholder(R.drawable.ic_broken_image);
                    Glide.with(context)
                            .load("")
                            .apply(requestOptions)
                            .into(leftVideoViewHolder.imageMessage);
                }
                leftVideoViewHolder.btnPlayVideo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            //FileOpenIntentHelper.openFile(v.getContext(), message);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                            intent.setDataAndType(Uri.parse(message), "video/mp4");
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case RIGHT_HANDWRITE_DOWNLOADING_MESSAGE:
                RightImageVideoViewHolder rightHandwriteDownloadingHolder = (RightImageVideoViewHolder) holder;
                rightHandwriteDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightHandwriteDownloadingHolder.imageTitle.setText("Handwrite Message");
                rightHandwriteDownloadingHolder.btnPlayVideo.setVisibility(View.GONE);
                rightHandwriteDownloadingHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightHandwriteDownloadingHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(messageStatus != 0){
                    rightHandwriteDownloadingHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightHandwriteDownloadingHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightHandwriteDownloadingHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightHandwriteDownloadingHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightHandwriteDownloadingHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightHandwriteDownloadingHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightHandwriteDownloadingHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                LocalStorageFactory.loadImage(context, imageUrl, rightHandwriteDownloadingHolder.imageMessage, R.drawable.ic_broken_image);
                rightHandwriteDownloadingHolder.imageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, imageUrl);
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            ((Activity)context).startActivityForResult(intent, 1111);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            case LEFT_HANDWRITE_DOWNLOADING_MESSAGE:
                LeftImageVideoViewHolder leftHandwriteDownloadingHolder = (LeftImageVideoViewHolder) holder;
                leftHandwriteDownloadingHolder.imageTitle.setText("Handwrite Message");
                leftHandwriteDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftHandwriteDownloadingHolder.btnPlayVideo.setVisibility(View.GONE);
                leftHandwriteDownloadingHolder.senderName.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftHandwriteDownloadingHolder.avatar, R.drawable.cc_default_avatar);
                }
                LocalStorageFactory.loadImage(context, imageUrl, leftHandwriteDownloadingHolder.imageMessage, R.drawable.ic_broken_image);
                leftHandwriteDownloadingHolder.imageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, imageUrl);
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            ((Activity)context).startActivityForResult(intent, 1111);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            case RIGHT_HANDWRITE_MESSAGE:
                RightImageVideoViewHolder rightHandwriteHolder = (RightImageVideoViewHolder) holder;
                rightHandwriteHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightHandwriteHolder.imageTitle.setText("Handwrite Message");
                rightHandwriteHolder.btnPlayVideo.setVisibility(View.GONE);
                rightHandwriteHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightHandwriteHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightHandwriteHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                if(messageStatus != 0){
                    rightHandwriteHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightHandwriteHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightHandwriteHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightHandwriteHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightHandwriteHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightHandwriteHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightHandwriteHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                LocalStorageFactory.loadImage(context, message, rightHandwriteHolder.imageMessage, R.drawable.ic_broken_image);
                rightHandwriteHolder.imageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            ((Activity)context).startActivityForResult(intent, 1111);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            case LEFT_HANDWRITE_MESSAGE:
                LeftImageVideoViewHolder lefthandwriteHolder = (LeftImageVideoViewHolder) holder;
                lefthandwriteHolder.imageTitle.setText("Handwrite Message");
                lefthandwriteHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                lefthandwriteHolder.btnPlayVideo.setVisibility(View.GONE);
                lefthandwriteHolder.senderName.setVisibility(View.GONE);
                lefthandwriteHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, lefthandwriteHolder.avatar, R.drawable.cc_default_avatar);
                }
                LocalStorageFactory.loadImage(context, imageUrl, lefthandwriteHolder.imageMessage, R.drawable.ic_broken_image);
                lefthandwriteHolder.imageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, imageUrl);
                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                            ((Activity)context).startActivityForResult(intent, 1111);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case LEFT_AUDIO_DOWNLOADING_MESSAGE:
                LeftAudioViewHolder leftAudiodownloadingHolder = (LeftAudioViewHolder) holder;
                leftAudiodownloadingHolder.senderName.setVisibility(View.GONE);
                leftAudiodownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftAudiodownloadingHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudiodownloadingHolder.audioSeekBar.getThumb().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudiodownloadingHolder.playAudio.setVisibility(View.GONE);
                leftAudiodownloadingHolder.audioSeekBar.setProgress(0);
                leftAudiodownloadingHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudiodownloadingHolder.fileLoadingProgressBar.getIndeterminateDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                if(!player.isPlaying()){
                    leftAudiodownloadingHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                }
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftAudiodownloadingHolder.avatar,R.drawable.cc_default_avatar);
                }
                leftAudiodownloadingHolder.audioLength.setText("00:00");
                break;

            case LEFT_AUDIO_MESSAGE:
                final LeftAudioViewHolder leftAudioViewHolder = (LeftAudioViewHolder) holder;
                leftAudioViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                leftAudioViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftAudioViewHolder.senderName.setVisibility(View.GONE);
                leftAudioViewHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudioViewHolder.audioSeekBar.getThumb().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                if(!player.isPlaying()){
                    leftAudioViewHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                }
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftAudioViewHolder.avatar,R.drawable.cc_default_avatar);
                }
                leftAudioViewHolder.audioSeekBar.setProgress(0);
                if (audioDurations.get(sentTimestamp) == null) {
                    player.reset();
                    try {
                        File file = new File(message);
                        if (file.exists()) {
                            player.setDataSource(message);
                            player.prepare();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int audioDuration = player.getDuration();
                    audioDurations.put(sentTimestamp, audioDuration);
                    leftAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(audioDuration));
                    //audioSeekBar.setMax(audioDuration);
                    leftAudioViewHolder.audioSeekBar.setProgress(0);
                } else {
                    int time = audioDurations.get(sentTimestamp);
                    leftAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(time));
                    //audioSeekBar.setMax(time);
                    leftAudioViewHolder.audioSeekBar.setProgress(0);
                }
                leftAudioViewHolder.playAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        if (!player.isPlaying()) {

                        if (!TextUtils.isEmpty(message)) {
                            try {
                                if (sentTimestamp == currentlyPlayingId) {
                                    Logger.error(TAG, "onClick: currently playing");
                                    currentPlayingSong = "";
//                                        currentlyPlayingId = 0l;
//                                        setBtnColor(holder.viewType, playBtn, true);
                                    try {
                                        if(player.isPlaying()){
                                            player.pause();
                                            Logger.error(TAG, "onClick: paused");
                                            leftAudioViewHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                                        }else {
//                                                player.setDataSource(message);
//                                                player.prepare();
                                            player.seekTo(player.getCurrentPosition());
                                            leftAudioViewHolder.audioSeekBar.setProgress(player.getCurrentPosition());
                                            leftAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(player.getDuration()));
                                            leftAudioViewHolder.audioSeekBar.setMax(player.getDuration());
                                            leftAudioViewHolder.playAudio.setImageResource(R.drawable.ic_pause);
                                            timerRunnable = new Runnable() {
                                                @Override
                                                public void run() {

                                                    int pos = player.getCurrentPosition();
                                                    leftAudioViewHolder.audioSeekBar.setProgress(pos);

                                                    if (player.isPlaying() && pos < player.getDuration()) {
                                                        leftAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(player.getCurrentPosition()));
                                                        seekHandler.postDelayed(this, 250);
                                                    } else {
                                                        seekHandler
                                                                .removeCallbacks(timerRunnable);
                                                        timerRunnable = null;
                                                    }
                                                }

                                            };
                                            seekHandler.postDelayed(timerRunnable, 100);
                                            notifyDataSetChanged();
                                            player.start();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
//                                        int audioDuration = player.getDuration();

                                } else {
                                    leftAudioViewHolder.playAudio.setImageResource(R.drawable.ic_pause);
                                    playAudio(message, sentTimestamp, player, leftAudioViewHolder.playAudio, leftAudioViewHolder.audioLength, leftAudioViewHolder.audioSeekBar);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
//                        }
                        /*else {
                            rightAudioViewHolder.playAudio.setImageResource(R.drawable.ic_pause);
                            player.stop();
                        }*/
                    }
                });
                break;

            case RIGHT_AUDIO_DOWNLOADING_MESSAGE:
                RightAudioViewHolder rightAudioDownloadingHolder = (RightAudioViewHolder) holder;
                rightAudioDownloadingHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightAudioDownloadingHolder.audioContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightAudioDownloadingHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                rightAudioDownloadingHolder.audioSeekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                rightAudioDownloadingHolder.audioSeekBar.setProgress(0);
                rightAudioDownloadingHolder.playAudio.setVisibility(View.GONE);
                rightAudioDownloadingHolder.fileLoadingProgressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                rightAudioDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightAudioDownloadingHolder.audioLength.setText("00:00");
                if (!player.isPlaying()) {
                    rightAudioDownloadingHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                }
                if(messageStatus != 0){
                    rightAudioDownloadingHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightAudioDownloadingHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightAudioDownloadingHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING) {
                        rightAudioDownloadingHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    } else {
                        rightAudioDownloadingHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightAudioDownloadingHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightAudioDownloadingHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                break;

            case RIGHT_AUDIO_MESSAGE:
                final RightAudioViewHolder rightAudioViewHolder = (RightAudioViewHolder) holder;
                rightAudioViewHolder.audioContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightAudioViewHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightAudioViewHolder.audioSeekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                rightAudioViewHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                rightAudioViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightAudioViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                if(!player.isPlaying()){
                    rightAudioViewHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                }
                rightAudioViewHolder.retry.setVisibility(View.INVISIBLE);
                Logger.error(TAG, "onBindViewHolder: RIGHT_AUDIO_MESSAGE: messageStatus: "+messageStatus);
                if (messageStatus == 2){
                    rightAudioViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightAudioViewHolder.retry.setVisibility(View.VISIBLE);
                    rightAudioViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback)context).onRetryClicked(localId);
                            }
                            rotateRetry(rightAudioViewHolder.retry,3);
                        }
                    });
                    break;
                }else if(messageStatus != 0){
                    rightAudioViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightAudioViewHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightAudioViewHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightAudioViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightAudioViewHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightAudioViewHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightAudioViewHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                rightAudioViewHolder.audioSeekBar.setProgress(0);
                if (audioDurations.get(sentTimestamp) == null) {
                    player.reset();
                    try {
                        File file = new File(message);
                        if (file.exists()) {
                            player.setDataSource(message);
                            player.prepare();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int audioDuration = player.getDuration();
                    audioDurations.put(sentTimestamp, audioDuration);
                    rightAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(audioDuration));
                    //audioSeekBar.setMax(audioDuration);
                    rightAudioViewHolder.audioSeekBar.setProgress(0);
                } else {
                    int time = audioDurations.get(sentTimestamp);
                    rightAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(time));
                    //audioSeekBar.setMax(time);
                    rightAudioViewHolder.audioSeekBar.setProgress(0);
                }
                rightAudioViewHolder.playAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        if (!player.isPlaying()) {

                            if (!TextUtils.isEmpty(message)) {
                                try {
                                    if (sentTimestamp == currentlyPlayingId) {
                                        Logger.error(TAG, "onClick: currently playing");
                                        currentPlayingSong = "";
//                                        currentlyPlayingId = 0l;
//                                        setBtnColor(holder.viewType, playBtn, true);
                                        try {
                                            if(player.isPlaying()){
                                                player.pause();
                                                Logger.error(TAG, "onClick: paused");
                                                rightAudioViewHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                                            }else {
//                                                player.setDataSource(message);
//                                                player.prepare();
                                                player.seekTo(player.getCurrentPosition());
                                                rightAudioViewHolder.audioSeekBar.setProgress(player.getCurrentPosition());
                                                rightAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(player.getDuration()));
                                                rightAudioViewHolder.audioSeekBar.setMax(player.getDuration());
                                                rightAudioViewHolder.playAudio.setImageResource(R.drawable.ic_pause);
                                                timerRunnable = new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        int pos = player.getCurrentPosition();
                                                        rightAudioViewHolder.audioSeekBar.setProgress(pos);

                                                        if (player.isPlaying() && pos < player.getDuration()) {
                                                            rightAudioViewHolder.audioLength.setText(CommonUtils.convertTimeStampToDurationTime(player.getCurrentPosition()));
                                                            seekHandler.postDelayed(this, 250);
                                                        } else {
                                                            seekHandler
                                                                    .removeCallbacks(timerRunnable);
                                                            timerRunnable = null;
                                                        }
                                                    }

                                                };
                                                seekHandler.postDelayed(timerRunnable, 100);
                                                notifyDataSetChanged();
                                                player.start();
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
//                                        int audioDuration = player.getDuration();

                                    } else {
                                        rightAudioViewHolder.playAudio.setImageResource(R.drawable.ic_pause);
                                        playAudio(message, sentTimestamp, player, rightAudioViewHolder.playAudio, rightAudioViewHolder.audioLength, rightAudioViewHolder.audioSeekBar);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
//                        }
                        /*else {
                            rightAudioViewHolder.playAudio.setImageResource(R.drawable.ic_pause);
                            player.stop();
                        }*/
                    }
                });
                break;

            case RIGHT_STICKER_MESSAGE:
                final RightMessageViewHolder rightStickerMessageHolder = (RightMessageViewHolder) holder;
                rightStickerMessageHolder.rightArrow.setVisibility(View.INVISIBLE);
                rightStickerMessageHolder.textMessage.setBackgroundColor(Color.argb(00,00,00,00));
                rightStickerMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightStickerMessageHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightStickerMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightStickerMessageHolder.retry.setVisibility(View.VISIBLE);
                    rightStickerMessageHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback)context).onRetryClicked(localId);
                            }
                            rotateRetry(rightStickerMessageHolder.retry,1);
                        }
                    });
                    break;
                }else if(messageStatus != 0){
                    rightStickerMessageHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightStickerMessageHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightStickerMessageHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightStickerMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightStickerMessageHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightStickerMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightStickerMessageHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                if(message.contains("CC^CONTROL_")){
                    rightStickerMessageHolder.textMessage.setText(Stickers.handleSticker(message));
                }else{
                    rightStickerMessageHolder.textMessage.setText(Stickers.getSpannableStickerString(message));
                }
                break;

            case LEFT_STICKER_MESSAGE:
                LeftMessageViewHolder leftStickerMessageHolder = (LeftMessageViewHolder) holder;
                leftStickerMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftStickerMessageHolder.senderName.setVisibility(View.GONE);
                leftStickerMessageHolder.leftArrow.setVisibility(View.GONE);
                leftStickerMessageHolder.textMessage.setBackgroundColor(Color.argb(00,00,00,00));
                if(!TextUtils.isEmpty(avatarUrl)){
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftStickerMessageHolder.avatar,R.drawable.cc_default_avatar);
                }
                if(message.contains("CC^CONTROL_")){
                    leftStickerMessageHolder.textMessage.setText(Stickers.handleSticker(message));
                }else{
                    leftStickerMessageHolder.textMessage.setText(Stickers.getSpannableStickerString(message));
                }
                break;

            case LEFT_WHITEBOARD_MESSAGE:
                LeftMessageViewHolder leftWhiteBoardMessageHolder = (LeftMessageViewHolder) holder;
                leftWhiteBoardMessageHolder.senderName.setVisibility(View.GONE);
                leftWhiteBoardMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if(!TextUtils.isEmpty(avatarUrl)){
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftWhiteBoardMessageHolder.avatar,R.drawable.cc_default_avatar);
                }
                leftWhiteBoardMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftWhiteBoardMessageHolder.textMessage.setText(createViewLink("has shared his/her whiteboard with you. Click here to view|#|"+message, 1));
                break;

            case RIGHT_WHITEBOARD_MESSAGE:
                RightMessageViewHolder rightWhiteBoardMessageHolder = (RightMessageViewHolder) holder;
                rightWhiteBoardMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightWhiteBoardMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightWhiteBoardMessageHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(messageStatus != 0){
                    rightWhiteBoardMessageHolder.messageStatus.setImageResource(R.drawable.iconsent);
                }
                if(showticks){
                    if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_SENT){
                        rightWhiteBoardMessageHolder.messageStatus.setImageResource(R.drawable.iconsent);
                    } else if (messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD) {
                        rightWhiteBoardMessageHolder.messageStatus.setImageResource(R.drawable.icondeliverd);
                    }else if(messagetick == CometChatKeys.MessageTypeKeys.MESSAGE_PENDING){
                        rightWhiteBoardMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_time_watch);
                    }else {
                        rightWhiteBoardMessageHolder.messageStatus.setImageResource(R.drawable.iconread);
                    }
                }else {
                    if(messageStatus == 0){
                        rightWhiteBoardMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                    }else {
                        rightWhiteBoardMessageHolder.messageStatus.setVisibility(View.INVISIBLE);
                    }
                }
                rightWhiteBoardMessageHolder.textMessage.setText("Successfully shared whiteboard");
                break;

            case WRITEBOARD_MESSAGE:
                LeftMessageViewHolder writeBoardMessageHolder = (LeftMessageViewHolder) holder;
                writeBoardMessageHolder.senderName.setVisibility(View.GONE);
                writeBoardMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, writeBoardMessageHolder.avatar, R.drawable.cc_default_avatar);
                }
                writeBoardMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                writeBoardMessageHolder.textMessage.setText(createViewLink("has shared his/her collaborative document with you. Click here to view|#|"+message, 2));
                break;

            case GROUP_INVITE_MESSAGE:
                LeftMessageViewHolder groupInviteMessageHolder = (LeftMessageViewHolder) holder;
                groupInviteMessageHolder.senderName.setVisibility(View.GONE);
                groupInviteMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, groupInviteMessageHolder.avatar, R.drawable.cc_default_avatar);
                }
                groupInviteMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                groupInviteMessageHolder.textMessage.setText(createViewLink("has invited you to join a group Join~"+message, 3));
                break;

            case LEFT_SCREENSHARE_MESSAGE:
                LeftMessageViewHolder screenShareMessageHolder = (LeftMessageViewHolder) holder;
                screenShareMessageHolder.senderName.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, screenShareMessageHolder.avatar, R.drawable.cc_default_avatar);
                }
                Logger.error(TAG,"Screenshare message = "+message);
                screenShareMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                screenShareMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                screenShareMessageHolder.textMessage.setText(createViewLink(message, 4));
                break;

            case RIGHT_SCREENSHARE_MESSAGE:
                RightMessageViewHolder rightScreenShareMessageHolder = (RightMessageViewHolder) holder;
                rightScreenShareMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightScreenShareMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightScreenShareMessageHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightScreenShareMessageHolder.textMessage.setText("Successfully shared screen");
                break;
            case BOT_MESSAGE:
                LeftImageVideoViewHolder botViewHolder = (LeftImageVideoViewHolder) holder;
                try {
                    JSONObject botJson = new JSONObject(message);
                    Bot bot = Bot.getBotDetails(botJson.getString("botid"));
                    if (bot != null && bot.botAvatar != null) {
                        LocalStorageFactory.loadImageUsingURL(context,bot.botAvatar,botViewHolder.avatar,R.drawable.cc_default_avatar);
                    }
                    botViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                    botViewHolder.btnPlayVideo.setVisibility(View.GONE);
                    botViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                    botViewHolder.senderName.setVisibility(View.GONE);
                    switch (botJson.getString("messagetype")) {
                        case "image":
                            Document doc = Jsoup.parseBodyFragment(botJson.getString("message"));
                            Element imageElement = doc.select("img").first();
                            final String absoluteUrl = imageElement.absUrl("src");
                            botViewHolder.imageMessage.setVisibility(View.VISIBLE);
                            LocalStorageFactory.loadImage(context, absoluteUrl, botViewHolder.imageMessage, R.drawable.ic_broken_image);
                            botViewHolder.imageTitle.setVisibility(View.GONE);
                            botViewHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    try {
                                        Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                        intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, absoluteUrl);
                                        intent.putExtra(StaticMembers.INTENT_IMAGE_SRC, absoluteUrl);
                                        intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                        ((Activity)context).startActivityForResult(intent, 1111);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            break;

                        case "anchor":
                            String temp = botJson.getString("message");
                            botViewHolder.imageMessage.setVisibility(View.GONE);
                            botViewHolder.imageTitle.setVisibility(View.VISIBLE);
                            CommonUtils.renderHtmlInATextView(context, botViewHolder.imageTitle, temp);
                            break;

                        default:
                            botViewHolder.imageMessage.setVisibility(View.GONE);
                            botViewHolder.imageTitle.setVisibility(View.VISIBLE);
                            if (bot != null && bot.botName != null && bot.botName.equals("SoundCloud")) {
                                Matcher matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(botJson.getString("message"));
                                final String html = botJson.getString("message");
                                matcher.find();
                                final String src = matcher.group(1);
                                Logger.error(TAG, "onBindViewHolder: html: "+html);
                                Logger.error(TAG, "onBindViewHolder: src: "+src);
                                botViewHolder.imageTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.cc_ic_play,0,0,0);
                                String soundCloudMesage = "SoundCloud Message. Click here to open";
                                int startIndex = soundCloudMesage.indexOf(".");
                                int endIndex = soundCloudMesage.length();
                                SpannableString soundCloudString = new SpannableString(soundCloudMesage);
                                ClickableSpan span = new ClickableSpan() {
                                    @Override
                                    public void onClick(View widget) {
                                        new CCChromeTabs(context).loadURL(src);
                                    }
                                };
                                soundCloudString.setSpan(span, startIndex + 2, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                soundCloudString.setSpan(new ForegroundColorSpan(color),startIndex + 2, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                botViewHolder.imageTitle.setMovementMethod(LinkMovementMethod.getInstance());
                                botViewHolder.imageTitle.setText(soundCloudString);
                            }else {
                                botViewHolder.imageTitle.setText(Html.fromHtml(botJson.getString("message")));
                            }
                            break;

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case AVBROADCAST_REQUEST:
                LeftMessageViewHolder avBroadcastMessageHolder = (LeftMessageViewHolder) holder;
                avBroadcastMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                avBroadcastMessageHolder.senderName.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, avBroadcastMessageHolder.avatar, R.drawable.cc_default_avatar);
                }
                avBroadcastMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                avBroadcastMessageHolder.textMessage.setText(AVBroadcastText(message, fromId,imageUrl));
                break;

            case AVBROADCAST_EXPIRED:
                LeftMessageViewHolder avBroadcastExpiredHolder = (LeftMessageViewHolder) holder;
                avBroadcastExpiredHolder.senderName.setVisibility(View.GONE);
                avBroadcastExpiredHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, avBroadcastExpiredHolder.avatar, R.drawable.cc_default_avatar);
                }
                avBroadcastExpiredHolder.textMessage.setText(message+"\n(Expired)");
                break;
            case AVCHAT_INCOMING_CALL_END:
            case AVCHAT_CALL_ACCEPTED:
            case AVCHAT_INCOMING_CALL:
                CallViewHolder avIncomingCallViewHolder = (CallViewHolder) holder;
                avIncomingCallViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                avIncomingCallViewHolder.callMessage.setText(processAVchatCallerName(message));
                break;
            case AVCHAT_BUSY_CALL:
                CallViewHolder avBusyViewHolder = (CallViewHolder) holder;
                avBusyViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                avBusyViewHolder.callMessage.setText("User is busy right now");
                break;
            case LEFT_FILE_MESSAGE:
                LeftMessageViewHolder fileMessageHolder = (LeftMessageViewHolder) holder;
                fileMessageHolder.senderName.setVisibility(View.GONE);
                fileMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, fileMessageHolder.avatar, R.drawable.cc_default_avatar);
                }
                fileMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                fileMessageHolder.textMessage.setText(createDownloadLink(message, String.valueOf(remoteId)));
                break;

            case LEFT_FILE_MESSAGE_DOWNLOADED:
                LeftMessageViewHolder fileDownloadedMessageHolder = (LeftMessageViewHolder) holder;
                fileDownloadedMessageHolder.senderName.setVisibility(View.GONE);
                fileDownloadedMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if (!TextUtils.isEmpty(avatarUrl)) {
                    LocalStorageFactory.loadImageUsingURL(context, avatarUrl, fileDownloadedMessageHolder.avatar, R.drawable.cc_default_avatar);
                }
                fileDownloadedMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                fileDownloadedMessageHolder.textMessage.setText(createOpenLink(message));
                break;
        }






        /*
        //long remoteId = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_REMOTE_ID));


        if(holder.isTypingView != null){
            holder.isTypingView.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(avatarUrl)) {
                LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
            }
        }else{
            int self = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SELF));
            long fromId = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_FROM_ID));
            int insertedBy = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SELF));
            final String message = cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE));
            final long sentTimestamp = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SENT_TIMESTAMP));
            final String imageUrl = cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_IMAGE_URL));
            final int messagetick = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TICK));
            boolean showticks = (boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.SHOW_TICKS));
            final int messageStatus = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_STATUS));
            conatctID = fromId;

            if(holder.viewType == TYPE_RIGHT){
                LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable.findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                shape.setColor(holder.rightBubbleColor);
                GradientDrawable drawable = (GradientDrawable) holder.normalMessageContainer.getBackground();
                drawable.setColor(holder.rightBubbleColor);
            }else{
                LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable.findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                shape.setColor(holder.leftBubbleColor);
                GradientDrawable drawable = (GradientDrawable) holder.normalMessageContainer.getBackground();
                drawable.setColor(holder.leftBubbleColor);
            }


            holder.messageTimestamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
            holder.messageArrow.setVisibility(View.VISIBLE);
            holder.imageHolder.setEnabled(true);
            holder.customImageHolder.setEnabled(true);

            holder.messageTick.setVisibility(View.VISIBLE);
            if (!CometChatKeys.MessageTypeKeys.AVCHAT.equals(cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TYPE)))) {
                if (holder.viewType == TYPE_RIGHT) {
                    if (showticks) {
                        switch (messagetick) {
                            case CometChatKeys.MessageTypeKeys.MESSAGE_SENT:
                                holder.messageTick.setImageResource(R.drawable.iconsent);
                                break;
                            case CometChatKeys.MessageTypeKeys.MESSAGE_DELIVERD:
                                holder.messageTick.setImageResource(R.drawable.icondeliverd);
                                break;
                            case CometChatKeys.MessageTypeKeys.MESSAGE_READ:
                                holder.messageTick.setImageResource(R.drawable.iconread);
                                break;
                            default:
                                holder.messageTick.setImageResource(R.drawable.iconsent);
                                break;
                        }
                    } else {
                        holder.messageTick.setVisibility(GONE);
                    }
                } else {
                    holder.messageTick.setVisibility(GONE);
                }
            }
            switch(cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TYPE))) {

                case MessageTypeKeys.NORMAL_MESSAGE:
                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.message.setText(message);
                    if (holder.viewType == TYPE_RIGHT) {
                        holder.avatar.setVisibility(View.GONE);
                        if (messageStatus == 0) {
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        } else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if (showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context, avatarUrl, holder.avatar, R.drawable.cc_default_avatar);
                        }
                    }
                    holder.message.setText(message);

                    break;

                case MessageTypeKeys.EMOJI_MESSAGE:
                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    Logger.error(TAG, "message : " + message);
                    holder.message.setText(message);
                    if (holder.viewType == TYPE_RIGHT) {
                        holder.avatar.setVisibility(View.GONE);
                        if (messageStatus == 0) {
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        } else {
                            holder.imagePendingMessage.setVisibility(View.GONE);
                            if (showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context, avatarUrl, holder.avatar, R.drawable.cc_default_avatar);
                        }
                    }

                    if (imageUrl != null && imageUrl.equals(MessageTypeKeys.NO_BACKGROUND)) {
                        holder.message.setVisibility(View.GONE);
                        holder.tvOnlySmiley.setVisibility(View.VISIBLE);
                        holder.tvOnlySmiley.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                        LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                        GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                                .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                        shape.setColor(Color.parseColor("#00000000"));
                        GradientDrawable drawable = (GradientDrawable) holder.normalMessageContainer.getBackground();
                        drawable.setColor(Color.parseColor("#00000000"));
                    }

                    holder.message.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));

                    break;

                case MessageTypeKeys.IMAGE_DOWNLOADING:

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.message.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.VISIBLE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.imageHolder.setVisibility(View.VISIBLE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if (holder.viewType == TYPE_RIGHT) {
                        holder.avatar.setVisibility(View.GONE);
                    } else {
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context, avatarUrl, holder.avatar, R.drawable.cc_default_avatar);
                        }
                    }
                    RequestOptions requestOptions = new RequestOptions()
                            .fitCenter()
                            .placeholder(R.drawable.cc_thumbnail_default);

                    Logger.error(TAG, "Downloading Image URL =  " + imageUrl);
                    Glide.with(context)
                            .load(imageUrl)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }


                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    holder.wheel.setVisibility(View.GONE);

                                    LayerDrawable ldrawableImage = (LayerDrawable) holder.messageArrow.getBackground();
                                    GradientDrawable shapeImage = ((GradientDrawable) ((RotateDrawable) (ldrawableImage
                                            .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                                    shapeImage.setColor(Color.parseColor("#00000000"));
                                    GradientDrawable drawableImage = (GradientDrawable) holder.normalMessageContainer.getBackground();
                                    drawableImage.setColor(Color.parseColor("#00000000"));

                                    holder.customImageHolder.setVisibility(View.VISIBLE);
                                    holder.imageHolder.setVisibility(View.GONE);
                                    holder.customImageHolder.setImageDrawable(resource);
                                    return false;
                                }
                            })
                            .apply(requestOptions)
                            .into(holder.imageHolder);

                *//*if (self != 1) {
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                if (!avatarUrl.equals("")) {
                    LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                }*//*

                    final String finalMessage = message;
                    holder.customImageHolder.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            try {
                                holder.customImageHolder.setEnabled(false);
                                Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                Logger.error(TAG, "Adapter : clicked_position : " + cursor.getPosition());
                                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, finalMessage);
                                ((Activity) context).startActivityForResult(intent, 1111);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    break;

                case MessageTypeKeys.IMAGE_MESSAGE:
                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.message.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.VISIBLE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if (holder.viewType == TYPE_RIGHT) {
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        Logger.error(TAG, "IMAGE_MESSAGE messageStatus : " + messageStatus);
                        if (messageStatus == 0) {
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        } else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if (showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context, avatarUrl, holder.avatar, R.drawable.cc_default_avatar);
                        }
                    }
                *//*if (self != 1) {
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }*//*

                    LayerDrawable ldrawableImage = (LayerDrawable) holder.messageArrow.getBackground();
                    GradientDrawable shapeImage = ((GradientDrawable) ((RotateDrawable) (ldrawableImage
                            .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                    shapeImage.setColor(Color.parseColor("#00000000"));
                    GradientDrawable drawableImage = (GradientDrawable) holder.normalMessageContainer.getBackground();
                    drawableImage.setColor(Color.parseColor("#00000000"));
                    Logger.error(TAG, "message value = " + message);
                    if (message.contains("content://")) {
//                        if(CommonUtils.isFileExists(message)){
                        LocalStorageFactory.loadImageWithRoundedCorners(context, message, holder.customImageHolder, R.drawable.cc_thumbnail_default);
                    *//*else {
                            LocalStorageFactory.loadImageUsingURL(context,message,holder.customImageHolder,R.drawable.cc_thumbnail_default);
                        }*//*
                    }else {
                        File file_downloaded = new File(message);
                        if (file_downloaded.exists()) {
//                        if (!message.contains("content://")) {
                            try {
                                Logger.error(TAG,"loading with rounded corners");
                                LocalStorageFactory.loadImageWithRoundedCorners(context, message, holder.customImageHolder, R.drawable.cc_thumbnail_default);
                            } catch (Exception e) {
                                Logger.error(TAG, e.toString());
                                e.printStackTrace();
                            }
//                        }
                        }else {
                            Logger.error(TAG,"loading");
                            LocalStorageFactory.loadImageUsingURL(context,message,holder.customImageHolder,R.drawable.cc_thumbnail_default);
                        }
                    }




                    holder.customImageHolder.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            try {
                                    holder.customImageHolder.setEnabled(false);
                                    Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                    Logger.error(TAG, "Adapter : clicked_position : " + cursor.getPosition());
                                    intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                    intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                                    ((Activity)context).startActivityForResult(intent, 1111);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    break;

                case MessageTypeKeys.VIDEO_MESSAGE:

                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    File video = new File(message);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    holder.audioNoteContainer.setVisibility(View.GONE);
                *//*if (self != 1) {
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }*//*
                    Logger.error(TAG,"Video exits ? "+video.exists());
                    if (video.exists()) {
                        holder.message.setVisibility(View.GONE);
                        holder.imageHolder.setVisibility(View.GONE);
                        holder.wheel.setVisibility(View.GONE);
                        holder.tvOnlySmiley.setVisibility(View.GONE);
                        holder.videoMessageButton.setImageResource(R.drawable.cc_play_video_button);
                        holder.videoMessageButton.setVisibility(View.VISIBLE);
                        holder.videoThumb.setVisibility(View.VISIBLE);
                        holder.customImageHolder.setVisibility(View.GONE);
                        if (videoThumbnails.get(sentTimestamp) == null) {
                            Cursor c = null;
                            try {
                                BitmapFactory.Options videoOptions = new BitmapFactory.Options();
                                videoOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                                videoOptions.inSampleSize = 2;
                                Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                                String[] requierddata = {BaseColumns._ID};
                                c = context.getContentResolver().query(videoUri, requierddata,
                                        MediaStore.MediaColumns.DATA + " like  \"" + message + "\"", null, null);
                                c.moveToFirst();
                                if (c != null && c.getCount() != 0) {
                                    Bitmap bmp = MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                                            c.getLong(0), MediaStore.Video.Thumbnails.MINI_KIND, videoOptions);
                                    holder.videoThumb.setImageBitmap(bmp);
                                    videoThumbnails.put(sentTimestamp, bmp);
                                } else {
                                    Bitmap bmp = ThumbnailUtils.createVideoThumbnail(message,
                                            MediaStore.Video.Thumbnails.MINI_KIND);
                                    holder.videoThumb.setImageBitmap(bmp);
                                    videoThumbnails.put(sentTimestamp, bmp);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (c != null && !c.isClosed()) {
                                    c.close();
                                }
                            }
                        } else {
                            holder.videoThumb.setImageBitmap(videoThumbnails.get(sentTimestamp));
                        }

                        View.OnClickListener videoClickListener1 = new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                try {
                                    //FileOpenIntentHelper.openFile(v.getContext(), message);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                                    intent.setDataAndType(Uri.parse(message), "video/mp4");
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };

                        holder.videoThumb.setOnClickListener(videoClickListener1);
                    } else if(message.contains("content://")){
                        holder.message.setVisibility(View.GONE);
                        holder.imageHolder.setVisibility(View.GONE);
                        holder.wheel.setVisibility(View.GONE);
                        holder.tvOnlySmiley.setVisibility(View.GONE);
                        holder.videoMessageButton.setImageResource(R.drawable.cc_play_video_button);
                        holder.videoMessageButton.setVisibility(View.VISIBLE);
                        holder.videoThumb.setVisibility(View.VISIBLE);
                        holder.customImageHolder.setVisibility(View.GONE);

                        InputStream image_stream = null;
                        Logger.error(TAG,"Message = "+message);

                        View.OnClickListener videoClickListener1 = new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                try {
                                    //FileOpenIntentHelper.openFile(v.getContext(), message);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                                    intent.setDataAndType(Uri.parse(message), "video/mp4");
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        holder.videoThumb.setOnClickListener(videoClickListener1);

                        try {
                            image_stream = context.getContentResolver().openInputStream(Uri.parse(message));
                            Bitmap bitmap= BitmapFactory.decodeStream(image_stream);
                            holder.videoThumb.setImageBitmap(bitmap);
                        } catch (FileNotFoundException e) {
                            Logger.error(TAG,"Exception = "+e);
                            e.printStackTrace();
                        }

                    }

                    break;

                case MessageTypeKeys.VIDEO_DOWNLOADING:

                    holder.message.setVisibility(View.GONE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setImageResource(R.drawable.cc_thumbnail_default);
                    holder.videoThumb.setVisibility(View.VISIBLE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    holder.wheel.setVisibility(View.VISIBLE);
                    holder.videoMessageButton.setVisibility(View.GONE);

                *//*if (self != 1) {
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }*//*
                    break;

                case MessageTypeKeys.AUDIO_MESSAGE:
                    holder.message.setVisibility(View.GONE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.VISIBLE);

               *//* if (self != 1) {
                   *//**//* Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }*//**//*
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }*//*

                    final TextView audioText = holder.audioTime;
                    final SeekBar audioSeekBar = holder.audioSeekbar;
                    final ImageView playBtn = holder.audioPlayButton;

                    audioSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                    if (android.os.Build.VERSION.SDK_INT >= 16){
                        audioSeekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                    }

                    if (sentTimestamp == currentlyPlayingId) {
                        setBtnColor(holder.viewType, holder.audioPlayButton, false);
                    } else {
                        setBtnColor(holder.viewType, holder.audioPlayButton, true);
                        if (audioDurations.get(sentTimestamp) == null) {
                            player.reset();
                            try {
                                File file = new File(message);
                                if (file.exists()) {
                                    player.setDataSource(message);
                                    player.prepare();
                                } *//*else {
                                AudioSharing.downloadAndStoreAudio(String.valueOf(message.remoteId), message.imageUrl, false);
                            }*//*
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int audioDuration = player.getDuration();
                            audioDurations.put(sentTimestamp, audioDuration);
                            holder.audioTime.setText(CommonUtils.convertTimeStampToDurationTime(audioDuration));
                            //audioSeekBar.setMax(audioDuration);
                            audioSeekBar.setProgress(0);
                        } else {
                            int time = audioDurations.get(sentTimestamp);
                            holder.audioTime.setText(CommonUtils.convertTimeStampToDurationTime(time));
                            //audioSeekBar.setMax(time);
                            audioSeekBar.setProgress(0);
                        }
                    }

                    holder.audioPlayButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!TextUtils.isEmpty(message)) {
                                try {
                                    if (sentTimestamp == currentlyPlayingId) {
                                        currentPlayingSong = "";
                                        currentlyPlayingId = 0l;
                                        player.stop();
                                        player.reset();
                                        setBtnColor(holder.viewType, playBtn, true);
                                        try {
                                            player.setDataSource(message);
                                            player.prepare();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        int audioDuration = player.getDuration();
                                        audioText.setText(CommonUtils.convertTimeStampToDurationTime(audioDuration));
                                        audioSeekBar.setMax(audioDuration);
                                        audioSeekBar.setProgress(player.getCurrentPosition());
                                    } else {
                                        playAudio(message, sentTimestamp, playBtn, player, holder.viewType, audioText, audioSeekBar);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    break;

                case MessageTypeKeys.AUDIO_DOWNLOADING:
                    holder.message.setVisibility(View.GONE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.VISIBLE);

                    break;

                case MessageTypeKeys.STICKER:
                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                    GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                            .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                    shape.setColor(Color.parseColor("#00000000"));
                    GradientDrawable drawable = (GradientDrawable) holder.normalMessageContainer.getBackground();
                    drawable.setColor(Color.parseColor("#00000000"));

                    if(message.contains("CC^CONTROL_")){
                        holder.message.setText(Stickers.handleSticker(message));
                    }else{
                        holder.message.setText(Stickers.getSpannableStickerString(message));
                    }
                    break;

                case MessageTypeKeys.HANDWRITE_DOWNLOADING:

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setImageDrawable(context.getResources().getDrawable(R.drawable.cc_thumbnail_default));
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }


                    Logger.error("HandwriteMess  = "+message);
                    Logger.error("imageUrl  = "+imageUrl);
                    holder.imageHolder.setVisibility(View.VISIBLE);
                    holder.wheel.setVisibility(View.VISIBLE);
                    holder.message.setText("HandWrite Message");

                    RequestOptions requestOptions1 = new RequestOptions()
                            .fitCenter()
                            .centerCrop()
                            .override(500,500)
                            .placeholder(R.drawable.cc_thumbnail_default);
                    Glide.with(context)
                            .load(imageUrl)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    holder.wheel.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .apply(requestOptions1)
                            .into(holder.imageHolder);
                    break;

                case MessageTypeKeys.HANDWRITE_MESSAGE:

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.VISIBLE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    holder.message.setText("HandWrite Message");

                    File handwritefile = new File(message);

                    BitmapFactory.Options options;
                    options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;

                    if (handwritefile.exists()) {
                        try {
                            holder.imageHolder.setImageBitmap(BitmapFactory.decodeFile(message, options));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            try {
                                holder.imageHolder.setEnabled(false);
                                Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                ((Activity)context).startActivityForResult(intent, 1111);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    break;

                case MessageTypeKeys.WHITEBOARD_MESSAGE:

                    holder.message.setVisibility(View.VISIBLE);
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(createViewLink("has shared his/her whiteboard with you. Click here to view|#|"+message, 1));
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;

                case MessageTypeKeys.WRITEBOARD_MESSAGE:

                    holder.message.setVisibility(View.VISIBLE);
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(createViewLink("has shared his/her collaborative document with you. Click here to view|#|"+message, 2));
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;

                case MessageTypeKeys.AVCHAT_CALL_ACCEPTED :
                case MessageTypeKeys.AVCHAT_INCOMING_CALL :

                    holder.normalMessageContainer.setVisibility(View.GONE);
                    holder.avchatMessageContainer.setVisibility(View.VISIBLE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.messageArrow.setVisibility(View.GONE);
                    holder.messageTick.setVisibility(View.GONE);
                    holder.messageTimestamp.setVisibility(View.GONE);
                    holder.avchatMessage.setText(processAVchatCallerName(message));
                    holder.avchatMessageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);



                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;

                case MessageTypeKeys.AVCHAT_INCOMING_CALL_END :

                    holder.normalMessageContainer.setVisibility(View.GONE);
                    holder.avchatMessageContainer.setVisibility(View.VISIBLE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.messageArrow.setVisibility(View.GONE);
                    holder.messageTick.setVisibility(View.GONE);
                    holder.messageTimestamp.setVisibility(View.GONE);
                    holder.avchatMessage.setText(processAVchatCallerName(message));
                    holder.avchatMessageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);


                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;


                case MessageTypeKeys.AVCHAT_BUSY_CALL :
                    holder.normalMessageContainer.setVisibility(View.GONE);
                    holder.avchatMessageContainer.setVisibility(View.VISIBLE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.messageArrow.setVisibility(View.GONE);
                    holder.messageTick.setVisibility(View.GONE);
                    holder.messageTimestamp.setVisibility(View.GONE);
                    Logger.error(TAG,"busy call message: "+message);
                    Logger.error(TAG,"busy call processed message: "+processAVchatCallerName(message));
                    holder.avchatMessage.setText("The user is busy right now");
                    holder.avchatMessageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);


                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;

                case MessageTypeKeys.AVBROADCAST_REQUEST :

                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(AVBroadcastText(message, fromId,imageUrl));

                    break;

                case MessageTypeKeys.AVBROADCAST_INVITE :

                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(AVBroadcastText(message, fromId,imageUrl));

                    break;

                case MessageTypeKeys.AVBROADCAST_EXPIRED :

                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.message.setText(message+"\n(Expired)");

                    break;

                case MessageTypeKeys.AVBROADCAST_END :

                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                    }
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(message);

                    break;

                case MessageTypeKeys.GROUP_INVITE:
                    Logger.error(TAG,"Group invite");
                    Logger.error(TAG,"Group invite message : "+message);
                    holder.message.setVisibility(View.VISIBLE);
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(createViewLink("has invited you to join a group Join~"+message, 3));
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                *//*if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                }else{
                    holder.avatar.setVisibility(View.VISIBLE);
                }*//*
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;

                case MessageTypeKeys.SCREENSHARE_MESSAGE:
                    Logger.error(TAG,"Screenshare message : "+message);
                    holder.message.setVisibility(View.VISIBLE);
                    holder.message.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.message.setText(createViewLink(message, 4));
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                *//*if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                }else{
                    holder.avatar.setVisibility(View.VISIBLE);
                }*//*
                    channel = imageUrl;
                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                            holder.messageTick.setVisibility(View.GONE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);

                            if(showticks)
                                holder.messageTick.setVisibility(View.VISIBLE);
                        }
                    }else{
                        holder.avatar.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }
                    break;

                case MessageTypeKeys.BOT_RESPONSE:
                    holder.message.setVisibility(View.VISIBLE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.videoThumb.setVisibility(View.GONE);
                    holder.videoMessageButton.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.normalMessageContainer.setVisibility(View.VISIBLE);
                    holder.avchatMessageContainer.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    try {
                        JSONObject botMessageJson = new JSONObject(message);
                        Logger.error(TAG,"botMessageJson: "+botMessageJson);
                        Bot bot = Bot.getBotDetails(botMessageJson.getString("botid"));
                        Logger.error(TAG,"BOT :"+bot);
                        Logger.error(TAG,"getAll Bots: "+Bot.getAllbots());
                        if (bot != null && bot.botAvatar != null) {
                            Logger.error(TAG,"Bot URL: "+bot.botAvatar);
                            LocalStorageFactory.loadImageUsingURL(context, bot.botAvatar, holder.avatar, R.drawable.default_avatar);
                        }

                        switch (botMessageJson.getString("messagetype")) {

                            case "image":
                                Document doc = Jsoup.parseBodyFragment(botMessageJson.getString("message"));
                                Element imageElement = doc.select("img").first();
                                final String absoluteUrl = imageElement.absUrl("src");
                                holder.customImageHolder.setVisibility(View.VISIBLE);
                                holder.message.setVisibility(GONE);

                                LayerDrawable layerDrawable = (LayerDrawable) holder.messageArrow.getBackground();
                                GradientDrawable gradientDrawable = ((GradientDrawable) ((RotateDrawable) (layerDrawable
                                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                                gradientDrawable.setColor(Color.parseColor("#00000000"));
                                GradientDrawable drawableBackground = (GradientDrawable) holder.normalMessageContainer.getBackground();
                                drawableBackground.setColor(Color.parseColor("#00000000"));
                                Logger.error(TAG, "Absolute url = " + absoluteUrl);

                                if (null != absoluteUrl) {
                                    Logger.error(TAG,"AbsoluteURL: "+absoluteUrl);
                                    LocalStorageFactory.loadImageUsingURL(context,absoluteUrl,holder.customImageHolder,R.drawable.cc_thumbnail_default);
                                *//*if (absoluteUrl.contains("giphy")) {

                                    *//**//*RequestOptions requestOptions2 = new RequestOptions()
                                    .fitCenter()
                                    .dontAnimate()
                                    .placeholder(R.drawable.cc_thumbnail_default)
                                    .priority(Priority.IMMEDIATE)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL);

                                    Glide.with(context)
                                            .load(absoluteUrl)
                                            .apply(requestOptions2)
                                            .into(holder.customImageHolder);*//**//*
                                } else {
                                    RequestOptions requestOptions2 = new RequestOptions()
                                    .fitCenter()
                                    .dontAnimate()
                                    .placeholder(R.drawable.cc_thumbnail_default)
                                    .priority(Priority.HIGH)
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
                                    Glide.with(context)
                                            .load(absoluteUrl)
                                            .apply(requestOptions2)
                                            .into(holder.customImageHolder);
                                }*//*
                                }

                                holder.customImageHolder.setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            holder.customImageHolder.setEnabled(false);
                                            Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                            intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, absoluteUrl);
                                            intent.putExtra(StaticMembers.INTENT_IMAGE_SRC, absoluteUrl);
                                            intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                            ((Activity)context).startActivityForResult(intent, 1111);
//                                        context.startActivity(intent);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                break;

                            case "anchor":
                                String temp = botMessageJson.getString("message");
                                CommonUtils.renderHtmlInATextView(context, holder.message, temp);
                                break;

                            default:
                                if (bot != null && bot.botName != null && bot.botName.equals("SoundCloud")) {

                                    Matcher matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(botMessageJson.getString("message"));
                                    final String html = botMessageJson.getString("message");
                                    matcher.find();
                                    final String src = matcher.group(1);

                                    holder.customImageHolder.setVisibility(View.VISIBLE);
                                    holder.message.setVisibility(GONE);

                                    ldrawableImage = (LayerDrawable) holder.messageArrow.getBackground();
                                    shapeImage = ((GradientDrawable) ((RotateDrawable) (ldrawableImage
                                            .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                                    shapeImage.setColor(Color.parseColor("#00000000"));
                                    drawableImage = (GradientDrawable) holder.normalMessageContainer.getBackground();
                                    drawableImage.setColor(Color.parseColor("#00000000"));

                                    RequestOptions requestOptions2 = new RequestOptions().placeholder(R.drawable.cc_ic_play);
                                    Glide.with(context)
                                            .load("")
                                            .apply(requestOptions2)
                                            .into(holder.customImageHolder);

                                    holder.customImageHolder.setOnClickListener(new View.OnClickListener() {

                                        @Override
                                        public void onClick(View v) {
//                                            Intent customTabsIntent = new Intent(context, CCWebViewActivity.class);
//                                            customTabsIntent.putExtra(IntentExtraKeys.WEBSITE_URL, Uri.parse(src));

                                            CCChromeTabs customTabs = new CCChromeTabs(context);
                                            customTabs.loadURL(src);

                                        }
                                    });
                                } else {
                                    holder.message.setText(Html.fromHtml(botMessageJson.getString("message")));
                                }
                                break;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    break;
            }
        }


    */}

    private void rotateRetry(ImageView retry,int repeatCount) {
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(700);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatCount(repeatCount);
        retry.startAnimation(rotate);
    }


    public OneOnOneMessage getMessageByPosition(int position) {
        OneOnOneMessage oneOnOneMessage = null;
        if(cursor.moveToPosition(position)) {
            oneOnOneMessage = new OneOnOneMessage();
            oneOnOneMessage.self = cursor.getInt(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SELF));
            oneOnOneMessage.sentTimestamp = cursor.getLong(cursor.getColumnIndex(OneOnOneMessage.COLUMN_SENT_TIMESTAMP));
            oneOnOneMessage.type = cursor.getString(cursor.getColumnIndex(OneOnOneMessage.COLUMN_MESSAGE_TYPE));
        }
        return oneOnOneMessage;
    }

    /*public void setBtnColor(int viewType, ImageView playbtn, boolean isPlayBtn) {
        if (viewType == TYPE_RIGHT) {
            if (isPlayBtn) {
                playbtn.setBackgroundResource(R.drawable.ic_play_arrow);
            } else {
                playbtn.setBackgroundResource(R.drawable.ic_pause);
            }
            playbtn.getBackground().setColorFilter(new LightingColorFilter(Color.WHITE, Color.WHITE));
        } else {
            if (isPlayBtn) {
                playbtn.setBackgroundResource(R.drawable.ic_play_arrow);
            } else {
                playbtn.setBackgroundResource(R.drawable.ic_pause);
            }
            playbtn.getBackground().setColorFilter(new LightingColorFilter(Color.WHITE,Color.parseColor("#8e8e92")));

        }
    }*/


    private SpannableString createViewLink(final String mess, int flag) {
        /**
         * flag : decides which type of request
         * If 1, used for whiteboard
         * If 2, used for writeboard
         * if 3, used for join chatroom
         * if 4, used for screenshare
         */
        Logger.error(TAG,"createViewLink Og"+mess);
        if (flag == 1) {
            Logger.error("String Whiteboard message = "+mess);
            int startIndex = mess.indexOf("."), endIndex = mess.indexOf("|#|");
            final SpannableString whiteboardrequest = new SpannableString(mess.substring(0, endIndex));
            try {
                final String room = mess.substring(endIndex + 3, mess.length());
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (whiteBoardState == FeatureState.INACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            viewWhiteBoard(room);
                        }
                    }
                };

                whiteboardrequest.setSpan(span, startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                whiteboardrequest.setSpan(new ForegroundColorSpan(color),startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return whiteboardrequest;
        } else if (flag == 2) {
            int startIndex = mess.indexOf("."), endIndex = mess.indexOf("|#|");
            final SpannableString writeboardrequest = new SpannableString(mess.substring(0, endIndex));
            try {
                final String url = mess.substring(endIndex + 3, mess.length());
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (writeBoardState == FeatureState.INACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            viewCollaborativeDocument(url);
                        }
                    }
                };

                writeboardrequest.setSpan(span, startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                writeboardrequest.setSpan(new ForegroundColorSpan(color),startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return writeboardrequest;
        }else if (flag == 3) {
            String jsonStr = mess.substring(mess.indexOf("~")+1,mess.length());
            String chatRoomId = null, chatRoomName = null, chatRoomPassword = null;
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                chatRoomId = String.valueOf(jsonObject.get("chatroom_id"));
                chatRoomName = String.valueOf(jsonObject.get("chatroom_name"));
                chatRoomPassword = String.valueOf(jsonObject.get("chatroom_password"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String str = mess.substring(0,mess.lastIndexOf("~"));
            String spanString = str.substring(str.lastIndexOf(" ")+1,str.length());
            final SpannableString groupInvite = new SpannableString(str);
            try {
                //final String rand = mess.substring(endIndex + 3, mess.length());
                final String finalChatRoomId = chatRoomId;
                final String finalChatRoomName = chatRoomName;
                final String finalChatRoomPassword = chatRoomPassword;
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (groupState == FeatureState.INACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            joinGroup(finalChatRoomId, finalChatRoomName, finalChatRoomPassword);
                        }
                    }
                };

                groupInvite.setSpan(span, str.lastIndexOf(" ")+1, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return groupInvite;
        }else if (flag == 4){
            int startIndex = mess.indexOf("."), endIndex = mess.length();
            final SpannableString screensharerequest = new SpannableString(mess);
            Logger.error(TAG,"ScreenShare spnabble = "+screensharerequest.toString());
            try {
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (screenshareState == FeatureState.ACCESSIBLE) {
                            Intent intent = new Intent(context, CCVideoChatActivity.class);
                            intent.putExtra(StaticMembers.SCREENSHARE_MODE, true);
                            Logger.error(TAG, "screenShare : channel: "+channel);
                            intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, channel);
                            intent.putExtra("CONTACT_ID", String.valueOf(conatctID));
                            intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG, true);
                            intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG, false);
                            context.startActivity(intent);
                        } else {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        }
                    }
                };

                screensharerequest.setSpan(span, startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                screensharerequest.setSpan(new ForegroundColorSpan(color), startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return screensharerequest;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return null;

    }


    private Spannable createDownloadLink(final String messageURL, final String remoteID) {
        Logger.error(TAG, "messageURL : " + messageURL);

        String fileName = messageURL.substring(messageURL.lastIndexOf("=") + 1, messageURL.length());
        Logger.error(TAG, "fileName : " + fileName);
        String message = "has sent a file(" + fileName + ").";
        String fileDownloadStr = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_FILE_DOWNLOAD));
        Logger.error(TAG, "FILE_MESSAGE fileDownload : " + fileDownloadStr);
        message += "\n" + fileDownloadStr;
        SpannableString downloadLink = new SpannableString(message);
        Logger.error(TAG, "FILE_MESSAGE message : " + message);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Logger.error(TAG, "FILE_MESSAGE clicking");
                new FileDownloadHelper().execute(remoteID, messageURL, "0", "0",  "1");
            }
        };
        int startIndex = message.indexOf("\n") + 1;
        downloadLink.setSpan(clickableSpan, startIndex, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return downloadLink;
    }

    private Spannable createOpenLink(String messageLink) {
        Logger.error(TAG, "createOpenLink message : " + messageLink);

        String localFileName = messageLink.substring(messageLink.lastIndexOf("/") + 1, messageLink.length());
        Logger.error(TAG, "createOpenLink fileName : " + localFileName);

        final String localFilePath = messageLink.substring(messageLink.lastIndexOf("#") + 1, messageLink.length());
        Logger.error(TAG, "createOpenLink localFilePath : " + localFilePath);

        String message = "has sent a file(" + localFileName + ").";

//        String fileDownloadStr = (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_FILE_DOWNLOAD));
        String fileDownloadStr = "Click here to open the File";
        Logger.error(TAG, "FILE_MESSAGE fileDownload : " + fileDownloadStr);

        message += "\n" + fileDownloadStr;
        SpannableString openLink = new SpannableString(message);

        Logger.error(TAG, "createOpenLink FILE_MESSAGE message : " + message);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Logger.error(TAG, "createOpenLink FILE_MESSAGE clicking");

                try {
                    Logger.error(TAG, "onClick: localFilePath: "+localFilePath );
                    FileOpenIntentHelper.openFile(context, localFilePath);
                } catch (Exception e) {
                    Logger.error(TAG, "createOpenLink : " + e.toString());
                    e.printStackTrace();
                }
            }
        };
        int startIndex = message.indexOf("\n") + 1;
        openLink.setSpan(clickableSpan, startIndex, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return openLink;
    }

    private void joinGroup(final String finalChatRoomId, final String finalChatRoomName, String finalChatRoomPassword) {
        cometChat.joinGroup(finalChatRoomId, finalChatRoomName, finalChatRoomPassword, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG,"JoinChatroom success = "+jsonObject);

                /*try {
                    Groups group = new Groups();
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
                    group.save();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }*/

                    try {
                        if(!jsonObject.has("type")) {
                            jsonObject.put("type", 2);
                        }
                        if(!jsonObject.has("createdby")){
                            jsonObject.put("createdby", 0);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                Groups.insertNewGroup(jsonObject);

                Intent intent = new Intent(context, CCGroupChatActivity.class);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_ID, finalChatRoomId);
                intent.putExtra(StaticMembers.INTENT_CHATROOM_NAME, finalChatRoomName);
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
                context.startActivity(intent);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error("JoinChatroom fail  = "+jsonObject);
                try{
                    if(jsonObject.has("code")
                            && jsonObject.getString("code").equals("403")
                            && jsonObject.has("message")) {
                        Toast.makeText(context, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void viewWhiteBoard(String room) {
        Intent i = new Intent(context, CCWebViewActivity.class);
        i.putExtra(IntentExtraKeys.WEBSITE_URL,room);
        i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME,"White Board");
        context.startActivity(i);
    }

    private void viewCollaborativeDocument(String url) {
        Intent i = new Intent(context, CCWebViewActivity.class);
        i.putExtra(IntentExtraKeys.WEBSITE_URL,url);
        i.putExtra(IntentExtraKeys.USERNAME, SessionData.getInstance().getName());
        Logger.error(TAG,"WEBSITE URL: "+url);
        i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME,"Collaborative Document");
        context.startActivity(i);
    }

    private Spannable AVBroadcastText(final String message, final Long buddyId , final String roomname) {


        String messageText;
        messageText = message + "\n" + (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_JOIN));

        final SpannableString avbroadcastMessage = new SpannableString(messageText);

        ClickableSpan clickable = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (avBroadcaseState == FeatureState. ACCESSIBLE) {
                    Logger.error(TAG,"Room Name = "+roomname);
                    Intent intent = new Intent(context, CCVideoChatActivity.class);
                    intent.putExtra(StaticMembers.INTENT_ROOM_NAME, roomname);
                    intent.putExtra(StaticMembers.INTENT_VIDEO_FLAG, true);
                    intent.putExtra(StaticMembers.INTENT_AUDIO_FLAG, true);
                    intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.CONTACT_ID, String.valueOf(buddyId));
                    intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG, true);
                    intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG, false);
                    context.startActivity(intent);
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(widget.getContext());
                    alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                }
            }
        };

        avbroadcastMessage.setSpan(clickable, message.length(), messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int color = (int) CometChat.getInstance(context).getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        avbroadcastMessage.setSpan(new ForegroundColorSpan(color),message.length(), messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return avbroadcastMessage;
    }

    public void playAudio(String message, final long sentTimestamp, final ImageView playBtn, final MediaPlayer player, final int viewtype, final TextView audioText, final SeekBar audioSeekBar) {
        try {
            currentPlayingSong = message;
            currentlyPlayingId = sentTimestamp;
            if (timerRunnable != null) {
                seekHandler.removeCallbacks(timerRunnable);
                timerRunnable = null;
            }
//            setBtnColor(viewtype, playBtn, false);
            player.reset();
            player.setDataSource(currentPlayingSong);
            player.prepare();
            player.start();

            final int duration = player.getDuration();
            audioSeekBar.setMax(duration);
            timerRunnable = new Runnable() {
                @Override
                public void run() {

                    int pos = player.getCurrentPosition();
                    audioSeekBar.setProgress(pos);

                    if (player.isPlaying() && pos < duration) {
                        audioText.setText(CommonUtils.convertTimeStampToDurationTime(player.getCurrentPosition()));
                        seekHandler.postDelayed(this, 250);
                    } else {
                        seekHandler
                                .removeCallbacks(timerRunnable);
                        timerRunnable = null;
                    }
                }

            };
            seekHandler.postDelayed(timerRunnable, 100);
            notifyDataSetChanged();

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPlayingSong = "";
                    currentlyPlayingId = 0l;
//                    setBtnColor(viewtype, playBtn, true);
                    seekHandler
                            .removeCallbacks(timerRunnable);
                    timerRunnable = null;
                    mp.stop();
                    audioText.setText(CommonUtils.convertTimeStampToDurationTime(duration));
                    audioSeekBar.setProgress(0);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAudio(String message, long sentTimeStamp, final MediaPlayer player, final ImageView playButton, final TextView audioLength, final SeekBar audioSeekBar){
        try {
            currentPlayingSong = message;
            currentlyPlayingId = sentTimeStamp;
            if (timerRunnable != null) {
                seekHandler.removeCallbacks(timerRunnable);
                timerRunnable = null;
            }
//            setBtnColor(viewtype, playBtn, false);
            player.reset();
            player.setDataSource(currentPlayingSong);
            player.prepare();
            player.start();

            final int duration = player.getDuration();
            audioSeekBar.setMax(duration);
            timerRunnable = new Runnable() {
                @Override
                public void run() {

                    int pos = player.getCurrentPosition();
                    audioSeekBar.setProgress(pos);

                    if (player.isPlaying() && pos < duration) {
                        audioLength.setText(CommonUtils.convertTimeStampToDurationTime(player.getCurrentPosition()));
                        seekHandler.postDelayed(this, 250);
                    } else {
                        seekHandler
                                .removeCallbacks(timerRunnable);
                        timerRunnable = null;
                    }
                }

            };
            seekHandler.postDelayed(timerRunnable, 100);
            notifyDataSetChanged();

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPlayingSong = "";
                    currentlyPlayingId = 0l;
//                    setBtnColor(viewtype, playBtn, true);
                    seekHandler
                            .removeCallbacks(timerRunnable);
                    timerRunnable = null;
                    mp.stop();
                    audioLength.setText(CommonUtils.convertTimeStampToDurationTime(duration));
                    audioSeekBar.setProgress(0);
                    playButton.setImageResource(R.drawable.ic_play_arrow);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Spannable processAVchatCallerName(String messageText) {
        SpannableString AVchatString = new SpannableString(messageText);
        final StyleSpan boldStyle = new StyleSpan(android.graphics.Typeface.BOLD);
        int startIndex = messageText.length(), endIndex = messageText.length();
            if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_FROM)))) {
                startIndex = ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_FROM))).length();
            } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_TO)))) {
                startIndex = ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_TO))).length();
                if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REJECTED)))) {
                    endIndex = messageText.indexOf((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REJECTED)));
                } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCELLED)))) {
                    endIndex = messageText.indexOf((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCELLED)));
                } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)))) {
                    endIndex = messageText.indexOf((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)));
                }
            } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_ENEDED_DURATION)))) {
                startIndex = messageText.length();
            } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_ANSWER_FROM)))) {
                startIndex = ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCELLED))).length();
            }
            if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_FROM)))) {
                startIndex = ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_FROM))).length();
            } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_TO)))) {
                startIndex = ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_TO))).length();
                if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REJECTED)))) {
                    endIndex = messageText.indexOf((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_REJECTED)));
                } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCELLED)))) {
                    endIndex = messageText.indexOf((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCELLED)));
                } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)))) {
                    endIndex = messageText.indexOf((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BUSY)));
                }
            } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CALL_ENEDED_DURATION)))) {
                startIndex = messageText.length();
            } else if (messageText.contains((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_ANSWER_FROM)))) {
                startIndex = ((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CANCELLED))).length();
            }
//        }
        AVchatString.setSpan(boldStyle, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return AVchatString;
    }

    @Override
    public long getHeaderId(int i) {
        OneOnOneMessage oneOnOneMessage = getMessageByPosition(i);
        return Long.parseLong(CommonUtils.getDateId(oneOnOneMessage.sentTimestamp));
    }

    @Override
    public DateItemHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_message_list_header, parent, false);
        return new OneOnOneMessageAdapter.DateItemHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(DateItemHolder dateItemHolder, int i , long key) {
        String str = key+"";
        if(str.length() == 7){
            str = "0"+str;
        }
        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        try {
            Date date = dateFormat.parse(str);
            dateItemHolder.txtMessageDate.setText(CommonUtils.getFormattedDate(date.getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }




    /*static class MessageItemHolder extends RecyclerView.ViewHolder {
          EmojiTextView message,tvOnlySmiley;
          TextView messageTimestamp, audioTime, avchatMessageTimeStamp, avchatMessage;
          ImageView imageHolder, audioPlayButton, videoThumb, videoMessageButton, messageTick,customImageHolder,imagePendingMessage;
          RelativeLayout normalMessageContainer, avchatMessageContainer, audioNoteContainer;
          View messageArrow,isTypingView;
          ProgressBar wheel;
          RoundedImageView avatar;
          SeekBar audioSeekbar;
          CometChat cometChat = CometChat.getInstance(context);

         int viewType;
         int rightBubbleColor = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
         int rightBubbleTextColor = Color.WHITE;
         int leftBubbleColor = Color.parseColor("#e6e9ed");
         int leftBubbleTextColor = Color.parseColor("#8e8e92");

        public MessageItemHolder(View view,int viewType) {
            super(view);
            this.viewType = viewType;
//            wheel.getIndeterminateDrawable().setColorFilter(rightBubbleColor, PorterDuff.Mode.SRC_ATOP);
            if(viewType == TYPE_FOOTER){
                isTypingView =  view;
                avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
            }else if(viewType == TYPE_RIGHT){
                message = (EmojiTextView) view.findViewById(R.id.textViewOneOnOneMessageRight);
                messageTimestamp = (TextView) view.findViewById(R.id.textViewOneOnOneTimestampRight);
                imageHolder = (ImageView) view.findViewById(R.id.imageViewOneOnOneImageMessageRight);
                videoThumb = (ImageView) view.findViewById(R.id.imageViewOneOnOneVideoMessageRight);
                videoMessageButton = (ImageView) view.findViewById(R.id.imageViewOneOnOneVideoMessageButton);
                normalMessageContainer = (RelativeLayout) view
                        .findViewById(R.id.linearLayoutParentOneOnOneMessageRightContainer);
                avchatMessageContainer = (RelativeLayout) view
                        .findViewById(R.id.relativeLayoutAVchatMessageContainer);
                avchatMessageTimeStamp = (TextView) view.findViewById(R.id.textViewAVchatMessageTimeStamp);
                avchatMessage = (TextView) view.findViewById(R.id.textViewAVchatMessage);
                messageArrow = view.findViewById(R.id.rightArrow);
                wheel = (ProgressBar) view.findViewById(R.id.progressWheelVideo);
                avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
                messageTick = (ImageView) view.findViewById(R.id.imageviewMessageTick);
                customImageHolder = (ImageView) view.findViewById(R.id.customImageViewOneOnOneImageMessageRight);
                audioNoteContainer = (RelativeLayout) view.findViewById(R.id.relativeLayoutAudioNoteContainer);
                audioPlayButton = (ImageView) view.findViewById(R.id.imageViewPlayIcon);
                tvOnlySmiley = (EmojiTextView) view.findViewById(R.id.textViewOneOnOneMessageSmileyRight);
                audioTime = (TextView) view.findViewById(R.id.textViewTime);
                audioSeekbar = (SeekBar) view.findViewById(R.id.seek_bar);
                imagePendingMessage = (ImageView) view.findViewById(R.id.img_message_pending);
                LayerDrawable ldrawable = (LayerDrawable) messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                shape.setColor(rightBubbleColor);
                GradientDrawable drawable = (GradientDrawable) normalMessageContainer.getBackground();
                drawable.setColor(rightBubbleColor);
                message.setTextColor(rightBubbleTextColor);
                audioTime.setTextColor(Color.WHITE);
            }else{
                message = (EmojiTextView) view.findViewById(R.id.textViewOneToOneMessageLeft);
                messageTimestamp = (TextView) view.findViewById(R.id.textViewOneOnOneTimestampLeft);
                imageHolder = (ImageView) view.findViewById(R.id.imageViewOneOnOneImageMessageLeft);
                videoThumb = (ImageView) view.findViewById(R.id.imageViewOneOnOneVideoMessageLeft);
                videoMessageButton = (ImageView) view.findViewById(R.id.imageViewOneOnOneVideoMessageButton);
                normalMessageContainer = (RelativeLayout) view
                        .findViewById(R.id.linearLayoutParentOneOnOneMessageLeftContainer);
                avchatMessageContainer = (RelativeLayout) view
                        .findViewById(R.id.relativeLayoutAVchatMessageContainer);
                avchatMessageTimeStamp = (TextView) view.findViewById(R.id.textViewAVchatMessageTimeStamp);
                avchatMessage = (TextView) view.findViewById(R.id.textViewAVchatMessage);
                messageArrow = view.findViewById(R.id.leftArrow);
                wheel = (ProgressBar) view.findViewById(R.id.progressWheelVideo);
                avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
                messageTick = (ImageView) view.findViewById(R.id.imageviewMessageTick);
                customImageHolder = (ImageView) view.findViewById(R.id.customImageViewOneOnOneImageMessageLeft);
                tvOnlySmiley = (EmojiTextView) view.findViewById(R.id.textViewOneOnOneMessageSmileyLeft);
                audioNoteContainer = (RelativeLayout) view.findViewById(R.id.relativeLayoutAudioNoteContainer);
                audioPlayButton = (ImageView) view.findViewById(R.id.imageViewPlayIcon);
                audioTime = (TextView) view.findViewById(R.id.textViewTime);
                audioSeekbar = (SeekBar) view.findViewById(R.id.seek_bar);
                LayerDrawable ldrawable = (LayerDrawable) messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                shape.setColor(leftBubbleColor);
                GradientDrawable drawable = (GradientDrawable) normalMessageContainer.getBackground();
                drawable.setColor(leftBubbleColor);
                message.setTextColor(leftBubbleTextColor);
                audioTime.setTextColor(leftBubbleTextColor);
            }
        }
    }*/

    static class DateItemHolder extends RecyclerView.ViewHolder{

        public TextView txtMessageDate;
        public DateItemHolder(View view) {
            super(view);
            txtMessageDate = (TextView) view.findViewById(R.id.txt_message_date);
        }

    }

    public interface RetryCallback{
        public void onRetryClicked(long localMessageId);
    }
}
