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
public class SignatureTest implements IDispatchFunctor
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // TODO code application logic here
        LinkDispatcher d = new LinkDispatcher(new SignatureTest());
        Message m = new Message(
                "LinkTest|O+2253282501120165700000000000000000577Rainey 7925 A       00000000792507925",
                "dummy");
        
        System.err.printf("raw : %s\n", m.getMessage());
        System.err.printf("mask: %s\n", m.getMask());
        System.err.println("");
    }

    @Override
    public void processDispatch(String linkMsg, StructuredMessage smsg) 
    {
        System.err.println("no_op");
    }
}
