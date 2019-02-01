package helpers;

import com.inscripts.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class NotificationDataHelper {
    private static final String TAG = "NotificationDataHelper";
    private static HashMap<Integer, ArrayList<String>> notifications = new HashMap<>();

    public static void addToMap(int notificationId, String alert) {
        Logger.error(TAG, "addToMap");
        ArrayList<String> temp = notifications.get(notificationId);
        if(temp == null){
            temp = new ArrayList<>();
            temp.add(alert);
        }else {
            temp.add(alert);
        }
        notifications.put(notificationId, temp);
    }

    public static ArrayList<String> getFromMap(Integer notificationId) {
        Logger.error(TAG, "getFromMap: " );
        return notifications.get(notificationId);
    }

    public static void deleteFromMap(Integer notificationId) {
        Logger.error(TAG, "deleteFromMap: " );
        if (notifications.containsKey(notificationId)) {
            notifications.remove(notificationId);
        }
    }

}
