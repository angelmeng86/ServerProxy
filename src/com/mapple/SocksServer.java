package com.mapple;

import socks.Socks5Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SocksServer {
	private Thread serverThread;
	private ServerSocket localSock;
	private boolean running = false;
	
	static int iddleTimeout = 180000; //3 minutes
	
//	private ConcurrentHashMap<SocketChannel, SocketChannel> routeList = new ConcurrentHashMap<SocketChannel, SocketChannel>();
    
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
	    
	    public boolean handle(SelectionKey key) {
	        SocketChannel channel = (SocketChannel)key.channel();
	        channel.read(buffer);
            if(buffer.hasRemaining()) {
               return true; 
            }
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
                  inform("socks version " + version);
                  if (version != 5) {
                      inform("Unknow protocol version.");
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
                      inform("Unknow protocol version.");
                      return false;
                  }
	                if (buffer.get(1) != 1) { 
	                    inform("Command not supported. " + buffer.get(1));
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
	                    inform("Address type not supported. " + buffer.get(3));
	                    return false;
	                }
	            }
	                break;
	            case STATE_RECV_IP:
                {
                    byte[] addr = new byte[4];
                    buffer.position(4);
                    buffer.get(addr);
                    InetAddress ip = bytes2IP(addr);
                    host = ip.getHostName();
                    
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
	    
	    private void connectServer() {
	        state = STATE_CONNECTING;
	        
	        SocketChannel remoteChannel = SocketChannel.open();
	        remoteChannel.configureBlocking(false);
	        remoteChannel.register(selector, SelectionKey.OP_CONNECT, this);
	        remoteChannel.connect(new InetSocketAddress(host, port));
	    }
	}
	
	static void inform(String s){
	      System.out.print(new Date() + ": ");
	      System.out.println(s);
	}
	
	public static void main(String[] args){
	    
	    int bind = 1080;
        SocksServer s = new SocksServer();
        System.out.println("SocksServer Listening " + bind + " ...");
        s.start("0.0.0.0", bind);
        s.join();
        System.out.println("Stopped.");
	    /*
		if (args.length != 4) {
			System.out.println("java SocksServer <localAddress> <localPort>");
			return;
		}
		String addr = args[0];
		int bind = Integer.parseInt(args[1]);
		
		SocksServer s = new SocksServer();
        System.out.println("Listening " + bind + " ...");
		s.start(addr, bind);
		s.join();
        System.out.println("Stopped.");
        */
	}
	
	
	public boolean start(int port) {
		return start("127.0.0.1", port);
	}
	
    public void syncStart(String address, int port) {
        running = true;
        ServerSocketChannel channel;
        try {
            selector = Selector.open();
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            localSock = channel.socket();
            localSock.bind(new InetSocketAddress(address, port));
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
        clientChannel.register(selector, SelectionKey.OP_READ, new Sock5Message());
        Socket sock = clientChannel.socket();
        inform("Accept client " + sock.getInetAddress().getHostName() + ":" + sock.getPort());
    }
    
    private void channelConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel)key.channel();
        channel.finishConnect();
        if(channel.isConnected()) {
            
        }
        else {
            Sock5Message msg = (Sock5Message)key.attachment();
        }
    }
    
    private void channelRead(SelectionKey key) {
        SocketChannel originCh = (SocketChannel)key.channel();
        Object obj = key.attachment();
        
        if(obj instanceof Sock5Message) {
           if(!((Sock5Message)obj).handle(key)) {
               key.cancel();
               try {
                   originCh.close();
               } catch (IOException e1) {
                   e1.printStackTrace();
               }
           }
           return;
        }
        
        if(obj instanceof SocketChannel) {
            SocketChannel targetCh = (SocketChannel)obj;
            try {
                readBuffer.clear();
                int read = originCh.read(readBuffer);
                if (read == 0) {

                }
                else if (read == -1) {
                    key.cancel();
                    try {
                        originCh.close();
                        targetCh.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                else {
                    readBuffer.flip();
                    targetCh.write(readBuffer);
                }

            } catch (IOException e) {
                key.cancel();
                try {
                    originCh.close();
                    targetCh.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return;
        }
        
        key.cancel();
        try {
            originCh.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

	public boolean start(String address, int port) {
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
	}
    
    public void join() {
        try {
            serverThread.join();
        } catch (InterruptedException e) { }
    }
	
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
	
	class ListenSocket implements Runnable {
	    
		public void run() {
			try {
				ServerSocketChannel serverChannel = localSock.getChannel();
				while (running) {
					SocketChannel localChannel = serverChannel.accept();
					SocksWorker worker = new SocksWorker(localChannel);
					Thread td = new Thread(worker);
					td.start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			running = false;
		}
	}
	
	class SocksWorker implements Runnable {
        SocketChannel localChannel;
        
      //Class methods
        InetAddress bytes2IP(byte[] addr){
           String s = bytes2IPV4(addr,0);
           try{
              return InetAddress.getByName(s);
           }catch(UnknownHostException uh_ex){
             return null;
           }
        }
        final String bytes2IPV4(byte[] addr,int offset){
            String hostName = ""+(addr[offset] & 0xFF);
            for(int i = offset+1;i<offset+4;++i)
              hostName+="."+(addr[i] & 0xFF);
            return hostName;
         }
        
        public void run() { 
            try {
                Socket sock = localChannel.socket();
                inform("Accept client " + sock.getInetAddress().getHostName() + ":" + sock.getPort());
                DataInputStream localIn = new DataInputStream(sock.getInputStream());
                OutputStream localOut = sock.getOutputStream();
            
//          Version identifier/method selection message:
//             +----+----------+----------+
//             |VER | NMETHODS | METHODS  |
//             +----+----------+----------+
//             | 1  |    1     | 1 to 255 |
//             +----+----------+----------+
//          Will be ignored directly.
                int version = localIn.read();
//                inform("socks version " + version);
                if (version != 5) {
                    inform("Unknow protocol version.");
                    sock.close();
                    return;
                }
                localIn.readFully(new byte[localIn.read() | 0]); // ignore methods
                byte[] ver = {5, 0};
                localOut.write(ver); // send version(5)/method(0)
            
//          Request:
//              +----+-----+-------+------+----------+----------+
//              |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
//              +----+-----+-------+------+----------+----------+
//              | 1  |  1  | X'00' |  1   | Variable |    2     |
//              +----+-----+-------+------+----------+----------+
//            o  CMD
//               o  CONNECT X'01'
//            o  ATYP   address type of following address
//               o  IP V4 address: X'01'
//               o  DOMAINNAME: X'03'
//                      The first octet of the address field contains 
//                      the number of octets of name that follow,
//                      there is no terminating NUL octet.
            
                byte[] req = new byte[4];
                localIn.readFully(req); // load VER, CMD, RSV
                if (req[1] != 1) { 
                    inform("Command not supported. " + req[1]);
                    /*
                    byte[] reply = {5, 7, 0, 1 ,0, 0, 0, 0, 1, 1};
                    localOut.write(reply); // Command not supported
                    */
                    sock.close();
                    
                    return;
                }
                byte addrType = req[3];
                String host = null;
                int port;
                if (addrType == 1) { // IP address
                    byte[] addr = new byte[4];
                    localIn.readFully(addr);
                    InetAddress ip = bytes2IP(addr);
                    host = ip.getHostName();
                } else if(addrType == 3) { // Domain name
                    int addrLen = localIn.read();
                    byte[] addr = new byte[addrLen];
                    localIn.readFully(addr);
                    
                    host = new String(addr);
                    /*
                    String domainName = new String(addr);
                    System.out.println("domainName " + domainName);
                    try {
                        InetAddress ip = InetAddress.getByName(domainName);
                        host = ip.getHostAddress();
                    } catch (UnknownHostException e) {
                    }
                    */
                } else {
                    inform("Address type not supported.");
                    /*
                    byte[] reply = {5, 8, 0, 1 ,0, 0, 0, 0, 1, 1};
                    localOut.write(reply); // Address type not supported
                    */
                    sock.close();
                    return;
                }
                int ch1 = localIn.read();
                int ch2 = localIn.read();
                port = (ch1 << 8) + (ch2 << 0);
            
//          Replies:
//          +----+-----+-------+------+----------+----------+
//          |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
//          +----+-----+-------+------+----------+----------+
//          | 1  |  1  | X'00' |  1   | Variable |    2     |
//          +----+-----+-------+------+----------+----------+
//        o  REP    Reply field:
//            o  X'00' succeeded
//            o  X'01' general SOCKS server failure
//            o  X'02' connection not allowed by ruleset
//            o  X'03' Network unreachable
//            o  X'04' Host unreachable
//            o  X'05' Connection refused
//            o  X'06' TTL expired
//            o  X'07' Command not supported
//            o  X'08' Address type not supported
//            o  X'09' to X'FF' unassigned
            
                SocketChannel remoteChannel;
                try {
//                    inform("Connecting " + host + ":" + port);
                    remoteChannel = SocketChannel.open(new InetSocketAddress(host,  
                            port));
                } catch (Exception e) {
                    inform("Connect failure " + host + ":" + port);
                    byte[] reply = {5, 1, 0, 1 ,0, 0, 0, 0, 1, 1};
                    localOut.write(reply); // general SOCKS server failure
                    sock.close();
                    return;
                }
                
                byte[] reply = {5, 0, 0, 1 ,0, 0, 0, 0, 1, 1};
                localOut.write(reply);
                String info = "Client " + sock.getInetAddress().getHostName() + ":" + sock.getPort() + " Connected " + host + ":" + port;
                inform("Client " + sock.getInetAddress().getHostName() + ":" + sock.getPort() + " Connected " + host + ":" + port);
                
                Router router = new Router(localChannel, remoteChannel, info);
                Thread routerThread = new Thread(router);
                routerThread.start();
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        SocksWorker(SocketChannel local) throws IOException {
            localChannel = local;
        }
    }
	
	class Router implements Runnable {
	    
		public Selector selector;		
		SocketChannel localCh;
		SocketChannel remoteCh;
		String name;
		int sendLen = 0;
		int recvLen = 0;
		
		public void run() {	
			ByteBuffer buffer = ByteBuffer.allocate(1024);
				try {
					while (running) {
						int nKey = selector.select();
						if (nKey <= 0) {
						    timeOut();
						    return;
						}
						for(SelectionKey key : selector.selectedKeys()) {
							SocketChannel recvCh = (SocketChannel) key.channel();
							buffer.clear();
							int read = recvCh.read(buffer);
							if (read == 0) {
							    continue;
							}
							else if (read == -1) {
							    inform(name + " close." + " send[" + sendLen + "] recv[" + recvLen + "]");
								recvCh.close();
								if (recvCh == localCh) 
								    remoteCh.close();
								else 
								    localCh.close();
								selector.close();
								return;
							}
							if (recvCh == localCh) {
							    sendLen += read;
								buffer.flip();
								remoteCh.write(buffer);
							} else {
							    recvLen += read;
								buffer.flip();
								localCh.write(buffer);
							}
						}
						selector.selectedKeys().clear();
					}
				} catch (IOException e) {
						try {
							selector.close();
							if (localCh.isConnected()) localCh.close();
							if (remoteCh.isConnected()) remoteCh.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						e.printStackTrace();
				}
		}
		
		private void timeOut() {
		    inform(name + " selector timeout.");
		    try {
                selector.close();
                if (localCh.isConnected()) localCh.close();
                if (remoteCh.isConnected()) remoteCh.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
		}
		
		Router(SocketChannel local, SocketChannel remote, String id) throws IOException {
			selector = Selector.open();
			localCh = local;
			remoteCh = remote;
			name = id;
			localCh.configureBlocking(false);
			remoteCh.configureBlocking(false);
			localCh.register(selector, SelectionKey.OP_READ);
			remoteCh.register(selector, SelectionKey.OP_READ);
		}
	}
}