package org.dew.test;

import java.security.cert.X509Certificate;

import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;

import javax.xml.ws.handler.Handler;

// wsimport -s src -d out -p org.dew.hello.client http://localhost:8080/wservices/hello/HelloServices?wsdl
import org.dew.hello.client.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestWServices extends TestCase {
  
  public static final String CONNECT_TIMEOUT = "com.sun.xml.internal.ws.connect.timeout";
  public static final String REQUEST_TIMEOUT = "com.sun.xml.internal.ws.request.timeout";
  
  public TestWServices(String testName) {
    super(testName);
    
    // System properties proxy settings:
    // 
    // System.setProperty("http.proxyHost", "proxy.dew.org");
    // System.setProperty("http.proxyPort", "8080");
    
    // System.setProperty("http.nonProxyHosts", "localhost|*.dew.org|xxx.yyy.www.zzz");
    
    // System.setProperty("https.proxyHost", "proxy.dew.org");
    // System.setProperty("https.proxyPort", "8080");
    
    // System.setProperty("https.nonProxyHosts", "localhost|*.dew.org|xxx.yyy.www.zzz");
    
    // Programmatically proxy settings:
    // 
    // ProxySelector.setDefault(new CustomProxySelector("test.dew.org"));
    
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null;}
          public void checkClientTrusted(X509Certificate[] certs, String authType) {}
          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
      };
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      
      HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });
      
      // Disable Server Name Indication (SNI) extension of Transport Layer Security (TLS) 
      System.setProperty("jsse.enableSNIExtension", "false");
    }
    catch(Throwable th) {
      System.err.println("HttpsURLConnection.setDefaultSSLSocketFactory: " + th);
      return;
    }
  }
  
  public static Test suite() {
    return new TestSuite(TestWServices.class);
  }
  
  public void testApp() throws Exception {
    
    String sOperation = System.getProperty("dew.test.op", "");
    
    if(sOperation == null || sOperation.length() == 0) {
      System.out.println("dew.test.op not setted (ex. -Ddew.test.op=hello)");
      return;
    }
    
    boolean traceEnabled = true;
    
    Hello_Service service = new Hello_Service();
    
    IHelloService hello = service.getHelloServicesPort();
    
    if(hello instanceof BindingProvider) {
      
      BindingProvider bindingProvider = (BindingProvider) hello;
      
      Map<String, Object> requestContext = bindingProvider.getRequestContext();
      
      requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:8080/wservices/hello/HelloServices?g=Ciao");
      // Basic Auth
      requestContext.put(BindingProvider.USERNAME_PROPERTY, "admin");
      requestContext.put(BindingProvider.PASSWORD_PROPERTY, "admin");
      // Timetout
      requestContext.put(REQUEST_TIMEOUT, 20000); // ms
      requestContext.put(CONNECT_TIMEOUT, 20000); // ms
      
      // Add handler chain programmatically
      Binding binding = bindingProvider.getBinding();
      
      @SuppressWarnings("rawtypes")
      List<Handler> handlerChain = binding.getHandlerChain();
      
      handlerChain.add(new WSSecurityHandler(traceEnabled));
      
      // ... If the returned chain is modified a call to setHandlerChainis required to configure the binding instance with the new chain.
      binding.setHandlerChain(handlerChain);
    }
   
    String result = hello.hello("World");
    
    System.out.println(result);
  }
}
