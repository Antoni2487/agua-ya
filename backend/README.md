# backend

API REST de AguaYa (Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway).

## Correr en local

```bash
cp .env.example .env              # desde la raíz del repo
docker compose up -d postgres     # Postgres 16 en localhost:5432
cd backend && ./mvnw spring-boot:run
```

O todo en contenedores: `docker compose up --build` (desde la raíz).

- Health: <http://localhost:8080/api/health>
- Swagger UI: <http://localhost:8080/swagger-ui.html>

Perfiles: `dev` (por defecto) y `prod` (`SPRING_PROFILES_ACTIVE=prod`; credenciales solo por variables de entorno).

## Tests

```bash
./mvnw verify
```

Los tests de integración levantan Postgres con Testcontainers (requiere Docker).

## Autenticación

- **Registro del vecino**: `POST /api/auth/otp/solicitar` (teléfono) → `POST /api/auth/registro/vecino`
  (datos + código SMS + contraseña + pin). Queda activo y con sesión iniciada.
- **Login** (vecino, conductor y admin): `POST /api/auth/login` con DNI + contraseña.

- Respuesta: `accessToken` (JWT, 15 min) + `refreshToken` (opaco, 30 días). Enviar `Authorization: Bearer <accessToken>`.
- `POST /api/auth/refresh` rota el refresh token; reusar uno ya rotado revoca todas las sesiones del usuario.
- `POST /api/auth/logout` revoca el refresh token.
- OTP (solo registro): `OTP_PROVIDER=log` en dev, el código sale en el log y en la respuesta
  (`OTP_EXPONER_CODIGO=true`; prohibido en prod). `OTP_PROVIDER=twilio` usa Twilio Verify con
  `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` y `TWILIO_VERIFY_SID`.
  - Con una cuenta de prueba de Twilio solo llegan SMS a números verificados en su consola.
- Rutas por rol: `/api/admin/**` ADMIN, `/api/conductor/**` CONDUCTOR, `/api/vecino/**` VECINO,
  `/api/incidencias` CONDUCTOR o VECINO; `/api/auth/**`, `/api/public/**` y `/api/health` son públicas.

## Catálogos del administrador (rol ADMIN)

| Recurso | Endpoint | Baja |
|---|---|---|
| Sectores | `/api/admin/sectores` (`?distrito=&activo=`) | lógica (`activo=false`) |
| Instituciones críticas | `/api/admin/instituciones` (`?sectorId=&tipo=&activo=`) | lógica |
| Puntos de llenado | `/api/admin/puntos-llenado` (`?tipo=&activo=`) | lógica |
| Cisternas | `/api/admin/cisternas` (`?estado=`) | física, solo si no tiene rutas (si no, 409) |
| Conductores | `/api/admin/conductores` (`?estado=&turno=&activo=`) | lógica: no puede ingresar y se cierran sus sesiones |
| Puntos de entrega | `/api/admin/puntos-entrega` (`?sectorId=&activo=`) | lógica |

Cada uno tiene `GET` (lista y por id), `POST`, `PUT /{id}` y `DELETE /{id}`. Las listas son paginadas
(`?page=0&size=20&sort=campo,asc`, máximo 100 por página) y responden `{ contenido, page, size, totalElements, totalPages }`.
La cisterna informa `autorizacionVigente` (regla 2) y el admin crea los conductores con una contraseña inicial.

## Rutas (RF01 y RF03)

1. `POST /api/admin/rutas` con `sectorId`, `fecha`, `turno` y `puntoLlenadoId`: genera las paradas
   (instituciones críticas primero; luego hogares o puntos de entrega según el sector) en recorrido encadenado.
2. `GET /api/admin/rutas/{id}/sugerencias-asignacion`: cisternas asignables con los viajes que necesitarían
   (`⌈UU × dotación × frecuencia ÷ capacidad⌉`) y conductores libres en ese turno.
3. `PATCH /api/admin/rutas/{id}/asignar` con `cisternaId`, `conductorId` y `viajesPlanificados` opcional:
   valida la regla 2 (cisterna OPERATIVA con autorización vigente, conductor activo, ambos libres en el turno).

Errores de negocio: `RUTA_DUPLICADA`, `FECHA_PASADA`, `RUTA_SIN_PARADAS`, `CISTERNA_NO_OPERATIVA`,
`CISTERNA_AUTORIZACION_VENCIDA`, `CISTERNA_OCUPADA`, `CONDUCTOR_INACTIVO`, `CONDUCTOR_OCUPADO`, `RUTA_NO_EDITABLE`.

## Monitoreo GPS y geocerca (RF04 y RF05)

Conductor (app móvil):
1. `GET /api/conductor/rutas/hoy` → `PATCH /api/conductor/rutas/{id}/iniciar` (solo el día programado).
2. `POST /api/conductor/rutas/{id}/posicion` cada ~15 s con `{ latitud, longitud }` (responde 204).
3. `PATCH /api/conductor/paradas/{id}/atender` (a ≤ 50 m de la parada) o `/no-atendida`.
4. `PATCH /api/conductor/rutas/{id}/finalizar` cuando no quedan paradas pendientes.

Vecino: `GET /api/vecino/rutas/{id}/posicion`, solo rutas de su sector y mientras están EN_CURSO.
Admin: `GET /api/admin/rutas/{id}/recorrido` con las posiciones y los intentos fuera de la geocerca.

Tiempo real (panel web): STOMP en `ws://<host>/ws` con la cabecera `Authorization: Bearer <accessToken>`
en el CONNECT; tópicos `/topic/rutas/{id}/posicion` y `/topic/admin/rutas`.

## Datos de prueba (referenciales)

`V3__datos_referenciales.sql` carga datos **referenciales, no oficiales de EPSEL** (coordenadas aproximadas,
placas, DNI y teléfonos ficticios):

- 5 sectores: Cerropón (Chiclayo), P.J. Atusparias y P.J. Urrunaga (José Leonardo Ortiz), Callanca y
  Valle Hermoso (Monsefú), con 8 instituciones críticas.
- 1 punto de llenado (planta EPSEL, ubicación aproximada).
- 5 cisternas `AYA-101` a `AYA-105`: `AYA-104` con autorización sanitaria vencida y `AYA-105` en mantenimiento.

Cuentas de prueba (login con DNI + contraseña, **solo para desarrollo y demos**):

| Rol | DNI | Contraseña |
|---|---|---|
| Admin | `10000001` | `Admin2026!` |
| Conductores | `20000001` a `20000003` | `Conductor2026!` |
| Vecinos (12) | `30000001` a `30000012` | `Vecino2026!` |

> Estas cuentas también se crean en el despliegue: cambiar o desactivar sus contraseñas si el backend queda público.

## Base de datos

- Esquema gestionado solo por Flyway (`src/main/resources/db/migration`); Hibernate en modo `validate`.
- Todo cambio de esquema es una migración nueva `V{n}__descripcion.sql`. Nunca editar una migración ya mergeada.

## Errores

Todas las respuestas de error tienen la forma:

```json
{ "timestamp": "...", "status": 400, "codigo": "VALIDACION", "mensaje": "...", "detalles": [{ "campo": "dni", "mensaje": "..." }] }
```

Las reglas de negocio lanzan `NegocioException(status, codigo, mensaje)`.
