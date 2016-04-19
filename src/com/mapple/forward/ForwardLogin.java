package com.mapple.forward;

import com.mapple.forward.ForwardCmd;
import com.mapple.forward.ForwardMessage;
import com.mapple.forward.ForwardVersion;

import io.netty.channel.Channel;

public class ForwardLogin implements ForwardMessage {

	private final String userName;
	private String remoteAddr;
    private int remotePort;
    private Channel remoteChannel;
	
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

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public Channel getRemoteChannel() {
        return remoteChannel;
    }

    public void setRemoteChannel(Channel remoteChannel) {
        this.remoteChannel = remoteChannel;
    }

}
