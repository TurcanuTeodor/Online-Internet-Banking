# Online Banking System - Documentation

This is a college project demonstrating a full-stack online banking application with modern security features.

## 📚 Documentation Files

### Quick Start
- **[QUICK_START.md](QUICK_START.md)** - Get the app running in minutes
  - Pre-created test accounts
  - How to start backend & frontend
  - Basic features overview

### Technical Guides
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design and architecture diagrams
- **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** - Implementation details for all features
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - How to test the application
- **[DATABASE.md](DATABASE.md)** - Database schema and migrations
- **[SECURITY_KEYS_AND_ALGORITHMS.md](SECURITY_KEYS_AND_ALGORITHMS.md)** - Keys, tokens, encryption/hashing algorithms

## 🎯 Project Features

### Authentication & Security
- User registration and login
- JWT-based authentication with refresh tokens
- Two-factor authentication (2FA) with TOTP
- Role-based access control (Admin/User)

### Banking Features
- Multiple account types and currencies (EUR, USD, RON, GBP)
- Money transfers between accounts
- Transaction history and categorization
- Account balance tracking

### Admin Features
- View all clients and their accounts
- View all transactions across the system
- Admin dashboard with system-wide data

## 🔧 Technology Stack

### Backend
- **Java 17** with **Spring Boot 3**
- **PostgreSQL** database with Flyway migrations
- **Spring Security** for authentication
- **JWT** for token-based auth
- **TOTP** for 2FA

### Frontend
- **React 18** with **Vite**
- **TailwindCSS** for styling
- **Axios** for HTTP requests
- **React Router** for navigation

## 🚀 Getting Started

1. Read [QUICK_START.md](QUICK_START.md) to run the application
2. Check [ARCHITECTURE.md](ARCHITECTURE.md) to understand the system design
3. See [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) for code details
4. Use [TESTING_GUIDE.md](TESTING_GUIDE.md) to test features

## 📝 Pre-created Test Accounts

**Admin:**
- Email: `admin@cashtactics.com`
- Password: `password123`

**User:**
- Email: `user@cashtactics.com`
- Password: `password123`

## 🗂️ Project Structure

```
Online-Internet-Banking/
├── backend/                # Spring Boot backend
│   ├── src/main/java/     # Java source code
│   ├── src/main/resources/# Configuration & migrations
│   └── explanations/      # This documentation
├── frontend/               # React frontend
│   ├── src/               # React components
│   └── services/          # API client services
└── README.md              # Main project readme
```
