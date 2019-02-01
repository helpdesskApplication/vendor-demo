package cometchat.inscripts.com.cometchatui.services;

/**
 * Created by Inscripts on 22/06/17.
 */


import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import activities.CometChatActivity;
import cometchat.inscripts.com.cometchatui.R;
import utils.CCNotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public static final String PUSH_CHANNEL = "push_channel";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage){
        CCNotificationHelper.processCCNotificationData(getApplicationContext(),remoteMessage,false,R.drawable.ic_launcher,R.drawable.ic_launcher_small);
//        CCNotificationHelper.processCCNotificationData(getApplicationContext(),remoteMessage,R.drawable.ic_launcher,R.drawable.ic_launcher_small, CometChatActivity.class );
    }
}
