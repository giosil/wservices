package org.dew.test;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import javax.xml.namespace.QName;

import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.dew.auth.AuthUtil;
import org.dew.auth.Base64Coder;

public 
class WSSecurityHandler implements SOAPHandler<SOAPMessageContext>
{
  public static final String NAMESPACEURI_PREFIX          = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-";
  public static final String NAMESPACEURI_WSSECURITY_WSSE = NAMESPACEURI_PREFIX + "wssecurity-secext-1.0.xsd";
  public static final String NAMESPACEURI_WSSECURITY_WSU  = NAMESPACEURI_PREFIX + "wssecurity-utility-1.0.xsd";
  public static final String ATTRIBUTENAME_X509TOKEN      = NAMESPACEURI_PREFIX + "x509-token-profile-1.0#X509v3";
  public static final String NAMESPACEURI_WSSECURITY_ENC  = NAMESPACEURI_PREFIX + "soap-message-security-1.0#Base64Binary";
  public static final String MUST_UNDERSTAND              = "0";
  
  @Override
  public 
  boolean handleMessage(SOAPMessageContext context) 
  {
    System.out.println("WSSecurityHandler.handleMessage(" + context + ")...");
    
    Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    if(outbound == null || !outbound.booleanValue()) {
      // Invoke the next handler
      return true;
    }
    
    PrivateKey      privateKey  = null;
    X509Certificate certificate = null;
    String          base64X509  = null;
    try {
      privateKey  = AuthUtil.loadPrivateKey("authentication.pem");
      certificate = AuthUtil.loadCertificate("authentication.crt");
      base64X509  = new String(Base64Coder.encode(certificate.getEncoded()));
    }
    catch(Exception ex) {
      System.err.println("Exception during load certificate: " + ex);
      // Invoke the next handler
      return true;
    }
    
    try {
      SOAPMessage  message  = context.getMessage();
      SOAPPart     part     = message.getSOAPPart();
      SOAPEnvelope envelope = part.getEnvelope();
      SOAPHeader   header   = envelope.getHeader();
      if(header == null) header = envelope.addHeader();
      
      QName qh = header.getElementQName();
      
      SOAPHeaderElement securityElement = header.addHeaderElement(new QName(NAMESPACEURI_WSSECURITY_WSSE, "Security", "wsse"));
      securityElement.addNamespaceDeclaration("wsu", NAMESPACEURI_WSSECURITY_WSU);
      securityElement.addAttribute(new QName(qh.getNamespaceURI(), "mustUnderstand", qh.getPrefix()), MUST_UNDERSTAND);
      
      // Build timestamp
      SOAPElement timeStampElement = securityElement.addChildElement("Timestamp", "wsu");
      timeStampElement.addAttribute(new QName(NAMESPACEURI_WSSECURITY_WSU, "Id", "wsu"), "timestamp");
      SOAPElement createdElement = timeStampElement.addChildElement("Created", "wsu");
      createdElement.addTextNode(getTimestamp(0));
      SOAPElement expiresElement = timeStampElement.addChildElement("Expires", "wsu");
      expiresElement.addTextNode(getTimestamp(5));
      
      SOAPElement binarySecurityTokenElement = securityElement.addChildElement("BinarySecurityToken", "wsse");
      binarySecurityTokenElement.addAttribute(new QName("EncodingType"), NAMESPACEURI_WSSECURITY_ENC);
      binarySecurityTokenElement.addAttribute(new QName(NAMESPACEURI_WSSECURITY_WSU, "Id", "wsu"), "cert");
      binarySecurityTokenElement.addAttribute(new QName("ValueType"), ATTRIBUTENAME_X509TOKEN);
      binarySecurityTokenElement.addTextNode(base64X509);
      
      header.addChildElement(securityElement);
      
      XMLSignatureFactory signFactory   = XMLSignatureFactory.getInstance("DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());
      CanonicalizationMethod c14nMethod = signFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);
      DigestMethod         digestMethod = signFactory.newDigestMethod(DigestMethod.SHA1, null);
      SignatureMethod        signMethod = signFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
      Transform               transform = signFactory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null);
      
      List<Transform> transformList = Collections.singletonList(transform);
      Reference reference = signFactory.newReference("#timestamp", digestMethod, transformList, null, null);
      
      List<Reference> referenceList = new ArrayList<Reference>();
      referenceList.add(reference);
      
      SignedInfo signInfo = signFactory.newSignedInfo(c14nMethod, signMethod, referenceList);
      
      DOMSignContext dsc = new DOMSignContext(privateKey, securityElement);
      XMLSignature signature = signFactory.newXMLSignature(signInfo, null);
      signature.sign(dsc);
      
      SOAPElement signatureElement = (SOAPElement) securityElement.getLastChild();
      SOAPElement keyInfoElement = signatureElement.addChildElement("KeyInfo");
      
      SOAPElement securityTokenReferenceElement = keyInfoElement.addChildElement("SecurityTokenReference", "wsse");
      
      SOAPElement referenceElement = securityTokenReferenceElement.addChildElement("Reference", "wsse");
      referenceElement.setAttribute("URI",      "#cert");
      referenceElement.setAttribute("ValueType", ATTRIBUTENAME_X509TOKEN);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    
    // Invoke the next handler
    return true;
  }

  @Override
  public 
  boolean handleFault(SOAPMessageContext context) 
  {
    System.out.println("WSSecurityHandler.handleFault(" + context + ")...");
    
    // Invoke the next handler
    return true;
  }

  @Override
  public 
  void close(MessageContext context) 
  {
    System.out.println("WSSecurityHandler.close(" + context + ")...");
  }

  @Override
  public 
  Set<QName> getHeaders() 
  {
    System.out.println("WSSecurityHandler.getHeaders()...");
    return null;
  }
  
  protected
  String getTimestamp(int iMM)
  {
    GregorianCalendar timeStamp = new GregorianCalendar();
    timeStamp.add(Calendar.MINUTE, -2);
    if(iMM != 0) timeStamp.add(Calendar.MINUTE, iMM);
    DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dfm.format(timeStamp.getTime());
  }
}
