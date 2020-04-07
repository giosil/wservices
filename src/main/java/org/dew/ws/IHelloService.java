package org.dew.ws;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public
interface IHelloService
{
  public String hello(@WebParam(name = "name") String name);
}
