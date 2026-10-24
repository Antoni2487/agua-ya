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

## Base de datos

- Esquema gestionado solo por Flyway (`src/main/resources/db/migration`); Hibernate en modo `validate`.
- Todo cambio de esquema es una migración nueva `V{n}__descripcion.sql`. Nunca editar una migración ya mergeada.

## Errores

Todas las respuestas de error tienen la forma:

```json
{ "timestamp": "...", "status": 400, "codigo": "VALIDACION", "mensaje": "...", "detalles": [{ "campo": "dni", "mensaje": "..." }] }
```

Las reglas de negocio lanzan `NegocioException(status, codigo, mensaje)`.
