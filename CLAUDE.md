# CLAUDE.md — AguaYa

> Contexto completo del proyecto para Claude Code. Léelo entero antes de escribir código.
> Si algo de este archivo choca con lo que pide el usuario en la sesión, **pregunta antes de actuar**.

---

## 1. Qué es AguaYa

AguaYa es un sistema de gestión logística del **abastecimiento de agua potable mediante camiones cisterna** para **EPSEL S.A.** (Entidad Prestadora de Servicios de Saneamiento de Lambayeque, Perú). Cubre Chiclayo y distritos aledaños (Monsefú, etc.).

Tiene tres clientes:
- **App móvil** (React Native / Expo) para **Vecinos** (hogares) y **Conductores** de cisterna.
- **Panel web** (React / Vite) para el **Administrador** de EPSEL.
- **Panel público de transparencia** (web, solo lectura, sin login).

Todos consumen un **backend REST** en Spring Boot.

### Contexto académico (importante)
- Proyecto universitario de la **Universidad Tecnológica del Perú (UTP), sede Chiclayo**. Equipo de 4 integrantes.
- **El sistema se desarrolla para el curso "Sistemas de Información Empresarial" (SIE)**. Las entregas son por avances.
- El mismo caso (AguaYa/EPSEL) se documenta para el curso de **Arquitectura Empresarial (TOGAF)**, pero ese curso **solo pide documentación, no sistema**. Este repo es para SIE.
- Es un MVP académico: prioriza que funcione bien, sea demostrable y esté ordenado. No sobre-ingeniería.
- Los datos (sectores, cisternas, placas) son **datos referenciales**, no oficiales de EPSEL.

---

## 2. El problema real (y el enfoque del sistema)

En zonas altas (cerros, laderas, asentamientos humanos) el agua no llega por red y depende de cisternas. Investigación previa del equipo concluyó que los fallos **reales y recurrentes** son:

1. **Falta de capacidad**: no alcanzan camiones/viajes para la demanda. La propia SUNASS reconoce que las empresas prestadoras no tienen capacidad logística para cubrir el número de viajes requeridos.
2. **Incumplimiento de rutas/horarios por conductores**: hubo denuncias en Chiclayo de cisternas de EPSEL desviadas a viviendas particulares o negocios; EPSEL abrió investigación.
3. **El vecino no sabe cuándo llega el agua** ni si su zona está programada.
4. **Zonas altas de difícil acceso.**

Las **averías mecánicas en ruta NO son un problema recurrente documentado**. Por eso:

> **Núcleo de valor del sistema** = planificación por capacidad + verificación de entregas con GPS + información al vecino.
> **La reasignación de cisternas es una acción MANUAL y SECUNDARIA** del Administrador. **No construir un algoritmo automático de reasignación.**

### Marco normativo (para reglas de negocio)
- Reglamento de Calidad de la Prestación de los Servicios de Saneamiento (SUNASS), arts. 76 y 77 (modificados por Res. 012-2022 y 095-2022-SUNASS-CD; TUO vigente: Res. 058-2023-SUNASS-CD).
- Dotación mínima diaria: **80 litros por unidad de uso** (20 L si la interrupción afecta a >10% de conexiones activas).
- Corte > 18 h → obligación de abastecer con cisterna. Establecimientos de salud, bomberos y penales → desde las 6 h.
- Número de cisternas se calcula por zona según: unidades de uso, volumen del camión y número de viajes.
- Las cisternas para consumo humano requieren **autorización sanitaria vigente** (DIRESA/GERESA).

---

## 3. Alcance

