/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bt.tmb.fastlinkstack;

/**
 * This is the southbound (to Link) message type
 * @author 606335827
 */
public class LinkBoundMessage
{
    private int m_id;
    private int m_dataLength;
    private String m_data;
    
    /**
     * Ctor - client must set values in the ctor
     * @param id The message ID from the Programmers Reference
     * @param dataLength The length of the data payload (must match
     * the specification in the Programmers Reference for specified message ID
     * @param dataBody 
     */
    public LinkBoundMessage(int id, int dataLength, String dataBody)
    {
        m_id = id;
        m_dataLength = dataLength;
        m_data = dataBody;
    }
    
    public int getID()
    {
        return m_id;
    }
    
    public int getDataLength()
    {
        return m_dataLength;
    }
    
    public String getDataBody()
    {
        return m_data;
    }
    
}
