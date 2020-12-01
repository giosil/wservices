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

The class **org.dew.test.WSSecurityHandler** uses internal class **org.jcp.xml.dsig.internal.dom.XMLDSigRI** to build XML signature.

To disable restrictions of javac add *-XDignore.symbol.file* compiler argument.

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

## Contributors

* [Giorgio Silvestris](https://github.com/giosil)
