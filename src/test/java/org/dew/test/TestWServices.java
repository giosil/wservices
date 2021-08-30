package org.dew.test;

import java.io.InputStream;

import java.net.URL;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
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
    boolean enableMutual = sOperation.endsWith("s");
    
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
      
      if(enableMutual) {
        // SSL/TLS Mutual Authentication
        SSLSocketFactory sslSocketFactory = getSSLSocketFactoryMutualAuth("keystore.jks", "password_kst", "password_key");
        requestContext.put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
        requestContext.put("com.sun.xml.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
      }
      
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
  
  public static
  SSLSocketFactory getSSLSocketFactoryMutualAuth(String sFile, String keystorePassword, String keyPassword)
    throws Exception
  {
    InputStream is = null;
    URL url = null;
    try {
      url = Thread.currentThread().getContextClassLoader().getResource(sFile);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    if(url != null) {
      is = url.openStream();
    }
    else {
      return null;
    }
    
    KeyStore keystore = KeyStore.getInstance("JKS");
    keystore.load(is, keystorePassword != null ? keystorePassword.toCharArray() : new char[0]);
    
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    keyManagerFactory.init(keystore, keyPassword != null ? keyPassword.toCharArray() : new char[0]);
    KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
    
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagers, null, new SecureRandom());
    
    return sslContext.getSocketFactory();
  }
}
