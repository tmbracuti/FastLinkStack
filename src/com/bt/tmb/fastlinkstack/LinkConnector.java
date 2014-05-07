/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bt.tmb.fastlinkstack;
import java.util.Properties;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 *
 * @author 606335827
 */
public class LinkConnector implements Runnable
{
    final private Properties m_props;
    final private LinkDispatcher m_dispatcher;
    String m_targetHost;    //this is the its link host
    private int m_targetPort;
    private String m_srcTag;
    private Socket m_server;
    private int m_version; //ITS Link 4 or 5, defaults to 5
    private Framer m_framer = null;
    
   /**
     * The last sent sequence letter used by housekeeping messages
     */
    private char lastSentSeq = ' ';
    
    private boolean isConnected = false;
    private final Object connGuard = new Object(); //guard on connection state
    private final Object frameGuard = new Object(); //guard on m_frame lifecycle
    
    
    public LinkConnector(Properties props, LinkDispatcher ld)
    {
        m_props = props;
        m_dispatcher = ld;        
    }
    
    public boolean isLinkConnected()
    {
        synchronized(connGuard)
        {
            return isConnected;
        }
    }
    
    private void setLinkConnected(boolean status)
    {
        synchronized(connGuard)
        {
            isConnected = status;
        }        
    }
    
    public char publishToLink(LinkBoundMessage msg) throws LinkPublishException
    {
      
        synchronized(frameGuard)
        {
            if(m_framer != null)
            {
                try
                {
                    String type = Integer.toString(msg.getID());
                    while(type.length() < 3)
                    {
                        type = "0" + type;
                    }
                    
                    String len = Integer.toString(msg.getDataLength());
                    while(len.length() < 2)
                    {
                        len = "0" + len;
                    }
                    
                    StringBuilder outMsg = new StringBuilder(type);
                    outMsg.append(len).append(msg.getDataBody());
                    byte[] mbytes = outMsg.toString().getBytes(Charset.forName("US-ASCII"));
                    char ret = m_framer.frameMsg(mbytes);
                    return ret;
                }
                catch(Exception any)
                {
                    throw new LinkPublishException(any.getLocalizedMessage());
                }
            }
            else
            {
                throw new LinkPublishException(
                        "invalid connection state - message framer is null");
            }
        }                
    }        
    
    private void snooze(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(Exception e){}
    }
    
    private Socket connect()
    {
        try
        {
            Socket s = new Socket(this.m_targetHost, this.m_targetPort);
            return s;
        }
        catch(Exception e)
        {
            System.err.printf("error: %s", e.getMessage());
            return null;                    
        }
    }
    
