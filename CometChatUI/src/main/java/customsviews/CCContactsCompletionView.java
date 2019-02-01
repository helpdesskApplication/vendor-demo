/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package customsviews;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.pojos.CCSettingMapper;
import com.tokenautocomplete.TokenCompleteTextView;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import pojo.ContactPojo;

public class CCContactsCompletionView extends TokenCompleteTextView {

    public CCContactsCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    private CometChat cometChat;
    @Override
    protected View getViewForObject(Object object) {
        ContactPojo contact = (ContactPojo) object;

        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout view = (LinearLayout) l.inflate(R.layout.cc_contact_token,
                (ViewGroup) CCContactsCompletionView.this.getParent(), false);
        ((TextView) view.findViewById(R.id.name)).setText(contact.name);
        cometChat = CometChat.getInstance(PreferenceHelper.getContext());
        view.setBackgroundColor(Color.parseColor((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_ACTIONBAR))));
        return view;
    }

    @Override
    protected Object defaultObject(String completionText) {
        return new ContactPojo(completionText, completionText.replace(" ", ""));
    }

    @Override
    protected void onSelectionChanged(int arg0, int arg1) {
        try {
            super.onSelectionChanged(arg0, arg1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}