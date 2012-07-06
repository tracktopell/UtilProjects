package com.xpressosystems.testws.services;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface CalculatorService {
    @WebMethod
    Double add(Double a,Double b);
}
