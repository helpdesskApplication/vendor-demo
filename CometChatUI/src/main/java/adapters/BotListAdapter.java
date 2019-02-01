package adapters;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.inscripts.custom.RoundedImageView;
import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Bot;

import java.util.List;

public class BotListAdapter extends BaseAdapter {

    Context context;
    List<Bot> botList;
    private int colorPrimary, colorPrimaryDark;
    private CometChat cometChat;
    public BotListAdapter(Context context , List<Bot> botList) {
        this.botList = botList;
        this.context = context;
        cometChat = CometChat.getInstance(context);
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
    }

    @Override
    public int getCount() {
        return botList.size();
    }

    @Override
    public Bot getItem(int i) {
        return botList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return botList.get(i).botId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        View view = convertView;
        final ViewHolder holder;

        if (null == view) {
            view = LayoutInflater.from(context).inflate(R.layout.cc_custom_list_item_bot, viewGroup, false);

            holder = new ViewHolder();
            holder.avatar = (RoundedImageView) view.findViewById(R.id.imageViewUserAvatar);
            holder.botName = (TextView) view.findViewById(R.id.textviewUserName);
            holder.botDescription = (TextView) view.findViewById(R.id.textviewBotDescription);
            holder.botKey = (TextView) view.findViewById(R.id.textviewBotKey);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final Bot bot = getItem(position);

        holder.botName.setText(bot.botName);
        String keyName = bot.botName.toLowerCase().trim().replaceAll(" ","");
        holder.botKey.setText("@"+keyName.trim());
        if(bot.botDescription != null && !bot.botDescription.isEmpty()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.botDescription.setText(Html.fromHtml(bot.botDescription, Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.botDescription.setText(Html.fromHtml(bot.botDescription));
            }
        }else {
            holder.botDescription.setText("Hi.. I am helper Bot.");
        }

        holder.avatar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        holder.botKey.setTextColor(colorPrimary);


        /** Repacling Picasso with glide**/

        LocalStorageFactory.loadImageUsingURL(context,bot.botAvatar,holder.avatar,R.drawable.cc_ic_robot);

       /* Glide.with(context)
                .load(bot.botAvatar)
                .centerCrop()
                .placeholder(R.drawable.vector_drawable_ic_default_avtar)
                .into(holder.avatar);*/

        return view;
    }

    private static class ViewHolder {
        public TextView botName;
        public TextView botDescription;
        public TextView botKey;
        public RoundedImageView avatar;
    }

}
