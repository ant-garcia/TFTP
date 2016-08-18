/* This class implements the Handler for
 * the TFTPServer class of the TFTP protocol which 
 * breaks down the specfific packet that is received from
 * the UDP Socket and decivers the information inside of it*/

import java.net.URL;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.io.BufferedInputStream;

/*The constructor for the TransferHandler clase
 * that retrieves the port, url, host address
 * and whether to use the sliding window formula
 * with the drop simulation.*/
public class TransferHandler extends Thread
{
    private static boolean slidingWindow;
    private static boolean useDrop;
    protected static int port;
    protected static int retries;
    protected static InetAddress host;
    protected static BufferedInputStream bin;
    protected URL url;
    protected static DatagramSocket dgs;
    protected final ReadPacket pack;
    protected static ArrayList<byte[]> dataPacks;
    
    public TransferHandler(ReadPacket rp)
    {
        this.pack = rp;
        try
        {
            dgs = new DatagramSocket();
            dgs.setSoTimeout(10000);
            host = rp.getHost();
            port = rp.getPort();
            url = new URL(rp.getUrl());
            slidingWindow = TFTPServer.slidingWindow;
            useDrop = TFTPServer.useDrop;
            this.start();
            
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
    }
    
    /* This run method starts a BufferedInputStream that reads in the information
     * from the specified url that was stored in the ReadPacket and creates
     * a ByteBuffer to read in the information stored in the url and places it into
     * the dataPacks ArrayList to be written to a file specified by the client*/
    public void run()
    {
        
        try
        {
            bin = new BufferedInputStream(this.url.openConnection().getInputStream());
            dataPacks = new ArrayList();
            boolean isDone = false;
            int packetCount = 0;
            /*keep reading in one byte at a time from the url supplied
             * until the end of the file is reached.*/
            while(!isDone)
            {
                packetCount++;
                ByteBuffer buf = ByteBuffer.allocate(TFTPPacket.MAX_DATA_SIZE);
                while(buf.hasRemaining())
                {
                    byte b[] = new byte[1];
                    int result = bin.read(b);
                    if(result == -1)
                    {
                        isDone = true;
                        break;
                    }
                    else
                        buf.put(b);
                }
                
                if(((buf.capacity() - buf.remaining()) > 0) && ((buf.capacity() - buf.remaining()) == TFTPPacket.MAX_DATA_SIZE))
                {
                    byte tmp[] = new byte[TFTPPacket.MAX_DATA_SIZE];
                    buf.rewind();
                    buf.get(tmp);
                    System.out.println("Added full packet of size: " + tmp.length);
                    dataPacks.add(tmp);
                }
                else if(((buf.capacity() - buf.remaining()) > 0) && ((buf.capacity() - buf.remaining()) < TFTPPacket.MAX_DATA_SIZE))
                {
                    byte tmp[] = new byte[buf.capacity() - buf.remaining()];
                    buf.rewind();
                    buf.get(tmp);
                    System.out.println("Added packet of size: " + tmp.length);
                    dataPacks.add(tmp);
                }
            }
            
            int clientPort = port;
            InetAddress clientAddress = host;
            
            /*Uses a sliding window algorithm that that uses a window
             *of size four to send the DataPackets back to the client
             *that are located in the specified windowIndex,and then waits
             *for an acknoledgement from the client to increment to the next
             *window.*/
            if(slidingWindow)
            {
                System.out.println("Number of DataPackets: " + dataPacks.size());
                retries = 0;
                
                int windowIndex = 0;
                int windowSize = 4;
                int lastPacketBlockNum = -1;
                boolean isLastWindow = false;
                
                while(windowIndex < dataPacks.size())
                {
                    for(int i = 0;i < windowSize;i++)
                    {
                        if(((windowIndex * windowSize) + i) >= dataPacks.size())
                        {
                            if(lastPacketBlockNum == -1)
                            {
                                System.out.println("Sending last Packet");
                                isLastWindow = true;
                                lastPacketBlockNum = i - 1;
                            }   
                            break;
                        }
                        
                        DataPacket dp = new DataPacket(i, dataPacks.get((windowIndex * windowSize) + i));
                        System.out.println("Sending to Port: " + port);
                        dp.send(clientAddress, dgs, clientPort, useDrop);
                        System.out.println("Sending Packet No.: " + i);
                    }
                    
                    if(((windowIndex * windowSize) >= dataPacks.size()) && (lastPacketBlockNum == -1))
                    {
                        isLastWindow = true;
                        lastPacketBlockNum = windowSize -1;
                    }
                    try
                    {
                        System.out.println("Waiting for an Ack");
                        TFTPPacket thisPacket = TFTPPacket.receive(clientAddress, dgs);

                        if(thisPacket instanceof AckPacket)
                        {
                            AckPacket ack = (AckPacket)thisPacket;
                            System.out.println("Recieved Ack Packet: " + ack.getBlockNum());
                            int blockNum = ack.getBlockNum();
                            if((blockNum == (windowSize - 1)) || (isLastWindow && (lastPacketBlockNum == blockNum)))
                            {
                                windowIndex++;
                                if(isLastWindow && (lastPacketBlockNum == blockNum))
                                    break;
                            }
                        }
                    }
                    catch(SocketTimeoutException ex)
                    {
                        System.out.println("No more Acks");
                        System.exit(0);
                    }
                                
                }
            }
            /*Sends a DataPacket from the DataPacks ArrayList to the client
             *and then waits for an AckPacket from the client to confirm that
             *said packet has been received.*/
            else
            {
                retries = 0;

                for(int blockNum = 0; blockNum < dataPacks.size(); blockNum++)
                {
                    try
                    {
                        DataPacket outPack = new DataPacket(blockNum, dataPacks.get(blockNum));

                        System.out.println("Sending packet of block number: " + blockNum);
                        outPack.send(clientAddress, dgs, clientPort, useDrop);

                        TFTPPacket thisPacket = TFTPPacket.receive(clientAddress, dgs);
                        if(thisPacket instanceof AckPacket)
                        {
                            AckPacket ack = (AckPacket)thisPacket;
                            blockNum = ack.getBlockNum();
                            retries = 0;
                        }
                        else
                        {
                            System.out.println("Did not recieve a Ack Packet");
                            retries++;
                            if(retries >= 5)
                                break;
                        }
                    }
                    catch(SocketTimeoutException ex)
                    {
                        System.out.println("No more Packets");
                        break;
                    }
                }
                System.out.println("Finished sending data");
                dgs.close();
                System.exit(0);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
