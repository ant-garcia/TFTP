/* This class implements the Protocol for what a
 * TFTP packet is made of.*/

import java.util.Random;
import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;


public class TFTPPacket 
{
    protected static final int PORT = 2709;
    protected static final int REMOTE_PORT = 9072;
    public static final int MAX_PACKET_SIZE = 516;
    public static final int MAX_DATA_SIZE = 512;
    
    protected static final short RRQ = 1;
    protected static final short WRQ = 2;
    protected static final short DATA = 3;
    protected static final short ACK = 4;
    protected static final short ERROR = 5;
    
    protected static final int opOFFSET = 0;
    protected static final int fileOFFSET = 2;
    protected static final int blockOFFSET = 2;
    protected static final int dataOFFSET = 4;
    protected static final int numOFFSET = 2;
    protected static final int msgOFFSET = 4;
    
    protected int length;
    protected int port;
    protected int type;
    protected byte message[];
    protected InetAddress host;
    
    public TFTPPacket()
    {
        message = new byte[MAX_PACKET_SIZE];
        length = MAX_PACKET_SIZE;
    }
    /*Sets the length, message block, port, and the type of said 
     *specific packet*/
    private void setPacket(int length, byte[] message, InetAddress host, int port, int type)
    {
        this.length = length;
        this.message = message;
        this.host = host;
        this.port = port;
        this.type = type;
    }
    
    /*Creates a default TFTP Packet, that is reassigned based on the type
     *that is found in the Datagram Packet recieved from the Datagram
     *Socket, and then set based on the data located inside of the Datagram
     *Packet.*/
    public static TFTPPacket receive(InetAddress thisHost, DatagramSocket ds)throws IOException
    {
        TFTPPacket in = new TFTPPacket(), retPack = null;
        DatagramPacket packet = new DatagramPacket(in.message,in.length, thisHost, PORT);
        
        ds.receive(packet);
        System.out.println("Packet recieved from Port: " + packet.getPort());
        byte data[] = packet.getData();
        switch(data[1])
        {
            case RRQ:
                retPack = new ReadPacket();
                break;
            case WRQ:
                retPack = new WritePacket();
                break;
            case DATA:
                retPack = new DataPacket();
                break;
            case ACK:
                retPack = new AckPacket();
                break;
            case ERROR:
                retPack = new ErrorPacket();
                break;
            default:
                System.out.println("Unspecified Packet Number.");
                break;
        }
        
        retPack.setPacket(packet.getLength(), packet.getData(), packet.getAddress(), packet.getPort(), data[1]);
        return retPack;
    }
    
    /*Sends a new Datagram Packet that has the same information
     *of the TFTP Packet to the specified host on a pre-determined
     *port. Note that this has a chance to fail if useDrop is active.*/
    public boolean send(InetAddress ip, DatagramSocket ds, boolean useDrop)throws IOException
    {
        int num = 26;
        int ranNum;
        
        if(useDrop)
        {
            ranNum = new Random().nextInt(100);
        }
        else
            ranNum = 1;
        if(num != ranNum)
        {
            ds.send(new DatagramPacket(message, length, ip, PORT));
            return true;
        }
        else
            System.out.println("Dropped a packet...you only have yourself to blame");
        return false;
    }
    
    /*Sends a new Datagram Packet that has the same information
     *of the TFTP Packet to the specified host on a user specified
     *port. Note that this has a chance to fail if useDrop is active.*/
    public boolean send(InetAddress ip, DatagramSocket ds, int thisPort, boolean useDrop)throws IOException
    {
        int num = 26;
        int ranNum;
        
        if(useDrop)
        {
            ranNum = new Random().nextInt(100);
        }
        else
            ranNum = 1;
        if(num != ranNum)
        {
            ds.send(new DatagramPacket(message, length, ip, thisPort));
            return true;
        }
        else
            System.out.println("Dropped a packet...you only have yourself to blame");
        return false;
    }
    
    /*returns the host of the Packet*/
    public InetAddress getHost()
    {
        return host;
    }
    
    /*returns the port of the Packet*/
    public int getPort()
    {
        return port;
    }
    
    /*returns the length of the Packet*/
    public int getLength()
    {
        return length;
    }
    
    /*Places the type number in bytes at the Operation Offset*/
    protected void put(int offset, short value)
    {
        message[offset++] = (byte)(value >>> 8);
        message[offset] = (byte)(value % 256);
    }
    
    /*Places the data value of the specified TFTP Packet at the
     *Operation Offset.*/
    protected void put(int offset, String value, byte remove)
    {
        char tmp[] = new String(message).toCharArray();
        value.getChars(0, value.length(), tmp, offset);
        message = new String(tmp).getBytes();
        message[offset + value.length()] = remove;
    }
    
    /*Returns the number located at the Operation Offset*/
    protected int get(int offset)
    {
        return ((message[offset] & 0xff) << 8) | (message[offset+1] & 0xff);
    }
    
    /*Returns the data value located at the Operation Offset*/
    protected String get(int offset, byte remove)
    {
        char tmp[] = new char[message.length];
        while(message[offset] != remove)
            tmp[offset] = (char)message[offset++];
        return new String(tmp);
    }
}
