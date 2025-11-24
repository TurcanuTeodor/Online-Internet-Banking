# Online Internet Banking — Backend (Java Spring Boot)

A Spring Boot backend system simulating the core logic of an online banking application.  
The project provides RESTful APIs for managing clients, accounts, and transactions, built with a layered architecture and PostgreSQL persistence.

## Table of Contents

- [Project Overview](#project-overview)  
- [Architecture](#architecture)  
- [Key Features](#key-features)  
- [Technology Stack](#technology-stack)  
- [Project Structure](#project-structure)  
- [Database Design](#database-design)  
- [Requirements](#requirements)  
- [Setup and Configuration](#setup-and-configuration)  
- [Running the Application](#running-the-application)  
- [REST API — Endpoints and Examples](#rest-api---endpoints-and-examples)  
  - [Client Endpoints](#client-endpoints)  
  - [Account Endpoints](#account-endpoints)  
  - [Transaction Endpoints](#transaction-endpoints)  
- [How It Works Internally](#how-it-works-internally)  
- [Validation and Error Handling](#validation-and-error-handling)

---

## Project Overview

This backend simulates the core functionality of an online banking platform, including:

- Client management: Create, view, update, and soft-delete clients  
- Account operations: Open/close accounts, deposit, withdraw, transfer funds, and check balances  
- Transaction records: Log transactions automatically and generate aggregated summaries

The project is implemented with Java Spring Boot, using Spring Data JPA for ORM and PostgreSQL as the database layer. It follows a clean, layered design pattern that separates controller logic, business logic, and data persistence.

## Architecture

[ Controller ]  →  [ Service ]  →  [ Repository ]  →  [ Entity (Model) ]  →  [ Database ]  
     ↑  
   [ DTO ↔ Mapper ]

- Controller — Defines REST endpoints and request mappings  
- Service — Implements business logic and validations  
- Repository — Handles data access with Spring Data JPA  
- DTOs & Mappers — Transfer data between layers cleanly  
- Model (Entities) — Define the database tables  
- Exception Handling — Provides structured error responses

## Key Features

- Full CRUD operations for clients, including contact-info updates
- Account management APIs: open/close accounts, list accounts by client, and check balance
- Core bankng operations with balance tracking: deposit, withdrawal, and transfer
- Transaction logging for every balance operation, plus advanced read-only filtering endpoints
- IBAN generator for new accounts   
- Native SQL aggregation for daily transaction totals  
- Database views for read-only access  
- Centralized exception handling and DTO-based validation  
- Easily extendable architecture for future additions (authentication, reports, etc.)

## Technology Stack

Layer | Technology
--- | ---
Language | Java 17
Framework | Spring Boot 3.x
Persistence | Spring Data JPA
Database | PostgreSQL
Build Tool | Maven
Data Format | JSON (REST API)
Tools | VS Code / IntelliJ / Postman

## Project Structure

src/main/java/ro/app/backend_Java_SpringBoot

- BackendJavaSpringBootApplication.java — Entry point

- controller/ — REST controllers  
  - ClientController.java  
  - AccountController.java  
  - TransactionController.java

- service/ — Business logic layer  
  - ClientService.java  
  - AccountService.java  
  - TransactionService.java

- repository/ — Data access layer (JPA Repositories)  
  - ClientRepository.java  
  - AccountRepository.java  
  - TransactionRepository.java  
  - CurrencyTypeRepository.java  
  - TransactionTypeRepository.java  
  - ViewAccountRepository.java  
  - ViewClientRepository.java  
  - ViewTransactionRepository.java

- model/ — Entities and database mappings  
  - ClientTable.java  
  - AccountTable.java  
  - TransactionTable.java  
  - Lookup entities: CurrencyType, TransactionType, SexType, ClientType  
  - Read-only views: ViewClientTable, ViewAccountTable, ViewTransactionTable

- dto/ — Data Transfer Objects  
  - ClientDTO.java  
  - AccountDTO.java  
  - TransactionDTO.java

- dto/mapper/ — Mappers between DTOs and Entities  
  - ClientMapper.java  
  - AccountMapper.java  
  - TransactionMapper.java

- dto/request/ — Request models for endpoints  
  - OpenAccountRequest.java  
  - TransferRequest.java  
  - AmountRequest.java

- exception/  
  - GlobalExceptionHandler.java  
  - ResourceNotFoundException.java  
  - ErrorResponse.java

## Database Design

The database includes 8 tables and 3 view tables.

Main Tables
- client — stores personal data and client type  
- cont — account details with currency and IBAN  
- tranzactie — transaction logs (amount, type, date, references)

Lookup Tables
- tip_client  
- tip_tranzactie  
- valuta  
- tip_sex

Auxiliary Tables
- date_de_contact

Views (Read-only)
- view_client  
- view_account  
- view_transaction

## Requirements

- Java 17+  
- Maven 3.6+  
- PostgreSQL 14+  
- IDE: VS Code / IntelliJ / Eclipse  
- Internet connection (for dependencies)

## Setup and Configuration

Edit the file: src/main/resources/application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cibernetica?currentSchema=public
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
server.port=8080
```

Adjust the database credentials if needed. Ensure the schema exists — ddl-auto=validate means tables must already exist.

## Running the Application

Option 1: Maven Wrapper

```
mvnw spring-boot:run
```

Option 2: Package and Run

```
mvn clean package
java -jar target/backend_Java_SpringBoot-0.0.1-SNAPSHOT.jar
```

Server starts at: http://localhost:8080

## REST API — Endpoints and Examples

### Client Endpoints

Method | Endpoint | Description
--- | --- | ---
POST | /api/clients | Create a new client
GET | /api/clients/search?name={name} | Search clients by name
PUT | /api/clients/{id}/contact | Update contact info
DELETE | /api/clients/{id} | Soft delete client
GET | /api/clients/view | Get all clients (read-only view)

Example — Create Client

```json
{
  "firstName": "Maria",
  "lastName": "Popescu",
  "email": "maria.popescu@example.com",
  "clientTypeCode": "PF",
  "sexCode": "F"
}
```

### Account Endpoints

Method | Endpoint | Description
--- | --- | ---
POST | /api/accounts/open | Open a new account
POST | /api/accounts/{iban}/deposit | Deposit funds
POST | /api/accounts/{iban}/withdraw | Withdraw funds
POST | /api/accounts/transfer | Transfer between accounts
GET | /api/accounts/view | List all accounts (read-only view)

Example — Transfer

```json
{
  "sourceIban": "RORON123456789",
  "targetIban": "ROEUR987654321",
  "amount": 1000.00
}
```

### Transaction Endpoints

Method | Endpoint | Description
--- | --- | ---
GET | /api/transactions/view | Get all transactions
GET | /api/transactions/account/{iban} | Transactions for an account
GET | /api/transactions/filter?... | Filter by client, date, or type
GET | /api/transactions/daily-totals | Get daily totals (aggregation)

## How It Works Internally

The application follows a layered flow to process each request:

1. Request sent to the Controller  
   - A REST endpoint (for example, /api/accounts/transfer) receives the HTTP request.  
   - The Controller validates input data and passes it to the corresponding Service layer.

2. Service layer processes the logic  
   - The Service contains the business rules:  
     - For account transfers, it verifies that: both source and target IBANs exist; the source account has sufficient balance; the currency and client details are valid.  
     - It then updates both account balances and creates a new transaction record.

3. Repository handles database communication  
   - The Service calls JPA Repositories (e.g., AccountRepository, TransactionRepository).  
   - These repositories handle save(), findBy...(), and update() operations using Spring Data JPA.

4. Entities and database tables  
   - Each entity (e.g., AccountTable, TransactionTable) maps directly to a table in the PostgreSQL database.  
   - Hibernate automatically translates entity changes into SQL queries.

5. DTOs and Mappers  
   - Data Transfer Objects (DTOs) ensure that only required fields are sent to or received from the client.  
   - Mappers (e.g., AccountMapper) convert between entity objects and DTOs.

6. Response returned to the client  
   - The Service sends a processed result (DTO) back to the Controller.  
   - The Controller sends a clean JSON response to the client (Postman or frontend application).

This structure ensures clean separation of concerns, reusability, and testability across all layers.

## Validation and Error Handling

Input fields are validated via Spring annotations (@NotNull, @Email, etc.). Invalid data or missing resources trigger structured JSON error responses from GlobalExceptionHandler.

Example error:

```json
{
  "timestamp": "2025-11-08T12:30:15",
  "status": 404,
  "error": "Resource not found",
  "message": "Client with ID 15 not found"
}
```