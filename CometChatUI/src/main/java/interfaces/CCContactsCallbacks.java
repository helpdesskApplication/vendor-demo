/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package interfaces;

import java.util.ArrayList;

import pojo.ContactPojo;

public interface CCContactsCallbacks {

	public void successCallback(ArrayList<ContactPojo> contactList);

	public void failCallback(String errorMessage);

}