    private String createPaddedField(String value, int fieldSize, char padChar)
    {
        StringBuilder bld = new StringBuilder(value);
        int add = fieldSize - value.length();
        for(int i = 0; i < add; ++i)
        {
            bld.append(padChar);
        }               
        return bld.toString();              
    }
        
    
    /**
     * 
     * @param msg the un-tagged message from the ITS Link server
     * @param output the output stream back to ITS Link
     * @return true if this was a housekeeping (COS registration and heartbeats)
     * function that was handled or false if the message was not about 
     * housekeeping chores.
     */
    private boolean handleHousekeeping(String msg, Framer framer) throws IOException
    {
        if(msg.length() == 0)
            return false;
        int mid = 0;
        String data = null;
        if(msg.charAt(1) != '+' )
        {
            //short header form
            mid = Integer.parseInt(msg.substring(1, 4));
            //message.m_size = atoi(msg.substr(4, 2).c_str());
            int dataSize = Integer.parseInt(msg.substring(4, 6));
            //data = msg.substring(6, 6 + dataSize);
            if(dataSize == 0)
            {
                data = "";
            }
            else
            {
                data = msg.substring(6);
            }
        }
        else
        {
            //extended header
            //System.err.printf("housekeeping parsing message: %s - of length %d\n", msg, msg.length());
            mid = Integer.parseInt(msg.substring(14, 17));
            int dataSize = Integer.parseInt(msg.substring(17, 19));   
            if(dataSize == 0)
            {
                data = "";
            }
            else
            {
                data = msg.substring(19);
            }            
        }
     
        if(mid == 4) //error message 
        {
            
            if(data.equals("0002"))
            {
                //need to send cos request
                StringBuilder buf = new StringBuilder();
                //Framer now applies the sequence letter
                //char c = this.lastSentSeq = this.getNextSeq();
                //buf.append(c);
                if(m_version == 4)
                {
                    String cos = m_props.getProperty("cos", "25");
                    buf.append("13467").append(cos).append("R");
                }
                if(m_version == 5)
                {                    
                    String cos = m_props.getProperty("cos", "35");
                    buf.append("13467").append(cos).append("R");
                }
                
                String hostname = m_props.getProperty("homehost","localhost");
                String appname = m_props.getProperty("homeapp","localapp");
                
                buf.append(this.createPaddedField(hostname, 32, ' '));
                buf.append(this.createPaddedField(appname, 32, ' '));                
                byte[] mbytes = buf.toString().getBytes(Charset.forName("US-ASCII"));  
                synchronized(frameGuard)
                {
                    this.lastSentSeq = framer.frameMsg(mbytes);
                }
                System.out.printf("sent COS request: %s\n", buf.toString());                
                return true; //handled
            }
            else
            {
                System.err.printf("!!! received error: %s !!!\n", data);
                return false; //not handled, pass on
            }
        }
        else if(mid == 1) //heartbeat
        {
            //Framer now applies the sequence letter
            //char seq = this.lastSentSeq = this.getNextSeq();
            
            StringBuilder hb = new StringBuilder();
            hb.append("13600");//.append("\r\n");                       
            byte[] mbytes = hb.toString().getBytes(Charset.forName("US-ASCII"));
            synchronized(frameGuard)
            {
                this.lastSentSeq = framer.frameMsg(mbytes);
            }
            System.out.printf("*** hearbeat handled with %s-%s \n", this.lastSentSeq, hb.toString());
            return true; //handled
        }
        else if(mid == 127)
        {
            //Note: an ack, this should be dispatched to client app if it is for a message
            // that is NOT sent by housekeeping
            if(data.length() >= 1)
            {
                char ackSeq = data.charAt(0);
                if(ackSeq == this.lastSentSeq)
                {
                    //swallow housekeeping acks
                    this.lastSentSeq = ' ';
                    System.err.println("housekeeping ack suppressed");                            
                    return true; //no need to send up to client code
                }
            }
            return false;
        }
        
        return false; //not a handled housekeeping message
    }

    @Override
    public void run() 
    {
        if(m_props == null)
        {
            System.err.println("properties are not initialized");
            return;
        }
        m_targetHost = m_props.getProperty("itslinkhost", "localhost");
        m_targetPort = Integer.parseInt(m_props.getProperty("itslinkport", "3001"));
        m_srcTag = m_props.getProperty("sourcetag", "A");
        m_version = Integer.parseInt(m_props.getProperty("version", "5"));
               
        while(true)
        {
            if(m_server == null)
            {
                m_server = connect();                
            }
            
            if(m_server != null)
            {             
                try
                {
                    InputStream input = m_server.getInputStream();  
                    OutputStream output = m_server.getOutputStream();
                    synchronized(frameGuard)
                    {
                        m_framer = new LinkFramer(input, output);
                    }
                    setLinkConnected(true);                    
                    byte[] msgBytes = null;
                    //read loop
                    
                    while( true )
                    {                                                
                        msgBytes = m_framer.nextMsg();//safe to not synchronize here                        
                        if(msgBytes == null)
                            break;
                        String msg = new String(msgBytes).trim();
                        System.out.println(this.m_targetHost  + ": incomming raw message: " + msg);
                        if(!handleHousekeeping(msg, m_framer))
                        {                            
                            //tag site and then dispatch northbound
                            msg = m_srcTag + "|" + msg;
                            Message m = new Message(msg, this.m_targetHost);
                            m_dispatcher.setMessage(m);                            
                        }                        
                    }
                    //server ended connection normally
                    this.setLinkConnected(false);
                    synchronized(frameGuard)
                    {
                        m_framer = null;
                    }
                    //connection will auto-restore when server is back and 
                    //willing to accept connections
                                        
                    m_server.close();
                    m_server = null;
                }
                catch(IOException ioe)
                {
                    System.err.printf("read error: %s\n", ioe.getMessage());
                    this.setLinkConnected(false);
                    synchronized(frameGuard)
                    {
                        m_framer = null;
                    }
                    
                    try
                    {
                        m_server.close();
                    }catch(Exception e){};
                    m_server = null;
                }
            
            }
                        
            snooze(1000 * 60); //we only get here on disconnect/problem state
        }
      
    } //end run
 
} //end class
