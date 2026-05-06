package ro.app.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe externalized configuration for downstream service routing.
 * Maps gateway routes to backend service URLs.
 * 
 * Binds to properties prefixed with "services.*" in application.properties or application-*.yml
 */
@Component
@ConfigurationProperties(prefix = "services")
public class RoutingProperties {

    private Auth auth = new Auth();
    private Client client = new Client();
    private Account account = new Account();
    private Transaction transaction = new Transaction();
    private Payment payment = new Payment();
    private Fraud fraud = new Fraud();

    public static class Auth {
        private String url = "http://localhost:8081";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Client {
        private String url = "http://localhost:8082";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Account {
        private String url = "http://localhost:8083";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Transaction {
        private String url = "http://localhost:8084";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Payment {
        private String url = "http://localhost:8085";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Fraud {
        private String url = "http://localhost:8086";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public Fraud getFraud() { return fraud; }
    public void setFraud(Fraud fraud) { this.fraud = fraud; }
}
