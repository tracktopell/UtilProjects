package com.xpressosystems.testws.services;

import javax.jws.WebMethod;
import javax.jws.WebService;
import org.springframework.stereotype.Repository;

@WebService(endpointInterface = "com.xpressosystems.testws.services.CalculatorService")
@Repository("calculatorService")
public class CalculatorServiceImpl implements  CalculatorService {

    public Double add(Double a,Double b){
        return a + b;
    }    
}
