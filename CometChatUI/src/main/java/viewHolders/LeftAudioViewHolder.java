package viewHolders;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.inscripts.custom.RoundedImageView;

import cometchat.inscripts.com.readyui.R;

import static android.content.Context.WINDOW_SERVICE;

public class LeftAudioViewHolder extends RecyclerView.ViewHolder{
    public TextView messageTimeStamp,senderName,audioLength;
    public RoundedImageView avatar;
    public View audioContainer;
    public ImageView playAudio;
    public ImageView leftArrow;
    public Guideline leftGuideLine;
    public ProgressBar fileLoadingProgressBar;
    public SeekBar audioSeekBar;
    public LeftAudioViewHolder(Context context, View leftAudioMessageView) {
        super(leftAudioMessageView);
        messageTimeStamp = leftAudioMessageView.findViewById(R.id.timeStamp);
        avatar = leftAudioMessageView.findViewById(R.id.imgAvatar);
        senderName = leftAudioMessageView.findViewById(R.id.senderName);
        audioContainer = leftAudioMessageView.findViewById(R.id.audioNoteContainer);
        playAudio = leftAudioMessageView.findViewById(R.id.playButton);
        leftArrow = leftAudioMessageView.findViewById(R.id.leftArrow);
        leftGuideLine = leftAudioMessageView.findViewById(R.id.leftGuideline);
        fileLoadingProgressBar = leftAudioMessageView.findViewById(R.id.fileLoadingProgressBar);
        audioSeekBar = leftAudioMessageView.findViewById(R.id.audioSeekBar);
        audioLength = leftAudioMessageView.findViewById(R.id.audioLength);
        Display display = ((WindowManager) context.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
        if(orientation == 1 || orientation == 3){
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) leftGuideLine.getLayoutParams();
            params.guidePercent = 0.5f;
            leftGuideLine.setLayoutParams(params);
        }
    }
}
