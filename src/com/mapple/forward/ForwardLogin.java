package com.mapple.forward;

import com.mapple.forward.ForwardCmd;
import com.mapple.forward.ForwardMessage;
import com.mapple.forward.ForwardVersion;

public class ForwardLogin implements ForwardMessage {

	private String userName = null;
	
	public ForwardLogin(String userName) {
		this.userName = userName;
	}
	
	@Override
	public ForwardVersion version() {
		return ForwardVersion.FORWARD1;
	}

	@Override
	public ForwardCmd cmd() {
		return ForwardCmd.LOGIN;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

}
