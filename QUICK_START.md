# CashTactics - Quick Start Guide

## 🔐 Pre-created Admin & User Accounts

After running the backend, you'll have these accounts:

### Admin Account
- **Email:** `admin@cashtactics.com`
- **Password:** `password123`
- **Access:** Admin Dashboard at `/admin` (view all clients, transactions)

### User Account
- **Email:** `user@cashtactics.com`
- **Password:** `password123`
- **Access:** Regular Dashboard at `/dashboard`

## 🚀 Starting the Application

### 1. Start Backend (Port 8443)
```powershell
cd D:\Users\Theodor\Desktop\Online-Internet-Banking\backend
.\mvnw.cmd spring-boot:run
```

### 2. Start Frontend (Port 5174)
```powershell
cd D:\Users\Theodor\Desktop\Online-Internet-Banking\frontend
npm run dev
```

### 3. Open in Browser
Navigate to: **http://localhost:5174**

## 📝 Creating New Accounts

### Option 1: Register via UI
1. Go to http://localhost:5174/register
2. Enter a Client ID (1-25 for sample clients)
3. Enter email/username and password
4. Click "Create Account"

### Option 2: Use Sample Data
The database has 25 pre-created clients (from migration V10). You can create user accounts for any of them.

## 🎯 Features

### Regular User Dashboard
- View account information
- Transaction history
- Transfer money

### Admin Dashboard (`/admin`)
- **View All Clients** - See all 25 clients with their details
- **View All Transactions** - See all transactions across all accounts
- Real-time data from views (read-only)

## 🗃️ Database Info

- **Database:** `banking` (PostgreSQL)
- **Port:** 5432
- **User:** postgres
- **Password:** Tglstmai8

Sample data includes:
- 25 clients
- 50 accounts (multiple currencies: EUR, USD, RON, GBP)
- 50 transactions

## 🔑 API Endpoints

Base URL: `https://localhost:8443/api`

### Auth
- `POST /auth/login` - Login
- `POST /auth/register` - Register
- `POST /auth/2fa/setup` - Setup 2FA
- `POST /auth/2fa/verify` - Verify 2FA code

### Admin Views
- `GET /clients/view` - All clients
- `GET /transactions/view-all` - All transactions

## 🛡️ Role-Based Access

The app automatically redirects based on role after login:
- **ADMIN** → `/admin` (Admin Dashboard)
- **USER** → `/dashboard` (User Dashboard)
