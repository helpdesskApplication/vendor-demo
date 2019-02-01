/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package customsviews;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ConfirmationWindow {

	private AlertDialog.Builder builder;

	public ConfirmationWindow(Context context, String positiveTitle, String negativeTitle) {
		builder = new AlertDialog.Builder(context);
		if (!positiveTitle.equals("")) {
			builder.setPositiveButton(positiveTitle, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					setPositiveResponse();

				}
			});
		}
		if (!negativeTitle.equals("")) {
			builder.setNegativeButton(negativeTitle, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					setNegativeResponse();
				}
			});
		}
	}

	public AlertDialog.Builder setMessage(String message) {
		return builder.setMessage(message);
	}

	public void setCancelable(boolean flag) {
		if (builder != null) {
			builder.setCancelable(flag);
		}
	}

	public void show() {
		builder.show();
	}

	protected void setPositiveResponse() {
	}

	protected void setNegativeResponse() {
	}

}