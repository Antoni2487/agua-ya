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
      <section className="login-portada" aria-hidden="true">
        <img src="/epsel-cisterna.webp" alt="" />
        <div className="login-portada-texto">
          <p className="login-sobretitulo">EPSEL S.A. · Lambayeque</p>
          <p className="login-lema">Cada cisterna, a tiempo y donde se necesita.</p>
          <p>Programa rutas, sigue cada camión en el mapa y verifica cada entrega con GPS.</p>
        </div>
      </section>
      <main className="login-lado">
        <form className="login" onSubmit={enviar} noValidate>
          <img src="/logo-epsel.webp" alt="EPSEL S.A." width={132} height={90} className="login-logo" />
          <h1>
            AguaYa <span>Panel del administrador</span>
          </h1>
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
          <button type="submit" className="boton boton-grande" disabled={enviando || dni.length !== 8 || !password}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
          <a href="/transparencia" className="enlace-suave">
            Ver el panel público de transparencia
          </a>
          <p className="login-nota">Propuesta académica (UTP). Los datos son referenciales.</p>
        </form>
      </main>
    </div>
  );
}
