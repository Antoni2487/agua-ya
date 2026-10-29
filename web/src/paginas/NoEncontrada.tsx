import { Link } from 'react-router-dom';

export function NoEncontrada() {
  return (
    <main className="contenido">
      <h1>Página no encontrada</h1>
      <Link to="/admin">Volver al panel</Link>
    </main>
  );
}
