import { Navigate, Route, Routes } from 'react-router-dom';

import { LayoutAdmin } from './componentes/LayoutAdmin';
import { RutaProtegida } from './componentes/RutaProtegida';
import { Inicio } from './paginas/admin/Inicio';
import { RutaDetalle } from './paginas/admin/RutaDetalle';
import { Rutas } from './paginas/admin/Rutas';
import { Login } from './paginas/Login';
import { NoEncontrada } from './paginas/NoEncontrada';
import { Transparencia } from './paginas/publico/Transparencia';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/transparencia" replace />} />
      <Route path="/transparencia" element={<Transparencia />} />
      <Route path="/login" element={<Login />} />
      <Route
        path="/admin"
        element={
          <RutaProtegida>
            <LayoutAdmin />
          </RutaProtegida>
        }
      >
        <Route index element={<Inicio />} />
        <Route path="rutas" element={<Rutas />} />
        <Route path="rutas/:id" element={<RutaDetalle />} />
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
