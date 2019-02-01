package viewHolders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import cometchat.inscripts.com.readyui.R;

public class CallViewHolder extends RecyclerView.ViewHolder{
   public TextView callMessage,messageTimeStamp;
    public CallViewHolder(View callView) {
        super(callView);
        callMessage = callView.findViewById(R.id.callMessage);
        messageTimeStamp = callView.findViewById(R.id.timeStamp);
    }
}
