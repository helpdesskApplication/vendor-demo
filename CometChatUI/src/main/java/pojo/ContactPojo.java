/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package pojo;

import java.io.Serializable;

public class ContactPojo implements Serializable {

	private static final long serialVersionUID = 3885812768370284104L;
	public String name, phone, id;

	public ContactPojo() {
	}

	public ContactPojo(String name, String phone) {
		this.name = name;
		this.phone = phone;
	}

	public ContactPojo(String id, String name, String phone) {
		this.id = id;
		this.name = name;
		this.phone = phone;
	}

	@Override
	public String toString() {
		return name;
	}
}
