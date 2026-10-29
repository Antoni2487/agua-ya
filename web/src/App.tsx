import { Navigate, Route, Routes } from 'react-router-dom';

import { LayoutAdmin } from './componentes/LayoutAdmin';
import { RutaProtegida } from './componentes/RutaProtegida';
import { Calificaciones } from './paginas/admin/Calificaciones';
import { Capacidad } from './paginas/admin/Capacidad';
import { IncidenciaDetalle } from './paginas/admin/IncidenciaDetalle';
import { Incidencias } from './paginas/admin/Incidencias';
import { Inicio } from './paginas/admin/Inicio';
import { Monitoreo } from './paginas/admin/Monitoreo';
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
        <Route path="monitoreo" element={<Monitoreo />} />
        <Route path="rutas" element={<Rutas />} />
        <Route path="rutas/:id" element={<RutaDetalle />} />
        <Route path="incidencias" element={<Incidencias />} />
        <Route path="incidencias/:id" element={<IncidenciaDetalle />} />
        <Route path="capacidad" element={<Capacidad />} />
        <Route path="calificaciones" element={<Calificaciones />} />
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
