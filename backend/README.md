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

## Base de datos

- Esquema gestionado solo por Flyway (`src/main/resources/db/migration`); Hibernate en modo `validate`.
- Todo cambio de esquema es una migración nueva `V{n}__descripcion.sql`. Nunca editar una migración ya mergeada.

## Errores

Todas las respuestas de error tienen la forma:

```json
{ "timestamp": "...", "status": 400, "codigo": "VALIDACION", "mensaje": "...", "detalles": [{ "campo": "dni", "mensaje": "..." }] }
```

Las reglas de negocio lanzan `NegocioException(status, codigo, mensaje)`.
