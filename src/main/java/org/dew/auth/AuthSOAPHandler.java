package org.dew.auth;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import javax.xml.soap.SOAPMessage;

import javax.xml.ws.handler.MessageContext;

import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public 
class AuthSOAPHandler implements SOAPHandler<SOAPMessageContext>
{
  public final static String MESSAGE_ASSERTIONS = "org.dew.ws.assertions";
  
  @Override
  public 
  boolean handleMessage(SOAPMessageContext context) 
  {
    System.out.println("WSOAPHandler.handleMessage(" + context + ")...");
    
    boolean isRequestMessage = false;
    Object outboundProperty = context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    if(outboundProperty instanceof Boolean) {
      isRequestMessage = !((Boolean) outboundProperty).booleanValue();
    }
    
    BasicAssertion basicAssertion = checkBasicAuth(context);
    
    try {
      SOAPMessage soapMessage = context.getMessage();
      
      if(isRequestMessage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        
        AuthContentHandler authContentHandler = new AuthContentHandler();
        authContentHandler.load(baos.toByteArray());
        
        List<AuthAssertion> listOfAuthAssertion = authContentHandler.getListOfAssertion();
        if(basicAssertion != null && listOfAuthAssertion != null) {
          listOfAuthAssertion.add(0, basicAssertion);
        }
        
        context.put(MESSAGE_ASSERTIONS, listOfAuthAssertion);
        context.setScope(MESSAGE_ASSERTIONS, MessageContext.Scope.APPLICATION);
      }
    }
    catch(Exception ex) {
      System.out.println("Exception in WSOAPHandler.handleMessage(" + context + "): " + ex);
    }
    
    // Invoke the next handler
    return true;
  }
  
  @Override
  public 
  boolean handleFault(SOAPMessageContext context) 
  {
    System.out.println("WSOAPHandler.handleFault(" + context + ")...");
    
    // Invoke the next handler
    return true;
  }
  
  @Override
  public 
  void close(MessageContext context) 
  {
    System.out.println("WSOAPHandler.close(" + context + ")...");
  }
  
  @Override
  public 
  Set<QName> getHeaders() 
  {
    System.out.println("WSOAPHandler.getHeaders()...");
    return null;
  }
  
  // Utilities 
  @SuppressWarnings("unchecked")
  public static
  List<AuthAssertion> getListOfAssertion(MessageContext messageContext)
  {
    if(messageContext == null) {
      return new ArrayList<AuthAssertion>();
    }
    Object messageAssertions = messageContext.get(MESSAGE_ASSERTIONS);
    if(messageAssertions == null) {
      return new ArrayList<AuthAssertion>();
    }
    if(messageAssertions instanceof List) {
      return (List<AuthAssertion>) messageAssertions;
    }
    return new ArrayList<AuthAssertion>();
  }
  
  public static
  String getHttpHeaderValue(MessageContext messageContext, String headerName)
  {
    if(messageContext == null) {
      return null;
    }
    Object httpRequestHeaders = messageContext.get(MessageContext.HTTP_REQUEST_HEADERS);
    if(httpRequestHeaders instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) httpRequestHeaders;
      Object headerValue = map.get(headerName);
      if(headerValue instanceof List) {
        int size = ((List<?>) headerValue).size();
        if(size > 0) {
          Object item0 = ((List<?>) headerValue).get(0);
          if(item0 != null) return item0.toString();
          return null;
        }
      }
      else if(headerValue != null) {
        return headerValue.toString();
      }
    }
    return null;
  }
  
  public static
  BasicAssertion checkBasicAuth(MessageContext messageContext)
  {
    String authorization = getHttpHeaderValue(messageContext, "Authorization");
    if(authorization == null || !authorization.startsWith("Basic ")) {
      return null;
    }
    String sBase64BasicAuth = authorization.substring(6);
    try {
      String sPlainBasicAuth = new String(Base64Coder.decode(sBase64BasicAuth), "UTF-8");
      int sep = sPlainBasicAuth.indexOf(':');
      if(sep > 0) {
        String subjectId = sPlainBasicAuth.substring(0,sep);
        String password  = sPlainBasicAuth.substring(sep+1);
        return new BasicAssertion(subjectId, password);
      }
    }
    catch(Exception ex) {
    }
    return null;
  }
}
