import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export function CallbackPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    
    const [statusData, setStatusData] = useState({ 
        loading: true, 
        success: false, 
        message: 'Validando el resultado de la transacción con Transbank...' 
    });

    useEffect(() => {
        const tokenWs = searchParams.get('token_ws');

        if (!tokenWs) {
            setStatusData({
                loading: false,
                success: false,
                message: 'No se encontró el token de respuesta (token_ws) en la URL.'
            });
            return;
        }

        // Llamada al backend para realizar el commit. 
        // Si es exitoso, mantiene el registro; si falla/rechaza, el backend reinicia los intentos.
        fetch(`http://localhost:8083/api/pagos/commit?token_ws=${tokenWs}`, {
            method: 'POST'
        })
        .then(async res => {
            const data = await res.json();
            if (res.ok && data.status === 'AUTHORIZED') {
                setStatusData({
                    loading: false,
                    success: true,
                    message: `¡Pago exitoso! Transacción aprobada por un monto de $${data.amount?.toLocaleString('es-CL')}.`
                });
            } else {
                setStatusData({
                    loading: false,
                    success: false,
                    message: data.message || 'La transacción fue rechazada o anulada. Los contadores de intentos han sido reiniciados.'
                });
            }
        })
        .catch(err => {
            setStatusData({
                loading: false,
                success: false,
                message: 'Ocurrió un error al comunicarse con el servidor de pagos.'
            });
        });
    }, [searchParams]);

    return (
        <div style={styles.body}>
            <div style={styles.containerCard}>
                <h2 style={{ color: statusData.loading ? '#38bdf8' : statusData.success ? '#22c55e' : '#ef4444', marginTop: 0 }}>
                    {statusData.loading ? 'Procesando...' : statusData.success ? '¡Pago Aprobado!' : 'Pago Rechazado'}
                </h2>
                
                <p style={{ color: '#94a3b8', fontSize: '1rem', lineHeight: '1.5', margin: '20px 0' }}>
                    {statusData.message}
                </p>

                <button 
                    onClick={() => navigate('/payment')} 
                    style={styles.buttonBlue}
                >
                    Volver al Panel de Pagos
                </button>
            </div>
        </div>
    );
}

const styles = {
    body: { backgroundColor: '#0f172a', color: '#f8fafc', minHeight: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center', fontFamily: 'Arial, sans-serif' },
    containerCard: { background: '#1e293b', padding: '30px', borderRadius: '12px', maxWidth: '450px', width: '100%', textAlign: 'center', border: '1px solid #334155', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.3)' },
    buttonBlue: { padding: '10px 20px', borderRadius: '6px', border: 'none', background: '#3b82f6', color: 'white', fontWeight: 'bold', cursor: 'pointer', width: '100%' }
};