### Incluye
- Gestión y autenticación de usuarios: Administrador, Conductor, Vecino.
- Registro de vecino con verificación del teléfono por SMS (OTP), contraseña y ubicación de su vivienda en mapa.
- Gestión de sectores, puntos de llenado, cisternas, conductores e instituciones críticas.
- Programación de rutas por sector y turno, con paradas priorizadas.
- Planificación de capacidad (cálculo de viajes y cisternas necesarios vs. disponibles).
- Asignación de cisterna + conductor (sugerencia por cercanía, confirma el Admin).
- Monitoreo GPS en tiempo real (GPS del celular del conductor).
- Verificación de entrega por geocerca.
- Reporte de incidencias (conductor y vecino) y reasignación manual de paradas.
- Calificaciones y comentarios por parada atendida.
- Notificaciones push (vecino) y alertas en tiempo real (admin).
- Panel público de transparencia con información agregada.

### NO incluye (no implementar aunque parezca útil)
- Pagos, cobros o facturación.
- Telemetría o control mecánico de los camiones (solo GPS del celular del conductor).
- Integración con ERP o sistemas comerciales de EPSEL (AguaYa es un sistema satélite independiente).
- Zonas fuera de la jurisdicción de EPSEL.
- Algoritmo automático de optimización/reasignación de rutas (VRP, etc.).
- SMS para notificaciones (solo para el OTP del registro).

---

## 4. Actores y permisos

| Rol | Cliente | Puede |
|---|---|---|
| `ADMIN` | Panel web | CRUD de sectores, puntos de llenado, cisternas, conductores, instituciones; programar rutas; ver planificación de capacidad; asignar; ver mapa en vivo; gestionar incidencias y reasignar paradas; ver calificaciones y reportes. |
| `CONDUCTOR` | App móvil | Ver su ruta del día y paradas; iniciar/finalizar ruta y viajes; enviar posición GPS; marcar paradas atendidas (validado por geocerca); reportar incidencias. |
| `VECINO` | App móvil | Registrarse; ver su próxima atención y la cisterna en camino; recibir push; reportar incidencias; calificar la entrega. |
| Público | Web pública | Ver panel de transparencia (sin login, solo datos agregados). |

---

## 5. Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.x (Web, Data JPA, Security, Validation, WebSocket), Lombok |
| Auth | JWT (jjwt), access + refresh token |
| BD | PostgreSQL 16, migraciones con **Flyway** (nunca `ddl-auto=update`; usar `validate`) |
| Docs API | springdoc-openapi (Swagger UI en `/swagger-ui.html`) |
| Tiempo real | WebSocket + STOMP |
| OTP | Interfaz `ProveedorOtp`: impl `ProveedorOtpLocal` (dev, código en log) e impl `ProveedorOtpTwilio` (prod, Twilio Verify por API REST) |
| Push | Expo Push API (HTTP) desde el backend |
| Tests | JUnit 5, Spring Boot Test, Testcontainers (Postgres) |
| Móvil | React Native + Expo, `expo-location`, `react-native-maps`, `expo-notifications` |
| Web | React + Vite, Leaflet + OpenStreetMap (sin API key) |
| Infra | Docker Compose (postgres + backend). Despliegue de `develop` en Railway (o Render). |

---

## 6. Estructura del monorepo

```
aguaya/
├── CLAUDE.md
├── README.md
├── .gitignore
├── docker-compose.yml
├── backend/          # Spring Boot
├── mobile/           # Expo (Vecino + Conductor en una sola app, según rol)
└── web/              # Vite: panel Admin + rutas públicas /transparencia
```

### Backend: paquetes por funcionalidad
```
pe.epsel.aguaya
├── auth/           # registro, OTP, login, JWT, refresh
├── persona/        # persona (clase padre), vecino, conductor, administrador
├── sector/         # sector, institucion_critica
├── puntollenado/
├── cisterna/
├── ruta/           # ruta, parada, viaje, programación, asignación
├── capacidad/      # cálculo de planificación (sin tabla propia)
├── monitoreo/      # GPS, geocerca, WebSocket
├── incidencia/     # reportes y reasignación manual
├── calificacion/
├── notificacion/   # push Expo, dispositivos
├── transparencia/  # endpoints públicos agregados
└── common/         # config, seguridad, excepciones, utils (Haversine)
```

