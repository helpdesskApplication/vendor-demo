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
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inscripts.glide.Glide;
import com.inscripts.glide.load.DataSource;
import com.inscripts.glide.load.engine.GlideException;
import com.inscripts.glide.request.RequestListener;
import com.inscripts.glide.request.RequestOptions;
import com.inscripts.glide.request.target.Target;
import com.inscripts.activities.CCWebViewActivity;
import com.inscripts.custom.EmojiTextView;
import com.inscripts.custom.RoundedImageView;
import com.inscripts.custom.StickyHeaderAdapter;
import com.inscripts.enums.FeatureState;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.RecyclerViewCursorAdapter;
import com.inscripts.helpers.FileOpenIntentHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.IntentExtraKeys;
import com.inscripts.plugins.Smilies;
import com.inscripts.plugins.Stickers;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;
import com.inscripts.utils.StaticMembers;
import com.inscripts.custom.CCChromeTabs;
import com.inscripts.helpers.CCPermissionHelper;

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
import activities.CCImagePreviewActivity;
import activities.CCSingleChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import helpers.FileDownloadHelper;
import models.Bot;
import models.Contact;
import models.GroupMessage;
import videochat.CCVideoChatActivity;
import viewHolders.LeftAudioViewHolder;
import viewHolders.LeftImageVideoViewHolder;
import viewHolders.LeftMessageViewHolder;
import viewHolders.RightAudioViewHolder;
import viewHolders.RightImageVideoViewHolder;
import viewHolders.RightMessageViewHolder;

import static android.view.View.GONE;

