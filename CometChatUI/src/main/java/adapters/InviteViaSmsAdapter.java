/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import cometchat.inscripts.com.readyui.R;
import pojo.ContactPojo;

import java.util.List;

public class InviteViaSmsAdapter extends ArrayAdapter<ContactPojo>{

	public InviteViaSmsAdapter(Context context, List<ContactPojo> objects) {
		super(context, 0, objects);
	}

	private static class ViewHolder {
		public TextView userName;
		public TextView userPhone;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = convertView;
		ViewHolder holder;

		if (null == view) {
			view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.cc_custom_list_item_sms_invite, parent, false);
			holder = new ViewHolder();

			holder.userName = (TextView) view.findViewById(R.id.textViewUserToInvite);
			holder.userPhone = (TextView) view.findViewById(R.id.textViewUserStatusToInvite);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		ContactPojo contact = getItem(position);

		holder.userName.setText(Html.fromHtml(contact.name));
		holder.userPhone.setText(contact.phone);
		return view;
	}
}