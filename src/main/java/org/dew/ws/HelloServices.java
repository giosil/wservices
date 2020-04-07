package org.dew.ws;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import javax.jws.HandlerChain;
import javax.jws.WebService;

import javax.servlet.http.HttpServletRequest;

import javax.xml.ws.WebServiceContext;

import javax.xml.ws.handler.MessageContext;

import org.dew.auth.AuthAssertion;
import org.dew.auth.AuthSOAPHandler;

@Stateless
@Remote(IHelloService.class)
@WebService(serviceName = "hello", targetNamespace = "http://hello.dew.org/", endpointInterface = "org.dew.ws.IHelloService")
@HandlerChain(file = "handler-chain.xml") // see org.dew.auth.AuthSOAPHandler
@PermitAll
public 
class HelloServices implements IHelloService
{
  @Resource
  protected WebServiceContext webServiceContext;
  
  @Override
  public 
  String hello(String name) 
  {
    // MessageContext examples
    MessageContext messageContext = webServiceContext.getMessageContext();
    
    // Http headers (Map<String, List<String>)
    System.out.println(MessageContext.HTTP_REQUEST_HEADERS);
    Object httpRequestHeaders = messageContext.get(MessageContext.HTTP_REQUEST_HEADERS);
    if(httpRequestHeaders instanceof Map) {
      Map<?, ?> mapHttpRequestHeaders = (Map<?, ?>) httpRequestHeaders;
      mapHttpRequestHeaders.forEach((k, v) -> System.out.println("  " + k + "=" + v));
    }
    
    // QueryString parameter
    String greeting = null;
    Object servletRequest = messageContext.get(MessageContext.SERVLET_REQUEST);
    if(servletRequest instanceof HttpServletRequest) {
      System.out.println(MessageContext.SERVLET_REQUEST + " = " + servletRequest);
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      greeting = httpServletRequest.getParameter("g");
    }
    if(greeting == null || greeting.length() == 0) greeting = "Hello";
    
    // Custom properties (setted in WSOAPHandler)
    Object assertions = messageContext.get(AuthSOAPHandler.MESSAGE_ASSERTIONS);
    System.out.println(AuthSOAPHandler.MESSAGE_ASSERTIONS + " = " + assertions);
    // Same thing, but more robust
    List<AuthAssertion> listOfAuthAssertion = AuthSOAPHandler.getListOfAssertion(messageContext);
    System.out.println(AuthSOAPHandler.MESSAGE_ASSERTIONS + " = " + listOfAuthAssertion);
    
    return greeting + " " + name + "!";
  }
}
