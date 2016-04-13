/*  Shadowsocks-java - A java port of shadowsocks.
 *  Copyright (C) 2013 @xierch
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.DataInputStream;
import java.io.EOFException;
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
import java.util.concurrent.ConcurrentHashMap;

public class SocksServer {
	private Thread serverThread;
	private ServerSocket localSock;
	private boolean running = false;
	
	static int iddleTimeout    = 180000; //3 minutes
	
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
	
	class MainRouter implements Runnable {
        
        public Selector selector;       
        SocketChannel localCh;
        SocketChannel remoteCh;
        
        public void run() { 
            ByteBuffer buffer = ByteBuffer.allocate(1024);
                try {
                    while (running) {
                        int nKey = selector.select(iddleTimeout);
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
                                recvCh.close();
                                if (recvCh == localCh) 
                                    remoteCh.close();
                                else 
                                    localCh.close();
                                selector.close();
                                return;
                            }
                            if (recvCh == localCh) {
                                buffer.flip();
                                remoteCh.write(buffer);
                            } else {
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
                }
        }
        
        private void timeOut() {
            try {
                selector.close();
                if (localCh.isConnected()) localCh.close();
                if (remoteCh.isConnected()) remoteCh.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        
        MainRouter(SocketChannel local, SocketChannel remote) throws IOException {
            selector = Selector.open();
            localCh = local;
            remoteCh = remote;
            localCh.configureBlocking(false);
            remoteCh.configureBlocking(false);
            localCh.register(selector, SelectionKey.OP_READ);
            remoteCh.register(selector, SelectionKey.OP_READ);
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