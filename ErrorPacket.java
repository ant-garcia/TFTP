/* This class implements the Protocol for what a
 * TFTP Error packet is made of.*/

class ErrorPacket extends TFTPPacket 
{
    protected ErrorPacket() {}
    
    public ErrorPacket(int num, String msg)
    {
        length = 4 + msg.length() + 1;
        message = new byte[length];
        type = ERROR;
        
        put(opOFFSET,ERROR);
        put(numOFFSET, (short)num);
        put(msgOFFSET, msg, (byte)0);
    }

    /*Returns the error number of the Error Packet.*/
    public int getNum()
    {
        return this.get(numOFFSET);
    }
    
    /*Returns the error message of the Error Packet.*/
    public String getMessage()
    {
        return this.get(msgOFFSET, (byte)0);
    }
}
