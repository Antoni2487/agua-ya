# web

Panel del Administrador de EPSEL y panel público `/transparencia` de AguaYa.
React 19 + Vite 7 + TypeScript, React Router, TanStack Query; Leaflet + OpenStreetMap para mapas.

## Correr en local

```bash
cp .env.example .env      # VITE_API_URL: backend local o https://aguaya-backend.onrender.com
npm install
npm run dev               # http://localhost:5173
```

El backend debe permitir el origen en `CORS_ORIGINS` (por defecto ya incluye `http://localhost:5173`).
Ingreso: DNI `10000001` / `Admin2026!` (datos de prueba). Solo el rol ADMIN entra al panel; vecinos y
conductores usan la app móvil.

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga |
| `npm run typecheck` | Verificación de tipos |
| `npm test` | Tests (Vitest + Testing Library) |
| `npm run build` | Build de producción en `dist/` |

## Estructura

```
src/
├── api/          cliente HTTP (token, refresh automático, errores uniformes) y tipos del backend
├── auth/         sesión del administrador
├── componentes/  layout, ruta protegida, estados de carga y error
├── paginas/      admin/ (panel) y publico/ (transparencia)
└── utilidades.ts fechas en hora de Lima y etiquetas
```

- El access token vive en memoria y el refresh token en `localStorage`; ante un 401 se renueva una sola vez
  y se reintenta la petición.
- Los errores del backend (`{ codigo, mensaje, detalles }`) se muestran tal cual: ya vienen en español.

## Páginas

| Ruta | Acceso | Contenido |
|---|---|---|
| `/transparencia` | Público | Cronograma de los próximos 7 días, mapa de sectores coloreado por cumplimiento, última y próxima atención por sector e indicadores del periodo (RF09). Solo datos agregados. |
| `/login` | Público | Ingreso del administrador por DNI. |
| `/admin` | ADMIN | Resumen del día: alerta de capacidad y rutas de hoy. |
| `/admin/rutas` | ADMIN | Rutas por fecha, estado y sector (filtros en la URL, paginado) y formulario para programar (RF01). |
| `/admin/rutas/:id` | ADMIN | Detalle: asignación de cisterna y conductor con sugerencias (RF03), mapa del recorrido con paradas numeradas por estado, viajes y eliminación si aún no inició. |

El mapa usa Leaflet + OpenStreetMap (sin API key) y se carga aparte para que los datos aparezcan primero.

## Despliegue en Vercel

1. En Vercel: **Add New → Project**, importar `Antoni2487/agua-ya` y en **Root Directory** elegir `web`.
2. Variable de entorno `VITE_API_URL=https://aguaya-backend.onrender.com`.
3. Deploy. `vercel.json` ya redirige todas las rutas a `index.html` (SPA).
4. En Render, agregar la URL de Vercel a `CORS_ORIGINS` (separada por coma de las demás).
