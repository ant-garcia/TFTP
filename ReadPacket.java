/* This class implements the Protocol for what a
 * TFTP Read packet is made of.*/

public final class ReadPacket extends TFTPPacket 
{
    /*Initailizes the ReadPacket based on what the url
     *is being used and the request type.*/
    private void init(String url, String reqType)
    {
        length = 2 + url.length() + 1 + reqType.length() + 1;
        message = new byte[length];
        type = RRQ;
        
        put(opOFFSET, RRQ);
        put(fileOFFSET,url,(byte)0);
        put(fileOFFSET + url.length()+1,reqType,(byte)0);
    }

    protected  ReadPacket(){} 

    public ReadPacket(String url, String reqType)
    {
       init(url, reqType);
    }
    
    /*Returns the url located in the ReadPacket*/
    public String getUrl()
    {
        return this.get(fileOFFSET,(byte)0);
    }
    
    /*Returns the request type located in the ReadPacket*/
    public String getRequestType()
    {
        String url = getUrl();
        return this.get(fileOFFSET+url.length()+1,(byte)0);
    }
}
