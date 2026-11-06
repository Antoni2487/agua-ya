# AguaYa: app móvil (vecino y conductor)

Una sola app de Expo (SDK 58, React Native, TypeScript) para los dos roles. Según quién inicia sesión, muestra las pantallas del vecino o las del conductor. El administrador usa el panel web.

## Cómo probarla en el celular

1. Instala **Expo Go** en el celular (Play Store o App Store).
2. En esta carpeta: `npm install` y luego `npx expo start --tunnel`. Con `--tunnel` no hace falta estar en la misma red wifi.
3. Escanea el código QR con Expo Go (Android) o con la cámara (iPhone).

La app usa el backend de Render (`https://aguaya-backend.onrender.com`). Para usar otro: `EXPO_PUBLIC_API_URL=http://192.168.x.x:8080 npx expo start`.

## Cuentas de prueba (datos de V3)

| Rol | Cómo entra |
|---|---|
| Conductor | DNI `20000001`, `20000002` o `20000003`, contraseña `Conductor2026!` |
| Vecino | DNI `30000001` y siguientes: se pide el código por SMS. Con `OTP_PROVIDER=log` el código sale en los logs del backend; en dev (`exponer-codigo`) la app lo escribe sola |

Para que el conductor vea algo, el administrador debe programar una ruta **para hoy** en el panel web y asignarle cisterna y conductor.

## Qué hace

**Vecino:**
- se registra con DNI, celular y código SMS; marca su casa con el GPS o moviendo el pin del mapa;
- ve su próxima cisterna (día, turno y cuántas paradas faltan antes que la suya);
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
