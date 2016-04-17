package com.mapple.socksforward;

public class ForwardLoginRequest implements ForwardMessage {

	private String userName = null;
	
	public ForwardLoginRequest(String userName) {
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
