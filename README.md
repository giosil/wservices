# WServices

Boilerplate for web services implementation with security handler chain example.

## Build

- `git clone https://github.com/giosil/wservices.git`
- `mvn clean install`

## Test Client

For more details see src/test/java.

```java
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
  
  // Add handler chain programmatically
  Binding binding = bindingProvider.getBinding();
  
  @SuppressWarnings("rawtypes")
  List<Handler> handlerChain = binding.getHandlerChain();
  
  handlerChain.add(new WSSecurityHandler(traceEnabled));
  
  // ... If the returned chain is modified a call to setHandlerChainis 
  // required to configure the binding instance with the new chain.
  binding.setHandlerChain(handlerChain);
}

String result = hello.hello("World");
```

## Generate Client

`wsimport -s src -d out -p org.dew.hello.client http://localhost:8080/wservices/hello/HelloServices?wsdl`

## Notice

If you use **org.jcp.xml.dsig.internal.dom.XMLDSigRI** in **org.dew.test.WSSecurityHandler** you have to disable restrictions of javac adding *-XDignore.symbol.file* in compiler arguments.

```xml
...
<plugins>
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.3</version>
    <configuration>
      <source>1.8</source> 
      <target>1.8</target> 
      <fork>true</fork>
      <compilerArgs>
        <arg>-XDignore.symbol.file</arg>
      </compilerArgs>
    </configuration>
  </plugin>
</plugins>
...
```

Alternatively it is recommended to use **org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI** of Apache Santuario&trade; (https://santuario.apache.org/).

```xml
...
    <dependency>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
      <version>1.1.1</version>
    </dependency>
    <dependency>
      <groupId>org.apache.santuario</groupId>
      <artifactId>xmlsec</artifactId>
      <version>1.5.7</version>
    </dependency>
...
```

## Enabling SSL/TLS Mutual Authentication in JBoss / Wildfly

Edit standalone.xml:

- Copy application.keystore and client.keystore in $JBOSS_HOME/standalone/configuration folder;
- Modify keystore configuration;
- Add truststore configuration in authentication;
- Add verify-client="REQUIRED" attribute in https-listener tag.

```xml
...
        <security-realms>
            ...
            <security-realm name="ApplicationRealm">
                <server-identities>
                    <ssl>
                        <keystore path="application.keystore" relative-to="jboss.server.config.dir" keystore-password="password" alias="server" key-password="password" generate-self-signed-certificate-host="localhost"/>
                    </ssl>
                </server-identities>
                <authentication>
                    <truststore path="client.keystore" relative-to="jboss.server.config.dir" keystore-password="password"/>
                    ...
                </authentication>
                ...
            </security-realm>
        </security-realms>
...
        <subsystem xmlns="urn:jboss:domain:undertow:12.0" ... >
            ...
            <server name="default-server">
                ...
                <https-listener name="https" socket-binding="https" security-realm="ApplicationRealm" verify-client="REQUIRED" enable-http2="true"/>
                ...
            </server>
            ...
        </subsystem>
...
```

Get client certificate from HttpServletRequest

```java
  ...
  @Resource
  protected WebServiceContext webServiceContext;
  ...
  
  // Method implementation
  MessageContext messageContext = webServiceContext.getMessageContext();
  
  HttpServletRequest servletRequest = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
  
  X509Certificate[] certificates = (X509Certificate[]) httpServletRequest.getAttribute("javax.servlet.request.X509Certificate");
```

## Enabling SSL/TLS debugging

`mvn test -DargLine="-Ddew.test.op=hello_s -Djavax.net.debug=all"`

`mvn test -DargLine="-Ddew.test.op=hello_s -Djavax.net.debug=ssl,handshake"`

`mvn test -DargLine="-Ddew.test.op=hello_s -Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager -Djava.security.debug=access:stack"`

`mvn test -DargLine="-Ddew.test.op=hello_s -Djavax.net.debug=ssl:record:plaintext"`

## Apache CXF Logging configuration in JBoss / Wildfly

To enable logging in standalone.xml:

```xml
...
    </extensions>
    <system-properties>
        <property name="org.apache.cxf.logging.enabled" value="true"/>
    </system-properties>
    <management>
...
```

To disable printing Fault stacktrace in standalone.xml:

```xml
...
        <subsystem xmlns="urn:jboss:domain:logging:8.0">
...
            <logger category="org.apache.cxf.phase.PhaseInterceptorChain">
                <level name="ERROR"/>
            </logger>
...
        </subsystem>
...
```

To disable printing INFO service client creation in standalone.xml:

```xml
...
        <subsystem xmlns="urn:jboss:domain:logging:8.0">
...
            <logger category="org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean">
                <level name="ERROR"/>
            </logger>
...
        </subsystem>
...
```

## Contributors

* [Giorgio Silvestris](https://github.com/giosil)
