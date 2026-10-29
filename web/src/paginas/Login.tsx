import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { useSesion } from '../auth/ContextoSesion';
import { MensajeError } from '../componentes/Estado';

export function Login() {
  const { usuario, iniciarSesion } = useSesion();
  const navegar = useNavigate();
  const desde = (useLocation().state as { desde?: string } | null)?.desde ?? '/admin';
  const [dni, setDni] = useState('');
  const [password, setPassword] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<unknown>(null);

  if (usuario?.rol === 'ADMIN') return <Navigate to={desde} replace />;

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    try {
      await iniciarSesion(dni, password);
      navegar(desde, { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="pantalla-login">
      <form className="tarjeta login" onSubmit={enviar} noValidate>
        <h1>
          <img src="/gota.svg" alt="" width={28} height={28} /> AguaYa
        </h1>
        <p className="texto-suave">Panel del Administrador · EPSEL</p>
        <label>
          DNI
          <input
            name="dni"
            inputMode="numeric"
            autoComplete="username"
            maxLength={8}
            pattern="\d{8}"
            required
            value={dni}
            onChange={(e) => setDni(e.target.value.replace(/\D/g, ''))}
          />
        </label>
        <label>
          Contraseña
          <input
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {error !== null && <MensajeError error={error} />}
        <button type="submit" className="boton" disabled={enviando || dni.length !== 8 || !password}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
        <a href="/transparencia" className="enlace-suave">
          Ver el panel público de transparencia
        </a>
      </form>
    </div>
  );
}
