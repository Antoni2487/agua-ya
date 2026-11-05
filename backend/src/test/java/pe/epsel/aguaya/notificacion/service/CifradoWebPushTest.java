package pe.epsel.aguaya.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

/**
 * Verifica el cifrado haciendo el papel del navegador: descifra con su clave privada siguiendo RFC 8291 y
 * RFC 8188, de forma independiente al código que cifra.
 */
class CifradoWebPushTest {

    @Test
    void elNavegadorDescifraElMensaje() throws Exception {
        KeyPair navegador = parP256();
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);
        byte[] publicaNavegador = CifradoWebPush.puntoSinComprimir((ECPublicKey) navegador.getPublic());
        byte[] mensaje = "{\"titulo\":\"La cisterna está cerca\"}".getBytes(StandardCharsets.UTF_8);

        byte[] cuerpo = CifradoWebPush.cifrar(mensaje, CifradoWebPush.codificar(publicaNavegador),
                CifradoWebPush.codificar(auth));

        assertThat(descifrarComoNavegador(cuerpo, navegador, publicaNavegador, auth)).isEqualTo(mensaje);
    }

    @Test
    void cadaMensajeUsaSalYClaveEfimeraDistintas() throws Exception {
        KeyPair navegador = parP256();
        String p256dh = CifradoWebPush.codificar(CifradoWebPush.puntoSinComprimir((ECPublicKey) navegador.getPublic()));
        String auth = CifradoWebPush.codificar(new byte[16]);

        byte[] a = CifradoWebPush.cifrar("hola".getBytes(StandardCharsets.UTF_8), p256dh, auth);
        byte[] b = CifradoWebPush.cifrar("hola".getBytes(StandardCharsets.UTF_8), p256dh, auth);

        assertThat(Arrays.copyOf(a, 16)).isNotEqualTo(Arrays.copyOf(b, 16));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void jwtVapidSeVerificaConLaClavePublica() throws Exception {
        KeyPair vapid = parP256();
        String jwt = CifradoWebPush.jwtVapid("https://fcm.googleapis.com", "mailto:aguaya@example.com", 1_900_000_000L,
                (ECPrivateKey) vapid.getPrivate());

        String[] partes = jwt.split("\\.");
        assertThat(partes).hasSize(3);
        String carga = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        assertThat(carga).contains("\"aud\":\"https://fcm.googleapis.com\"", "\"exp\":1900000000",
                "\"sub\":\"mailto:aguaya@example.com\"");

        Signature verificador = Signature.getInstance("SHA256withECDSAinP1363Format");
        verificador.initVerify(vapid.getPublic());
        verificador.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verificador.verify(Base64.getUrlDecoder().decode(partes[2]))).isTrue();
    }

    @Test
    void clavePrivadaDesdeBase64urlFirmaIgualQueLaOriginal() throws Exception {
        KeyPair vapid = parP256();
        byte[] d = ((ECPrivateKey) vapid.getPrivate()).getS().toByteArray();
        byte[] d32 = new byte[32];
        System.arraycopy(d, Math.max(0, d.length - 32), d32, Math.max(0, 32 - d.length), Math.min(32, d.length));

        ECPrivateKey reconstruida = CifradoWebPush.clavePrivada(CifradoWebPush.codificar(d32));

        assertThat(reconstruida.getS()).isEqualTo(((ECPrivateKey) vapid.getPrivate()).getS());
    }

    @Test
    void rechazaClavesMalFormadas() {
        assertThatThrownBy(() -> CifradoWebPush.clavePublica(new byte[64]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CifradoWebPush.clavePrivada(CifradoWebPush.codificar(new byte[10])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static KeyPair parP256() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
        generador.initialize(new ECGenParameterSpec("secp256r1"));
        return generador.generateKeyPair();
    }

    /** Lo que hace el navegador al recibir el push (RFC 8291 §3.4 y RFC 8188 §2). */
    private static byte[] descifrarComoNavegador(byte[] cuerpo, KeyPair navegador, byte[] publicaNavegador,
                                                 byte[] auth) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(cuerpo);
        byte[] sal = new byte[16];
        buffer.get(sal);
        assertThat(buffer.getInt()).isEqualTo(CifradoWebPush.TAMANO_REGISTRO);
        byte[] publicaServidor = new byte[buffer.get()];
        buffer.get(publicaServidor);
        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);

        KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
        acuerdo.init(navegador.getPrivate());
        acuerdo.doPhase(CifradoWebPush.clavePublica(publicaServidor), true);
        byte[] ecdh = acuerdo.generateSecret();

        byte[] ikm = CifradoWebPush.hkdf(auth, ecdh, CifradoWebPush.concatenar(
                "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), publicaNavegador, publicaServidor), 32);
        byte[] cek = CifradoWebPush.hkdf(sal, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = CifradoWebPush.hkdf(sal, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] plano = aes.doFinal(cifrado);
        assertThat(plano[plano.length - 1]).as("delimitador de último registro").isEqualTo((byte) 2);
        return Arrays.copyOf(plano, plano.length - 1);
    }
}
