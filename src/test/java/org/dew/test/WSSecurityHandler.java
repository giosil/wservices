package org.dew.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Document;

import org.dew.auth.AuthUtil;
import org.dew.auth.Base64Coder;
import org.dew.auth.SAMLAttributeAssertion;

public 
class WSSecurityHandler implements SOAPHandler<SOAPMessageContext>
{
  public static final String NAMESPACEURI_PREFIX          = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-";
  public static final String NAMESPACEURI_WSSECURITY_WSSE = NAMESPACEURI_PREFIX + "wssecurity-secext-1.0.xsd";
  public static final String NAMESPACEURI_WSSECURITY_WSU  = NAMESPACEURI_PREFIX + "wssecurity-utility-1.0.xsd";
  public static final String ATTRIBUTENAME_X509TOKEN      = NAMESPACEURI_PREFIX + "x509-token-profile-1.0#X509v3";
  public static final String NAMESPACEURI_WSSECURITY_ENC  = NAMESPACEURI_PREFIX + "soap-message-security-1.0#Base64Binary";
  public static final String MUST_UNDERSTAND              = "0";
  
  protected String  action;
  protected boolean trace = false;
  
  public WSSecurityHandler()
  {
  }
  
  public WSSecurityHandler(boolean trace)
  {
    this.trace = trace;
  }
  
  public WSSecurityHandler(String action)
  {
    this.action = action;
  }
  
  public WSSecurityHandler(String action, boolean trace)
  {
    this.action = action;
    this.trace  = trace;
  }
  
  @Override
  public 
  boolean handleMessage(SOAPMessageContext context) 
  {
    System.out.println("WSSecurityHandler.handleMessage(" + context + ")...");
    
    Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    if(outbound == null || !outbound.booleanValue()) {
      // Trace response...
      if(trace) {
        try {
          SOAPMessage message = context.getMessage();
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          message.writeTo(baos);
          System.out.println(new String(baos.toByteArray()));
        }
        catch(Exception ex) {
          ex.printStackTrace();
        }
      }
      
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
      
      if(action != null && action.length() > 0) {
        SOAPHeaderElement addrAction = header.addHeaderElement(new QName("http://www.w3.org/2005/08/addressing", "Action", "addr"));
        addrAction.addTextNode("http://www.dew.org/Schemas/PDD/DEW/" + action);
      }
      
      SAMLAttributeAssertion samlAssertion = new SAMLAttributeAssertion("XXXXXX01A01H501X^^^&2.16.840.1.113883.2.9.4.3.2&ISO");
      samlAssertion.setIssuer("100");
      samlAssertion.setSubjectRole("APR");
      samlAssertion.setLocality("100100");
      samlAssertion.setPurposeOfUse("TREATMENT");
      samlAssertion.setOrganizationId("100");
      samlAssertion.setOrganization("Italia");
      samlAssertion.setResourceId("RSSMRA75C03F839K^^^&2.16.840.1.113883.2.9.4.3.2&ISO");
      samlAssertion.setPatientConsent(true);
      samlAssertion.setActionId("READ");
      samlAssertion.sign(privateKey, certificate);
      
      SOAPElement soapSAMLAssertion = stringToSOAPElement(samlAssertion.toXML("saml"));
      if(soapSAMLAssertion != null) {
        header.addChildElement(soapSAMLAssertion);
      }
      
      SOAPHeaderElement addrMessageId = header.addHeaderElement(new QName("http://www.w3.org/2005/08/addressing", "MessageID", "addr"));
      addrMessageId.addTextNode("uuid:" + UUID.randomUUID());
      
      if(trace) {
        // Trace request...
        try {
          String sEnvelope = soapElementToString(envelope);
          System.out.println(sEnvelope);
        }
        catch(Exception ex) {
          ex.printStackTrace();
        }
      }
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
    
    if(trace) {
      // Trace fault...
      try {
        SOAPMessage message = context.getMessage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        System.out.println(new String(baos.toByteArray()));
      }
      catch(Exception ex) {
        ex.printStackTrace();
      }
    }
    
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
    
    QName securityHeader = new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
    HashSet<QName> headers = new HashSet<QName>(); 
    headers.add(securityHeader);
    return headers;
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
  
  protected static 
  SOAPElement stringToSOAPElement(String xmlText) 
  {
    try {
      // Load the XML text into a DOM Document
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(true);
      InputStream stream  = new ByteArrayInputStream(xmlText.getBytes());
      Document doc = builderFactory.newDocumentBuilder().parse(stream); 
      
      // Use SAAJ to convert Document to SOAPElement
      // Create SoapMessage
      MessageFactory msgFactory = MessageFactory.newInstance();
      SOAPMessage    message    = msgFactory.createMessage();
      SOAPBody       soapBody   = message.getSOAPBody();
      
      // This returns the SOAPBodyElement 
      // that contains ONLY the Payload
      return soapBody.addDocument(doc);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
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
      ex.printStackTrace();
    }
    return "";
  }
}
