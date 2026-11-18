import { useIsAuthenticated, useMsal } from "@azure/msal-react";
import { Profiler, useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router-dom";

export function DashboardPage(){
    const isAuthenticatedAzure= useIsAuthenticated();
    const {accounts} = useMsal();
    const cognito= useAuth();
    const navigate= useNavigate();

    const isAuthenticated= isAuthenticatedAzure || cognito.isAuthenticated;
    const proveedor= isAuthenticatedAzure? "Azure Entra ID": "AWS Cognito";

    const [wsEnpoint, setWsEndpoint]=useState("");
    const [clientId, setClientId]= useState("");
    const [isConnected, setIsConnected]= useState(false);
    const [trucksData, setTrucksData]= useState({});
    const [logs, setLogs]= useState([]);
    const socketRef= useRef(null);
    useEffect(()=>{
        if(!isAuthenticated){
            navigate("/login");
            return;
        }
        if(isAuthenticatedAzure && accounts.length >0){
            const emailAzure= accounts[0].username || accounts[0].name || "cliente-azure";
            setClientId(emailAzure.replace(/[^a-zA-Z0-9-_]/g,"_"));
        }else if(cognito.isAuthenticated && cognito.user?.profile){
            const emailCognito= cognito.user.profile.email || cognito.user.profile.preferred_username || "cliente-cognito";
            setClientId(emailCognito);
        }
    }, [isAuthenticated, isAuthenticatedAzure,accounts, cognito,navigate]);

    const addLog=(message) =>{
        const time= new Date().toLocaleDateString();
        setLogs(prev=> [...prev.slice(-19),`[${time}] ${message}`]);
    }
    const connectWebSocket=()=>{
        if(!wsEnpoint){
            alert("Debe agregar el endpoint de websocket");
            return ;

        }
        if(!clientId){
            alert("Debe agregar cliente ");
        }
        if (socketRef.current){
            socketRef.current.close();
        }
        const urlWithParams = `${wsEnpoint}?clientId=${encodeURIComponent(clientId)}`;
        addLog("conectando con web socket");
        const ws= new WebSocket(urlWithParams);
        ws.onopen=()=>{
            setIsConnected(true);
            addLog("Conexion exitosa");
        }

        ws.onmessage=(event)=>{
            try {
                addLog("Datos Recibidos")
                const batch = JSON.parse(event.data);
                setTrucksData(prev=>{
                    const update = {...prev};
                    batch.forEach(telemetry => {
                        update[telemetry.camionId]= telemetry;
                    });
                    return update;
                })
            } catch (error) {
                addLog("error en la recepcion de mensajes");
            }
        }
        ws.onclose=()=>{
            setIsConnected(false);
            addLog("Cerrando conexion ¡");
        }
        ws.onerror=(error)=>{
            addLog("error en el web socket "+ JSON.stringify(error));
        }
    }

    const disconnectWebSocket=()=>{
        if(socketRef.current){
            socketRef.current.close();
            socketRef.current= null;
        }
    }

    useEffect(()=>{
        return()=>{
            if(socketRef.current){ 
                socketRef.current.close();
            }
        }
    }, []);

    if(!isAuthenticated) return null;

    return(
        <div style={styles.body}>
             <div style={styles.container}>
                <header style={styles.header}>
                        <div>
                        <h2 style={{ margin: 0, color: '#f8fafc' }}>Dashboard Cloud OIDC & Telemetría</h2>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>
                        Sesión activa con: <strong style={{ color: '#38bdf8' }}>{proveedor}</strong>
                        </span>
                    </div>
                    <span style={{ ...styles.status, backgroundColor: isConnected ? '#22c55e' : '#ef4444' }}>
                        {isConnected ? 'WebSocket Conectado' : 'WebSocket Desconectado'}
                    </span>
                </header>
                {/* Configuración de Conexión WebSocket */}
                <div style={styles.configBox}>
                    <input
                    type="text"
                    placeholder="wss://tu-api-id.execute-api.region.amazonaws.com/prod"
                    value={wsEndpoint}
                    onChange={(e) => setWsEndpoint(e.target.value)}
                    style={styles.input}
                    />
                <input
                    type="text"
                    placeholder="ID de Cliente (Auto-asignado por sesión)"
                    value={clientId}
                    onChange={(e) => setClientId(e.target.value)}
                    style={{ ...styles.input, maxWidth: '250px' }}
                                />
                <button onClick={connectWebSocket} style={styles.buttonBlue}>Conectar
                WebSocket</button>
                <button onClick={disconnectWebSocket}
                style={styles.buttonRed}>Desconectar</button>
            </div>
            {/* Grid de Camiones en Tiempo Real */}
            <h2 style={{ color: "white" }}>Camiones Activos</h2>
            <div style={styles.grid}>
                {Object.keys(trucksData).length === 0 ? (
                <p style={{ color: '#64748b' }}>Esperando datos de telemetría en vivo para este
                cliente...</p>
                ) : (
                Object.values(trucksData).map(t => (
                <div key={t.camionId} style={styles.card}>
                    <h3 style={{ color: '#60a5fa', marginTop: 0 }}>Camión: {t.camionId || 'N/D'}</h3>
                    <div style={styles.metric}><span>ID Registro:</span> <strong>{t.id ||
                    'N/D'}</strong></div>
                <div style={styles.metric}>
                    <span>Temperatura:</span>
                    <strong style={{ color: t.temperatura > 15 ? '#ef4444' : '#38bdf8' }}>
                    {t.temperatura ?? 0} °C
                </strong>
            </div>
            <div style={styles.metric}><span>Humedad:</span> <strong>{t.humedad ?? 0}
                %</strong></div>
            <div style={styles.metric}><span>Estado:</span> <strong>{t.estado ||
            'N/D'}</strong></div>
            <div style={styles.metric}>
            <small style={{ color: '#94a3b8' }}>
            Actualizado: {new Date(t.timestamp).toLocaleTimeString()}
            </small>
            </div>
            </div>
            ))
            )}
            </div>
        {/* Registro de Eventos / Logs */}
        <h3 style={{ marginTop: '30px' }}>Registro de Eventos</h3>
        <div style={styles.logBox}>
            {logs.map((log, index) => (
            <div key={index}>{log}</div>
            ))}
        </div>
    </div>
</div>
);
}
// Estilos limpios adaptados al diseño oscuro
const styles = {
    body: { backgroundColor: '#0f172a', color: '#f8fafc', minHeight: '100vh', padding: '40px 20px',
    fontFamily: 'Arial, sans-serif' },
    container: { maxWidth: '900px', margin: '0 auto' },
    header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #334155', paddingBottom: '15px', marginBottom: '20px' },
    status: { padding: '6px 12px', borderRadius: '20px', fontSize: '0.85rem', fontWeight: 'bold', color:
    'white' },
    configBox: { background: '#1e293b', padding: '15px', borderRadius: '8px', marginBottom: '25px',
    display: 'flex', gap: '10px', flexWrap: 'wrap', border: '1px solid #334155' },
    input: { flex: 1, padding: '10px', borderRadius: '6px', border: '1px solid #475569', background:
    '#0f172a', color: 'white', minWidth: '220px' },
    buttonBlue: { padding: '10px 16px', borderRadius: '6px', border: 'none', background: '#3b82f6',
    color: 'white', fontWeight: 'bold', cursor: 'pointer' },
    buttonRed: { padding: '10px 16px', borderRadius: '6px', border: 'none', background: '#ef4444',
    color: 'white', fontWeight: 'bold', cursor: 'pointer' },
    grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '15px' },
    card: { background: '#1e293b', border: '1px solid #334155', borderRadius: '8px', padding: '15px',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' },
    metric: { display: 'flex', justifyContent: 'space-between', margin: '8px 0', fontSize: '0.95rem' },
    logBox: { background: '#090d16', border: '1px solid #1e293b', borderRadius: '8px', padding: '15px',
    height: '150px', overflowY: 'auto', fontFamily: 'monospace', fontSize: '0.85rem', color: '#94a3b8' }
};