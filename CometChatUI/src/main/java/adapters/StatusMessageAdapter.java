package adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import cometchat.inscripts.com.readyui.R;
import models.Status;

import java.util.ArrayList;

/**
 * Created by inscripts on 16/1/17.
 */

public class StatusMessageAdapter extends ArrayAdapter<String> {

    private ArrayList<Status> listStatusMessages;

    public StatusMessageAdapter(Context context, ArrayList<Status> listStatusMessages) {
        super(context, R.layout.cc_custom_list_item_status_message);
        this.listStatusMessages = listStatusMessages;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final ViewHolder holder;

        if (null == view) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cc_custom_list_item_status_message, parent, false);
            holder = new ViewHolder();
            holder.tvStatus = (TextView) view.findViewById(R.id.tv_status_message);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.tvStatus.setText(listStatusMessages.get(position).message);
        return view;
    }

    @Override
    public int getCount() {
        return listStatusMessages.size();
    }

    private static class ViewHolder{
        public TextView tvStatus;
    }
}
