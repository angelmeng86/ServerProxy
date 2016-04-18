package com.mapple.forward;

public class ForwardBeat implements ForwardMessage {

	@Override
	public ForwardVersion version() {
		return ForwardVersion.FORWARD1;
	}

	@Override
	public ForwardCmd cmd() {
		return ForwardCmd.BEAT;
	}

}
