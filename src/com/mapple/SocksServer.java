package com.mapple;

import java.io.IOException;
import java.lang.Thread;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class SocksServer {
	private ServerSocket localSock;
	private boolean running = false;
	
	static int iddleTimeout = 180000; //3 minutes
    
	private Selector selector;  
	private ByteBuffer readBuffer = ByteBuffer.allocate(2048);
	
	class Sock5Message {
	    
	    public static final int STATE_NONE             =0;
	    public static final int STATE_RECV_METHODS     =1;
	    public static final int STATE_SEND_METHOD      =2;
	    public static final int STATE_RECV_IP          =3;
	    public static final int STATE_RECV_DOMAIN      =4;
	    public static final int STATE_CONNECTING       =5;
	    
	    /** Host as an IP address */
	    public InetAddress ip=null;
	    /** SOCKS version, or version of the response for SOCKS4*/
	    public int version;
	    /** Port field of the request/response*/
	    public int port;
	    /** Request/response code as an int*/
	    public int command;
	    /** Host as string.*/
	    public String host=null;
	    /** User field for SOCKS4 request messages*/
	    public String user=null;
	    
	    public int state = STATE_NONE;
	    
	    public SocketChannel sockChannel = null;
	    
	    private ByteBuffer buffer = ByteBuffer.allocate(256);
	    
	    Sock5Message() {
	        buffer.limit(2);
	    }
	    
        @Override
        public String toString() {
            return "Sock5Message["+ host + ":" + port + "]";
        }

        InetAddress bytes2IP(byte[] addr) {
            String s = bytes2IPV4(addr, 0);
            try {
                return InetAddress.getByName(s);
            } catch (UnknownHostException uh_ex) {
                return null;
            }
        }

        final String bytes2IPV4(byte[] addr, int offset) {
            String hostName = "" + (addr[offset] & 0xFF);
            for (int i = offset + 1; i < offset + 4; ++i)
                hostName += "." + (addr[i] & 0xFF);
            return hostName;
        }
        
        public boolean handle(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel)key.channel();
            int len = channel.read(buffer);
            do {
                if(len == -1) {
                    return false;
                }
                if(!handleEvent(key)) {
                    return false;
                }
                len = channel.read(buffer);
            } while(len > 0);
//            logdebug(" Handle Zero return");
            return true;
        }
	    
	    private boolean handleEvent(SelectionKey key) throws IOException {
	        SocketChannel channel = (SocketChannel)key.channel();
            if(buffer.hasRemaining()) {
               return true; 
            }
            logdebug(" Handle socks5 " + state);
	        switch(state) {
	            case STATE_NONE:
	            {
//	              Version identifier/method selection message:
//	              +----+----------+----------+
//	              |VER | NMETHODS | METHODS  |
//	              +----+----------+----------+
//	              | 1  |    1     | 1 to 255 |
//	              +----+----------+----------+
//	           Will be ignored directly.
                  int version = buffer.get(0);
                  loginfo("socks version " + version);
                  if (version != 5) {
                	  loginfo("Unknow protocol version.");
                      return false;
                  }
                  int nMethods = buffer.get(1);
                  buffer.clear();
                  if(nMethods > 0) {
                      buffer.limit(nMethods);
                      state = STATE_RECV_METHODS;
                  }
                  else {
                      byte[] ver = {5, 0};
                      channel.write(ByteBuffer.wrap(ver)); 
                      buffer.limit(5);
                      state = STATE_SEND_METHOD;
                  }
	            }
	                break;
	            case STATE_RECV_METHODS:
	            {
	                /*StringBuilder info = new StringBuilder("methods[" + buffer.limit() + "] ");
	                for(int i = 0; i < buffer.limit(); i++) {
	                    info.append( buffer.get(i) + " ");
	                }
	                logdebug(info.toString());*/
	                
	                buffer.clear();
	                buffer.limit(5);
	                byte[] ver = {5, 0};
                    channel.write(ByteBuffer.wrap(ver)); 
	                state = STATE_SEND_METHOD;
	            }
	                break;
	            case STATE_SEND_METHOD:
	            {
//	              Request:
//	              +----+-----+-------+------+----------+----------+
//	              |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
//	              +----+-----+-------+------+----------+----------+
//	              | 1  |  1  | X'00' |  1   | Variable |    2     |
//	              +----+-----+-------+------+----------+----------+
//	            o  CMD
//	               o  CONNECT X'01'
//	            o  ATYP   address type of following address
//	               o  IP V4 address: X'01'
//	               o  DOMAINNAME: X'03'
//	                      The first octet of the address field contains 
//	                      the number of octets of name that follow,
//	                      there is no terminating NUL octet.
                  if (buffer.get(0) != 5) {
                	  loginfo("Unknow protocol version.");
                      return false;
                  }
	                if (buffer.get(1) != 1) { 
	                	loginfo("Command not supported. " + buffer.get(1));
	                    return false;
	                }
	                if(buffer.get(3) == 1) {
	                    buffer.limit(10);
	                    state = STATE_RECV_IP;
	                }
	                else if(buffer.get(3) == 3) {
                        buffer.limit(5 + (buffer.get(4) & 0xFF) + 2);
                        state = STATE_RECV_DOMAIN;
                    }
	                else {
	                	loginfo("Address type not supported. " + buffer.get(3));
	                    return false;
	                }
	            }
	                break;
	            case STATE_RECV_IP:
                {
                    byte[] addr = new byte[4];
                    buffer.position(4);
                    buffer.get(addr);
                    host = bytes2IPV4(addr, 0);
                    /*
                    InetAddress ip = bytes2IP(addr);
                    host = ip.getHostName();
                    */
                    
                    int ch1 = (buffer.get() & 0xFF);
                    int ch2 = (buffer.get() & 0xFF);
                    port = (ch1 << 8) + (ch2 << 0);
                    
                    sockChannel = channel;
                    key.cancel();
                    connectServer();
                }
                    break;
	            case STATE_RECV_DOMAIN:
                {
                    byte[] addr = new byte[buffer.get(4) & 0xFF];
                    buffer.position(5);
                    buffer.get(addr);
                    host = new String(addr);
                    
                    int ch1 = (buffer.get() & 0xFF);
                    int ch2 = (buffer.get() & 0xFF);
                    port = (ch1 << 8) + (ch2 << 0);
                    
                    sockChannel = channel;
                    key.cancel();
                    connectServer();
                }
                    break;
	        }
	        return true;
	    }
	    
	    private void connectServer() throws IOException {
	        state = STATE_CONNECTING;
	        logdebug("connectServer " + host + ":" + port);
	        SocketChannel remoteChannel = SocketChannel.open();
	        remoteChannel.configureBlocking(false);
	        
/*	        remoteChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
	        remoteChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);*/
	        remoteChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
	        remoteChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
	        remoteChannel.connect(new InetSocketAddress(host, port));
	        remoteChannel.register(selector, SelectionKey.OP_CONNECT, this);
	    }
	}
	
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	static void loginfo(String s){
	      System.out.print("INFO " + formatter.format(new Date()) + ": ");
	      System.out.println(s);
	}
	
	static void logdebug(String s){
//	      System.out.print("DEBUG " + formatter.format(new Date()) + ": ");
//	      System.out.println(s);
	}
	
	public static void main(String[] args){
	    
	    int bind = 1080;
        SocksServer s = new SocksServer();
        System.out.println("SocksServer Listening " + bind + " ...");
        s.syncStart("0.0.0.0", bind);
        System.out.println("Stopped.");
	}
	
	
