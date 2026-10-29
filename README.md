# AguaYa

Sistema de gestión logística del abastecimiento de agua potable mediante camiones cisterna para **EPSEL S.A.** (Lambayeque, Perú).

Proyecto académico del curso **Sistemas de Información Empresarial** — Universidad Tecnológica del Perú (UTP), sede Chiclayo.

> Los datos de sectores, cisternas y placas incluidos en el sistema son **referenciales**, no oficiales de EPSEL.
> Las cuentas de prueba están en [`backend/README.md`](backend/README.md#datos-de-prueba-referenciales).

## Núcleo de valor

- Planificación por capacidad (viajes y cisternas necesarios vs. disponibles, según SUNASS).
- Verificación de entregas con GPS y geocerca.
- Información al vecino sobre cuándo llega el agua.

## Estado

**v0.1.0 (Avance 1)**: backend REST completo del MVP (RF01–RF09). Pendientes: app móvil y panel web.
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
| Panel web (React / Vite) | Administrador + panel público de transparencia | [`web/`](web/) |

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

Las instrucciones de cada cliente se agregan en su carpeta a medida que se configura (`feature/backend-setup`, `feature/mobile-setup`, `feature/web-setup`).

## Documentación para el equipo

El contexto completo del proyecto (alcance, modelo de datos, reglas de negocio, API) está en [`CLAUDE.md`](CLAUDE.md).
