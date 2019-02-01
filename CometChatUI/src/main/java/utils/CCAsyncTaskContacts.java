/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import java.util.ArrayList;

import interfaces.CCContactsCallbacks;
import pojo.ContactPojo;

public class CCAsyncTaskContacts extends AsyncTask<Void, Void, Void> {

    private Context context;
    private CCContactsCallbacks callback;
    private ArrayList<ContactPojo> allContacts;

    public CCAsyncTaskContacts(Context context, CCContactsCallbacks callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {

        try {
            allContacts = new ArrayList<>();
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone._ID}, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String contactNumber = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String contactName = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                int phoneContactID = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
                ContactPojo contact = new ContactPojo();
                contact.id = String.valueOf(phoneContactID);
                contact.name = contactName;
                contact.phone = contactNumber;
                allContacts.add(contact);
                cursor.moveToNext();
            }
            cursor.close();
            cursor = null;
        } catch (Exception e) {
            e.printStackTrace();
            callback.failCallback(e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        callback.successCallback(allContacts);
    }
}