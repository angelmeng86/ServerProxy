import com.stfl.misc.Config;
import com.stfl.network.LocalServer;
import com.stfl.network.NioLocalServer;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;



public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
/*        Config config = new Config();
        LocalServer server = null;
        try {
            server = new LocalServer(config);
            Thread td = new Thread(server);
            td.start();
            td.join();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        
        int bind = 1080;
        SocksServer s = new SocksServer();
        System.out.println("SocksServer Listening " + bind + " ...");
        s.start("0.0.0.0", bind);
        s.join();
        System.out.println("Stopped.");
        
    }

}
