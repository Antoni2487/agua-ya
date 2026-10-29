# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones con
[SemVer](https://semver.org/lang/es/).

## [Sin publicar]

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

### Añadido (despliegue)
- Blueprint de Render (`render.yaml`): backend Docker + PostgreSQL 16, despliegue automático de `develop`.
- El perfil `prod` acepta `DB_HOST`, `DB_PORT` y `DB_NAME` además de `DB_URL`.

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
