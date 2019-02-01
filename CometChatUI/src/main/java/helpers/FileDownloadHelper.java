/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package helpers;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import Keys.BroadCastReceiverKeys;
import Keys.MessageTypeKeys;
import models.GroupMessage;
import models.OneOnOneMessage;

public class FileDownloadHelper extends AsyncTask<String, Void, Void> {

    private static final java.lang.String TAG = FileDownloadHelper.class.getSimpleName();

    /**
     * Downloads the file and returns it's location on the sdcard with full path
     */
    private static String downloadFile(String remoteFileLocation, boolean isVideo, boolean isNormalFile) {
        int count;
        try {
            URL url = new URL(remoteFileLocation);
            URLConnection conection = url.openConnection();
            conection.connect();

            // for progress bar
            // int lenghtOfFile = conection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            String storagePath;
            if (isNormalFile) {
                storagePath = LocalStorageFactory.getNormalStoragePath();
            } else if (isVideo) {
                storagePath = LocalStorageFactory.getVideoStoragePath();
            } else {
                storagePath = LocalStorageFactory.getAudioStoragePath();
            }


            LocalStorageFactory.createDirectory(storagePath);
            String fileName = getFileName(remoteFileLocation);
            if(fileName.contains("/")){
                fileName = fileName.replace("/", "");
            }
//            String fileName = String.valueOf(System.currentTimeMillis());
            String finalPath = storagePath + fileName;
            Logger.error(TAG,"fileName: "+fileName);
            Logger.error(TAG,"file final path: "+finalPath);
            // Output stream
            FileOutputStream output = new FileOutputStream(finalPath);

            byte data[] = new byte[1024];
            // long total = 0;

            while ((count = input.read(data)) != -1) {
                // total += count;
                // Logger.error("count: " + count);
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();
            // closing streams
            output.close();
            input.close();
            LocalStorageFactory.scanFileForGallery(finalPath, false);
            return finalPath;
        } catch (Exception e) {
            Logger.error("Exception in downloading: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    private static String getFileName(String remoteFileLocation) {
        if(remoteFileLocation.contains("s3") && remoteFileLocation.contains("amazonaws")){
            return remoteFileLocation.substring(remoteFileLocation.lastIndexOf("/") + 1, remoteFileLocation.length());
        }else {
            return remoteFileLocation.substring(remoteFileLocation.lastIndexOf("=") + 1,
                    remoteFileLocation.length());
        }
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            Logger.error(TAG,"doInBackground");
            Context context = PreferenceHelper.getContext();

            Logger.error(TAG,"params[1] = "+params[1]);
            Logger.error(TAG,"params[3] = "+params[3]);
            Logger.error(TAG,"params[4] = "+params[4]);
            Logger.error(TAG,"params[4] = "+params[2]);
            boolean isVideo = params[3].equals("1");
            boolean isNormalFile = params[4].equals("1");

            String videoLocalPath = downloadFile(params[1], isVideo, isNormalFile);

            Logger.error(TAG,"VideoLocalPath = "+videoLocalPath);
            if (isNormalFile) {
                if ("0".equals(params[2])) {
                    OneOnOneMessage message = OneOnOneMessage.findById(params[0]);
                    if (null != message) {
                        message.message = message.message + "#" + videoLocalPath;
                        message.type = MessageTypeKeys.FILE_DOWNLOADED;
                        message.save();

                        Intent imageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                        imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                        context.sendBroadcast(imageIntent);
                    }
                } else {
                    GroupMessage message = GroupMessage.findById(params[0]);
                    if (null != message) {
                        message.message = message.message + "#" + videoLocalPath;
                        message.type = MessageTypeKeys.FILE_DOWNLOADED;
                        message.save();

                        Intent imageIntent = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                        imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                        imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.FILE_DOWNLOAD, true);
                        PreferenceHelper.getContext().sendBroadcast(imageIntent);
                    }
                }
            } else {
                if (isVideo) {
                    if ("0".equals(params[2])) {
                        OneOnOneMessage message = OneOnOneMessage.findById(params[0]);
                        if (null != message) {
                            message.message = videoLocalPath;
                            message.type = MessageTypeKeys.VIDEO_MESSAGE;
                            message.save();

                            Intent imageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                            imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                            PreferenceHelper.getContext().sendBroadcast(imageIntent);
                        }
                    } else {
                        GroupMessage message = GroupMessage.findById(params[0]);
                        if (null != message) {
                            message.message = videoLocalPath;
                            message.type = MessageTypeKeys.VIDEO_MESSAGE;
                            message.save();
                            Intent imageIntent = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                            imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                            PreferenceHelper.getContext().sendBroadcast(imageIntent);
                        }
                    }
                } else {
                    if ("0".equals(params[2])) {
                        Logger.error(TAG,"Audio downloaded");
                        OneOnOneMessage message = OneOnOneMessage.findById(params[0]);
                        if (null != message) {
                            message.message = videoLocalPath;
                            message.type = MessageTypeKeys.AUDIO_MESSAGE;
                            message.save();

                            Intent imageIntent = new Intent(BroadCastReceiverKeys.MESSAGE_DATA_UPDATED_BROADCAST);
                            imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                            PreferenceHelper.getContext().sendBroadcast(imageIntent);
                        }
                    } else {
                        GroupMessage message = GroupMessage.findById(params[0]);
                        if (null != message) {
                            message.message = videoLocalPath;
                            message.type = MessageTypeKeys.AUDIO_MESSAGE;
                            message.save();

                            Intent imageIntent = new Intent(BroadCastReceiverKeys.GROUP_MESSAGE_DATA_UPDATED_BROADCAST);
                            imageIntent.putExtra(BroadCastReceiverKeys.IntentExtrasKeys.NEW_MESSAGE, 1);
                            PreferenceHelper.getContext().sendBroadcast(imageIntent);
                        }
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
