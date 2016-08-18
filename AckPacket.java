/* This class implements the Protocol for what a
 * TFTP Ack packet is made of.*/

public final class AckPacket extends TFTPPacket 
{

    protected AckPacket() {}
    
    public AckPacket(int blockNum)
    {
        length = 4;
        message = new byte[length];
        type = ACK;
        
        put(opOFFSET, ACK);
        put(blockOFFSET, (short)blockNum);
    }
    
    /*Returns the block number of the Ack Packet.*/
    public int getBlockNum()
    {
        return this.get(blockOFFSET);
    }
}
