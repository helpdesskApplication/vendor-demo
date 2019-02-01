package models;

import com.inscripts.orm.SugarRecord;
import com.inscripts.orm.dsl.Column;

import java.util.List;
import java.util.Locale;

/**
 * Created by inscripts on 17/1/17.
 */

public class Status extends SugarRecord {

    public static final String TABLE_NAME = Status.class.getSimpleName().toLowerCase(Locale.US);
    public static final String COLUMN_STATUS_MESSAGE = "message";

    @Column(name=COLUMN_STATUS_MESSAGE)
    public String message;

    public Status() {
//        Logger.error("Status : Status() : Constructor invoked");
    }

    public static List<Status> getAllStatusMessages() {
        return findWithQuery(Status.class, "SELECT * FROM `" + TABLE_NAME + "` ORDER BY `id" + "` DESC;", new String[0]);
    }

    public static List<Status> getStatusFromMessage(String message) {
        String[] messages = new String[] {message};
        return findWithQuery(Status.class, "SELECT * FROM `" + TABLE_NAME + "` WHERE `" + COLUMN_STATUS_MESSAGE + "` = ?;", messages[0]);
    }

    public static void insertStatus(String status) {
        Status statusMessage = new Status();
        statusMessage.message = status;
        statusMessage.save();
    }

    public static void deleteStatus(Long id) {
        String whereClause = "`id`" + " =?";
        String[] whereArgs = {String.valueOf(id)};
        deleteAll(Status.class, whereClause, whereArgs);
    }
}
