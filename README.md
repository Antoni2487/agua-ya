# AguaYa

[![Backend](https://github.com/Antoni2487/agua-ya/actions/workflows/backend.yml/badge.svg?branch=develop)](https://github.com/Antoni2487/agua-ya/actions/workflows/backend.yml)
[![Web](https://github.com/Antoni2487/agua-ya/actions/workflows/web.yml/badge.svg?branch=develop)](https://github.com/Antoni2487/agua-ya/actions/workflows/web.yml)

Sistema de gestión logística del abastecimiento de agua potable mediante camiones cisterna para **EPSEL S.A.** (Lambayeque, Perú).

Proyecto académico del curso **Sistemas de Información Empresarial** — Universidad Tecnológica del Perú (UTP), sede Chiclayo.

> Los datos de sectores, cisternas y placas incluidos en el sistema son **referenciales**, no oficiales de EPSEL.
> Las cuentas de prueba están en [`backend/README.md`](backend/README.md#datos-de-prueba-referenciales).

## Núcleo de valor

- Planificación por capacidad (viajes y cisternas necesarios vs. disponibles, según SUNASS).
- Verificación de entregas con GPS y geocerca.
- Información al vecino sobre cuándo llega el agua.

## Estado

**v0.2.0 (Avance 2)**: backend REST completo del MVP (RF01–RF09), desplegado en Render y optimizado
(sin N+1, índices, caché del panel público y avisos asíncronos). Pendientes: app móvil y panel web.
Detalle en [`CHANGELOG.md`](CHANGELOG.md).

## API desplegada (rama `develop`)

| | URL |
|---|---|
| Base de la API | `https://aguaya-backend.onrender.com` |
| Swagger (documentación interactiva) | https://aguaya-backend.onrender.com/swagger-ui.html |
| Health check | https://aguaya-backend.onrender.com/api/health |
| WebSocket (STOMP) | `wss://aguaya-backend.onrender.com/ws` |

Plan free de Render: si no recibe tráfico en 15 min se duerme y la primera petición puede tardar 2–3 min.
Abrir `/api/health` antes de una demo. Cuentas de prueba en [`backend/README.md`](backend/README.md#datos-de-prueba-referenciales).
Para el panel web en otro dominio, agregarlo a `CORS_ORIGINS` en Render.

## Clientes

| Cliente | Usuarios | Carpeta |
|---|---|---|
| Backend REST (Spring Boot) | — | [`backend/`](backend/) |
| App móvil (React Native / Expo) | Vecinos y Conductores | [`mobile/`](mobile/) |
| Panel web (React / Vite) | Administrador | [`web/`](web/) |
| Web de EPSEL rediseñada (HTML, CSS y JS, sin librerías) | Público: información de las cisternas y anuncio de la app | [`rediseno-epsel/`](rediseno-epsel/) |
| Simulador de recorrido | Demos sin GPS real | [`scripts/`](scripts/) |

## Stack

- **Backend**: Java 21, Spring Boot 3, PostgreSQL 16, Flyway, JWT, WebSocket/STOMP, springdoc-openapi.
- **Móvil**: React Native + Expo.
- **Web**: React + Vite, Leaflet + OpenStreetMap.
- **Infra**: Docker Compose; despliegue de `develop` en Render ([guía](backend/README.md#despliegue-en-render)).

## Flujo de ramas

| Rama | Uso |
|---|---|
| `main` | Versiones entregadas. Solo recibe merges desde `release/*` o `hotfix/*`, con tag (`v0.1.0`). |
| `develop` | Integración. Solo entra código por Pull Request. |
| `feature/<modulo>` | Sale de `develop` y vuelve a `develop` por PR. |
| `release/avance-N` | Una por cada entrega de SIE; se mergea a `main` (con tag) y a `develop`. |
| `hotfix/<desc>` | Sale de `main`; vuelve a `main` y `develop`. |

Commits con [Conventional Commits](https://www.conventionalcommits.org/es/) en español: `feat(auth): registro de vecino con OTP`.

## Puesta en marcha

Todo corre en la PC. Requisitos: Docker Desktop, Node 20 y, para la app, Expo Go en el celular (misma red wifi que la PC).

| # | Qué | Comando (desde la raíz) | Se abre en |
|---|---|---|---|
| 1 | Base de datos + backend | `docker compose up --build` | http://localhost:8080/swagger-ui.html |
| 2 | Panel del admin | `cd web && npm install && npm run dev` | http://localhost:5173 |
| 3 | Web de EPSEL rediseñada | `npx serve rediseno-epsel -l 5500` | http://localhost:5500 |
| 4 | App (vecino y conductor) | `cd mobile && npm install && EXPO_PUBLIC_API_URL=http://<IP-de-la-PC>:8080 npx expo start` | Expo Go (escanear el QR) |
| 5 | Mover una cisterna | `node scripts/simular-ruta.mjs --api http://localhost:8080` | panel, web y app a la vez |

`npm run dev` solo existe en `web/`. La web rediseñada es estática: se sirve con `npx serve` (o Live Server en el puerto 5500) y, abierta desde `localhost`, lee el backend local.
En PowerShell el paso 4 es `$env:EXPO_PUBLIC_API_URL="http://<IP>:8080"; npx expo start` (la IP sale con `ipconfig`).

**Cuentas de prueba** (datos referenciales de V3): admin `10000001` / `Admin2026!`; conductores `20000001`–`20000003` / `Conductor2026!`; vecinos `30000001`–`30000012` / clave `1234`.

## Documentación para el equipo

El contexto completo del proyecto (alcance, modelo de datos, reglas de negocio, API) está en [`docs/especificacion.md`](docs/especificacion.md).
