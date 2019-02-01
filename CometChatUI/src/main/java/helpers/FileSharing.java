package helpers;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;

import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.BitmapProcessingHelper;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FileSharing {

    private static String fileName;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getImageBitmap(Context context, Intent data, final Long windowId, final boolean isChatroom) {
        try {
            String filePath = null;
            if(CommonUtils.isSamsung()){
                filePath = LocalStorageFactory.getFilePathFromIntent(data);
            }else if(CommonUtils.isXiaomi()){
                try{
                    filePath = LocalStorageFactory.getPath(data.getData(),true);
                }catch (Exception e){
                    e.printStackTrace();
                    filePath = data.getData().getPath().toString().replace("file://", "");
                }

            } else if (Build.VERSION.SDK_INT > 19) {
                try{
                    filePath = LocalStorageFactory.getPath(data.getData(),true);
                }catch (Exception e){
                    Uri imagePhotos = data.getData();
                    InputStream is = null;
                    String myPath;
                    if (imagePhotos.getAuthority() != null) {
                        try {
                            is = context.getContentResolver().openInputStream(imagePhotos);
                            Bitmap bmp = BitmapFactory.decodeStream(is);
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            filePath = MediaStore.Images.Media.insertImage(context.getContentResolver(), bmp, "Title", null);
                            //String path = writeToTempImageAndGetPathUri(this, bmp).toString();
                            Uri pathUri = Uri.parse(filePath);
                            if (pathUri != null) {
                                String[] filePathColumn = {MediaStore.MediaColumns.DATA};
                                Cursor cursor = PreferenceHelper.getContext().getContentResolver()
                                        .query(pathUri, filePathColumn, null, null, null);
                                cursor.moveToFirst();
                                filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));

                                cursor.close();
                            } else {
                                myPath = null;
                            }
                        }catch (Exception e2 ){
                            e2.printStackTrace();
                        }
                    }
                }
            } else {
                Uri imageUri = data.getData();
                String wholeID = DocumentsContract.getDocumentId(imageUri);
                String id = wholeID.split(":")[1];
                String[] column = {MediaStore.Images.Media.DATA};
                String sel = MediaStore.Images.Media._ID + "=?";

                Cursor cursor = PreferenceHelper.getContext().getContentResolver().
                        query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                column, sel, new String[]{id}, null);
                int columnIndex = cursor.getColumnIndex(column[0]);
                filePath = "";
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
            if (filePath != null) {
                return filePath;
            } else {
                Logger.error("File path is null");
            }
        } catch (Exception e) {
            Logger.error("Exception at ImageSharing.java sendImage() :" + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }
}
