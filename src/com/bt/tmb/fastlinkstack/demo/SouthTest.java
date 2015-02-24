/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bt.tmb.fastlinkstack.demo;
import com.bt.tmb.fastlinkstack.*;
/**
 *
 * @author 606335827
 */
public class SouthTest implements Runnable
{
    private SiteManager m_sm;
    
    public SouthTest(SiteManager sm)
    {
        m_sm = sm;
    }
    
    private void snooze(long ms)
    {
        try
        {
            Thread.sleep(ms);                
        }
        catch(Exception e){}
    }
    
    @Override
    public void run() 
    {
        if(m_sm == null)
        {
            System.err.println("bad SiteManager reference - null");
            return;
        }
        snooze(5000); //wait 5 sec before publishing loop starts
        while(true)
        {
            LinkBoundMessage lbm = new LinkBoundMessage(131, 0, "");
            try
            {
                char receipt = m_sm.publish(lbm);
            }
            catch(LinkPublishException lpe)
            {
                System.err.println("link publish exception: " + 
                        lpe.getMessage());
                return;
            }
            
            snooze(1000);
        }
    }
    
}
