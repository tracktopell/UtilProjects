/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tracktopell.testbluecovemaven;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 *
 * @author alfredo
 */
public class SendBytesToDevice {

    public static void main(String[] args) {
        OutputStream os = null;
        StreamConnection conn = null;
        String btAdress = args[0];
        String fileToSend = args[1];
        InputStream is = null;
        String urlBTAddress = null;
        
        System.err.println("==>> SendBytes to "+btAdress+", reading from: "+fileToSend);
            
        try {       
            System.err.println("==>> try to connect !");
            urlBTAddress = "btspp://"+btAdress.replace(":", "") +":1";
            
            conn = (StreamConnection) Connector.open(urlBTAddress);
            os = conn.openOutputStream();
            //input = conn.openInputStream();
            System.err.println("==>> ok connected !");
            
            is = new FileInputStream(fileToSend);
            byte[] buffer=new byte[16];
            int r=-1;
            while((r = is.read(buffer, 0, buffer.length)) != -1){
                //System.err.print  ("os.write(buffer,0,"+r+"); //");
                //System.err.println(new String(buffer,0,r));
                os.write(buffer, 0, r);
            }
            
        }catch(Exception ex) {
            ex.printStackTrace(System.err);
        } finally {
            try {
                if(os != null) {
                    os.close();
                    conn.close();
                    System.err.println("==>> ok disconnected !");
                }
                if(is != null) {
                    is.close();
                }
            } catch(IOException ioe) {
                ioe.printStackTrace(System.err);
            }
        }



    }
}
