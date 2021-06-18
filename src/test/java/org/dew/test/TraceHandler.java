package org.dew.test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public 
class TraceHandler implements SOAPHandler<SOAPMessageContext>
{
  protected OutputStream tracerRequest;
  protected OutputStream tracerResponse;
  
  public TraceHandler()
  {
  }

  public TraceHandler(String fileTracerRequest, String fileTracerResponse)
  {
    try {
      if(fileTracerRequest != null && fileTracerRequest.length() > 0) {
        this.tracerRequest = new FileOutputStream(fileTracerRequest, false);
      }
      if(fileTracerResponse != null && fileTracerResponse.length() > 0) {
        this.tracerResponse = new FileOutputStream(fileTracerResponse, false);
      }
    }
    catch(Exception ex) {
      System.out.println("Exception in TraceHandler: " + ex);
    }
  }

  public OutputStream getTracerRequest() {
    return tracerRequest;
  }
  
  public void setTracerRequest(OutputStream tracerRequest) {
    this.tracerRequest = tracerRequest;
  }
  
  public OutputStream getTracerResponse() {
    return tracerResponse;
  }
  
  public void setTracerResponse(OutputStream tracerResponse) {
    this.tracerResponse = tracerResponse;
  }
  
  @Override
  public 
  boolean handleMessage(SOAPMessageContext context) 
  {
    Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    if(outbound == null || !outbound.booleanValue()) {
      try {
        SOAPMessage message = context.getMessage();
        if(tracerResponse != null) {
          message.writeTo(tracerResponse);
        }
        else {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          message.writeTo(baos);
          System.out.println(new String(baos.toByteArray()));
        }
      }
      catch(Exception ex) {
        System.out.println("Exception in TraceHandler.handleMessage(outbound=" + outbound + "): " + ex);
      }
      
      // Invoke the next handler
      return true;
    }
    
    try {
//      If SOAPMessage is modified this don't work fine.
//
//      SOAPMessage message = context.getMessage();
//      if(tracerRequest != null) {
//        message.writeTo(tracerRequest);
//      }
//      else {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        message.writeTo(baos);
//        System.out.println(new String(baos.toByteArray()));
//      }
      
      // This is preferred
      
      SOAPMessage  message  = context.getMessage();
      SOAPPart     part     = message.getSOAPPart();
      SOAPEnvelope envelope = part.getEnvelope();
      String sEnvelope = soapElementToString(envelope);
      if(sEnvelope != null && sEnvelope.length() > 0) {
        if(tracerRequest != null) {
          tracerRequest.write(sEnvelope.getBytes());
        }
        else {
          System.out.println(sEnvelope);
        }
      }
    }
    catch(Exception ex) {
      System.out.println("Exception in TraceHandler.handleMessage(outbound=" + outbound + "): " + ex);
    }
    
    // Invoke the next handler
    return true;
  }
  
  @Override
  public 
  boolean handleFault(SOAPMessageContext context) 
  {
    try {
      SOAPMessage message = context.getMessage();
      if(tracerResponse != null) {
        message.writeTo(tracerResponse);
      }
      else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        System.out.println(new String(baos.toByteArray()));
      }
    }
    catch(Exception ex) {
      System.out.println("Exception in TraceHandler.handleFault: " + ex);
    }
    
    // Invoke the next handler
    return true;
  }
  
  @Override
  public 
  void close(MessageContext context) 
  {
  }
  
  @Override
  public 
  Set<QName> getHeaders() 
  {
    HashSet<QName> headers = new HashSet<QName>();
    return headers;
  }
  
  protected static 
  String soapElementToString(SOAPElement soapElement) 
  {
    try {
      DOMSource domSource = new DOMSource(soapElement);
      
      StringWriter stringWriter = new StringWriter();
      
      TransformerFactory.newInstance().newTransformer().transform(domSource, new StreamResult(stringWriter));
      
      return stringWriter.toString();
    }
    catch (Exception ex) {
      System.out.println("Exception during trace: " + ex);
    }
    return "";
  }
}