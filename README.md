# WServices

Boilerplate for web services implementation with security handler chain example.

## Build

- `git clone https://github.com/giosil/wservices.git`
- `mvn clean install`

## Generate Client

`wsimport -s src -d out -p org.dew.hello.client http://localhost:8080/wservices/hello/HelloServices?wsdl`

## Contributors

* [Giorgio Silvestris](https://github.com/giosil)
