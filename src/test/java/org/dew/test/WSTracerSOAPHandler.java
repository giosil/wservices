package org.dew.test;

import java.io.ByteArrayOutputStream;

import java.util.Set;

import javax.xml.namespace.QName;

import javax.xml.soap.SOAPMessage;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public 
class WSTracerSOAPHandler implements SOAPHandler<SOAPMessageContext> 
{
  public 
  boolean handleMessage(SOAPMessageContext context) 
  {
    System.out.println("WSTracerSOAPHandler.handleMessage(" + context + ")...");
    
    Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    boolean isClientRequest = outbound != null && outbound.booleanValue();
    
    try {
      SOAPMessage soapMessage = context.getMessage();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      soapMessage.writeTo(baos);
      
      if(isClientRequest) {
        System.out.println("Request:");
      }
      else {
        System.out.println("Response:");
      }
      System.out.println(new String(baos.toByteArray(), "UTF-8"));
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    
    return true;
  }

  public 
  boolean handleFault(SOAPMessageContext context) 
  {
    System.out.println("WSTracerSOAPHandler.handleFault(" + context + ")...");
    
    try {
      SOAPMessage soapMessage = context.getMessage();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      soapMessage.writeTo(baos);
      
      System.out.println(new String(baos.toByteArray(), "UTF-8"));
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    
    return true;
  }

  public 
  void close(MessageContext context) 
  {
    System.out.println("WSTracerSOAPHandler.close(" + context + ")...");
  }

  public 
  Set<QName> getHeaders() 
  {
    System.out.println("WSTracerSOAPHandler.getHeaders()...");
    return null;
  }
}