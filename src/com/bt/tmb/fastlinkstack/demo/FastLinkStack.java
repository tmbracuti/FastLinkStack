/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bt.tmb.fastlinkstack.demo;
import com.bt.tmb.fastlinkstack.IDispatchFunctor;
import com.bt.tmb.fastlinkstack.LinkConnector;
import com.bt.tmb.fastlinkstack.LinkDispatcher;
import com.bt.tmb.fastlinkstack.StructuredMessage;
import java.util.Properties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is a test driver
 * @author 606335827
 */
public class FastLinkStack implements IDispatchFunctor
{

    /**
     * 1. Create a dispatcher
     * 2. start the dispatchers dispatch pump
     * 3. set properties in a Properties object
     * 4. create a link connector with the properties and dispatcher as args
     * 5. start the connectors read loop
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        FastLinkStack driver = new FastLinkStack();  //for the callback
        //set the callback 'functor'
        LinkDispatcher dispatcher = new LinkDispatcher(driver);
        ExecutorService dispatchExec = Executors.newSingleThreadExecutor();        
        dispatchExec.execute(dispatcher);  //start dispatch pump
        
        Properties props = new Properties();
        //props.put("itslinkhost", "localhost");
        props.put("itslinkhost", "10.10.40.102");
        props.put("itslinkport", "3001");
        props.put("sourcetag", "Alpha");
        props.put("version", "5");
        props.put("cos", "35");//optional10.10.40.102
        props.put("homehost","BTG460704");
        props.put("homeapp","fastlink");
        //setup a connection with properties and the dispatcher
        LinkConnector conn1 = new LinkConnector(props, dispatcher);
        ExecutorService connExec = Executors.newSingleThreadExecutor();        
        connExec.execute(conn1); //start the connector read loop
        System.out.println("connector executing");
        
        //LinkConnector conn2 = new LinkConnector(props, dispatcher);
        //ExecutorService conn2Exec = Executors.newSingleThreadExecutor();        
        //conn2Exec.execute(conn2);
   
        //connExec.shutdown(); //waits on conn to finish (never will)
                
    }

    @Override
    public void processDispatch(String linkMsg, StructuredMessage smsg)
    {
        System.out.printf("site message: %s\n", linkMsg);
    }
}