Cada módulo: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/` (manual o MapStruct).

### Convenciones de código
- Código (clases, métodos, variables) y BD **en español**, sin tildes ni ñ en identificadores (`anio`, `institucion`).
- Tablas y columnas en `snake_case`; entidades en `PascalCase`.
- Los controladores **nunca** devuelven entidades: siempre DTOs (`record` de Java).
- Validación con Bean Validation en DTOs de entrada.
- Errores con `@RestControllerAdvice` → respuesta uniforme `{ timestamp, status, codigo, mensaje, detalles }`.
- Enums persistidos como `STRING`.
- Fechas en `OffsetDateTime` / `timestamptz`; zona horaria del negocio `America/Lima`.
- Coordenadas como `numeric(9,6)` / `BigDecimal` o `double`; distancia con Haversine en `common/util`. La API acepta más decimales (GPS, mapas) y los redondea a 6 con `Coordenada.normalizar`.
- Lógica de negocio en `service`, nunca en controladores.

---

## 7. Modelo de datos

`Persona` es la clase padre de `Vecino`, `Conductor` y `Administrador`: tabla base `persona` + tablas por subtipo (herencia **JOINED**). Todas las personas ingresan con DNI + contraseña.

| Tabla | Columnas |
|---|---|
| `persona` | id, dni (único, 8 díg.), nombres, apellidos, telefono (único), email (null), password_hash, rol, activo, creado_en |
| `vecino` | persona_id (PK/FK), direccion_referencia (texto libre), latitud, longitud, sector_id |
| `conductor` | persona_id, licencia, estado_disponibilidad (DISPONIBLE / EN_RUTA / INACTIVO), turno, lat_actual, lng_actual, ubicacion_actualizada_en |
| `administrador` | persona_id, cargo, nivel_acceso |
| `codigo_otp` | id, telefono, codigo_hash, expira_en, intentos, usado, creado_en (solo proveedor OTP local; con Twilio el código lo guarda Twilio) |
| `refresh_token` | id, persona_id, token_hash, expira_en, revocado |
| `dispositivo_push` | id, persona_id, expo_token (único), plataforma, actualizado_en |
| `sector` | id, nombre, distrito, modalidad (PERMANENTE / CONTINGENCIA), es_zona_alta, unidades_uso, dotacion_litros (def. 80), frecuencia_dias, max_viajes_diarios, lat_centro, lng_centro, tipo_entrega (DOMICILIO / PUNTO_ENTREGA), activo |
| `institucion_critica` | id, nombre, tipo (SALUD / BOMBEROS / EDUCACION / OTRO), sector_id, latitud, longitud, activo |
| `punto_entrega` | id, sector_id, nombre, referencia, latitud, longitud, activo (lugar fijo donde se detiene la cisterna en sectores PUNTO_ENTREGA) |
| `punto_llenado` | id, nombre, tipo (PLANTA / SURTIDOR / POZO), direccion, latitud, longitud, activo |
| `cisterna` | id, placa (único), capacidad_litros, estado_vehiculo (OPERATIVA / MANTENIMIENTO / AVERIADA), nro_autorizacion_sanitaria, vence_autorizacion |
| `ruta` | id, fecha_programada, turno (MANANA / TARDE), sector_id, punto_llenado_id, conductor_id, cisterna_id, administrador_id, viajes_planificados, estado (PROGRAMADA / EN_CURSO / INTERRUMPIDA / COMPLETADA), inicio, fin, creado_en |
| `viaje` | id, ruta_id, nro_viaje, punto_llenado_id, volumen_cargado_litros, salida, retorno, estado (EN_CARGA / EN_REPARTO / FINALIZADO) |
| `parada` | id, ruta_id, viaje_id (null), tipo (HOGAR / INSTITUCION / PUNTO_ENTREGA), vecino_id (null), institucion_id (null), punto_entrega_id (null), latitud, longitud, orden, prioridad (CRITICA / NORMAL), estado (PENDIENTE / ATENDIDA / NO_ATENDIDA / REASIGNADA), volumen_entregado_litros, verificada_gps, atendida_en |
| `monitoreo_gps` | id, ruta_id, latitud, longitud, registrado_en |
| `intento_geocerca` | id, parada_id, conductor_id, latitud (null), longitud (null), distancia_metros (null), registrado_en (intentos de atender fuera de la geocerca; evidencia ante desvíos) |
| `incidencia` | id, ruta_id, parada_id (null), reportado_por (persona_id), tipo, descripcion, estado (ABIERTA / REASIGNADA / CERRADA), ruta_destino_id (null), resuelta_por (null), creado_en, resuelto_en |
| `calificacion_servicio` | id, parada_id (único), vecino_id, estrellas (1–5, CHECK), comentario, creado_en |

**Tipos de incidencia**
- Conductor: `AVERIA`, `ACCESO_BLOQUEADO`, `AGUA_AGOTADA`, `OTRO`
- Vecino: `NO_LLEGO`, `LLEGO_TARDE`, `YA_PASO`, `VOLUMEN_INSUFICIENTE`, `OTRO`

**Relaciones clave**: Ruta 1→1..* Viaje · Ruta 1→1..* Parada · Viaje 0..1→* Parada · Parada 1→0..1 Calificación · Conductor 1→* Ruta · Cisterna 1→* Ruta · Sector 1→* Vecino / Institución / Ruta.

`totalParadas` y `paradasAtendidas` **no se guardan**: se calculan con `COUNT`.

---

## 8. Reglas de negocio (obligatorias)

1. **Capacidad (RF02)**
   - `viajes_necesarios = ceil(unidades_uso × dotacion_litros × frecuencia_dias ÷ capacidad_litros_cisterna)` (cada visita cubre los días hasta la siguiente; mínimo 1). Implementado en `capacidad/service/CalculadoraViajes`.
   - `cisternas_necesarias = ceil(viajes_necesarios ÷ max_viajes_diarios)`
   - Comparar con cisternas OPERATIVAS con autorización vigente y devolver el **déficit**. Es un cálculo; no se persiste (salvo `ruta.viajes_planificados`).
2. **Asignación (RF03)**: no se puede asignar una cisterna con `vence_autorizacion < fecha_programada` ni con estado ≠ OPERATIVA. No se puede asignar un conductor INACTIVO ni con otra ruta en el mismo turno y fecha. Una cisterna no puede tener dos rutas en la misma fecha y turno. El sistema **sugiere**: cisternas por menos viajes necesarios (no tienen GPS propio) y conductores primero del mismo turno y luego por cercanía de su última posición al centro del sector (Haversine); el Admin **confirma** y puede corregir los viajes planificados.
3. **Paradas**: una ruta por sector, fecha y turno, con fecha de hoy en adelante. Al programarla se generan paradas con las instituciones críticas activas (`CRITICA`) y, según `sector.tipo_entrega`, los vecinos activos (`HOGAR`) o los puntos de entrega activos (`PUNTO_ENTREGA`), ambos `NORMAL`. Orden: primero las `CRITICA` y luego las `NORMAL`, cada grupo como recorrido encadenado desde el punto de llenado (siguiente = la más cercana aún no visitada). Las paradas se generan al programar: un vecino registrado después entra en la siguiente ruta. Una ruta solo se elimina mientras está PROGRAMADA.
4. **Geocerca (RF05)**: una parada pasa a `ATENDIDA` solo si la última posición del conductor (`conductor.lat_actual/lng_actual`) tiene como máximo `aguaya.geocerca.vigencia-posicion-segundos` (def. 120 s) de antigüedad y está a ≤ `aguaya.geocerca.radio-metros` (def. 50 m) de la parada. Si no, se rechaza (409 `FUERA_DE_GEOCERCA`, `POSICION_DESACTUALIZADA`, o `SIN_POSICION_GPS` si aún no envió posición) y se registra el intento en `intento_geocerca`. Marcar `NO_ATENDIDA` no exige geocerca. Solo el conductor asignado opera su ruta; una ruta ajena responde 404.
5. **Estados de ruta**: PROGRAMADA → EN_CURSO (conductor inicia, solo el día programado y sin otra ruta EN_CURSO; queda EN_RUTA) → COMPLETADA (todas las paradas en estado final; vuelve a DISPONIBLE) | INTERRUMPIDA (incidencia del conductor con `detieneRuta: true`; vuelve a DISPONIBLE y las paradas pendientes quedan PENDIENTE para RF07). No se permiten saltos inválidos (409 `TRANSICION_INVALIDA`). El GPS solo se acepta con la ruta EN_CURSO.
6. **Reasignación (RF07, manual)**: el Admin elige la ruta destino; las paradas PENDIENTES de la ruta origen se mueven (copia en destino, origen queda `REASIGNADA`), la incidencia pasa a `REASIGNADA` con `ruta_destino_id`. El sistema solo sugiere la cisterna disponible más cercana. **Sin algoritmo automático.**
7. **Calificación**: solo el vecino dueño de la parada, solo si está ATENDIDA, una vez por parada.
8. **Registro de vecino**: el OTP confirma que el teléfono es suyo; el sector se asigna automáticamente al más cercano (por centro) y el vecino queda **activo de inmediato** (sin validación del Admin) y con sesión iniciada. El Admin solo puede desactivar cuentas (`persona.activo`).
9. **Privacidad**: el panel público nunca expone datos personales ni coordenadas de viviendas. La ubicación del conductor solo se expone a vecinos/admin mientras la ruta está EN_CURSO. La próxima atención del vecino solo incluye su propia parada (su hogar o el punto de entrega más cercano).
10. **Incidencias (RF06)**: el conductor reporta sobre su ruta (AVERIA, ACCESO_BLOQUEADO, AGUA_AGOTADA, OTRO) y el vecino sobre una ruta de su sector (NO_LLEGO, LLEGO_TARDE, YA_PASO, VOLUMEN_INSUFICIENTE, OTRO); otro tipo → 400 `TIPO_NO_PERMITIDO`. La parada, si se indica, debe ser de la ruta (y el vecino no puede indicar el hogar de otro). No se repite una incidencia ABIERTA de la misma persona, ruta y tipo (409). Cada reporte alerta al admin por `/topic/admin/incidencias`; el admin la cierra (CERRADA, `resuelta_por`, `resuelto_en`).

---

## 9. Requerimientos

### Funcionales (con prioridad MVP)

| RF | Descripción | Fase |
|---|---|---|
| RF01 | Programación de rutas por sector y turno, con paradas de hogares e instituciones priorizadas | 1 |
| RF02 | Planificación de capacidad (fórmula SUNASS) con alerta de déficit | 2 |
| RF03 | Asignación de cisterna y conductor (sugerencia por cercanía + validación de autorización sanitaria; confirma Admin) | 1 |
| RF04 | Monitoreo GPS en tiempo real | 1 |
| RF05 | Verificación de entrega por geocerca | 1 |
| RF06 | Reporte de incidencias (conductor y vecino) con categorías; alerta al Admin | 1 |
| RF07 | Reasignación manual de paradas pendientes | 2 |
| RF08 | Calificaciones y comentarios por parada atendida | 2 |
| RF09 | Panel público de transparencia (sin auth, agregado por distrito/sector) | 2 |

Fase 0 (base): repo, setup backend, auth + OTP, datos semilla.

### No funcionales
- **RNF01** Soportar ≥ 500 usuarios concurrentes en consultas de rastreo.
- **RNF02** Disponibilidad 99.9% en horario operativo.
- **RNF03** Tráfico cifrado (HTTPS/WSS); contraseñas con BCrypt; OTP guardado hasheado.
- **RNF04** App liviana y de bajo consumo de datos, usable en celulares gama baja (payloads pequeños, paginación, GPS cada ~15 s).

---

## 10. Flujos clave

### Registro del vecino (OTP solo aquí)
1. App: teléfono (+ DNI opcional para avisar si ya está registrado) → `POST /api/auth/otp/solicitar`.
2. El proveedor OTP envía el código por SMS. En dev (`ProveedorOtpLocal`): 6 dígitos, hash en BD, vence en 5 min, máx. 3 intentos y 3 envíos/hora por teléfono. En prod (Twilio Verify) Twilio genera, envía y valida el código con sus propios límites.
3. App: código → `POST /api/auth/registro/vecino` con nombres, apellidos, DNI, teléfono, código, **contraseña** (8–72 caracteres), `direccion_referencia` (texto) + latitud/longitud. Responde con la sesión abierta.
4. Ubicación: la app pide permiso con `expo-location` y centra el mapa en la ubicación actual; el usuario puede **arrastrar el pin** o elegir manualmente si niega el permiso. El texto de dirección puede prellenarse con `reverseGeocodeAsync` y el usuario lo corrige. **El dato que manda es el pin** (en asentamientos las direcciones son informales: "Mz. B Lt. 12, AA.HH. X").

### Login (todos los roles)
- `POST /api/auth/login` con DNI + contraseña (BCrypt) para vecino, conductor y admin. Las cuentas de conductor las crea el Admin.
- Se entregan access token JWT (15 min) + refresh token (30 días, se rota en cada uso).

### OTP
- Interfaz `ProveedorOtp` (`enviar` + `verificar`), elegida con `OTP_PROVIDER`.
- `log` → `ProveedorOtpLocal`: imprime el código en el log; lo devuelve en la respuesta si `aguaya.otp.exponer-codigo=true` (**solo dev**, prohibido en `prod`).
- `twilio` → `ProveedorOtpTwilio`: Twilio Verify vía API REST (requiere `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_VERIFY_SID`).

### GPS en tiempo real
- Conductor envía `POST /api/conductor/rutas/{id}/posicion` cada ~15 s (no socket abierto desde el móvil, ahorra batería/datos).
- Backend guarda en `monitoreo_gps`, actualiza `conductor.lat_actual/lng_actual` y publica por STOMP.
- Endpoint WebSocket: `/ws` (STOMP). El CONNECT lleva `Authorization: Bearer <accessToken>`; cada SUBSCRIBE se autoriza: `/topic/admin/**` solo ADMIN; `/topic/rutas/{id}/posicion` ADMIN, el conductor de la ruta o un vecino de su sector mientras está EN_CURSO. Los clientes no envían mensajes. Se publica después del commit.
- Tópicos: `/topic/rutas/{id}/posicion`, `/topic/admin/incidencias`, `/topic/admin/rutas` (eventos RUTA_INICIADA, PARADA_ATENDIDA, PARADA_NO_ATENDIDA, RUTA_COMPLETADA).

### Notificaciones
- Vecino: push vía Expo (ruta iniciada, cisterna cerca, parada atendida, reasignación).
- Admin: alertas por WebSocket en el panel web.

---

## 11. API (resumen)

```
# Auth
POST /api/auth/otp/solicitar
POST /api/auth/registro/vecino
POST /api/auth/login                   (DNI + password: todos los roles)
POST /api/auth/refresh
POST /api/auth/logout

# Admin
CRUD /api/admin/sectores
CRUD /api/admin/instituciones
CRUD /api/admin/puntos-entrega
CRUD /api/admin/puntos-llenado
CRUD /api/admin/cisternas
CRUD /api/admin/conductores
GET  /api/admin/capacidad?sectorId=&fecha=          (RF02)
POST /api/admin/rutas                                (RF01)
GET  /api/admin/rutas/{id}/sugerencias-asignacion    (RF03)
PATCH /api/admin/rutas/{id}/asignar                  (RF03)
GET  /api/admin/rutas?fecha=&estado=&sectorId=
GET  /api/admin/rutas/{id}                           (detalle con paradas)
DELETE /api/admin/rutas/{id}                        (solo PROGRAMADA)
GET  /api/admin/rutas/{id}/recorrido                 (posiciones GPS + intentos fuera de geocerca)
GET  /api/admin/incidencias?estado=&tipo=&rutaId=
GET  /api/admin/incidencias/{id}
POST /api/admin/incidencias/{id}/reasignar           (RF07)
PATCH /api/admin/incidencias/{id}/cerrar
GET  /api/admin/calificaciones?sectorId=

# Conductor
GET  /api/conductor/rutas/hoy
PATCH /api/conductor/rutas/{id}/iniciar
PATCH /api/conductor/rutas/{id}/finalizar
POST /api/conductor/rutas/{id}/viajes
PATCH /api/conductor/viajes/{id}/finalizar
POST /api/conductor/rutas/{id}/posicion              (RF04)
PATCH /api/conductor/paradas/{id}/atender            (RF05, geocerca)
PATCH /api/conductor/paradas/{id}/no-atendida

# Vecino
GET  /api/vecino/me
GET  /api/vecino/proxima-atencion                    (ruta en curso o siguiente + última, con su parada)
GET  /api/vecino/rutas/{id}/posicion
POST /api/vecino/calificaciones                      (RF08)
POST /api/incidencias                                (RF06, conductor o vecino)
GET  /api/incidencias                                (las que reportó la persona autenticada)
POST /api/dispositivos                               (registrar expo_token)

# Público (sin auth)
GET  /api/public/transparencia/resumen               (RF09)
GET  /api/public/transparencia/sectores
GET  /api/public/transparencia/cronograma?distrito=
```

Respuestas paginadas con `page`, `size`, `totalElements`. Swagger documenta todo.

---

## 12. Git flow (estricto)

- **`main`**: solo recibe merges desde `release/*` o `hotfix/*`, con tag semántico (`v0.1.0`). **Nunca** commits ni push directo a `main`.
- **`develop`**: rama de integración. Solo entra código por Pull Request.
- **`feature/<modulo>`**: sale de `develop`, vuelve a `develop` por PR. Ej.: `feature/backend-setup`, `feature/auth`, `feature/rutas`.
- **`release/avance-N`**: sale de `develop` para cada entrega de SIE; se estabiliza y se mergea a `main` (con tag) y de vuelta a `develop`.
- **`hotfix/<desc>`**: sale de `main` para urgencias; vuelve a `main` y `develop`.

**Commits**: Conventional Commits en español: `feat(auth): registro de vecino con OTP`, `fix(ruta): ...`, `chore:`, `docs:`, `test:`, `refactor:`. Commits pequeños y atómicos.

**PRs**: descripción con qué hace, RF relacionado, cómo probar. Debe compilar y pasar tests.

### Reglas para Claude Code
- Antes de empezar una tarea: verifica la rama actual. Si estás en `main` o `develop`, **crea la rama `feature/*` correspondiente** desde `develop` actualizado.
- Nunca hagas `push --force` sobre `main`/`develop`, nunca hagas merge a `main` sin que el usuario lo pida explícitamente.
- No hagas push ni abras PR sin confirmación del usuario.
- Toda modificación de esquema = **nueva migración Flyway** (`V{n}__descripcion.sql`). Nunca edites una migración ya mergeada.
- Corre `./mvnw verify` (o los tests relevantes) antes de declarar una tarea terminada.
- No agregues dependencias pesadas sin justificar. No implementes nada de la sección "NO incluye".
- Si un requisito es ambiguo, pregunta. No inventes reglas de negocio.
- Comunica en español, de forma breve y directa.

---

## 13. Entornos y configuración

- Perfiles: `dev` (local, docker-compose) y `prod` (Railway).
- Variables de entorno (nunca commitear secretos; usar `.env` en `.gitignore` y un `.env.example`):
  `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_MINUTOS`, `JWT_REFRESH_DIAS`, `OTP_PROVIDER` (log|twilio), `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_VERIFY_SID`, `GEOCERCA_RADIO_METROS`, `GEOCERCA_VIGENCIA_POSICION_SEGUNDOS`, `CORS_ORIGINS`.
- CORS: permitir el origen del panel web y del panel público.
- Para probar GPS desde el celular el backend debe ser accesible por red: Railway (rama `develop`) o `ngrok` en local.

---

## 14. Datos semilla (migración `V3__datos_referenciales.sql`)

Marcar como **datos referenciales** (comentario en la migración y en README).
- 4–5 sectores reales de Chiclayo y Monsefú (zonas altas / asentamientos), con coordenadas aproximadas de OpenStreetMap, `unidades_uso` estimadas y `dotacion_litros = 80`.
- 1–2 instituciones críticas por sector (posta, colegio).
- Punto de llenado: planta de tratamiento de agua potable de EPSEL en Chiclayo (coordenadas aproximadas).
- 5 cisternas con **placas ficticias**, capacidades realistas, una con autorización vencida (para probar la regla 2) y una en MANTENIMIENTO.
- 1 admin, 3 conductores y 10–15 vecinos distribuidos en sectores (contraseñas de prueba documentadas solo en `.env.example`/README de dev).

---

## 15. Plan de trabajo

### Sprint 0 (hoy)
1. **Repo base**: estructura del monorepo, README, `.gitignore`, `CLAUDE.md`, rama `develop`. (El usuario configura protecciones de rama en GitHub.)
2. **`feature/backend-setup`**: proyecto Spring Boot (Java 21), perfiles, docker-compose con Postgres, Flyway `V1__esquema_inicial.sql` con **todo** el modelo de la sección 7, Swagger, handler global de errores, endpoint de health. PR → `develop`.
3. **`feature/auth`**: entidades de persona, OTP de registro (local + Twilio Verify), registro de vecino, login con DNI + contraseña, JWT + refresh, seguridad por roles. Tests de servicio. PR → `develop`.
4. **`feature/seed-data`**: `V3` con datos referenciales. PR → `develop`.
5. **Despliegue** de `develop` en Railway.

En paralelo (otros integrantes): `feature/mobile-setup` (Expo) y `feature/web-setup` (Vite).

### Fase 1 (siguiente)
`feature/catalogos` (sectores, instituciones, puntos de llenado, cisternas, conductores) → `feature/rutas` (RF01 + RF03) → `feature/monitoreo-gps` (RF04 + RF05 + WebSocket) → `feature/incidencias` (RF06) → `feature/notificaciones`.

### Fase 2
`feature/capacidad` (RF02) → `feature/reasignacion` (RF07) → `feature/calificaciones` (RF08) → `feature/transparencia` (RF09).

Cada entrega de SIE → `release/avance-N` → `main` con tag.

---

## 16. Glosario

- **EPSEL**: Entidad Prestadora de Servicios de Saneamiento de Lambayeque S.A.
- **SUNASS**: Superintendencia Nacional de Servicios de Saneamiento (regulador).
- **Unidad de uso**: vivienda/conexión que recibe el servicio.
- **Dotación**: litros por unidad de uso por día.
- **Punto de llenado**: planta, surtidor o pozo donde la cisterna carga agua.
- **Viaje**: un ciclo carga → reparto → retorno. Una ruta puede tener varios.
- **Parada**: punto de entrega dentro de una ruta (hogar o institución).
- **Geocerca**: radio alrededor de una parada dentro del cual se valida la entrega.
- **AA.HH.**: asentamiento humano.
