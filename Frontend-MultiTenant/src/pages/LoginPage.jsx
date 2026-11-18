import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

export function LoginPage(){
    const isAuthenticatedAzure = useIsAuthenticated();
    const cognito = useAuth();
    const navigate= useNavigate();
      // Hay dos proveedores de identidad conviviendo: la sesion es valida si
      // cualquiera de los dos entrego un token.
    const isAuthenticated = isAuthenticatedAzure || cognito.isAuthenticated;
    const proveedor = isAuthenticatedAzure ? 'Azure Entra ID' : 'AWS Cognito';
    useEffect(()=>{
        if(isAuthenticated){
            navigate("/dashboard")
        }
    },[isAuthenticated, navigate])
    return (
    <div style={{ padding: '40px', fontFamily: 'Arial, sans-serif', maxWidth: '800px', margin: '0 auto' }}>
        <h2>Dashboard Frontend - Arquitectura Cloud OIDC</h2>
        <hr style={{ margin: '20px 0' }} />

        <div style={{ marginBottom: '20px' }}>
        <LoginButton />
        </div>

        {isAuthenticated && (
        <p style={{ color: '#605e5c', fontSize: '13px' }}>
            Sesión activa con: <strong>{proveedor}</strong>
        </p>
        )}
    </div>
    );
}