/* This class implements the Protocol for what a
 * TFTP Write packet is made of.*/

public final class WritePacket extends TFTPPacket 
{
    /*Initailizes the WritePacket based on what the url
     *is being used and the request type.*/
    private void init(String fileName, String reqType)
    {
        length = 2 + fileName.length() + 1 + reqType.length() + 1;
        message = new byte[length];
        type = WRQ;
        
        put(opOFFSET, WRQ);
        put(fileOFFSET,fileName,(byte)0);
        put(fileOFFSET + fileName.length()+1,reqType,(byte) 0);
    }
    
    protected WritePacket() {}
    
    public WritePacket(String fileName, String reqType)
    {
        init(fileName, reqType);
    }

    /*Returns the url located in the WritePacket*/
    public String getFileName()
    {
        return this.get(fileOFFSET,(byte)0);
    }
    /*Returns the request type located in the WritePacket*/
    
    public String getRequestType()
    {
        String fileName = getFileName();
        return this.get(fileOFFSET+fileName.length()+1,(byte)0);
    }
}
