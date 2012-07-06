Calculator WebService Example using Apache CXF

Requeriments:
	* Java 6
	* Maven 2.x +
	* Servlet Container ( Apeche , Jetty, ...)

Build:

	mvn clean package	

Deploy & Run:

	1) Deploy the war generated : calculator-ws-web/target/calculator-ws-web-0.1-SNAPSHOT.war on your Servlet container.

	1.2) if the server:port is diferente that default used  "localhost:8080", edit the Source client and rebuld (mvn clean package)

<Example invocation with arguments >

	java -jar calculator-ws-client/target/calculator-ws-client-0.1-SNAPSHOT.jar 100 456	

