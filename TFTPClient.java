/* This class implements the client side
 * of the TFTP protocol which  communicates
 * with a proxy server to display a file hosted 
 * at a specified url*/

import java.util.ArrayList;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class TFTPClient
{
    protected static boolean slidingWindow;
    protected static boolean useDrop;
    protected static String version;
    
    public static void main(String[] args) 
    {
        String host = "host";
        String url = "url";
        String fileName = "filename";
        String Input = "input.txt";
        int packetCount = 0;
        int nextBlockNum = 0;
        int dataSize = 0;
        int lastDataSize = 0;
        int blockNum = 0;
        long startTime;
        boolean terminated;
        slidingWindow = false;
        useDrop = false;
        version = "IPv4";
        InetAddress serverAddress = null;
        DatagramSocket ds = null;
        FileOutputStream fileOut = null;
        PrintWriter thisFile = null;
        
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
                serverAddress = InetAddress.getByName("");
            else
                serverAddress = InetAddress.getByName(host);
            
            terminated = true;
            ds = new DatagramSocket();
            ds.setSoTimeout(10000);
            fileOut = new FileOutputStream(fileName);
            ReadPacket rp = new ReadPacket(url,"octet");
            boolean wasDropped = rp.send(serverAddress, ds, useDrop);
            if(!wasDropped)
                rp.send(serverAddress, ds, useDrop);
            System.out.println("Packet sent to: " + serverAddress);
            System.out.println("Sent Packet to Port: " + ds.getLocalPort());
            startTime = System.nanoTime();
            
            /*Uses a sliding window algorithm that that uses a window
             *of size four to recieve the DataPackets from the server,
             *writes the data to a specified file, and then waits for 
             *an acknoledgement from the client to clear the ArrayList 
             *to start receiving another window of DataPacks until all are received.*/
            if(slidingWindow)
            {
                int windowSize = 4;
                ArrayList<DataPacket> window = new ArrayList();
                int lastDataLength = 0;
                
                do
                {
                    TFTPPacket thisPacket = TFTPPacket.receive(serverAddress, ds);
                    System.out.println("Recieved Packet from: " + thisPacket.getHost());
                    System.out.println("Recieved Packet from Port: " + thisPacket.getPort());
                    if(thisPacket instanceof ErrorPacket)
                    {
                        ErrorPacket ep = (ErrorPacket)thisPacket;
                        fileOut.close();
                        ds.close();
                        throw new Exception(ep.getMessage());
                    }
                    try
                    {
                        if(thisPacket instanceof DataPacket)
                        {
                            DataPacket dp = (DataPacket)thisPacket;
                            System.out.println("Recieved Packet No.: " + dp.getBlockNum());
                            blockNum = dp.getBlockNum();
                            lastDataLength = dp.length;
                            boolean added = false;

                            for(int i = 0;i < window.size();i++)
                            {
                                if(dp.getBlockNum() < window.get(i).getBlockNum())
                                {
                                    window.add(i, dp);
                                    added = true;
                                    break;
                                }
                                else if(window.get(i).getBlockNum() == dp.getBlockNum())
                                {
                                    added = true;
                                    break;
                                }
                            }
                            if(!added)
                                window.add(dp);
                            if((window.size() == windowSize) || (lastDataLength < TFTPPacket.MAX_DATA_SIZE))
                            {
                                if(blockNum == (window.size() - 1))
                                {
                                    for(int i = 0;i < window.size();i++)
                                    {
                                        window.get(i).write(fileOut);
                                        packetCount++;
                                        dataSize += window.get(i).getLength() + windowSize;
                                    }

                                    AckPacket ap = new AckPacket(blockNum);
                                    wasDropped = ap.send(dp.getHost(), ds, dp.getPort(), useDrop);
                                    if(!wasDropped)
                                        ap.send(dp.getHost(), ds, dp.getPort(), useDrop);
                                    if(lastDataLength < TFTPPacket.MAX_DATA_SIZE)
                                        terminated = true;

                                    window.clear();
                                }
                            }
                        }
                        else
                        System.out.println("Did not recieve a data packet");
                    }
                    catch(SocketTimeoutException ex)
                    {
                            System.out.println("No more packets");
                            terminated = true;
                    }
                }while(lastDataLength >= DataPacket.MAX_DATA_SIZE);
            }
            
            /*Receives DataPackets from the server, writes them to a specified file,
             *and sends an Ack Packet back to the server to confirm that said DataPacket
             *was received.*/
            else
            {
                do
                {
                    try
                    {
                        TFTPPacket thisPacket = TFTPPacket.receive(serverAddress, ds);
                        System.out.println("Recieved Packet from: " + thisPacket.getHost());
                        if(thisPacket instanceof ErrorPacket)
                        {
                            ErrorPacket ep = (ErrorPacket)thisPacket;
                            fileOut.close();
                            ds.close();
                            throw new Exception(ep.getMessage());
                        }
                        else if(thisPacket instanceof DataPacket)
                        {
                            DataPacket dp= (DataPacket)thisPacket;
                            if(nextBlockNum == dp.getBlockNum())
                            {
                                lastDataSize = dp.getLength();
                                dp.write(fileOut);
                                System.out.println("Size of Packet is: " + lastDataSize);
                                packetCount++;
                                nextBlockNum++;

                                dataSize += lastDataSize + 4;
                            }

                            blockNum = dp.getBlockNum();
                            AckPacket ap = new AckPacket(blockNum);
                            wasDropped = ap.send(dp.getHost(), ds, dp.getPort(), useDrop);
                            if(!wasDropped)
                                ap.send(dp.getHost(), ds, dp.getPort(), useDrop);
                            System.out.println("Sent an Ack Packet to: " + dp.getHost());
                        }
                        else
                        {
                            System.out.println("Unexpected Error");
                            fileOut.flush();
                            fileOut.close();
                            ds.close();
                            terminated = false;
                            break;
                        }
                    }
                    catch(SocketTimeoutException ex)
                    {
                            System.out.println("No more packets");
                            terminated = true;
                            break;
                    }
                }while(lastDataSize >= TFTPPacket.MAX_DATA_SIZE);
                    
            }
            if(terminated)
            {
                thisFile = new PrintWriter(new FileWriter(Input));
                long endTime = System.nanoTime();
                long totalTime = endTime - startTime;
                double throughput = ((double)(dataSize * 8)) / ((double)(totalTime / 1000000000.0));
                System.out.println("Total data received: " + dataSize + " bytes");
                System.out.println("Total elasped time: " + totalTime + " nanosec(s)");
                System.out.println("Throughput: " + throughput + " bps");
                System.out.println("Amount of packets: " + packetCount);
                thisFile.println("Total data received: " + dataSize + " bytes");
                thisFile.println("Total elasped time: " + totalTime + " nanosec(s)");
                thisFile.println("Throughput: " + throughput + " bps");
                thisFile.println("Amount of packets: " + packetCount);
                thisFile.close();
            }
            else
                System.out.println("Never terminated.");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
