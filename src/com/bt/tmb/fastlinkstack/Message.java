/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bt.tmb.fastlinkstack;
import java.util.Date;
/**
 *
 * @author 606335827
 * baselined in github 5/7/14
 */
public class Message implements Comparable<Message>
{
    final String m_srcHost;
    /**
     * The tagged (pipe separator) raw message from the ITS Link server
     */
    final private String m_rawMessage;
    
    private String m_mask;
    
    /**
     * The message ID from the ITS Link Programmers
     * Reference 
     */    
    private int m_msgID = 0;
    /**
     * Timestamp of when this object was created. Relatively close
     * to message receipt, rounded to seconds
     */
    private long m_created = 0;
       
    public Message(String msgdata, String srcHost)
    {
        this.m_srcHost = srcHost;
        this.m_rawMessage = msgdata; 
        //filter sequence letter out of mask
        int ploc = msgdata.indexOf("|");
        if(ploc == -1)
        {
            m_mask = msgdata;
        }
        else
        {
            m_mask = msgdata.substring(0, ploc) +
                    msgdata.substring(ploc + 2);            
        }
        
        //if extended header, chop date/time out of mask
        int loc = m_mask.indexOf("+");
        if(loc != -1)
        {            
            m_mask  = m_mask.substring(0, loc + 1) +
                    m_mask.substring(loc + 7);                        
        }        
                
        m_created = (new Date().getTime()) / 1000; 
        this.m_msgID = this.hashCode();
    }
    
    public long getTS()
    {
        return m_created;
    }
    
    public String getMessage()
    {
        return m_rawMessage;
    }
    
    public String getMask()
    {
        return m_mask;
    }
    
    public StructuredMessage getStructuredMessage()
    {
        StructuredMessage m = new StructuredMessage();
        m.m_srcLink = this.m_srcHost;
        m.m_id = -1;
        m.m_body = "";
        m.m_sequence = '?';
        m.m_timestamp = "";
        String msg = this.m_rawMessage;
        if(msg.length() == 0)
            return m;
        
        int loc = msg.indexOf("|");
        if(loc == -1)
            return m;
        m.m_site = msg.substring(0, loc);
        msg = msg.substring(loc + 1);
        m.m_sequence = msg.charAt(0);
        
        if(msg.charAt(1) != '+' )
        {
            //short header form
            m.m_timestamp = "";
            m.m_id = Integer.parseInt(msg.substring(1, 4));
            //message.m_size = atoi(msg.substr(4, 2).c_str());
            int dataSize = Integer.parseInt(msg.substring(4, 6));
            //data = msg.substring(6, 6 + dataSize);
            if(dataSize == 0)
            {
                m.m_body = "";
            }
            else
            {
                m.m_body = msg.substring(6);
            }
        }
        else
        {
            //extended header
            m.m_timestamp = msg.substring(2, 14);
            m.m_id = Integer.parseInt(msg.substring(14, 17));
            int dataSize = Integer.parseInt(msg.substring(17, 19));   
            if(dataSize == 0)
            {
                m.m_body = "";
            }
            else
            {
                m.m_body = msg.substring(19);
            }            
        }
        
        return m;
    }
    
    public int getID()
    {
        return this.m_msgID;
    }
    
    @Override
    public int compareTo(Message t) 
    {
        if(this.m_msgID < t.m_msgID)
        {
            return -1;
        }
        else if(this.m_msgID > t.m_msgID)
        {
            return 1;
        }
        
        return 0;
    }
    
    @Override
    public boolean equals(Object o)
    {
        
        if(this == o)
            return true;
        if(o != null && o instanceof Message)
        {
            Message other = (Message) o;
            if(this.m_msgID == other.m_msgID)
                return true;
        }
        
        return false;
    }

    @Override
    public final int hashCode()
    {
        StringBuilder hString = new StringBuilder(this.m_mask).append(Long.toString(m_created));
        int hash = 3;        
        hash = 73 * hash + 
                (hString != null ? hString.toString().hashCode() : 0);                
        return hash;
    }

   
    
}
