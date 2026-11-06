# AguaYa: app móvil (vecino y conductor)

Una sola app de Expo (SDK 58, React Native, TypeScript) para los dos roles. Según quién inicia sesión, muestra las pantallas del vecino o las del conductor. El administrador usa el panel web.

## Cómo probarla en el celular

1. Instala **Expo Go** en el celular (Play Store o App Store).
2. En esta carpeta: `npm install` y luego `npx expo start --tunnel`. Con `--tunnel` no hace falta estar en la misma red wifi.
3. Escanea el código QR con Expo Go (Android) o con la cámara (iPhone).

La app usa el backend de Render (`https://aguaya-backend.onrender.com`). Para usar otro: `EXPO_PUBLIC_API_URL=http://192.168.x.x:8080 npx expo start`.

## Cuentas de prueba (datos de V3)

Todos entran por la misma pantalla de ingreso.

| Rol | Cómo entra |
|---|---|
| Vecino | DNI `30000001` a `30000012` y la clave `1234` (V11). Los de P.J. Atusparias son `30000004`, `30000005` y `30000006` |
| Conductor | Toca «Soy conductor de cisterna». DNI `20000001`, `20000002` o `20000003`, contraseña `Conductor2026!` |

Un vecino nuevo se registra con DNI, celular y código SMS, y elige su clave de 4 números. El SMS solo vuelve a usarse
si olvida la clave («¿Olvidaste tu clave?»). Con `OTP_PROVIDER=log` el código sale en los logs del backend; en dev
(`exponer-codigo`) la app lo escribe sola.

## Simular el recorrido de la cisterna

Sin manejar de verdad: desde la raíz del repo,

```bash
node scripts/simular-ruta.mjs
```

Ingresa como el conductor `20000001`, inicia su ruta de hoy, registra un viaje y lleva el camión de parada en parada
enviando posiciones (cada 3 s) y marcando cada entrega (la geocerca la valida). Se ve en vivo en el panel del admin
(Mapa en vivo) y en la app del vecino (pestaña Mapa). Antes, el admin debe programar una ruta **para hoy** y asignarle
ese conductor y una cisterna. Opciones: `--ruta ID`, `--cada SEGUNDOS`, `--sin-atender` (solo mueve el camión; las
entregas se marcan en la app del conductor), `--api URL` (por defecto Render).

## Qué hace

**Vecino** (diseño del Figma «Vecino · móvil», pestañas Inicio, Mapa, Reportar y Mi cuenta):
- se registra con DNI, celular, código SMS y una clave de 4 números; marca su casa con el GPS o moviendo el pin;
- ve su próxima cisterna: día, turno, cuántas paradas faltan antes de la suya (tira de paradas y gota de avance) y la placa del camión;
- ve el camión en el mapa mientras reparte en su sector (se actualiza cada 15 s);
- califica la última entrega, reporta problemas y sigue sus reportes.

**Conductor:**
- ve sus rutas de hoy e inicia la ruta;
- envía su GPS cada 15 s con la pantalla encendida;
- registra cada viaje (litros cargados) y lo termina;
- ve las paradas en el mapa, con la distancia a cada una, y marca entregas: "Entregué el agua", validado por la geocerca de 50 m, o "No pude";
- reporta incidencias (con la opción de interrumpir la ruta) y termina la ruta.

**Push:** se registra el token de Expo al iniciar sesión. En Expo Go (SDK 53+) Android no recibe push remotos: hace falta un build de desarrollo con EAS (`eas build --profile development`) y su `projectId`.

## Limitaciones del MVP

- **GPS solo con la app abierta.** El GPS se envía con la app en primer plano y la pantalla siempre encendida durante la ruta. Para enviarlo en segundo plano hace falta un build propio con `expo-task-manager`, porque Expo Go no lo permite.
- **Mapas en un APK propio.** Usan los nativos (Google Maps en Android). Para publicar el APK se necesita una API key de Google Maps en `app.json`.

## Comprobar antes de subir

```bash
npx tsc --noEmit
npx expo export --platform android
```

El segundo comando empaqueta la app y detecta errores de importación.
