# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones con
[SemVer](https://semver.org/lang/es/).

## [0.3.0] - 2026-11-06 · Avance 3 (SIE) y PA3 (IHM)

Las tres piezas del producto: web de EPSEL rediseñada, panel del admin y app móvil para el vecino y el conductor.

### Añadido
- **Panel del admin (Vite)**: rutas, asignación, mapa en vivo, incidencias, reasignación, capacidad explicada, calificaciones y catálogos, con el diseño de EPSEL.
- **App móvil (Expo)**: el vecino ve su próxima cisterna, el camión en el mapa, reporta y califica; el conductor opera su ruta con GPS.
- **Web de EPSEL rediseñada** (`rediseno-epsel/`, propuesta académica): accesible (axe: 0 violaciones en 7 páginas), liviana (0,8 MB), con la información pública de las cisternas y el anuncio de la app.
- El vecino ingresa con DNI + clave de 4 dígitos (V11); el SMS solo para registrarse o restablecerla; bloqueo de 15 min tras 5 intentos.
- La próxima atención del vecino trae placa y capacidad de la cisterna y si la entrega se verificó por GPS.
- Web Push para navegadores (disponible) y Expo Push para la app.
- `scripts/simular-ruta.mjs`: recorrido simulado de una cisterna para las demos.
- Documentación del PA3 en `docs/pa3/` y especificación en `docs/especificacion.md`.

### Cambiado
- El panel público de transparencia del Vite se retira: `/transparencia` redirige a la web rediseñada.

### Corregido
- Arranque en Render: constructor de `ProveedorDniMiApi` y Web Push en modo log sin claves VAPID.

## [0.2.0] - 2026-10-29 · Avance 2 (SIE)

Backend desplegado en Render y optimizado. Sin cambios en las reglas de negocio.

### Añadido
- `GET /api/vecino/me` (estaba en la API pero respondía 404).
- `SWAGGER_ENABLED`, `DB_POOL_MAX` y `CACHE_SEGUNDOS`.
- Guarda contra N+1 (`ConsultasPorPeticionTest`).

### Rendimiento
- Carga por lotes de Hibernate, detalle de ruta en 3 consultas (antes 7) y GPS con una consulta menos.
- V8: índices para FK consultadas y filtros frecuentes.
- Caché Caffeine del panel público.
- Avisos push totalmente asíncronos y aviso de cercanía sin duplicados.
- Respuestas JSON comprimidas (gzip).

### Despliegue
- Blueprint de Render (`render.yaml`): backend Docker + PostgreSQL 16, despliegue automático de `develop`.
  En línea en https://aguaya-backend.onrender.com.
- El perfil `prod` acepta `DB_HOST`, `DB_PORT` y `DB_NAME` además de `DB_URL`.

### Pendiente
- App móvil (Expo) y panel web (Vite).

## [0.1.0] - 2026-10-28 · Avance 1 (SIE)

Backend REST completo del MVP (RF01–RF09). Datos referenciales, no oficiales de EPSEL.

### Base
- Proyecto Spring Boot 3 (Java 21), PostgreSQL 16 con Flyway (V1–V7), Swagger en `/swagger-ui.html`,
  errores uniformes y `docker-compose` para desarrollo.
- Autenticación: registro de vecino con OTP por SMS (Twilio Verify en prod), login con DNI + contraseña para
  todos los roles, JWT de 15 min y refresh token rotativo de 30 días.
- Datos semilla: 5 sectores de Chiclayo y Monsefú, instituciones críticas, planta EPSEL, 5 cisternas y
  cuentas de prueba.
- Catálogos del administrador: sectores, instituciones, puntos de entrega, puntos de llenado, cisternas y
  conductores.

### Fase 1
- RF01 Programación de rutas por sector y turno con paradas priorizadas en recorrido encadenado.
- RF03 Asignación de cisterna y conductor con sugerencias y validación de autorización sanitaria.
- RF04 Monitoreo GPS en tiempo real (WebSocket/STOMP autenticado).
- RF05 Verificación de entrega por geocerca (50 m, posición de los últimos 2 minutos) con registro de intentos.
- RF06 Incidencias de conductor y vecino con alerta en vivo al administrador.
- Viajes de la ruta (salida cargada, reparto y retorno).
- Notificaciones push al vecino vía Expo.

### Fase 2
- RF02 Planificación de capacidad con alerta de déficit.
- RF07 Reasignación manual de paradas pendientes.
- RF08 Calificaciones por parada atendida y resumen por conductor.
- RF09 Panel público de transparencia.

### Pendiente
- App móvil (Expo) y panel web (Vite).
- Despliegue de `develop` en Render.
