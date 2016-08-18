/* This class implements the Protocol for what a
 * TFTP Data packet is made of.*/

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class DataPacket extends TFTPPacket 
{

    protected DataPacket() {}

    public DataPacket(int blockNum, byte[] buf)
    {
        message = buf;
        length = buf.length;
        type = DATA;
        
        put(opOFFSET, DATA);
        put(blockOFFSET, (short)blockNum);
    }
    
    public DataPacket(int blockNum, FileInputStream in)throws IOException
    {
        message = new byte[MAX_PACKET_SIZE];
        
        put(opOFFSET, DATA);
        put(blockOFFSET, (short)blockNum);
        length = in.read(message, dataOFFSET, MAX_DATA_SIZE) + dataOFFSET;
    }
    
    /*Returns the block number of the DataPacket.*/
    public int getBlockNum()
    {
        return this.get(blockOFFSET);
    }
    
    /*Contains just the data portion of the 
     *Data Packet.*/
    public void data(byte buf[])
    {
        buf = new byte[length-dataOFFSET];
        
        for(int i = 0;i < length-dataOFFSET;i++)
            buf[i] = message[i+dataOFFSET];
    }
    
    /*Writes the message block to a specified file using a
     *FileOutputStream.*/
    public int write(FileOutputStream out)throws IOException
    {
        out.write(message, dataOFFSET, length-dataOFFSET);
        return (length-dataOFFSET);
    }
}