public class GroupMessageAdapter extends RecyclerViewCursorAdapter<RecyclerView.ViewHolder> implements
        StickyHeaderAdapter<OneOnOneMessageAdapter.DateItemHolder> {

    private static final String TAG = GroupMessageAdapter.class.getSimpleName();
    private static final int RIGHT_TEXT_MESSAGE = 940;
    private static final int LEFT_TEXT_MESSAGE = 489;
    private static final int RIGHT_STICKER_MESSAGE = 169;
    private static final int LEFT_STICKER_MESSAGE = 825;
    private static final int LEFT_EMOJI_MESSAGE = 850;
    private static final int RIGHT_EMOJI_MESSAGE = 165;
    private static final int LEFT_WHITEBOARD_MESSAGE = 693;
    private static final int RIGHT_WHITEBOARD_MESSAGE = 946;
    private static final int LEFT_WRITEBOARD_MESSAGE = 127;
    private static final int RIGHT_WRITEBOARD_MESSAGE = 548;
    private static final int LEFT_SCREENSHARE_MESSAGE = 457;
    private static final int RIGHT_SCREENSHARE_MESSAGE = 279;
    private static final int BOT_MESSAGE = 189;
    private static final int LEFT_IMAGE_MESSAGE = 510;
    private static final int LEFT_IMAGE_DOWNLOADING_MESSAGE = 284;
    private static final int RIGHT_IMAGE_MESSAGE = 380;
    private static final int RIGHT_IMAGE_DOWNLOADING_MESSAGE = 804;
    private static final int LEFT_VIDEO_DOWNLOADING_MESSAGE = 790;
    private static final int LEFT_VIDEO_MESSAGE = 374;
    private static final int RIGHT_VIDEO_DOWNLOADING_MESSAGE = 999;
    private static final int RIGHT_VIDEO_MESSAGE = 188;
    private static final int LEFT_AUDIO_DOWNLOADING_MESSAGE = 38;
    private static final int LEFT_AUDIO_MESSAGE = 230;
    private static final int RIGHT_AUDIO_DOWNLOADING_MESSAGE = 909;
    private static final int RIGHT_AUDIO_MESSAGE = 393;
    private static final int LEFT_HANDWRITE_DOWNLOADING_MESSAGE = 617;
    private static final int LEFT_HANDWRITE_MESSAGE = 403;
    private static final int RIGHT_HANDWRITE_DOWNLOADING_MESSAGE = 525;
    private static final int RIGHT_HANDWRITE_MESSAGE = 427;
    private static final int RIGHT_AVBROADCAST_REQUEST_MESSAGE = 482;
    private static final int LEFT_AVBROADCAST_REQUEST_MESSAGE = 68;
    private static final int LEFT_AVBROADCAST_EXPIRED_MESSAGE = 952;
    private static final int RIGHT_AVBROADCAST_EXPIRED_MESSAGE = 635;
    private static final int AVCHAT_INCOMING_CALL = 281;
    private static final int AUDIOCHAT_INCOMING_CALL = 866;
    private static final int LEFT_FILE_MESSAGE = 914;
    private static final int LEFT_FILE_DOWNLOADED = 11;
    private static final int LEFT_FILE_DOWNLOADING = 434;
    private static final int RIGHT_FILE_MESSAGE = 90;
    private static final int RIGHT_FILE_DOWNLOADED = 221;
    private static final int RIGHT_FILE_DOWNLOADING = 665;
    private Activity context;
    private Cursor cursor;
    private static final int TYPE_LEFT = 0;
    private static final int TYPE_RIGHT = 1;
    private static LongSparseArray<Bitmap> videoThumbnails;
    private static LongSparseArray<Integer> audioDurations;
    private Long currentlyPlayingId = 0l;
    private MediaPlayer player;
    private String currentPlayingSong = "";
    private Runnable timerRunnable;
    private Handler seekHandler = new Handler();
    private String chatroomid;
    private CometChat cometChat;
    private String channel;
    private long ContactID;
    private int color;
    private FeatureState grpWhiteBoardState;
    private FeatureState grpWriteBoardState;
    private String avatarUrl;
    private FeatureState grpAvCallState, grpAudioCallState;
    private FeatureState grpAVBroadcastState;

    public GroupMessageAdapter(Context context, Cursor c) {
        super(c);
        this.context = (Activity) context;
        this.cursor = c;
        videoThumbnails = new LongSparseArray<>();
        audioDurations = new LongSparseArray<>();
        player = CommonUtils.getPlayerInstance();
    }

    public GroupMessageAdapter(Activity context, Cursor c,String chatroomID) {
        super(c);
        this.context = context;
        this.cursor = c;
        videoThumbnails = new LongSparseArray<>();
        audioDurations = new LongSparseArray<>();
        player = CommonUtils.getPlayerInstance();
        chatroomid = chatroomID;
        cometChat = CometChat.getInstance(context);
        color = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        initializeFeatureState();
    }

    private void initializeFeatureState() {
        grpWhiteBoardState = (FeatureState)cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_WHITEBOARD_ENABLED));
        grpWriteBoardState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_WRITEBOARD_ENABLED));
        grpAvCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_VIDEO_CALL_ENABLED));
        grpAudioCallState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_AUDIO_CALL_ENABLED));
        grpAVBroadcastState = (FeatureState) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE, SettingSubType.GRP_AV_BROADCAST_ENABLED));
    }


    @Override
    public void swapCursor(Cursor newCursor) {
        super.swapCursor(newCursor);
        this.cursor = newCursor;
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = 0;
        GroupMessage message = getMessageByPosition(position);
        Logger.error(TAG,"\ngetItemViewType : " + position + " GroupMsg : " + message);
        Logger.error(TAG, "getItemViewType: messatype: "+cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_TYPE)));
        Logger.error(TAG, "getItemViewType: isMyMessage: "+(message.fromId == SessionData.getInstance().getId()));
        if (message.fromId == SessionData.getInstance().getId() && !message.type.equals(MessageTypeKeys.BOT_RESPONSE)) {
            switch(cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_TYPE))) {
                case MessageTypeKeys.NORMAL_MESSAGE:
                    viewType = RIGHT_TEXT_MESSAGE;
                    break;
                case MessageTypeKeys.STICKER:
                    viewType = RIGHT_STICKER_MESSAGE;
                    break;
                /*case MessageTypeKeys.EMOJI_MESSAGE:
                    viewType = RIGHT_EMOJI_MESSAGE;
                    break;*/
                case MessageTypeKeys.WHITEBOARD_MESSAGE:
                    viewType = RIGHT_WHITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.WRITEBOARD_MESSAGE:
                    viewType = RIGHT_WRITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.SCREENSHARE_MESSAGE:
                    viewType = RIGHT_SCREENSHARE_MESSAGE;
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
                case MessageTypeKeys.AUDIO_DOWNLOADING:
                    viewType = RIGHT_AUDIO_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    viewType = RIGHT_AUDIO_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_DOWNLOADING:
                    viewType = RIGHT_HANDWRITE_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_MESSAGE:
                    viewType = RIGHT_HANDWRITE_MESSAGE;
                    break;
                case MessageTypeKeys.GRP_AVBROADCAST_REQUEST:
                    viewType = RIGHT_AVBROADCAST_REQUEST_MESSAGE;
                    break;
                case MessageTypeKeys.AVBROADCAST_EXPIRED:
                    viewType = RIGHT_AVBROADCAST_EXPIRED_MESSAGE;
                    break;
            }
        }else {
            switch(cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_TYPE))) {
                case MessageTypeKeys.NORMAL_MESSAGE:
                    viewType = LEFT_TEXT_MESSAGE;
                    break;
                case MessageTypeKeys.STICKER:
                    viewType = LEFT_STICKER_MESSAGE;
                    break;
                /*case MessageTypeKeys.EMOJI_MESSAGE:
                    viewType = LEFT_EMOJI_MESSAGE;
                    break;*/
                case MessageTypeKeys.WHITEBOARD_MESSAGE:
                    viewType = LEFT_WHITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.WRITEBOARD_MESSAGE:
                    viewType = LEFT_WRITEBOARD_MESSAGE;
                    break;
                case MessageTypeKeys.SCREENSHARE_MESSAGE:
                    viewType = LEFT_SCREENSHARE_MESSAGE;
                    break;
                case MessageTypeKeys.BOT_RESPONSE:
                    viewType = BOT_MESSAGE;
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
                case MessageTypeKeys.AUDIO_DOWNLOADING:
                    viewType = LEFT_AUDIO_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.AUDIO_MESSAGE:
                    viewType = LEFT_AUDIO_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_DOWNLOADING:
                    viewType = LEFT_HANDWRITE_DOWNLOADING_MESSAGE;
                    break;
                case MessageTypeKeys.HANDWRITE_MESSAGE:
                    viewType = LEFT_HANDWRITE_MESSAGE;
                    break;
                case MessageTypeKeys.GRP_AVBROADCAST_REQUEST:
                    viewType = LEFT_AVBROADCAST_REQUEST_MESSAGE;
                    break;
                case MessageTypeKeys.AVBROADCAST_EXPIRED:
                    viewType = LEFT_AVBROADCAST_EXPIRED_MESSAGE;
                    break;
                case MessageTypeKeys.AVCHAT_INCOMING_CALL:
                    viewType = AVCHAT_INCOMING_CALL;
                    break;
                case MessageTypeKeys.AUDIOCHAT_INCOMING_CALL:
                    viewType = AUDIOCHAT_INCOMING_CALL;
                    break;
                case MessageTypeKeys.FILE_MESSAGE:
                    viewType = LEFT_FILE_MESSAGE;
                    break;
                case MessageTypeKeys.FILE_DOWNLOADED:
                    viewType = LEFT_FILE_DOWNLOADED;
                    break;
            }
        }
        return viewType;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       /*if(viewType == TYPE_RIGHT){
           final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_chat_message_chatroom_right, parent, false);
           return new MessageItemHolder(view,viewType);
       }else {
           final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_chat_message_chatroom_left, parent, false);
           return new MessageItemHolder(view,viewType);
       }*/
        RecyclerView.ViewHolder messageHolder = null;
        switch (viewType){
            case LEFT_TEXT_MESSAGE:
                View leftTextMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftTextMessage);
                break;
            case RIGHT_TEXT_MESSAGE:
                View rightTextMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightTextMessage);
                break;
            case RIGHT_STICKER_MESSAGE:
                View rightStickerMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightStickerMessage);
                break;
            case LEFT_STICKER_MESSAGE:
                View leftStickerMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftStickerMessage);
                break;
            /*case LEFT_EMOJI_MESSAGE:
                View leftEmojiMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftEmojiMessage);
                break;
            case RIGHT_EMOJI_MESSAGE:
                View rightEmojiMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightEmojiMessage);
                break;*/
            case LEFT_WHITEBOARD_MESSAGE:
                View leftWhiteBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftWhiteBoardMessage);
                break;
            case RIGHT_WHITEBOARD_MESSAGE:
                View rightWhiteBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightWhiteBoardMessage);
                break;
            case LEFT_WRITEBOARD_MESSAGE:
                View leftWriteBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftWriteBoardMessage);
                break;
            case RIGHT_WRITEBOARD_MESSAGE:
                View rightWriteBoardMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightWriteBoardMessage);
                break;
            case RIGHT_SCREENSHARE_MESSAGE:
                View rightScreenShareMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightScreenShareMessage);
                break;
            case LEFT_SCREENSHARE_MESSAGE:
                View leftScreenShareMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftScreenShareMessage);
                break;
            case BOT_MESSAGE:
                View botMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, botMessage);
                break;
            case LEFT_IMAGE_DOWNLOADING_MESSAGE:
                View leftImageDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, leftImageDownloadingMessage);
                break;
            case LEFT_IMAGE_MESSAGE:
                View leftImageMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, leftImageMessage);
                break;
            case RIGHT_IMAGE_DOWNLOADING_MESSAGE:
                View rightImageDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                messageHolder = new RightImageVideoViewHolder(context, rightImageDownloadingMessage);
                break;
            case RIGHT_IMAGE_MESSAGE:
                View rightImageMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                messageHolder = new RightImageVideoViewHolder(context, rightImageMessage);
                break;
            case RIGHT_VIDEO_DOWNLOADING_MESSAGE:
                View rightVideoDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                messageHolder = new RightImageVideoViewHolder(context, rightVideoDownloadingMessage);
                break;
            case RIGHT_VIDEO_MESSAGE:
                View rightVideoMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                messageHolder = new RightImageVideoViewHolder(context, rightVideoMessage);
                break;
            case LEFT_VIDEO_DOWNLOADING_MESSAGE:
                View leftVideoDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, leftVideoDownloadingMessage);
                break;
            case LEFT_VIDEO_MESSAGE:
                View leftVideoMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, leftVideoMessage);
                break;
            case LEFT_AUDIO_DOWNLOADING_MESSAGE:
                View leftAudioDownloadingMessageHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_left, parent, false);
                messageHolder = new LeftAudioViewHolder(context, leftAudioDownloadingMessageHolder);
                break;
            case LEFT_AUDIO_MESSAGE:
                View leftAudioMessageHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_left, parent, false);
                messageHolder = new LeftAudioViewHolder(context, leftAudioMessageHolder);
                break;
            case RIGHT_AUDIO_DOWNLOADING_MESSAGE:
                View rightAudioDownloadingHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_right, parent, false);
                messageHolder = new RightAudioViewHolder(context, rightAudioDownloadingHolder);
                break;
            case RIGHT_AUDIO_MESSAGE:
                View rightAudioMessageHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_audionote_layout_right, parent, false);
                messageHolder = new RightAudioViewHolder(context, rightAudioMessageHolder);
                break;
            case RIGHT_HANDWRITE_DOWNLOADING_MESSAGE:
                View righthandwriteDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                messageHolder = new RightImageVideoViewHolder(context, righthandwriteDownloadingMessage);
                break;
            case RIGHT_HANDWRITE_MESSAGE:
                View rightHandwriteMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_right, parent, false);
                messageHolder = new RightImageVideoViewHolder(context, rightHandwriteMessage);
                break;
            case LEFT_HANDWRITE_DOWNLOADING_MESSAGE:
                View leftHandwriteDownloadingMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, leftHandwriteDownloadingMessage);
                break;
            case LEFT_HANDWRITE_MESSAGE:
                View leftHandwriteMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_image_video_layout_left, parent, false);
                messageHolder = new LeftImageVideoViewHolder(context, leftHandwriteMessage);
                break;
            case LEFT_AVBROADCAST_REQUEST_MESSAGE:
                View leftAVBroadcastRequestMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(leftAVBroadcastRequestMessage);
                break;
            case RIGHT_AVBROADCAST_REQUEST_MESSAGE:
                View rightAVBroadcastRequestMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightAVBroadcastRequestMessage);
                break;
            case LEFT_AVBROADCAST_EXPIRED_MESSAGE:
                View avBroadcastExpiredMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(avBroadcastExpiredMessage);
                break;
            case RIGHT_AVBROADCAST_EXPIRED_MESSAGE:
                View rightAVBroadcastExpiredMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_right, parent, false);
                messageHolder = new RightMessageViewHolder(rightAVBroadcastExpiredMessage);
                break;
            case AVCHAT_INCOMING_CALL:
                View avChatIncomingCallMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(avChatIncomingCallMessage);
                break;
            case AUDIOCHAT_INCOMING_CALL:
                View audioChatIncomingCallMessage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(audioChatIncomingCallMessage);
                break;
            case LEFT_FILE_MESSAGE:
                View fileMesage = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(fileMesage);
                break;
            case LEFT_FILE_DOWNLOADED:
                View fileMessageDownloaded = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_text_layout_left, parent, false);
                messageHolder = new LeftMessageViewHolder(fileMessageDownloaded);
                break;
        }
        return messageHolder;
    }





    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final Cursor cursor) {
        long remoteId = cursor.getLong(cursor.getColumnIndex(GroupMessage.COLUMN_REMOTE_ID));
        long fromId = cursor.getLong(cursor.getColumnIndex(GroupMessage.COLUMN_FROM_ID));
        int insertedBy = cursor.getInt(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_INSERTED_BY));
        final String message = cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE));
        final long sentTimestamp = cursor.getLong(cursor.getColumnIndex(GroupMessage.COLUMN_SENT_TIMESTAMP));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_IMAGE_URL));
        final String textColor = cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_TEXT_COLOR));
        final String senderName = cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_SENDER_NAME));
        final int messageStatus = cursor.getInt(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_STATUS));
        final long localId = cursor.getLong(cursor.getColumnIndex("ID"));
        Contact contact = Contact.getContactDetails(fromId);
        channel = imageUrl;
        if (contact != null) {
            avatarUrl = contact.avatarURL;
        }

        switch (holder.getItemViewType()){
            case LEFT_TEXT_MESSAGE:
                LeftMessageViewHolder leftTextMessageHolder = (LeftMessageViewHolder) holder;
                leftTextMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftTextMessageHolder.senderName.setText(senderName);
                if(message.contains("<img class=\"cometchat_smiley\"")){
                    Spannable spannable = Smilies.convertImageTagToEmoji(message, context, false,
                            R.drawable.class,(int)context.getResources().getDimension(R.dimen.emoji_size));
                    leftTextMessageHolder.textMessage.setText(spannable);
                }else {
                    leftTextMessageHolder.textMessage.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                }
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftTextMessageHolder.avatar, R.drawable.cc_default_avatar);
                break;
            case RIGHT_TEXT_MESSAGE:
                final RightMessageViewHolder rightTextMessageHolder = (RightMessageViewHolder) holder;
                rightTextMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightTextMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightTextMessageHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(message.contains("<img class=\"cometchat_smiley\"")){
                    Spannable spannable = Smilies.convertImageTagToEmoji(message, context, false,
                            R.drawable.class,(int)context.getResources().getDimension(R.dimen.emoji_size));
                    rightTextMessageHolder.textMessage.setText(spannable);
                }else {
                    rightTextMessageHolder.textMessage.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                }
                rightTextMessageHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightTextMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightTextMessageHolder.retry.setVisibility(View.VISIBLE);
                    rightTextMessageHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback) context).onRetryClicked(localId);
                                rotateRetry(rightTextMessageHolder.retry,1);
                            }
                        }
                    });
                    break;
                }else if(messageStatus == 0){
                    rightTextMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightTextMessageHolder.messageStatus.setVisibility(View.GONE);
                }
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
                                ((RetryCallback) context).onRetryClicked(localId);
                                rotateRetry(rightStickerMessageHolder.retry,1);
                            }
                        }
                    });
                    break;
                }else if(messageStatus == 0){
                    rightStickerMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightStickerMessageHolder.messageStatus.setVisibility(View.GONE);
                }
                if(message.contains("CC^CONTROL_")){
                    rightStickerMessageHolder.textMessage.setText(Stickers.handleSticker(message));
                }else{
                    rightStickerMessageHolder.textMessage.setText(Stickers.getSpannableStickerString(message));
                }
                break;
            case LEFT_STICKER_MESSAGE:
                LeftMessageViewHolder leftStickerMessage = (LeftMessageViewHolder) holder;
                leftStickerMessage.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftStickerMessage.leftArrow.setVisibility(View.GONE);
                leftStickerMessage.textMessage.setBackgroundColor(Color.argb(00,00,00,00));
                leftStickerMessage.senderName.setText(senderName);
                if(message.contains("CC^CONTROL_")){
                    leftStickerMessage.textMessage.setText(Stickers.handleSticker(message));
                }else{
                    leftStickerMessage.textMessage.setText(Stickers.getSpannableStickerString(message));
                }
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftStickerMessage.avatar, R.drawable.cc_default_avatar);
                break;

            /*case LEFT_EMOJI_MESSAGE:
                LeftMessageViewHolder leftEmojiMessageHolder = (LeftMessageViewHolder) holder;
                leftEmojiMessageHolder.senderName.setText(senderName);
                leftEmojiMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftEmojiMessageHolder.textMessage.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftEmojiMessageHolder.avatar, R.drawable.cc_default_avatar);
                break;
            case RIGHT_EMOJI_MESSAGE:
                RightMessageViewHolder rightEmojiMessageHolder = (RightMessageViewHolder) holder;
                rightEmojiMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightEmojiMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightEmojiMessageHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(messageStatus == 0){
                    rightEmojiMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightEmojiMessageHolder.messageStatus.setVisibility(View.GONE);
                }
                rightEmojiMessageHolder.textMessage.setEmojiText(message, (int) context.getResources().getDimension(R.dimen.emoji_size));
                break;*/
            case LEFT_WHITEBOARD_MESSAGE:
                LeftMessageViewHolder leftWhiteBoardmessageHolder = (LeftMessageViewHolder) holder;
                leftWhiteBoardmessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftWhiteBoardmessageHolder.senderName.setText(senderName);
                leftWhiteBoardmessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftWhiteBoardmessageHolder.textMessage.setText(createViewLink("has shared a whiteboard. Click here to view|#|"+message, 1));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftWhiteBoardmessageHolder.avatar, R.drawable.cc_default_avatar);
                break;
            case RIGHT_WHITEBOARD_MESSAGE:
                RightMessageViewHolder rightWhiteBoardMessageHolder = (RightMessageViewHolder) holder;
                rightWhiteBoardMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightWhiteBoardMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightWhiteBoardMessageHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightWhiteBoardMessageHolder.textMessage.setText("Successfully shared whiteboard");
                if(messageStatus == 0){
                    rightWhiteBoardMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightWhiteBoardMessageHolder.messageStatus.setVisibility(View.GONE);
                }
                break;
            case RIGHT_WRITEBOARD_MESSAGE:
                RightMessageViewHolder rightWriteBoardMessageHolder = (RightMessageViewHolder) holder;
                rightWriteBoardMessageHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightWriteBoardMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightWriteBoardMessageHolder.textMessage.setText("Successfully share WriteBoard");
                rightWriteBoardMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if(messageStatus == 0){
                    rightWriteBoardMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightWriteBoardMessageHolder.messageStatus.setVisibility(View.GONE);
                }
                break;
            case LEFT_WRITEBOARD_MESSAGE:
                LeftMessageViewHolder leftWriteBoardMessageHolder = (LeftMessageViewHolder) holder;
                leftWriteBoardMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftWriteBoardMessageHolder.senderName.setText(senderName);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftWriteBoardMessageHolder.avatar, R.drawable.cc_default_avatar);
                leftWriteBoardMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftWriteBoardMessageHolder.textMessage.setText(createViewLink("has shared his/her collaborative document with you. Click here to view|#|"+message, 2));
                break;
            case LEFT_SCREENSHARE_MESSAGE:
                LeftMessageViewHolder leftScreenShareMessage = (LeftMessageViewHolder) holder;
                leftScreenShareMessage.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftScreenShareMessage.senderName.setText(senderName);
                leftScreenShareMessage.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftScreenShareMessage.textMessage.setText(createViewLink(message, 4));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftScreenShareMessage.avatar, R.drawable.cc_default_avatar);
                break;
            case RIGHT_SCREENSHARE_MESSAGE:
                RightMessageViewHolder rightScreenShareMessage = (RightMessageViewHolder) holder;
                rightScreenShareMessage.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightScreenShareMessage.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightScreenShareMessage.textMessage.setText("Successfully shared screen");
                rightScreenShareMessage.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                if(messageStatus == 0){
                    rightScreenShareMessage.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightScreenShareMessage.messageStatus.setVisibility(View.GONE);
                }
                break;

            case BOT_MESSAGE:
                LeftImageVideoViewHolder botMessageHolder = (LeftImageVideoViewHolder) holder;
                try {
                    JSONObject botJson = new JSONObject(message);
                    Bot bot = Bot.getBotDetails(botJson.getString("botid"));
                    if (bot != null && bot.botAvatar != null) {
                        LocalStorageFactory.loadImageUsingURL(context,bot.botAvatar,botMessageHolder.avatar,R.drawable.cc_default_avatar);
                        botMessageHolder.btnPlayVideo.setVisibility(View.GONE);
                        botMessageHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                        botMessageHolder.senderName.setVisibility(View.GONE);
                        botMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                        switch (botJson.getString("messagetype")){
                            case "image":
                                Document doc = Jsoup.parseBodyFragment(botJson.getString("message"));
                                Element imageElement = doc.select("img").first();
                                final String absoluteUrl = imageElement.absUrl("src");
                                botMessageHolder.imageMessage.setVisibility(View.VISIBLE);
                                LocalStorageFactory.loadImageUsingURL(context, absoluteUrl, botMessageHolder.imageMessage, R.drawable.ic_broken_image);
                                botMessageHolder.imageTitle.setVisibility(View.GONE);
                                botMessageHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

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
                                botMessageHolder.imageMessage.setVisibility(View.GONE);
                                botMessageHolder.imageTitle.setVisibility(View.VISIBLE);
                                CommonUtils.renderHtmlInATextView(context, botMessageHolder.imageTitle, temp);
                                break;

                            default:
                                botMessageHolder.imageMessage.setVisibility(View.GONE);
                                botMessageHolder.imageTitle.setVisibility(View.VISIBLE);
                                Logger.error(TAG, "onBindViewHolder: bot name: "+bot.botName);
                                if (bot != null && bot.botName != null && bot.botName.equals("SoundCloud")) {
                                    Matcher matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(botJson.getString("message"));
                                    final String html = botJson.getString("message");
                                    matcher.find();
                                    final String src = matcher.group(1);
                                    Logger.error(TAG, "onBindViewHolder: html: "+html);
                                    Logger.error(TAG, "onBindViewHolder: src: "+src);
                                    botMessageHolder.imageTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.cc_ic_play,0,0,0);
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
                                    botMessageHolder.imageTitle.setMovementMethod(LinkMovementMethod.getInstance());
                                    botMessageHolder.imageTitle.setText(soundCloudString);
                                }else {
                                    botMessageHolder.imageTitle.setText(Html.fromHtml(botJson.getString("message")));
                                }
                                break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case LEFT_IMAGE_DOWNLOADING_MESSAGE:
                final LeftImageVideoViewHolder leftImageDownloadingHolder = (LeftImageVideoViewHolder) holder;
                leftImageDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftImageDownloadingHolder.senderName.setText(senderName);
                leftImageDownloadingHolder.btnPlayVideo.setVisibility(View.GONE);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftImageDownloadingHolder.avatar, R.drawable.cc_default_avatar);
                leftImageDownloadingHolder.imageTitle.setVisibility(View.GONE);
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
                                leftImageDownloadingHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target, DataSource dataSource, boolean b) {
                                Logger.error(TAG, "Glide onResourceReady: called");
                                leftImageDownloadingHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }
                        }).apply(leftImageDownloadingOptions).into(leftImageDownloadingHolder.imageMessage);
                final String leftImageDownloadingMessage = message;
                Logger.error(TAG, "onBindViewHolder: LEFT_IMAGE_DOWNLOADING_MESSAGE : leftImageMessage: "+leftImageDownloadingMessage);
                leftImageDownloadingHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

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

            case LEFT_IMAGE_MESSAGE:
                LeftImageVideoViewHolder leftImageMessageHolder = (LeftImageVideoViewHolder) holder;
                leftImageMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftImageMessageHolder.imageTitle.setVisibility(View.GONE);
                leftImageMessageHolder.senderName.setText(senderName);
                leftImageMessageHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                leftImageMessageHolder.btnPlayVideo.setVisibility(View.GONE);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftImageMessageHolder.avatar, R.drawable.cc_default_avatar);
                LocalStorageFactory.loadImage(context,message,leftImageMessageHolder.imageMessage,R.drawable.ic_broken_image);
                leftImageMessageHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

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

            case RIGHT_IMAGE_DOWNLOADING_MESSAGE:
                final RightImageVideoViewHolder rightImageDownloadingHolder = (RightImageVideoViewHolder) holder;
                rightImageDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightImageDownloadingHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightImageDownloadingHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightImageDownloadingHolder.btnPlayVideo.setVisibility(View.GONE);
                rightImageDownloadingHolder.imageTitle.setVisibility(View.GONE);
                if(messageStatus == 0){
                    rightImageDownloadingHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightImageDownloadingHolder.messageStatus.setVisibility(View.GONE);
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
                                rightImageDownloadingHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target, DataSource dataSource, boolean b) {
                                Logger.error(TAG, "Glide onResourceReady: called");
                                rightImageDownloadingHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                                return false;
                            }
                        }).apply(rightImageDownloadingOptions).into(rightImageDownloadingHolder.imageMessage);

                final String rightImageDownloadingMessage = message;
                Logger.error(TAG, "onBindViewHolder: RIGHT_IMAGE_DOWNLOADING_MESSAGE : rightImageDownloadingMessage: "+rightImageDownloadingMessage);
                rightImageDownloadingHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

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

            case RIGHT_IMAGE_MESSAGE:
                final RightImageVideoViewHolder rightImageMessageHolder = (RightImageVideoViewHolder) holder;
                rightImageMessageHolder.imageTitle.setVisibility(View.GONE);
                rightImageMessageHolder.btnPlayVideo.setVisibility(View.GONE);
                rightImageMessageHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                rightImageMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightImageMessageHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightImageMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightImageMessageHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightImageMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightImageMessageHolder.retry.setVisibility(View.VISIBLE);
                    rightImageMessageHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback) context).onRetryClicked(localId);
                                rotateRetry(rightImageMessageHolder.retry,5);
                            }
                        }
                    });
                    break;
                }else if(messageStatus == 0){
                    rightImageMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightImageMessageHolder.messageStatus.setVisibility(View.GONE);
                }
                LocalStorageFactory.loadImage(context,message,rightImageMessageHolder.imageMessage,R.drawable.ic_broken_image);

                rightImageMessageHolder.imageMessage.setOnClickListener(new View.OnClickListener() {

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

            case LEFT_VIDEO_DOWNLOADING_MESSAGE:
                LeftImageVideoViewHolder leftVideoDownloadingHolder = (LeftImageVideoViewHolder) holder;
                leftVideoDownloadingHolder.senderName.setText(senderName);
                leftVideoDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftVideoDownloadingHolder.imageTitle.setVisibility(View.GONE);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftVideoDownloadingHolder.avatar, R.drawable.cc_default_avatar);
                LocalStorageFactory.loadImage(context, message, leftVideoDownloadingHolder.imageMessage, R.drawable.ic_broken_image);
                break;
            case LEFT_VIDEO_MESSAGE:
                LeftImageVideoViewHolder leftVideoMessageHolder = (LeftImageVideoViewHolder) holder;
                leftVideoMessageHolder.imageTitle.setVisibility(View.GONE);
                leftVideoMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftVideoMessageHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                if(message.contains("content://")){
                    try {
                        InputStream image_stream = context.getContentResolver().openInputStream(Uri.parse(message));
                        Bitmap bitmap= BitmapFactory.decodeStream(image_stream);
                        leftVideoMessageHolder.imageMessage.setImageBitmap(bitmap);
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
                                leftVideoMessageHolder.imageMessage.setImageBitmap(bmp);
                                videoThumbnails.put(sentTimestamp, bmp);
                            } else {
                                Bitmap bmp = ThumbnailUtils.createVideoThumbnail(message,
                                        MediaStore.Video.Thumbnails.MINI_KIND);
                                leftVideoMessageHolder.imageMessage.setImageBitmap(bmp);
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
                        leftVideoMessageHolder.imageMessage.setImageBitmap(videoThumbnails.get(sentTimestamp));
                    }
                }else {
                    RequestOptions requestOptions = new RequestOptions()
                            .fitCenter()
                            .placeholder(R.drawable.ic_broken_image);
                    Glide.with(context)
                            .load("")
                            .apply(requestOptions)
                            .into(leftVideoMessageHolder.imageMessage);
                }
                leftVideoMessageHolder.btnPlayVideo.setOnClickListener(new View.OnClickListener() {
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

            case RIGHT_VIDEO_DOWNLOADING_MESSAGE:
                RightImageVideoViewHolder rightVideoDownloadingHolder = (RightImageVideoViewHolder) holder;
                rightVideoDownloadingHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoDownloadingHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightVideoDownloadingHolder.imageTitle.setVisibility(View.GONE);
                if(messageStatus == 0){
                    rightVideoDownloadingHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightVideoDownloadingHolder.messageStatus.setVisibility(View.GONE);
                }
                LocalStorageFactory.loadImage(context, message, rightVideoDownloadingHolder.imageMessage, R.drawable.ic_broken_image);
                break;

            case RIGHT_VIDEO_MESSAGE:
                final RightImageVideoViewHolder rightVideoMessageHolder = (RightImageVideoViewHolder) holder;
                rightVideoMessageHolder.imageTitle.setVisibility(View.GONE);
                rightVideoMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightVideoMessageHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                rightVideoMessageHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoMessageHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightVideoMessageHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightVideoMessageHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightVideoMessageHolder.retry.setVisibility(View.VISIBLE);
                    rightVideoMessageHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback) context).onRetryClicked(localId);
                                rotateRetry(rightVideoMessageHolder.retry,8);
                            }
                        }
                    });
                    break;
                }else if(messageStatus == 0){
                    rightVideoMessageHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightVideoMessageHolder.messageStatus.setVisibility(View.GONE);
                }
                if(message.contains("content://")){
                    try {
                        InputStream image_stream = context.getContentResolver().openInputStream(Uri.parse(message));
                        Bitmap bitmap= BitmapFactory.decodeStream(image_stream);
                        rightVideoMessageHolder.imageMessage.setImageBitmap(bitmap);
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
                                rightVideoMessageHolder.imageMessage.setImageBitmap(bmp);
                                videoThumbnails.put(sentTimestamp, bmp);
                            } else {
                                Bitmap bmp = ThumbnailUtils.createVideoThumbnail(message,
                                        MediaStore.Video.Thumbnails.MINI_KIND);
                                rightVideoMessageHolder.imageMessage.setImageBitmap(bmp);
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
                        rightVideoMessageHolder.imageMessage.setImageBitmap(videoThumbnails.get(sentTimestamp));
                    }

                }else {
                    RequestOptions requestOptions = new RequestOptions()
                            .fitCenter()
                            .placeholder(R.drawable.ic_broken_image);
                    Glide.with(context)
                            .load("")
                            .apply(requestOptions)
                            .into(rightVideoMessageHolder.imageMessage);
                }
                rightVideoMessageHolder.btnPlayVideo.setOnClickListener(new View.OnClickListener() {
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

            case LEFT_AUDIO_DOWNLOADING_MESSAGE:
                LeftAudioViewHolder leftAudioDownloadingHolder = (LeftAudioViewHolder) holder;
                leftAudioDownloadingHolder.senderName.setText(senderName);
                leftAudioDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftAudioDownloadingHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudioDownloadingHolder.audioSeekBar.getThumb().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudioDownloadingHolder.playAudio.setVisibility(View.GONE);
                leftAudioDownloadingHolder.audioSeekBar.setProgress(0);
                leftAudioDownloadingHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudioDownloadingHolder.fileLoadingProgressBar.getIndeterminateDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudioDownloadingHolder.audioLength.setText("00:00");
                LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftAudioDownloadingHolder.avatar,R.drawable.cc_default_avatar);
                break;
            case LEFT_AUDIO_MESSAGE:
                final LeftAudioViewHolder leftAudioViewHolder = (LeftAudioViewHolder) holder;
                leftAudioViewHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                leftAudioViewHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftAudioViewHolder.senderName.setText(senderName);
                leftAudioViewHolder.audioSeekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                leftAudioViewHolder.audioSeekBar.getThumb().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                LocalStorageFactory.loadImageUsingURL(context,avatarUrl,leftAudioViewHolder.avatar,R.drawable.cc_default_avatar);
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

                        if (!TextUtils.isEmpty(message)) {
                            try {
                                if (sentTimestamp == currentlyPlayingId) {
                                    Logger.error(TAG, "onClick: currently playing");
                                    currentPlayingSong = "";
                                    try {
                                        if(player.isPlaying()){
                                            player.pause();
                                            Logger.error(TAG, "onClick: paused");
                                            leftAudioViewHolder.playAudio.setImageResource(R.drawable.ic_play_arrow);
                                        }else {
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
                if(messageStatus == 0){
                    rightAudioDownloadingHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightAudioDownloadingHolder.messageStatus.setVisibility(View.GONE);
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
                rightAudioViewHolder.retry.setVisibility(View.INVISIBLE);
                if (messageStatus == 2){
                    rightAudioViewHolder.messageStatus.setImageResource(R.drawable.cc_ic_alert);
                    rightAudioViewHolder.retry.setVisibility(View.VISIBLE);
                    rightAudioViewHolder.retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(context instanceof RetryCallback){
                                ((RetryCallback) context).onRetryClicked(localId);
                                rotateRetry(rightAudioViewHolder.retry,3);
                            }
                        }
                    });
                    break;
                }else if(messageStatus == 0){
                    rightAudioViewHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightAudioViewHolder.messageStatus.setVisibility(View.GONE);
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

            case LEFT_HANDWRITE_DOWNLOADING_MESSAGE:
                LeftImageVideoViewHolder leftHandwriteDownloadingHolder = (LeftImageVideoViewHolder) holder;
                leftHandwriteDownloadingHolder.imageTitle.setText("Handwrite Message");
                leftHandwriteDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftHandwriteDownloadingHolder.btnPlayVideo.setVisibility(View.GONE);
                leftHandwriteDownloadingHolder.senderName.setText(senderName);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftHandwriteDownloadingHolder.avatar, R.drawable.cc_default_avatar);
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

            case LEFT_HANDWRITE_MESSAGE:
                LeftImageVideoViewHolder leftHandwriteHolder = (LeftImageVideoViewHolder) holder;
                leftHandwriteHolder.imageTitle.setText("Handwrite Message");
                leftHandwriteHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                leftHandwriteHolder.btnPlayVideo.setVisibility(View.GONE);
                leftHandwriteHolder.senderName.setText(senderName);
                leftHandwriteHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftHandwriteHolder.avatar, R.drawable.cc_default_avatar);
                LocalStorageFactory.loadImage(context, imageUrl, leftHandwriteHolder.imageMessage, R.drawable.ic_broken_image);
                leftHandwriteHolder.imageMessage.setOnClickListener(new View.OnClickListener() {
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

            case RIGHT_HANDWRITE_DOWNLOADING_MESSAGE:
                RightImageVideoViewHolder rightHandwriteDownloadingHolder = (RightImageVideoViewHolder) holder;
                rightHandwriteDownloadingHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightHandwriteDownloadingHolder.imageTitle.setText("Handwrite Message");
                rightHandwriteDownloadingHolder.btnPlayVideo.setVisibility(View.GONE);
                rightHandwriteDownloadingHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightHandwriteDownloadingHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(messageStatus == 0){
                    rightHandwriteDownloadingHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightHandwriteDownloadingHolder.messageStatus.setVisibility(View.INVISIBLE);
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

            case RIGHT_HANDWRITE_MESSAGE:
                RightImageVideoViewHolder rightHandwriteHolder = (RightImageVideoViewHolder) holder;
                rightHandwriteHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightHandwriteHolder.imageTitle.setText("Handwrite Message");
                rightHandwriteHolder.btnPlayVideo.setVisibility(View.GONE);
                rightHandwriteHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightHandwriteHolder.imageContainer.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightHandwriteHolder.fileLoadingProgressBar.setVisibility(View.GONE);
                if(messageStatus == 0){
                    rightHandwriteHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightHandwriteHolder.messageStatus.setVisibility(View.INVISIBLE);
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

            case RIGHT_AVBROADCAST_REQUEST_MESSAGE:
                RightMessageViewHolder rightAVBroadcastRequestHolder = (RightMessageViewHolder) holder;
                rightAVBroadcastRequestHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                rightAVBroadcastRequestHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightAVBroadcastRequestHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(messageStatus == 0){
                    rightAVBroadcastRequestHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightAVBroadcastRequestHolder.messageStatus.setVisibility(View.INVISIBLE);
                }
                rightAVBroadcastRequestHolder.textMessage.setText("Successfully sent an audio/video broadcast");
                break;
            case LEFT_AVBROADCAST_REQUEST_MESSAGE:
                LeftMessageViewHolder leftAVBroadcastRequestHolder = (LeftMessageViewHolder) holder;
                leftAVBroadcastRequestHolder.senderName.setText(senderName);
                leftAVBroadcastRequestHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftAVBroadcastRequestHolder.avatar, R.drawable.cc_default_avatar);
                leftAVBroadcastRequestHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftAVBroadcastRequestHolder.textMessage.setText(getAVBroadcastText(message, fromId, imageUrl));
                break;
            case LEFT_AVBROADCAST_EXPIRED_MESSAGE:
                LeftMessageViewHolder avBroadcastExpiredHolder = (LeftMessageViewHolder) holder;
                avBroadcastExpiredHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                avBroadcastExpiredHolder.senderName.setText(senderName);
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, avBroadcastExpiredHolder.avatar, R.drawable.cc_default_avatar);
                avBroadcastExpiredHolder.textMessage.setText(message+"\n(Expired)");
                break;
            case RIGHT_AVBROADCAST_EXPIRED_MESSAGE:
                RightMessageViewHolder rightAVBroadcastExpiredHolder = (RightMessageViewHolder) holder;
                rightAVBroadcastExpiredHolder.rightArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                rightAVBroadcastExpiredHolder.textMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                if(messageStatus == 0){
                    rightAVBroadcastExpiredHolder.messageStatus.setVisibility(View.VISIBLE);
                }else {
                    rightAVBroadcastExpiredHolder.messageStatus.setVisibility(View.INVISIBLE);
                }
                rightAVBroadcastExpiredHolder.textMessage.setText("audio/video broadcast Expired");
                break;

            case AVCHAT_INCOMING_CALL:
                LeftMessageViewHolder avChatIncomingCallMessageHolder = (LeftMessageViewHolder) holder;
                avChatIncomingCallMessageHolder.senderName.setText(senderName);
                avChatIncomingCallMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, avChatIncomingCallMessageHolder.avatar, R.drawable.cc_default_avatar);
                avChatIncomingCallMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                avChatIncomingCallMessageHolder.textMessage.setText(getAVConferenceText(message,fromId,false,imageUrl));
                break;

            case AUDIOCHAT_INCOMING_CALL:
                LeftMessageViewHolder audioChatIncomingCallMessageHolder = (LeftMessageViewHolder) holder;
                audioChatIncomingCallMessageHolder.senderName.setText(senderName);
                audioChatIncomingCallMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, audioChatIncomingCallMessageHolder.avatar, R.drawable.cc_default_avatar);
                audioChatIncomingCallMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                audioChatIncomingCallMessageHolder.textMessage.setText(getAVConferenceText(message,fromId,true,imageUrl));
                break;

            case LEFT_FILE_MESSAGE:
                LeftMessageViewHolder leftFileMessageHolder = (LeftMessageViewHolder) holder;
                leftFileMessageHolder.senderName.setText(senderName);
                leftFileMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftFileMessageHolder.avatar, R.drawable.cc_default_avatar);
                leftFileMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftFileMessageHolder.textMessage.setText(createDownloadLink(message, String.valueOf(remoteId)));
                break;

            case LEFT_FILE_DOWNLOADED:
                LeftMessageViewHolder leftFileDownloadedMessageHolder = (LeftMessageViewHolder) holder;
                leftFileDownloadedMessageHolder.senderName.setText(senderName);
                leftFileDownloadedMessageHolder.messageTimeStamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
                LocalStorageFactory.loadImageUsingURL(context, avatarUrl, leftFileDownloadedMessageHolder.avatar, R.drawable.cc_default_avatar);
                leftFileDownloadedMessageHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                leftFileDownloadedMessageHolder.textMessage.setText(createOpenLink(message));
                break;
        }
        /*Logger.error(TAG, "message : " + message + " messageStatus : " + messageStatus);
        Logger.error(TAG, "message : " + message + " senderName : " + senderName);
        String avatarUrl;
        RequestOptions requestOptions1 ;

        if(holder.viewType == TYPE_RIGHT){
            LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
            GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable.findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
            shape.setColor(holder.rightBubbleColor);
            GradientDrawable drawable = (GradientDrawable) holder.messageContainer.getBackground();
            drawable.setColor(holder.rightBubbleColor);
        }else{
            LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
            GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable.findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
            shape.setColor(holder.leftBubbleColor);
            GradientDrawable drawable = (GradientDrawable) holder.messageContainer.getBackground();
            drawable.setColor(holder.leftBubbleColor);
            holder.senderTextField.setText(senderName);
        }

        holder.messageTimestamp.setText(CommonUtils.convertTimestampToDate(sentTimestamp));
        holder.messageArrow.setVisibility(View.VISIBLE);
        holder.imageHolder.setEnabled(true);
        holder.customImageHolder.setEnabled(true);

        Logger.error(TAG,"Message Type : "+cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_TYPE)));
        switch(cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_TYPE))) {

            case MessageTypeKeys.NORMAL_MESSAGE:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);

                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                Logger.error(TAG,"Text Color value = "+textColor);
                if(textColor != null)
                try {
                    holder.chatroomMessage.setTextColor(Color.parseColor(textColor));
                } catch (Exception iae) {
                    // This color string is not valid
                }

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);

                    //if(remoteId == 0L){
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);

                    holder.senderTextField.setText(senderName);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                if (insertedBy == 1) {
                    if (Stickers.isSticker(message)) {
                        holder.chatroomMessage.setText(Stickers.handleSticker(message));
                    } else {
                        if (message.contains("<img class=\"cometchat_smiley\"")) {
                            Spannable spannable = Smilies.convertImageTagToEmoji(message, context, false,
                                    R.drawable.class,(int)context.getResources().getDimension(R.dimen.emoji_size));
                            if(EmoticonUtils.isOnlySmileyHtmlSmiley(spannable)){
                                holder.chatroomMessage.setVisibility(View.GONE);
                                holder.tvOnlySmiley.setVisibility(View.VISIBLE);
                                holder.tvOnlySmiley.setText(spannable);
                                LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable.findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                                shape.setColor(Color.parseColor("#00000000"));
                                GradientDrawable drawable = (GradientDrawable) holder.messageContainer.getBackground();
                                drawable.setColor(Color.parseColor("#00000000"));
                            }else{
                                holder.chatroomMessage.setText(spannable);
                            }
                        } else {

                            if(EmoticonUtils.isOnlySmileyMessage(message)){

                                holder.chatroomMessage.setVisibility(View.GONE);
                                holder.tvOnlySmiley.setVisibility(View.VISIBLE);
                                holder.tvOnlySmiley.setEmojiText(message,(int)context.getResources().getDimension(R.dimen.emoji_size));

                                LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                                shape.setColor(Color.parseColor("#00000000"));
                                GradientDrawable drawable = (GradientDrawable) holder.messageContainer.getBackground();
                                drawable.setColor(Color.parseColor("#00000000"));
                            }else{
                                holder.chatroomMessage.setEmojiText(message,(int)context.getResources().getDimension(R.dimen.emoji_size));
                            }

                        }
                    }
                } else {
                    if (message.contains("<img class=\"cometchat_smiley\"")) {

                        Spannable spannable = Smilies.convertImageTagToEmoji(message, context, false,
                                R.drawable.class,(int)context.getResources().getDimension(R.dimen.emoji_size));
                        if(EmoticonUtils.isOnlySmileyHtmlSmiley(spannable)){
                            holder.chatroomMessage.setVisibility(View.GONE);
                            holder.tvOnlySmiley.setVisibility(View.VISIBLE);
                            holder.tvOnlySmiley.setText(spannable);
                            LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                            GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable.findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());
                            shape.setColor(Color.parseColor("#00000000"));
                            GradientDrawable drawable = (GradientDrawable) holder.messageContainer.getBackground();
                            drawable.setColor(Color.parseColor("#00000000"));
                        }else{
                            holder.chatroomMessage.setText(spannable);
                        }

                    } else if (Stickers.isSticker(message)) {
                        holder.chatroomMessage.setText(Stickers.handleSticker(message));
                    } else {
                        holder.chatroomMessage.setText(message);
                    }
                }

                break;

            case MessageTypeKeys.IMAGE_DOWNLOADING:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.VISIBLE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }
                //                Logger.error(TAG,"Image URL = "+imageUrl);

                *//*requestOptions1 = new RequestOptions()
                        .override(800,600)
                        .fitCenter()
                        .dontAnimate()
                        .placeholder(R.drawable.cc_thumbnail_default);

                Glide.with(context)
                        .load(imageUrl)
                        .apply(requestOptions1)
                        .into(holder.imageHolder);*//*
                LocalStorageFactory.loadImageUsingURL(context,imageUrl,holder.imageHolder,R.drawable.cc_thumbnail_default);

                holder.imageHolder.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            try {
                                holder.imageHolder.setEnabled(false);
                                Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                ((Activity)context).startActivityForResult(intent, 1113);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case MessageTypeKeys.IMAGE_MESSAGE:

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    //if(remoteId == 0L){
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.VISIBLE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                String downloaded_filepath = message;
                File downloaded_file = new File(downloaded_filepath);

                LayerDrawable ldrawableImage = (LayerDrawable) holder.messageArrow.getBackground();
                GradientDrawable shapeImage = ((GradientDrawable) ((RotateDrawable) (ldrawableImage
                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                shapeImage.setColor(Color.parseColor("#00000000"));
                GradientDrawable drawableImage = (GradientDrawable) holder.messageContainer.getBackground();
                drawableImage.setColor(Color.parseColor("#00000000"));
                Logger.error(TAG, "message value: "+message);
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
                            try {
                                holder.customImageHolder.setEnabled(false);
                                Intent intent = new Intent(context, CCImagePreviewActivity.class);
                                intent.putExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE, message);
                                intent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.IMAGE_ITEM_POSITION, cursor.getPosition());
                                ((Activity)context).startActivityForResult(intent, 1113);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                break;

            case MessageTypeKeys.VIDEO_MESSAGE:

                File video = new File(message);
                if (video.exists()) {
                    holder.chatroomMessage.setVisibility(View.GONE);
                    holder.imageHolder.setVisibility(View.GONE);
                    holder.wheel.setVisibility(View.GONE);
                    holder.audioNoteContainer.setVisibility(View.GONE);
                    holder.videoMessageButton.setImageResource(R.drawable.cc_play_video_button);
                    holder.videoMessageButton.setVisibility(View.VISIBLE);
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.customImageHolder.setVisibility(View.GONE);
                    holder.tvOnlySmiley.setVisibility(View.GONE);

                    if(holder.viewType == TYPE_RIGHT){
                        holder.avatar.setVisibility(View.GONE);
                        holder.senderTextField.setVisibility(View.GONE);
                        //if(remoteId == 0L){
                        if(messageStatus == 0){
                            holder.imagePendingMessage.setVisibility(View.VISIBLE);
                        }else {
                            holder.imagePendingMessage.setVisibility(View.GONE);
                        }
                    }else{
                        avatarUrl = "";
                        holder.avatar.setVisibility(View.VISIBLE);
                        holder.senderTextField.setVisibility(View.VISIBLE);
                        Contact contact = Contact.getContactDetails(fromId);
                        if (contact != null) {
                            avatarUrl = contact.avatarURL;
                        }
                        if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                        }
                    }

                    if (videoThumbnails.get(sentTimestamp) == null) {
                        try {
                            BitmapFactory.Options videoOptions = new BitmapFactory.Options();
                            videoOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                            videoOptions.inSampleSize = 2;
                            Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            String[] requierddata = {BaseColumns._ID};
                            Cursor c = context.getContentResolver().query(videoUri, requierddata,
                                    MediaStore.MediaColumns.DATA + " like  \"" + message + "\"", null, null);
                            c.moveToFirst();
                            if (c.getCount() != 0) {
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
                            c.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        holder.videoThumb.setImageBitmap(videoThumbnails.get(sentTimestamp));
                    }
                    holder.videoThumb.setVisibility(View.VISIBLE);

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
                }

                break;

            case MessageTypeKeys.VIDEO_DOWNLOADING:

                holder.wheel.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                final ProgressBar wheel2 = holder.wheel;
                wheel2.setVisibility(View.GONE);

                final ImageView vMessageButton = holder.videoMessageButton;
                vMessageButton.setImageResource(R.drawable.cc_download_video_button);
                vMessageButton.setVisibility(View.VISIBLE);

                holder.videoThumb.setImageResource(R.drawable.cc_thumbnail_default);
                holder.videoThumb.setVisibility(View.VISIBLE);
                break;

            case MessageTypeKeys.AUDIO_MESSAGE:
                holder.chatroomMessage.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.VISIBLE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);


                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);

                    //if(remoteId == 0L){
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }

                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                final TextView audioText = holder.audioTime;
                final SeekBar audioSeekBar = holder.audioSeekbar;
                final ImageView playBtn = holder.audioPlayButton;

                audioSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                if (android.os.Build.VERSION.SDK_INT >= 16){
                    audioSeekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                }
                if (sentTimestamp == currentlyPlayingId) {
                    setBtnColor(holder.viewType, playBtn, false);
                } else {
                    setBtnColor(holder.viewType, holder.audioPlayButton, true);
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

                holder.chatroomMessage.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.customImageHolder.setVisibility(View.GONE);
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.audioNoteContainer.setVisibility(View.VISIBLE);
                break;

            case MessageTypeKeys.STICKER:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                //holder.chatroomMessage.setTextColor(Color.parseColor(textColor));
                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    //if(remoteId == 0L){
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                LayerDrawable ldrawable = (LayerDrawable) holder.messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                shape.setColor(Color.parseColor("#00000000"));
                GradientDrawable drawable = (GradientDrawable) holder.messageContainer.getBackground();
                drawable.setColor(Color.parseColor("#00000000"));
                if(message.contains("CC^CONTROL_")){
                    holder.chatroomMessage.setText(Stickers.handleSticker(message));
                }else{
                    holder.chatroomMessage.setText(Stickers.getSpannableStickerString(message));
                }
                break;

            case MessageTypeKeys.HANDWRITE_DOWNLOADING:

                Logger.error("Message type handwrite new ");
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.imageHolder.setVisibility(View.VISIBLE);
                holder.imageHolder.setImageDrawable(context.getResources().getDrawable(R.drawable.cc_thumbnail_default));
                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                            avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                            LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }

                }


                holder.chatroomMessage.setText("HandWrite Message");
                requestOptions1 = new RequestOptions()
                        .fitCenter()
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

                Logger.error("Message type handwrite new ");
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.imageHolder.setVisibility(View.VISIBLE);
                holder.imageHolder.setImageDrawable(context.getResources().getDrawable(R.drawable.cc_thumbnail_default));
                holder.avatar.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.chatroomMessage.setText("HandWrite Message");

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
                            ((Activity)context).startActivityForResult(intent, 1113);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                break;

            case MessageTypeKeys.AVCHAT_INCOMING_CALL:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                ContactID = fromId;
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(getAVConferenceText(message,fromId,false,imageUrl));

                break;

            case MessageTypeKeys.AUDIOCHAT_INCOMING_CALL:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                ContactID = fromId;
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(getAVConferenceText(message,fromId,true,imageUrl));

                break;

            case MessageTypeKeys.GRP_AVBROADCAST_REQUEST:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(getAVBroadcastText(message, fromId, imageUrl));

                break;

            case MessageTypeKeys.AVBROADCAST_INVITE :
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                //holder.normalMessageContainer.setVisibility(View.VISIBLE);
                //holder.avchatMessageContainer.setVisibility(View.GONE);


                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(getAVBroadcastText(message, fromId,imageUrl));

                break;

            case MessageTypeKeys.AVBROADCAST_END :
                Logger.error(TAG,"GRP AVBROADCAST END");
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                //holder.normalMessageContainer.setVisibility(View.VISIBLE);
                //holder.avchatMessageContainer.setVisibility(View.GONE);
                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(message);

                break;


            case MessageTypeKeys.AVCHAT_INCOMING_CALL_CONNECTED_END :

                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                ContactID = fromId;
                if(holder.viewType == TYPE_RIGHT){
                    holder.senderTextField.setVisibility(View.GONE);
                    holder.avatar.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }
                holder.chatroomMessage.setText("This call has been ended");


                break;



            case MessageTypeKeys.AVBROADCAST_EXPIRED:

                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);

                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.senderTextField.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                holder.chatroomMessage.setText(message+"\n(Expired)");
                break;

            case MessageTypeKeys.WRITEBOARD_MESSAGE:

                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(createViewLink("has shared his/her collaborative document with you. Click here to view|#|"+message, 2));
                holder.imageHolder.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                break;

            case MessageTypeKeys.WHITEBOARD_MESSAGE:
                Logger.error(TAG,"Whiteboard Message");
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(createViewLink("has shared a whiteboard. Click here to view|#|"+message, 1));
                holder.imageHolder.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    holder.senderTextField.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                break;

            case MessageTypeKeys.SCREENSHARE_MESSAGE:
                Logger.error(TAG,"Screenshare Message : " + message);
                holder.chatroomMessage.setVisibility(View.VISIBLE);
                holder.chatroomMessage.setMovementMethod(LinkMovementMethod.getInstance());
                holder.chatroomMessage.setText(createViewLink(message, 4));
                holder.imageHolder.setVisibility(View.GONE);
                holder.videoThumb.setVisibility(View.GONE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);

                channel = imageUrl;
                ContactID = fromId;
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                    if(messageStatus == 0){
                        holder.imagePendingMessage.setVisibility(View.VISIBLE);
                    }else {
                        holder.imagePendingMessage.setVisibility(View.GONE);
                    }
                }else{
                    avatarUrl = "";
                    holder.avatar.setVisibility(View.VISIBLE);
                    Contact contact = Contact.getContactDetails(fromId);
                    if (contact != null) {
                        avatarUrl = contact.avatarURL;
                    }
                    if (!avatarUrl.equals("")) {
                        LocalStorageFactory.loadImageUsingURL(context,avatarUrl,holder.avatar,R.drawable.cc_default_avatar);
                    }
                }

                break;

            case MessageTypeKeys.BOT_RESPONSE:
                holder.videoThumb.setVisibility(View.GONE);
                holder.videoMessageButton.setVisibility(View.GONE);
                holder.wheel.setVisibility(View.GONE);
                holder.imageHolder.setVisibility(View.GONE);
                holder.messageContainer.setVisibility(View.VISIBLE);
                holder.audioNoteContainer.setVisibility(View.GONE);
                holder.customImageHolder.setVisibility(View.GONE);
                holder.tvOnlySmiley.setVisibility(View.GONE);
                Logger.error(TAG,"Bot direction: "+holder.viewType);
                if(holder.viewType == TYPE_RIGHT){
                    holder.avatar.setVisibility(View.GONE);
                }else{
                    holder.avatar.setVisibility(View.VISIBLE);
                }

                try {
                    JSONObject botMessageJson = new JSONObject(message);
                    Bot bot = Bot.getBotDetails(botMessageJson.getString("botid"));
                    if (bot != null && bot.botAvatar != null) {
                        LocalStorageFactory.loadImageUsingURL(context, bot.botAvatar, holder.avatar, R.drawable.default_avatar);
                    }

                    switch (botMessageJson.getString("messagetype")) {

                        case "image":
                            Document doc = Jsoup.parseBodyFragment(botMessageJson.getString("message"));
                            Element imageElement = doc.select("img").first();
                            final String absoluteUrl = imageElement.absUrl("src");
                            holder.customImageHolder.setVisibility(View.VISIBLE);
                            holder.chatroomMessage.setVisibility(GONE);

                            LayerDrawable layerDrawable = (LayerDrawable) holder.messageArrow.getBackground();
                            GradientDrawable gradientDrawable = ((GradientDrawable) ((RotateDrawable) (layerDrawable
                                    .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                            gradientDrawable.setColor(Color.parseColor("#00000000"));
                            GradientDrawable drawableBackground = (GradientDrawable) holder.messageContainer.getBackground();
                            drawableBackground.setColor(Color.parseColor("#00000000"));
                            Logger.error(TAG, "Absolute url = " + absoluteUrl);

                            if (null != absoluteUrl) {
                               *//* if (absoluteUrl.contains("giphy")) {

                                    RequestOptions requestOptions = new RequestOptions()
                                            .fitCenter()
                                            .dontAnimate()
                                            .placeholder(R.drawable.cc_thumbnail_default)
                                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                            .priority(Priority.IMMEDIATE);
                                    Glide.with(context)
                                            .load(absoluteUrl)
                                            .apply(requestOptions)
                                            .into(holder.customImageHolder);
                                } else {
                                    RequestOptions requestOptions = new RequestOptions()
                                            .fitCenter()
                                            .dontAnimate()
                                            .placeholder(R.drawable.cc_thumbnail_default)
                                            .priority(Priority.HIGH);

                                    Glide.with(context)
                                            .load(absoluteUrl)
                                            .apply(requestOptions)
                                            .into(holder.customImageHolder);
                                }*//*
                               LocalStorageFactory.loadImageUsingURL(context,absoluteUrl,holder.customImageHolder,R.drawable.cc_thumbnail_default);
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
                                        ((Activity)context).startActivityForResult(intent, 1113);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            break;

                        case "anchor":
                            String temp = botMessageJson.getString("message");
                            CommonUtils.renderHtmlInATextView(context, holder.chatroomMessage, temp);
                            break;

                        default:
                            if (bot != null && bot.botName != null && bot.botName.equals("SoundCloud")) {

                                Matcher matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(botMessageJson.getString("message"));
                                final String html = botMessageJson.getString("message");
                                matcher.find();
                                final String src = matcher.group(1);

                                holder.customImageHolder.setVisibility(View.VISIBLE);
                                holder.chatroomMessage.setVisibility(GONE);

                                ldrawableImage = (LayerDrawable) holder.messageArrow.getBackground();
                                shapeImage = ((GradientDrawable) ((RotateDrawable) (ldrawableImage
                                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                                shapeImage.setColor(Color.parseColor("#00000000"));
                                drawableImage = (GradientDrawable) holder.messageContainer.getBackground();
                                drawableImage.setColor(Color.parseColor("#00000000"));


                                RequestOptions requestOptions = new RequestOptions().placeholder(R.drawable.cc_ic_play);
                                Glide.with(context)
                                        .load("")
                                        .apply(requestOptions)
                                        .into(holder.customImageHolder);

                                holder.customImageHolder.setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
//                                        Intent customTabsIntent = new Intent(context, CCWebViewActivity.class);
//                                        customTabsIntent.putExtra(IntentExtraKeys.WEBSITE_URL, Uri.parse(src));
                                        CCChromeTabs customTabs = new CCChromeTabs(context);
                                        customTabs.loadURL(src);
                                    }
                                });
                            } else {
                                holder.chatroomMessage.setText(Html.fromHtml(botMessageJson.getString("message")));
                            }
                            break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                break;
        }*/
    }

    private void rotateRetry(ImageView retry, int repeatCount) {
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(700);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatCount(repeatCount);
        retry.startAnimation(rotate);
    }

    public void setBtnColor(int viewType, ImageView playbtn, boolean isPlayBtn) {
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
    }

    public GroupMessage getMessageByPosition(int position) {
        GroupMessage groupMessage = null;
        if(cursor.moveToPosition(position)) {
            groupMessage = new GroupMessage();
            groupMessage.fromId = cursor.getInt(cursor.getColumnIndex(GroupMessage.COLUMN_FROM_ID));
            groupMessage.type = cursor.getString(cursor.getColumnIndex(GroupMessage.COLUMN_MESSAGE_TYPE));
            groupMessage.sentTimestamp = cursor.getLong(cursor.getColumnIndex(GroupMessage.COLUMN_SENT_TIMESTAMP));
        }
        return groupMessage;
    }

    private SpannableString createViewLink(final String mess, int flag) {
        /**
         * flag : decides which type of request
         * If 1, used for whiteboard
         * If 2, used for writeboard
         * if 3, used for screenshare
         * if 4, used for videochatactivity
         */

        if (flag == 1) {
            Logger.error("String Whiteboard message = "+mess);
            int startIndex = mess.indexOf("."), endIndex = mess.indexOf("|#|");
            final SpannableString whiteboardrequest = new SpannableString(mess.substring(0, endIndex));
            try {
                final String room = mess.substring(endIndex + 3, mess.length());
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (grpWhiteBoardState == FeatureState.INACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        }else {
                            viewWhiteBoard(room);
                        }
                    }
                };

                whiteboardrequest.setSpan(span, startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                whiteboardrequest.setSpan(new ForegroundColorSpan(color), startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                        if (grpWriteBoardState == FeatureState.INACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            viewWriteBoard(url);
                        }


                    }
                };

                writeboardrequest.setSpan(span, startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                writeboardrequest.setSpan(new ForegroundColorSpan(color), startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return writeboardrequest;
        } else if (flag == 3) {
            int startIndex = mess.indexOf("."), endIndex = mess.indexOf("|#|");
            final SpannableString screensharerequest = new SpannableString(mess.substring(0, endIndex));
            try {
                final String url = mess.substring(endIndex + 3, mess.length());
                Logger.error(TAG,"Screnshare URL : "+url);
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent screenshareIntent = new Intent(context, CCWebViewActivity.class);
                        screenshareIntent.putExtra(IntentExtraKeys.WEBSITE_URL, url);
                        screenshareIntent.putExtra(IntentExtraKeys.USERNAME, SessionData.getInstance().getName());
                        screenshareIntent.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME, "Screenshare");
                        context.startActivity(screenshareIntent);
                    }
                };

                screensharerequest.setSpan(span, startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                screensharerequest.setSpan(new ForegroundColorSpan(color), startIndex + 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return screensharerequest;
        }else if (flag == 4){
            int startIndex = mess.indexOf("."), endIndex = mess.length();
            final SpannableString screensharerequest = new SpannableString(mess);
            try {
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (grpAVBroadcastState == FeatureState.INACCESSIBLE) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(widget.getContext());
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        } else {
                            Intent intent = new Intent(context, CCVideoChatActivity.class);
                            intent.putExtra(StaticMembers.SCREENSHARE_MODE, true);
                            intent.putExtra(CometChatKeys.AVchatKeys.ROOM_NAME, channel);
                            Logger.error(TAG,"initiator id: "+ContactID);
                            intent.putExtra("CONTACT_ID", String.valueOf(ContactID));
                            intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG, true);
                            intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG, false);
                            context.startActivity(intent);
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

    private void viewWriteBoard(String url) {
        Intent i = new Intent(context, CCWebViewActivity.class);
        i.putExtra(IntentExtraKeys.WEBSITE_URL,url);
        i.putExtra(IntentExtraKeys.USERNAME, SessionData.getInstance().getName());
        Logger.error(TAG,"username: "+SessionData.getInstance().getName());
        Logger.error(TAG,"WEBSITE URL: "+url);
        i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME,"Collaborative Document");
        context.startActivity(i);
    }

    private void viewWhiteBoard(String room) {
        Intent i = new Intent(context, CCWebViewActivity.class);
        i.putExtra(IntentExtraKeys.WEBSITE_URL,room);
        i.putExtra(IntentExtraKeys.WEBSITE_TAB_NAME,"White Board");
        context.startActivity(i);
    }

    private Spannable getAVConferenceText(String message, final Long buddyId, final boolean isAduio, final String callID) {
//        final Chatrooms chatroom = JsonPhp.getInstance().getLang().getChatrooms();
        Logger.error(TAG,"CALL_ID : "+callID);
        final SpannableString avbroadcastMessage;
        String messageText = message;

        if (buddyId == SessionData.getInstance().getId()) {
            avbroadcastMessage = new SpannableString(messageText);
        } else {
            messageText = messageText + "\n" + (String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_JOIN));
            avbroadcastMessage = new SpannableString(messageText);

            ClickableSpan clickable = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if(isAduio){
                        if(grpAudioCallState != FeatureState.ACCESSIBLE){
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(widget.getContext());
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        }else {
                            openVideoChatActivity(callID, isAduio);
                        }
                    }else {
                        if(grpAvCallState != FeatureState.ACCESSIBLE){
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(widget.getContext());
                            alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        }else {
                            openVideoChatActivity(callID, isAduio);
                        }
                    }
                }
            };
            avbroadcastMessage.setSpan(clickable, message.length(), messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        avbroadcastMessage.setSpan(new ForegroundColorSpan(color),message.length(), messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return avbroadcastMessage;
    }

    private void openVideoChatActivity(String callID, boolean isAduio) {
        Logger.error(TAG, "Clicking join");
        PreferenceHelper.save("CALL_ID",callID);
        if (CommonUtils.isConnected()) {
            if (Build.VERSION.SDK_INT >= 16) {
                String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                        CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
                if(CCPermissionHelper.hasPermissions(context, PERMISSIONS)){
                    Intent intent = new Intent(context, CCVideoChatActivity.class);
                    intent.putExtra(StaticMembers.INTENT_ROOM_NAME,callID);
                    intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);
                    intent.putExtra("CONTACT_ID", String.valueOf(ContactID));
                    intent.putExtra("GRP_ID", String.valueOf(chatroomid));
                    if(isAduio){
                        intent.putExtra("VIDEO", false);
                    }else {
                        intent.putExtra("VIDEO", true);
                    }

                    context.startActivity(intent);
                }else {
                    if(isAduio){
                        CCPermissionHelper.requestPermissions(context, PERMISSIONS, CCPermissionHelper.PERMISSION_GROUP_AUDIO_CONFERENCE);
                    }else {
                        CCPermissionHelper.requestPermissions(context, PERMISSIONS, CCPermissionHelper.PERMISSION_GROUP_AV_CONFERENCE);
                    }
                }
            } else {
                Toast.makeText(context, "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
            }
        } else {Toast.makeText(context, "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
//                    Intent intent = new Intent(context, CCVideoChatActivity.class);
//                    intent.putExtra(StaticMembers.INTENT_ROOM_NAME,callID);
//                    intent.putExtra(StaticMembers.INTENT_CR_AUDIO_CONFERENCE_FLAG, false);
//                    intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);
//                    intent.putExtra("GRP_ID", chatroomid);
//                    if(isAduio){
//                        intent.putExtra("VIDEO", false);
//                    }
//
//                    context.startActivity(intent);
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
                new FileDownloadHelper().execute(remoteID, messageURL, "1", "0",  "1");
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


    private Spannable getAVBroadcastText(String message, final Long buddyId, final String callID) {
        final SpannableString avbroadcastMessage;
        String messageText = message;

        if (buddyId == SessionData.getInstance().getId()) {
            avbroadcastMessage = new SpannableString(messageText);
        } else {
            messageText = messageText + "\n" +(String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_JOIN));
            avbroadcastMessage = new SpannableString(messageText);

            ClickableSpan clickable = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (grpAVBroadcastState == FeatureState.INACCESSIBLE) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(widget.getContext());
                        alertDialogBuilder.setMessage(R.string.rolebase_warning).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                    } else {
                        PreferenceHelper.save("CALL_ID",callID);
                        if (CommonUtils.isConnected()) {
                            if (Build.VERSION.SDK_INT >= 16) {
                                String[] PERMISSIONS = {CCPermissionHelper.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                                        CCPermissionHelper.REQUEST_PERMISSION_RECORD_AUDIO, CCPermissionHelper.REQUEST_PERMISSION_CAMERA};
                                if(CCPermissionHelper.hasPermissions(context, PERMISSIONS)){
                                    Intent intent = new Intent(context, CCVideoChatActivity.class);
                                    intent.putExtra(StaticMembers.INTENT_ROOM_NAME, callID);
                                    intent.putExtra(StaticMembers.INTENT_AVBROADCAST_FLAG,true);
                                    intent.putExtra(StaticMembers.INTENT_IAMBROADCASTER_FLAG,false);
                                    intent.putExtra(StaticMembers.INTENT_GRP_CONFERENCE_FLAG, true);
                                    intent.putExtra("GRP_ID",String.valueOf(chatroomid));
                                    intent.putExtra("CONTACT_ID",String.valueOf(buddyId));
                                    context.startActivity(intent);
                                }
                                else {
                                    CCPermissionHelper.requestPermissions(context, PERMISSIONS, CCPermissionHelper.PERMISSION_AVBROADCAST);
                                }
                            } else {
                                Toast.makeText(context, "Sorry, your device does not support this feature.", Toast.LENGTH_LONG).show();
                            }
                        } else {Toast.makeText(context, "Unable to connect. Please check your internet connection.", Toast.LENGTH_LONG).show();
                        }

                    }

                }
            };
            avbroadcastMessage.setSpan(clickable, message.length(), messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        avbroadcastMessage.setSpan(new ForegroundColorSpan(color),message.length(), messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return avbroadcastMessage;
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

    @Override
    public long getHeaderId(int i) {
        GroupMessage groupMessage = getMessageByPosition(i);
        return Long.parseLong(CommonUtils.getDateId(groupMessage.sentTimestamp));
    }

    @Override
    public OneOnOneMessageAdapter.DateItemHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_message_list_header, parent, false);
        return new OneOnOneMessageAdapter.DateItemHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(OneOnOneMessageAdapter.DateItemHolder dateItemHolder, int i, long key) {
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


    class MessageItemHolder extends RecyclerView.ViewHolder {
         public TextView senderTextField, messageTimestamp, audioTime , customSenderName;
         public EmojiTextView chatroomMessage,tvOnlySmiley;
         public ImageView imageHolder, videoThumb, videoMessageButton, audioPlayButton,customImageHolder,imagePendingMessage;
         public RelativeLayout messageContainer, audioNoteContainer;
         public View messageArrow;
         public ProgressBar wheel;
         public RoundedImageView avatar;
         public SeekBar audioSeekbar;
         private CometChat cometChat = CometChat.getInstance(context);
         int viewType;
         public int rightBubbleColor = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
         int rightBubbleTextColor = Color.WHITE;
         public int leftBubbleColor = Color.parseColor("#e6e9ed");
          int leftBubbleTextColor = Color.parseColor("#8e8e92");

        public MessageItemHolder(View view,int viewType) {
            super(view);
            this.viewType = viewType;

            if(viewType == TYPE_RIGHT){
                chatroomMessage = (EmojiTextView) view.findViewById(R.id.textViewChatroomMessageRight);
                messageTimestamp = (TextView) view.findViewById(R.id.textViewChatroomMessageTimestampRight);
                senderTextField = (TextView) view.findViewById(R.id.textViewChatroomSenderNameRight);
                imageHolder = (ImageView) view.findViewById(R.id.imageViewChatroomImageMessageRight);
                messageContainer = (RelativeLayout) view
                        .findViewById(R.id.linearLayoutChatroomMessageRightContainer);
                messageArrow = view.findViewById(R.id.rightArrow);

                videoThumb = (ImageView) view.findViewById(R.id.imageViewChatroomVideoMessageRight);
                videoMessageButton = (ImageView) view.findViewById(R.id.imageViewChatroomVideoMessageButton);
                wheel = (ProgressBar) view.findViewById(R.id.progressWheelVideo);
                avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
                tvOnlySmiley = (EmojiTextView) view.findViewById(R.id.textViewChatroomSmileyRight);
                audioNoteContainer = (RelativeLayout) view.findViewById(R.id.relativeLayoutAudioNoteContainer);
                audioPlayButton = (ImageView) view.findViewById(R.id.imageViewPlayIcon);
                audioTime = (TextView) view.findViewById(R.id.textViewTime);
                audioSeekbar = (SeekBar) view.findViewById(R.id.seek_bar);
                customImageHolder = (ImageView) view.findViewById(R.id.customImageViewOneOnOneImageMessageRight);
                imagePendingMessage = (ImageView) view.findViewById(R.id.img_message_pending);

                LayerDrawable ldrawable = (LayerDrawable) messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                shape.setColor(rightBubbleColor);
                GradientDrawable drawable = (GradientDrawable) messageContainer.getBackground();
                drawable.setColor(rightBubbleColor);
                chatroomMessage.setTextColor(rightBubbleTextColor);
                senderTextField.setTextColor(rightBubbleTextColor);
                audioTime.setTextColor(rightBubbleTextColor);
                imagePendingMessage.setVisibility(View.GONE);
                messageTimestamp.setTextColor(leftBubbleTextColor);

            } else {
                chatroomMessage = (EmojiTextView) view.findViewById(R.id.textViewChatroomMessageLeft);
                messageContainer = (RelativeLayout) view
                        .findViewById(R.id.linearLayoutChatroomMessageLeftContainer);
                messageTimestamp = (TextView) view.findViewById(R.id.textViewChatroomMessageTimestampLeft);
                senderTextField = (TextView) view.findViewById(R.id.textViewChatroomSenderNameLeft);
                imageHolder = (ImageView) view.findViewById(R.id.imageViewChatroomImageMessageLeft);
                messageArrow = view.findViewById(R.id.leftArrow);
                videoThumb = (ImageView) view.findViewById(R.id.imageViewChatroomVideoMessageLeft);
                videoMessageButton = (ImageView) view.findViewById(R.id.imageViewChatroomVideoMessageButton);
                wheel = (ProgressBar) view.findViewById(R.id.progressWheelVideo);
                avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
                customImageHolder = (ImageView) view.findViewById(R.id.customImageViewOneOnOneImageMessageLeft);
                audioNoteContainer = (RelativeLayout) view.findViewById(R.id.relativeLayoutAudioNoteContainer);
                audioPlayButton = (ImageView) view.findViewById(R.id.imageViewPlayIcon);
                audioTime = (TextView) view.findViewById(R.id.textViewTime);
                audioSeekbar = (SeekBar) view.findViewById(R.id.seek_bar);
                tvOnlySmiley = (EmojiTextView) view.findViewById(R.id.textViewChatroomSmileyLeft);

                LayerDrawable ldrawable = (LayerDrawable) messageArrow.getBackground();
                GradientDrawable shape = ((GradientDrawable) ((RotateDrawable) (ldrawable
                        .findDrawableByLayerId(R.id.layerItemRightArrow))).getDrawable());

                shape.setColor(leftBubbleColor);
                GradientDrawable drawable = (GradientDrawable) messageContainer.getBackground();
                drawable.setColor(leftBubbleColor);
                chatroomMessage.setTextColor(leftBubbleTextColor);
                senderTextField.setTextColor(leftBubbleTextColor);
                audioTime.setTextColor(leftBubbleTextColor);
                messageTimestamp.setTextColor(leftBubbleTextColor);

            }
        }
    }

    public interface RetryCallback{
        public void onRetryClicked(long localMessageId);
    }
}
