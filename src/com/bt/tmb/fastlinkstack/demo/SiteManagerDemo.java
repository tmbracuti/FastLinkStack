/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bt.tmb.fastlinkstack.demo;
import com.bt.tmb.fastlinkstack.IDispatchFunctor;
import com.bt.tmb.fastlinkstack.LinkConfigException;
import com.bt.tmb.fastlinkstack.SiteManager;
import com.bt.tmb.fastlinkstack.StructuredMessage;
import com.bt.tmb.fastlinkstack.LinkBoundMessage;
import com.bt.tmb.fastlinkstack.LinkPublishException;
import java.util.Properties;
import java.io.*;

/**
 *
 * @author 606335827
 */
public class SiteManagerDemo implements IDispatchFunctor
{
    private final Object throttle = new Object();
    private char pendingAck = ' ';
    char ack = ' ';
    int calibrationChk = 0;
    private int ackCount = 0;

    /**
     * @param args the command line arguments
     */
    private static Properties getProperties(String filename)
    {
        Properties props = new Properties();
        FileReader reader = null;
        try
        {
            reader = new FileReader(filename);
            props.load(reader);
            reader.close();
            return props;
        } 
        catch (IOException ex) 
        {
            System.err.println(ex.getMessage());
            return null;
        }
        
    }
    
    private void generateSouthBoundTraffic(SiteManager sm)
    {
        int counter = 0;
        
        while(true) //generate throttled southbound messages
        {
            System.err.println("generateSouthBoundTraffic - lock");
            synchronized(this.throttle)
            {
                if(this.pendingAck == ' ')
                {
                    LinkBoundMessage lbm = null;
                    if( (counter++ % 2) == 0 )
                    {
                        //version check
                        //lbm = new LinkBoundMessage(131, 0, "");
                        lbm = new LinkBoundMessage(174, 5, "07989");
                    }
                    else
                    {
                        //ddi vmail check
                        lbm = new LinkBoundMessage(174, 5, "07989");
                        //lbm = new LinkBoundMessage(131, 0, "");
                    }
                    
                    if(counter == 100)
                        counter = 1;
                    try
                    {
                        //System.err.println("\tpublishing on clear ack ");
                        this.pendingAck = sm.publish(lbm);
                        System.err.printf("%d : publishing on clear ack - new pendingAck = %s\n",
                                counter, pendingAck);
                    }
                    catch(LinkPublishException lpe)
                    {
                        System.err.println("link publish exception: " + 
                                lpe.getMessage());
                        return;
                    }
                }
                else
                {
                    System.err.println("\twaiting on ack clearence");
                }
            }
            System.err.println("generateSouthBoundTraffic - unlock");
            if(counter >= 30)
            {
                break;
            }
            //break;
            snooze(500);
        }
        System.err.printf("\nsouthbound message count = %d\n", counter);
        System.err.printf("total ack count = %d\n", ackCount);
    }
    
    private void snooze(long ms)
    {
        try
        {
            Thread.sleep(ms);                
        }
        catch(Exception e){}
    }
    
    public static void main(String[] args) throws LinkConfigException
    {
        SiteManagerDemo demo = new SiteManagerDemo();
        //load demo properties
        Properties props = getProperties("edison.properties");
        if(props == null)
            throw new LinkConfigException("no properties");
        
        //setup a SiteManager
        SiteManager edison = new SiteManager(props, demo);
        if(!edison.connectSite())
        {
            System.err.println("failed to start site data flow");
        }
        demo.snooze(5000); //time to get the connections properly set
        //System.err.println("Site demo ending");   
        //Thread sbt = new Thread(new SouthTest(site1));
        //sbt.start();
        //demo.generateSouthBoundTraffic(edison);
                   
    }

    private boolean isAck(String msg)
    {
        if(msg.length() == 0)
            return false;
        msg = msg.substring(msg.indexOf("|") + 1);
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
            mid = Integer.parseInt(msg.substring(14, 17));
            int dataSize = Integer.parseInt(msg.substring(17, 19));   
            if(dataSize == 0)
            {
                data = " ";
            }
            else
            {
                data = msg.substring(19);
            }            
        }
        
        if(mid == 127)
        {
            if(data.length() >= 1)
            {
                
                this.ack = data.charAt(0);
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void processDispatch(String linkMsg, StructuredMessage smsg)
    {        
        System.out.printf("site message: %s\n", smsg.toString());
        
        if(isAck(linkMsg))
        {
            //System.err.println("processDispatch - lock");
            this.ackCount++;
            synchronized(this.throttle)
            {
                if(this.pendingAck == ' ' || this.ack == this.pendingAck )
                {                    
                    pendingAck = ' ';
                }
                else
                {                
                    System.err.printf(
                            "error: out of sync message (%s) waiting on %s\n ",
                            smsg.m_sequence, this.pendingAck);
                    if(calibrationChk++ == 3)
                    {
                        calibrationChk = 0;
                        //resync
                        System.err.println("*** resynchronizing message IDs to server ***");
                        pendingAck = ' ';
                    }
                }
            }
            //System.err.println("processDispatch - unlock");
        }
    }
}
