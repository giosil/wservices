package org.dew.test;

import org.dew.auth.WSSUsernameToken;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestWServices extends TestCase {
  
  public TestWServices(String testName) {
    super(testName);
  }
  
  public static Test suite() {
    return new TestSuite(TestWServices.class);
  }
  
  public void testApp() throws Exception {
    
    WSSUsernameToken wsSUsernameToken = new WSSUsernameToken("admin", "admin");
    wsSUsernameToken.generatePasswordDigest();
    System.out.println(wsSUsernameToken.getSecurityHeader());
    
  }
}
