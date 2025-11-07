# Online-Internet-Banking (Backend - Spring Boot)

Small Spring Boot backend for an online banking system. Provides REST endpoints for clients, accounts and transactions and uses Spring Data JPA repositories.

## Requirements
- Java 17+ (or the version configured for the project)
- Maven (or use the bundled Maven wrapper `mvnw.cmd`)
- A relational database (configure via `src/main/resources/application.properties`)

## Quick start (Windows)
1. Configure your datasource in `src/main/resources/application.properties`.
2. From project root:
   - Run with Maven wrapper:
     ```
     .\mvnw.cmd spring-boot:run
     ```
   - Or build and run jar:
     ```
     mvn package
     java -jar target\*.jar
     ```

## Tests
Run unit/integration tests:
```
mvn test
```
or
```
.\mvnw.cmd test
```

## Inferred REST endpoints (common usages)
Note: these are inferred from controllers in the project. Adjust paths/fields if your controllers differ.

- Clients
  - GET  /clients
    - curl: `curl -s -X GET "http://localhost:8080/clients" -H "Accept: application/json"`
  - GET  /clients/{id}
    - curl: `curl -s -X GET "http://localhost:8080/clients/123" -H "Accept: application/json"`
  - POST /clients
    - curl:
      ```
      curl -s -X POST "http://localhost:8080/clients" -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe","clientType":"INDIVIDUAL","contactInfo":{"email":"john@example.com"}}'
      ```
  - PUT  /clients/{id}
    - curl: `curl -s -X PUT "http://localhost:8080/clients/123" -H "Content-Type: application/json" -d '{"firstName":"Jane"}'`
  - DELETE /clients/{id}
    - curl: `curl -s -X DELETE "http://localhost:8080/clients/123"`

- Accounts
  - GET  /accounts
    - curl: `curl -s -X GET "http://localhost:8080/accounts"`
  - GET  /accounts/{id}
    - curl: `curl -s -X GET "http://localhost:8080/accounts/456"`
  - POST /accounts
    - curl:
      ```
      curl -s -X POST "http://localhost:8080/accounts" -H "Content-Type: application/json" -d '{"accountNumber":"RO49...","clientId":123,"currency":"EUR","balance":0.0}'
      ```
  - PUT  /accounts/{id}
    - curl: `curl -s -X PUT "http://localhost:8080/accounts/456" -H "Content-Type: application/json" -d '{"balance":1000.5}'`
  - DELETE /accounts/{id}
    - curl: `curl -s -X DELETE "http://localhost:8080/accounts/456"`

- Transactions
  - GET  /transactions
    - curl: `curl -s -X GET "http://localhost:8080/transactions"`
  - GET  /transactions/{id}
    - curl: `curl -s -X GET "http://localhost:8080/transactions/789"`
  - POST /transactions
    - curl:
      ```
      curl -s -X POST "http://localhost:8080/transactions" -H "Content-Type: application/json" -d '{"fromAccountId":456,"toAccountId":457,"amount":250.00,"currency":"EUR","transactionType":"TRANSFER","description":"Payment"}'
      ```

## Configuration
Edit `src/main/resources/application.properties` to set:
- spring.datasource.url
- spring.datasource.username
- spring.datasource.password
- spring.jpa.* settings as needed

## Project structure (high level)
- src/main/java/ro/app/backend_Java_SpringBoot
  - controller/ — ClientController, AccountController, TransactionController
  - service/ — business logic
  - repository/ — Spring Data JPA repositories
  - model/ / DTO / mapper / exception

## Notes
- Endpoints and sample request bodies are inferred from the project structure. For exact payloads and validation rules consult controller and DTO classes.
- For production, secure endpoints (authentication/authorization), enable input validation and configure connection pooling and migrations.

License: (add your license)
