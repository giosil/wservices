package org.dew.test;

import org.dew.auth.WSSUsernameToken;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestWSSecurity extends TestCase {
  
  public TestWSSecurity(String testName) {
    super(testName);
  }
  
  public static Test suite() {
    return new TestSuite(TestWSSecurity.class);
  }
  
  public void testApp() throws Exception {
    
    WSSUsernameToken wsSUsernameToken = new WSSUsernameToken("admin", "admin");
    wsSUsernameToken.generatePasswordDigest();
    System.out.println(wsSUsernameToken.getSecurityHeader());
    
  }
}
