import { Routes, Route, Navigate } from 'react-router-dom';
import TwoFactorVerify from './pages/TwoFactorVerify';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AdminDashboard from './pages/AdminDashboard';
import Profile from './pages/Profile';
import { isAuthenticated } from '../services/authService';

function PrivateRoute({ children }) {
  return isAuthenticated() ? children : <Navigate to="/login" replace />;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/2fa-verify" element={<TwoFactorVerify />} />
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <Navigate to="/dashboard/accounts" replace />
          </PrivateRoute>
        }
      />
      <Route
        path="/dashboard/:section"
        element={
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <PrivateRoute>
            <Profile />
          </PrivateRoute>
        }
      />
      <Route
        path="/admin"
        element={
          <PrivateRoute>
            <AdminDashboard />
          </PrivateRoute>
        }
      />
      <Route path="/" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