/*	public boolean start(int port) {
		return start("127.0.0.1", port);
	}*/
	
    public void syncStart(String address, int port) {
        running = true;
        ServerSocketChannel channel;
        try {
            selector = Selector.open();
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(Inet4Address.getByName(address), port), 50);
            channel.register(selector, SelectionKey.OP_ACCEPT);
            while (running) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        channelConnect(key);
                    }
                    else if (key.isAcceptable()) {
                        channelAccept(key);
                    } else if (key.isReadable()) {
                        channelRead(key);
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void channelAccept(SelectionKey key) throws IOException {
        ServerSocketChannel acceptCh = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = acceptCh.accept();
        clientChannel.configureBlocking(false);
//        clientChannel.setOption(StandardSocketOptions.SO_LINGER, 1);
//        clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        clientChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        clientChannel.register(selector, SelectionKey.OP_READ, new Sock5Message());
        Socket sock = clientChannel.socket();
        loginfo("Accept client " + sock.getInetAddress().getHostAddress() + ":" + sock.getPort());
    }
    
    private void channelConnect(SelectionKey key) {
        SocketChannel channel = (SocketChannel)key.channel();
        Sock5Message msg = (Sock5Message)key.attachment();
        try {
            if (channel.finishConnect()) {
                byte[] reply = {
                        5, 0, 0, 1, 0, 0, 0, 0, 1, 1
                };
                msg.sockChannel.write(ByteBuffer.wrap(reply));

                loginfo("Connected " + msg.host + ":" + msg.port);
                msg.sockChannel.register(selector, SelectionKey.OP_READ, channel);
                
                key.interestOps(SelectionKey.OP_READ);
                key.attach(msg.sockChannel);
                msg.sockChannel = null;
            }
            else {
                key.cancel();
                loginfo("Connect failure " + msg.host + ":" + msg.port);
                if (msg.sockChannel != null) {
                    try {
                        msg.sockChannel.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            key.cancel();
            loginfo("Connect failure " + msg.host + ":" + msg.port);
            if (msg.sockChannel != null) {
                try {
                    msg.sockChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
    
    private void channelRead(SelectionKey key) {
        SocketChannel originCh = (SocketChannel)key.channel();
        Object obj = key.attachment();
        
        if(obj instanceof Sock5Message) {
			try {
				if (!((Sock5Message) obj).handle(key)) {
				    logdebug("1 Close " + obj);
					key.cancel();
					try { originCh.close();} catch (IOException e1) {e1.printStackTrace();}
				}
			} catch (IOException e) {
			    logdebug("2 Close " + obj);
				key.cancel();
				try { originCh.close();} catch (IOException e1) {e1.printStackTrace();}
				e.printStackTrace();
			}
           return;
        }
        
        if(obj instanceof SocketChannel) {
            SocketChannel targetCh = (SocketChannel)obj;
            try {
                readBuffer.clear();
                int read = originCh.read(readBuffer);
//                loginfo(originCh + " read " + read);
                if (read == 0) {

                }
                else if (read == -1) {
                    logdebug("3 Close " + originCh + " and " + targetCh);
                    key.cancel();
                    try { originCh.close();} catch (IOException e1) {e1.printStackTrace();}
                    try { targetCh.close();} catch (IOException e1) {e1.printStackTrace();}
                }
                else {
                    readBuffer.flip();
                    targetCh.write(readBuffer);
                }

            } catch (IOException e) {
                logdebug("4 Close " + originCh + " and " + targetCh);
                key.cancel();
                try { originCh.close();} catch (IOException e1) {e1.printStackTrace();}
                try { targetCh.close();} catch (IOException e1) {e1.printStackTrace();}
                e.printStackTrace();
            }
            return;
        }
        
        logdebug("5 Close " + originCh);
        key.cancel();
        try { originCh.close();} catch (IOException e1) {e1.printStackTrace();}
    }

	/*public boolean start(String address, int port) {
		if (running) return true;
		ServerSocketChannel channel;
		ListenSocket server;
		try {
			channel = ServerSocketChannel.open();
			localSock = channel.socket();
			localSock.bind(new InetSocketAddress(address, port));
			server = new ListenSocket();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		serverThread = new Thread(server);
		running = true;
		serverThread.start();
		return true;
	}*/
	
	public void stop() {
		if (running) return;
		running = false;
		try {
			localSock.close();
		} catch (IOException  e) {
			e.printStackTrace();
		}
	}
	
	public boolean isRunning() {
		return running;
	}
}