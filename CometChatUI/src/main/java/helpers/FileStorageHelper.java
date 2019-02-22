package helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.ImageView;


import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.glide.Glide;
import com.inscripts.glide.RequestBuilder;
import com.inscripts.glide.request.RequestOptions;
import com.inscripts.glide.request.target.SimpleTarget;
import com.inscripts.glide.request.transition.Transition;
import com.inscripts.helpers.BitmapProcessingHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.utils.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import Keys.BroadCastReceiverKeys;
import Keys.MessageTypeKeys;
import cometchat.inscripts.com.readyui.R;
import models.GroupMessage;
import models.OneOnOneMessage;

import static com.inscripts.factories.LocalStorageFactory.getImageStoragePath;
import static com.inscripts.factories.LocalStorageFactory.scanFileForGallery;

/**
 * Created by Inscripts on 07/06/17.
 */

public class FileStorageHelper {

    private static final java.lang.String TAG = FileStorageHelper.class.getSimpleName();
    private static SimpleTarget simpleTarget;
    private static int downloadAttempt = 0;

    /**
     * @param filePath :Creates directory if not exists for a specified path
     */
    public static void createDirectory(String filePath) {
        if (!new File(filePath).exists()) {
            new File(filePath).mkdirs();
        }
    }

    public static void saveIncomingImage(final String fileName, final String url, final ImageView imageview,
                                         final boolean isChatroom, final String messageId, final boolean isHandwrite) {
        try {

            final String filePath = getImageStoragePath();
            Logger.error(TAG,"Save Incomming Called");
            if (!url.contains(".gif")) {
                simpleTarget = new SimpleTarget<BitmapDrawable>() {
                    @Override
                    public void onResourceReady(BitmapDrawable resource, Transition<? super BitmapDrawable> transition) {
                        try {
                            Bitmap bitmap = resource.getBitmap();
                            Logger.error(TAG,"OnResource ready Called");
                            LocalStorageFactory.createDirectory(filePath);
                            bitmap = BitmapProcessingHelper.scaleBitmap(bitmap, bitmap.getHeight(), bitmap.getWidth(),
                                    filePath);
                            if (null != bitmap) {
                                File file = new File(filePath + fileName);
                                FileOutputStream ostream = new FileOutputStream(file);
                                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                                if (fileExtension.equalsIgnoreCase("jpg") || fileExtension.equalsIgnoreCase("jpeg")) {
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, ostream);
                                } else {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 80, ostream);
                                }
                                ostream.close();
                                scanFileForGallery(filePath + fileName, false);
                            }
                            Logger.error(TAG,"IS Chatroom Image ?"+isChatroom+"  IS handwrite image ? "+isHandwrite);
                            if (isChatroom) {
                                GroupMessage pojo = GroupMessage.findById(messageId);
                                if (isHandwrite) {
                                    pojo.message = filePath+fileName;
                                    pojo.type = MessageTypeKeys.HANDWRITE_MESSAGE;
                                } else {
                                    pojo.message = filePath + fileName;
                                    pojo.type = MessageTypeKeys.IMAGE_MESSAGE;
                                }
                                pojo.save();

                                Intent imageIntent = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                                imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                PreferenceHelper.getContext().sendBroadcast(imageIntent);
                            } else {
                                OneOnOneMessage temp = OneOnOneMessage.findById(messageId);
                                if (isHandwrite) {
                                    temp.message = filePath+fileName;
                                    temp.type = MessageTypeKeys.HANDWRITE_MESSAGE;
                                } else {
                                    temp.message = filePath + fileName;
                                    temp.type = MessageTypeKeys.IMAGE_MESSAGE;
                                }
                                temp.save();

                                Intent imageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                                imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                                PreferenceHelper.getContext().sendBroadcast(imageIntent);
                            }

                        } catch (IOException e) {
                            Logger.error("LocalStorageFactory.java saveIncomingImage()->onBitmapLoaded() : Exception = "
                                    + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }


                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);

                        if (downloadAttempt < 1) {
                            Logger.error(TAG, "onLoadFailed: " );
                            RequestOptions requestOptions = new RequestOptions()
                            .placeholder(R.drawable.cc_thumbnail_default)
                            .override(800, 600);


                            RequestBuilder drawableRequest = Glide.with(PreferenceHelper.getContext().getApplicationContext())
                                    .applyDefaultRequestOptions(requestOptions)
                                    .load(url);


                            //* Save image to local storage *//*
                            drawableRequest.into(simpleTarget);
                            //* Render image to imageview *//*
                            if (null != imageview) {
                                drawableRequest.into(imageview);
                            }
                            downloadAttempt = 1;
                            Logger.error("2nd attempt");
                        }
                    }

                };

                Handler uiHandler = new Handler(Looper.getMainLooper());
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
//                            if(!((Activity)PreferenceHelper.getContext()).isFinishing()) {
                                RequestBuilder drawableRequest = Glide.with(PreferenceHelper.getContext().getApplicationContext())
                                        .load(url);
                                drawableRequest.into(simpleTarget);
//                            }
                        }
                    });
            }

        } catch (Exception e) {
            Logger.error("LocalStorageFactory.java saveIncomingImage() : Exception =" + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            Logger.error("Save incoming image outof memory exception");
        }
    }



}
