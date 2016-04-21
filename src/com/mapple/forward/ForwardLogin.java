package com.mapple.forward;

import com.mapple.forward.ForwardCmd;
import com.mapple.forward.ForwardMessage;
import com.mapple.forward.ForwardVersion;

import net.sourceforge.pinyin4j.PinyinHelper;

public class ForwardLogin implements ForwardMessage {

	private final String userName;
	private String remoteAddr;
    private int remotePort;
    private String province;
    private String province2;
	private String city;
	private String carrier;
    
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

    public String getProvince() {
        return province;
    }
    
    public String getProvince2() {
        return province2;
    }

    public void setProvince(String province) {
        this.province = province;
        this.province2 = getPinYinHeadChar(province);
    }
    
    private static String getPinYinHeadChar(String str) {  
        StringBuilder convert = new StringBuilder();  
        for (int j = 0; j < str.length(); j++) {  
            char word = str.charAt(j);  
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);  
            if (pinyinArray != null) {  
                convert.append(pinyinArray[0].charAt(0));  
            } else {  
                convert.append(word);  
            }  
        }  
        return convert.toString();
    }

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCarrier() {
		return carrier;
	}

	public void setCarrier(String carrier) {
		this.carrier = carrier;
	}  

}
