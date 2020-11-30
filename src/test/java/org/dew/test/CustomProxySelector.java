package org.dew.test;

import java.io.IOException;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;

import java.util.ArrayList;
import java.util.List;

/**
 * So the way a ProxySelector works is that for all Connections made, it
 * delegates to a proxySelector(There is a default we're going to override with
 * this class) to know if it needs to use a proxy for the connection.
 * <p>
 * This class was specifically created with the intent to proxy connections
 * going to the allegiance soap service.
 * </p>
 * Usage: ProxySelector.setDefault(new CustomProxySelector("test.dew.org"));
 */
public 
class CustomProxySelector extends ProxySelector 
{
  protected ProxySelector defaultProxySelector;
  protected List<Proxy> proxyList = new ArrayList<Proxy>(1);
  
  protected String noProxyHost;

  /*
   * We want to hang onto the default and delegate everything to it unless
   * it's one of the url's we need proxied.
   */
  public CustomProxySelector() 
  {
    defaultProxySelector = ProxySelector.getDefault();
    proxyList.add(Proxy.NO_PROXY);
  }
  
  /*
   * We want to hang onto the default and delegate everything to it unless
   * it's one of the url's we need proxied.
   */
  public CustomProxySelector(String noProxyHost) 
  {
    this();
    
    this.noProxyHost = noProxyHost;
  }

  @Override
  public 
  List<Proxy> select(URI uri) 
  {
    if(uri == null) {
      throw new IllegalArgumentException("URI can't be null.");
    }
    if(noProxyHost != null && noProxyHost.length() > 0) {
      if(uri.getHost().contains(noProxyHost)) {
        return proxyList;
      }
    }
    return defaultProxySelector.select(uri);
  }

  /*
   * Method called by the handlers when it failed to connect to one of the proxies returned by select().
   */
  @Override
  public 
  void connectFailed(URI uri, SocketAddress sa, IOException ioe) 
  {
    if(uri == null || sa == null || ioe == null) {
      throw new IllegalArgumentException("Arguments can't be null.");
    }
    defaultProxySelector.connectFailed(uri, sa, ioe);
  }
}
