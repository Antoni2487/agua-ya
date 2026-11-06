// Notificaciones push (Expo). El backend avisa al vecino: ruta iniciada, cisterna cerca, entrega registrada
// y reasignación. Es de mejor esfuerzo: en Expo Go o sin projectId de EAS no hay token y la app sigue igual.
import Constants from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { api } from './api/cliente';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true, shouldShowList: true, shouldPlaySound: true, shouldSetBadge: false,
  }),
});

let tokenRegistrado: string | null = null;

export async function registrarPush(): Promise<void> {
  try {
    if (!Device.isDevice) return;
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('aguaya', {
        name: 'Avisos de AguaYa', importance: Notifications.AndroidImportance.HIGH,
      });
    }
    const permiso = await Notifications.requestPermissionsAsync();
    if (!permiso.granted) return;
    const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
    if (!projectId) return;
    const { data } = await Notifications.getExpoPushTokenAsync({ projectId });
    await api('/api/dispositivos', {
      metodo: 'POST',
      cuerpo: { expoToken: data, plataforma: Platform.OS === 'ios' ? 'IOS' : 'ANDROID' },
    });
    tokenRegistrado = data;
  } catch {
    // Sin push no se pierde nada: la app consulta su estado al abrirse.
  }
}

export async function darDeBajaPush(): Promise<void> {
  if (!tokenRegistrado) return;
  try {
    await api('/api/dispositivos', { metodo: 'DELETE', cuerpo: { expoToken: tokenRegistrado } });
  } catch {
    // Si falla, el backend la elimina cuando Expo responda DeviceNotRegistered.
  }
  tokenRegistrado = null;
}
