/* This class implements the server side
 * of the TFTP protocol which creates a Thread
 * which accepts a ReadPacket that handles all 
 * of the data inside of the packet*/

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;

public class TFTPServer 
{
    protected static DatagramSocket dgSocket;
    protected static InetAddress host;
    protected static boolean slidingWindow = true;
    protected static boolean useDrop = false;
    protected static String version = "IPv4";
    
    /*Creates a UDP Socket to listen on the specified port
     *for a packet and once it is recieved it passed it to 
     *to the TransferHandler to extract what the contents of 
     *the packet are.*/
    public static void main(String [] args)
    {
        if(args.length == 3)
        {
            version = args[0];
            if(args[1].contains("y"))
                slidingWindow = true;
            if(args[2].contains("y"))
                useDrop = true;
        }
        try
        {
            if(version.equalsIgnoreCase("ipv6"))
                host = InetAddress.getByName("fe80::214:4fff:fee8:adb4");
            else
                host = InetAddress.getByName("129.3.154.134");
            
            dgSocket = new DatagramSocket(TFTPPacket.PORT);
            
            System.out.println("Server Ready.  Port:  "+ dgSocket.getLocalPort());
            System.out.println("Waiting for client to connect...");
            do
            {
                TFTPPacket packet = TFTPPacket.receive(host, dgSocket);
                if(packet != null)
                {
                    System.out.println("Packet recieved from Host: " + packet.getHost());
                    System.out.println("Packet recieved from Port: " + packet.getPort());
                    if(packet instanceof ReadPacket)
                    {
                        System.out.println("Request from Host: " + packet.getHost());
                        TransferHandler t = new TransferHandler((ReadPacket)packet);
                    }
                }
                else
                    break;
            }while(true);
        }
        catch(IOException ex)
        {
            dgSocket.close();
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
