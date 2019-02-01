package services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;

import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.ImageUploadHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.CometchatCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

public class AvatarService extends IntentService {

    private static final String TAG = AvatarService.class.getSimpleName();
    private static final String CHANGE_AVATAR = "change_avatar";
    private static final String FILE_NAME = "fileName";

    private static CometchatCallbacks callbacks;
    private static Bitmap bitmap;
    private static Handler mHandler;

    public AvatarService() {
        super("AvatarService");
    }

    public static void setHandler(Handler handler) {
        mHandler = handler;
    }

    public static void startActionChangeAvatar(Context context, String fileName, CometchatCallbacks callbacks, Bitmap bitmap) {
        AvatarService.callbacks = callbacks;
        AvatarService.bitmap = bitmap;

        Intent intent = new Intent(context, AvatarService.class);
        intent.setAction(CHANGE_AVATAR);
        intent.putExtra(FILE_NAME, fileName);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
             if (CHANGE_AVATAR.equals(action)) {
                final String fileName = intent.getStringExtra(FILE_NAME);
                handleActionChangeAvatar(fileName, callbacks, bitmap);
            }
        }
    }

    private void handleActionChangeAvatar(String filename, final CometchatCallbacks callbacks, Bitmap bitmap) {
        try {
            ImageUploadHelper imageSendHelper = new ImageUploadHelper(PreferenceHelper.getContext(),
                    URLFactory.getPhoneRegisterURL(), mHandler);
            if (bitmap != null) {
                ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                String fileExtension = filename.substring(filename.lastIndexOf(".") + 1);
                if (fileExtension.equalsIgnoreCase("jpg") || fileExtension.equalsIgnoreCase("jpeg")) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstr);
                } else {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outstr);
                }
                byte[] outputData = outstr.toByteArray();

                File compressedImageFile = LocalStorageFactory.writeFile(LocalStorageFactory.getImageStoragePath(),
                        filename, outputData);
                imageSendHelper.addNameValuePair(CometChatKeys.AjaxKeys.ACTION, "change_avatar");
                imageSendHelper.addFile(CometChatKeys.FileUploadKeys.FILEDATA, compressedImageFile);
                imageSendHelper.sendImageAjax();
            }
        } catch (FileNotFoundException ex) {
            Logger.error(TAG, "handleActionChangeAvatar() : Exception : " + ex.getMessage());
        }
    }

}
