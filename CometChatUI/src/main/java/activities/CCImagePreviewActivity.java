/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.inscripts.custom.ZoomableImageView;
import com.inscripts.glide.Glide;
import com.inscripts.glide.request.RequestOptions;
import com.inscripts.glide.request.target.SimpleTarget;
import com.inscripts.glide.request.transition.Transition;
import com.inscripts.utils.Logger;
import com.inscripts.utils.StaticMembers;

import java.io.File;

import cometchat.inscripts.com.readyui.R;

public class CCImagePreviewActivity extends Activity {
	private static final String TAG = "CCImagePreviewActivity";
	private ZoomableImageView previewImage;
    private ImageView imageViewForGif;
    private ImageView imageViewShare;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cc_custom_image_preview);
		Intent intent = getIntent();
		final String msg = intent.getStringExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE);
		Logger.error(TAG,"Message : "+msg);
        previewImage = findViewById(R.id.imageViewLargePreview);
        imageViewForGif = findViewById(R.id.imageViewForGif);
        imageViewShare = findViewById(R.id.imgShare);
        if (msg.contains(".gif")) {
            imageViewShare.setVisibility(View.GONE);
        }

		setResult(1112, getIntent());
		if(msg.contains(".gif")){
		    previewImage.setVisibility(View.GONE);
		    imageViewForGif.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext())
                    .load(msg)
                    .into(imageViewForGif);

        }else {
		    // Hide in case of images
            imageViewForGif.setVisibility(View.GONE);
            RequestOptions requestOptions = new RequestOptions()
                    .override(800,600)
                    .fitCenter()
                    .placeholder(R.drawable.cc_thumbnail_default);
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(msg)
                    .apply(requestOptions)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                            previewImage.setImageBitmap(bitmap);
                        }
                    });
        }
		ImageView closePreview = (ImageView) findViewById(R.id.imageViewClosePreviewPopup);
		closePreview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});
		imageViewShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpg");
                final File photoFile = new File(msg);
                Logger.error(TAG, "onClick: getPath: "+photoFile.getPath());
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
                startActivity(Intent.createChooser(shareIntent, "Share image using"));
            }
        });
	}
}
