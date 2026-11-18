// src/App.jsx
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { PagosPage } from './pages/PagosPage';
import { CallbackPage } from './pages/CallbackPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/pagos" element={<PagosPage />} />
        <Route path="/callback" element={<CallbackPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
