package com.xpressosystems.testws.client;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import com.xpressosystems.testws.services.CalculatorService;


public class CalculatorServiceDynamicClient {

    private CalculatorServiceDynamicClient () {
    }

    public static void main(String args[]) throws Exception {
        //System.out.println("==>> 1: init contex");
        //ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"client-beans.xml"});
        //System.out.println("==>> 2: contex ok");

        CalculatorService client = null;
        JaxWsProxyFactoryBean factory = null;
        try {
            System.out.println("==>> 3:Creating Factory");
            factory = new JaxWsProxyFactoryBean();
            
            factory.setServiceClass(CalculatorService.class);

            // The default path of deployred applicaction, Edit if diferent !!!
            factory.setAddress("http://localhost:8080/calculator-ws-web-0.1-SNAPSHOT/services/CalculatorService");
            
            System.out.println("==>> 4:Creating Client");
            
            client = (CalculatorService)factory.create();
            
            System.out.println("==>> 5:Invoking service");
            Double a = Double.parseDouble(args[0]); 
            Double b = Double.parseDouble(args[1]); 

            // Real Invoacion to client
            Double r = client.add(a,b);
            System.out.println( a + " + "+ b +" = " + r );

        } catch(Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }        
    }
}
