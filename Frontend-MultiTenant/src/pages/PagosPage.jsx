
import React, { useState, useEffect, useRef } from 'react';
import { TokenSender } from '../TokenSender';
import { useIsAuthenticated, useMsal } from "@azure/msal-react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from 'react-router-dom';

export function PagosPage() {
    const isAuthenticatedAzure = useIsAuthenticated();
    const { accounts } = useMsal();
    const oidcAuth = useAuth();
    const navigate = useNavigate();

    const isAuthenticated = isAuthenticatedAzure || oidcAuth.isAuthenticated;
    const proveedor = isAuthenticatedAzure ? 'Azure Entra ID' : 'OpenID Connect (OIDC)';

    const [clientId, setClientId] = useState('');
    const [amount, setAmount] = useState('');
    const [paymentLoading, setPaymentLoading] = useState(false);
    const [paymentMessage, setPaymentMessage] = useState('');
    const [isBlockedUI, setIsBlockedUI] = useState(false);
    const [logs, setLogs] = useState([]);

    // Auto-asignar y limpiar el clientId según la sesión activa (Azure o OIDC)
    useEffect(() => {
        if (!isAuthenticated) {
            navigate('/login');
            return;
        }

        if (isAuthenticatedAzure && accounts.length > 0) {
            const emailAzure = accounts[0].username || accounts[0].name || "cliente-azure";
            setClientId(emailAzure.replace(/[^a-zA-Z0-9-_]/g, "_"));
        } else if (oidcAuth.isAuthenticated && oidcAuth.user?.profile) {
            const emailOidc = oidcAuth.user.profile.email || oidcAuth.user.profile.preferred_username || "cliente-oidc";
            setClientId(emailOidc.replace(/[^a-zA-Z0-9-_]/g, "_"));
        }
    }, [isAuthenticated, isAuthenticatedAzure, accounts, oidcAuth, navigate]);

    const addLog = (message) => {
        const time = new Date().toLocaleTimeString();
        setLogs(prev => [...prev.slice(-19), `[${time}] ${message}`]);
    };

    const handleInitiatePayment = async () => {
        if (!amount || Number(amount) <= 0) {
            setPaymentMessage("Por favor ingresa un monto válido.");
            return;
        }

        setPaymentLoading(true);
        setPaymentMessage('');

        try {
            // Extraer token de acceso según el proveedor activo
            const token = isAuthenticatedAzure ? "azure-token-mock" : oidcAuth.user?.access_token;

            const response = await fetch('http://localhost:8083/api/pagos/iniciar', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    clientId: clientId,
                    amount: Number(amount),
                    transactionId: `TX-${Date.now()}`
                })
            });

            const data = await response.json();

            if (!response.ok) {
                // Si el backend responde con bloqueo temporal de 30 minutos
                setIsBlockedUI(true);
                setPaymentMessage(data.message || "Demasiados intentos. Estás bloqueado temporalmente por 30 minutos.");
                addLog(`Acceso denegado/bloqueado temporalmente para: ${clientId}`);
                return;
            }

            setIsBlockedUI(false);
            addLog(`Pago inicializado correctamente ($${amount}). Redirigiendo a WebPay...`);
            
            // Redirección POST automática hacia Transbank WebPay Plus
            redirectToWebPay(data.url, data.token);

        } catch (error) {
            setPaymentMessage("Error de conexión con el microservicio de pagos.");
            addLog("Error en pasarela: " + error.message);
        } finally {
            setPaymentLoading(false);
        }
    };

    const redirectToWebPay = (url, tokenWs) => {
        const form = document.createElement('form');
        form.action = url;
        form.method = 'POST';

        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'token_ws';
        input.value = tokenWs;

        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    };

    if (!isAuthenticated) return null;

    return (
        <div style={styles.body}>
            <div style={styles.container}>
                {/* Cabecera Corporativa */}
                <header style={styles.header}>
                    <div>
                        <h2 style={{ margin: 0, color: '#f8fafc' }}>Portal de Pagos WebPay Plus</h2>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>
                            Sesión activa con: <strong style={{ color: '#38bdf8' }}>{proveedor}</strong>
                        </span>
                    </div>
                    <span style={{ ...styles.status, backgroundColor: '#22c55e' }}>
                        Autenticado
                    </span>
                </header>

                {/* Identidad del Cliente */}
                <div style={styles.configBox}>
                    <div style={{ flex: 1 }}>
                        <label style={{ display: 'block', fontSize: '12px', color: '#94a3b8', marginBottom: '5px' }}>ID de Cliente (Sanitizado)</label>
                        <input 
                            type="text" 
                            value={clientId}
                            disabled
                            style={{ ...styles.input, backgroundColor: '#111827', cursor: 'not-allowed', width: '100%' }}
                        />
                    </div>
                </div>

                {/* Módulo Principal de Pago */}
                <div style={{ ...styles.configBox, flexDirection: 'column', alignItems: 'stretch', background: '#111827' }}>
                    <h3 style={{ margin: '0 0 10px 0', color: '#38bdf8' }}>Nueva Transacción</h3>
                    <p style={{ fontSize: '14px', color: '#94a3b8', marginTop: 0 }}>
                        Ingresa el monto. El sistema validará tus intentos previos en base de datos.
                    </p>
                    
                    <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                        <input 
                            type="number" 
                            placeholder="Monto (Ej: 15000)" 
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                            disabled={isBlockedUI || paymentLoading}
                            style={styles.input}
                        />
                        <button 
                            onClick={handleInitiatePayment} 
                            disabled={isBlockedUI || paymentLoading}
                            style={{ 
                                ...styles.buttonBlue, 
                                backgroundColor: isBlockedUI ? '#64748b' : '#10b981',
                                cursor: isBlockedUI ? 'not-allowed' : 'pointer'
                            }}
                        >
                            {paymentLoading ? 'Verificando estado...' : isBlockedUI ? 'Bloqueado Temporalmente' : 'Pagar con WebPay'}
                        </button>
                    </div>

                    {paymentMessage && (
                        <div style={{ marginTop: '15px', padding: '12px', borderRadius: '6px', backgroundColor: isBlockedUI ? '#7f1d1d' : '#1e3a8a', color: '#fff', fontSize: '0.9rem' }}>
                            {isBlockedUI ? '⚠️ ' : 'ℹ️ '} {paymentMessage}
                        </div>
                    )}
                </div>

                {/* TokenSender opcional si lo requieres en tu vista */}
                {/* <TokenSender /> */}

                {/* Registro de Eventos / Logs */}
                <h3 style={{ marginTop: '30px' }}>Registro de Actividad</h3>
                <div style={styles.logBox}>
                    {logs.length === 0 ? (
                        <div style={{ color: '#475569' }}>Sin actividad reciente en esta sesión...</div>
                    ) : (
                        logs.map((log, index) => (
                            <div key={index}>{log}</div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
}
const styles = {
    body: { backgroundColor: '#0f172a', color: '#f8fafc', minHeight: '100vh', padding: '40px 20px', fontFamily: 'Arial, sans-serif' },
    container: { maxWidth: '800px', margin: '0 auto' },
    header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #334155', paddingBottom: '15px', marginBottom: '20px' },
    status: { padding: '6px 12px', borderRadius: '20px', fontSize: '0.85rem', fontWeight: 'bold', color: 'white' },
    configBox: { background: '#1e293b', padding: '20px', borderRadius: '8px', marginBottom: '25px', display: 'flex', gap: '15px', flexWrap: 'wrap', border: '1px solid #334155' },
    input: { flex: 1, padding: '10px', borderRadius: '6px', border: '1px solid #475569', background: '#0f172a', color: 'white', minWidth: '220px' },
    buttonBlue: { padding: '10px 20px', borderRadius: '6px', border: 'none', background: '#3b82f6', color: 'white', fontWeight: 'bold', cursor: 'pointer' },
    logBox: { background: '#090d16', border: '1px solid #1e293b', borderRadius: '8px', padding: '15px', height: '150px', overflowY: 'auto', fontFamily: 'monospace', fontSize: '0.85rem', color: '#94a3b8' }
};

