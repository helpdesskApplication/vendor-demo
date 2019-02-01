/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.utils.Logger;
import com.inscripts.utils.StaticMembers;

import cometchat.inscripts.com.readyui.R;

public class CCImagePreviewActivity extends Activity {
	private static final String TAG = "CCImagePreviewActivity";
	private ImageView previewImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cc_custom_image_preview);
		Intent intent = getIntent();
		String msg = intent.getStringExtra(StaticMembers.INTENT_IMAGE_PREVIEW_MESSAGE);
		Logger.error(TAG,"Message : "+msg);
		previewImage = (ImageView) findViewById(R.id.imageViewLargePreview);
		setResult(1112, getIntent());
   		LocalStorageFactory.loadImageUsingURL(this,msg,previewImage,R.drawable.cc_thumbnail_default);
		ImageView closePreview = (ImageView) findViewById(R.id.imageViewClosePreviewPopup);
		closePreview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				finish();
			}
		});
	}
}
