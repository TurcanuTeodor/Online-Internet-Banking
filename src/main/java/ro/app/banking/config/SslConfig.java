package ro.app.banking.config;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SslConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>{

    @Override
    public void customize(TomcatServletWebServerFactory factory){
        Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        httpConnector.setScheme("http");
        httpConnector.setPort(8080);
        httpConnector.setSecure(false);
        httpConnector.setRedirectPort(8443);
        factory.addAdditionalTomcatConnectors(httpConnector);
    }
    
}
