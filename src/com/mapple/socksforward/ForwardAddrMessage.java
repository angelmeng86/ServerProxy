package com.mapple.socksforward;

public class ForwardAddrMessage implements ForwardMessage {

	@Override
	public ForwardVersion version() {
		return ForwardVersion.FORWARD1;
	}

	@Override
	public ForwardCmd cmd() {
		return ForwardCmd.CONNECTACK;
	}

}
