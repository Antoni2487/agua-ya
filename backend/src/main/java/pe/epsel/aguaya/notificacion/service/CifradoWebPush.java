package pe.epsel.aguaya.notificacion.service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Criptografía de Web Push con la JDK, sin librerías externas:
 * <ul>
 *   <li>Cifrado del mensaje para el navegador: RFC 8291 (ECDH P-256 + HKDF-SHA256) con codificación
 *       {@code aes128gcm} de RFC 8188, en un solo registro.</li>
 *   <li>Autorización VAPID ante el servicio de push: RFC 8292, un JWT ES256 firmado con la clave privada.</li>
 * </ul>
 * Las claves viajan en base64url sin relleno, como las entrega el navegador ({@code PushSubscription.toJSON()}).
 */
public final class CifradoWebPush {

    /** Tamaño de registro declarado en la cabecera aes128gcm; el mensaje cabe en uno solo. */
    static final int TAMANO_REGISTRO = 4096;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();
    private static final ECParameterSpec P256 = parametrosP256();

    private CifradoWebPush() {
    }

    /**
     * Cifra {@code mensaje} para la suscripción del navegador.
     *
     * @param p256dh clave pública del navegador (65 bytes sin comprimir, base64url)
     * @param auth   secreto de autenticación del navegador (16 bytes, base64url)
     * @return el cuerpo de la petición: cabecera aes128gcm + texto cifrado
     */
    public static byte[] cifrar(byte[] mensaje, String p256dh, String auth) {
        try {
            byte[] clavePublicaNavegador = decodificar(p256dh);
            byte[] secretoAuth = decodificar(auth);
            ECPublicKey publicaNavegador = clavePublica(clavePublicaNavegador);

            // Par efímero del servidor, uno por mensaje
            KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
            generador.initialize(P256);
            KeyPair efimero = generador.generateKeyPair();
            byte[] clavePublicaServidor = puntoSinComprimir((ECPublicKey) efimero.getPublic());

            KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
            acuerdo.init(efimero.getPrivate());
            acuerdo.doPhase(publicaNavegador, true);
            byte[] secretoEcdh = acuerdo.generateSecret();

            // RFC 8291 §3.3: IKM = HKDF(auth, ecdh, "WebPush: info" || 0x00 || ua_public || as_public, 32)
            byte[] infoClave = concatenar("WebPush: info\0".getBytes(StandardCharsets.US_ASCII),
                    clavePublicaNavegador, clavePublicaServidor);
            byte[] ikm = hkdf(secretoAuth, secretoEcdh, infoClave, 32);

            // RFC 8188 §2.2: CEK y nonce a partir de una sal aleatoria
            byte[] sal = new byte[16];
            RANDOM.nextBytes(sal);
            byte[] cek = hkdf(sal, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
            byte[] nonce = hkdf(sal, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

            // Un solo registro: el mensaje + delimitador 0x02 (último registro), sin relleno extra
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            byte[] cifrado = aes.doFinal(concatenar(mensaje, new byte[] {2}));

            ByteBuffer cabecera = ByteBuffer.allocate(16 + 4 + 1 + clavePublicaServidor.length);
            cabecera.put(sal).putInt(TAMANO_REGISTRO).put((byte) clavePublicaServidor.length).put(clavePublicaServidor);
            return concatenar(cabecera.array(), cifrado);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo cifrar el mensaje Web Push", e);
        }
    }

    /**
     * JWT VAPID (RFC 8292) para el encabezado {@code Authorization: vapid t=<jwt>, k=<clave pública>}.
     *
     * @param audiencia origen del endpoint (p. ej. {@code https://fcm.googleapis.com})
     * @param expira    segundos epoch; como máximo 24 h en el futuro
     */
    public static String jwtVapid(String audiencia, String sujeto, long expira, ECPrivateKey clavePrivada) {
        String cabecera = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String carga = B64.encodeToString(("{\"aud\":\"" + audiencia + "\",\"exp\":" + expira + ",\"sub\":\""
                + sujeto + "\"}").getBytes(StandardCharsets.UTF_8));
        String firmado = cabecera + "." + carga;
        try {
            // ES256 de JWS: firma R || S de 64 bytes (formato P1363), no DER
            Signature firma = Signature.getInstance("SHA256withECDSAinP1363Format");
            firma.initSign(clavePrivada);
            firma.update(firmado.getBytes(StandardCharsets.US_ASCII));
            return firmado + "." + B64.encodeToString(firma.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el JWT VAPID", e);
        }
    }

    /** Clave privada VAPID desde su valor {@code d} en base64url (32 bytes). */
    public static ECPrivateKey clavePrivada(String base64url) {
        byte[] d = decodificar(base64url);
        if (d.length != 32) {
            throw new IllegalArgumentException("La clave privada VAPID debe tener 32 bytes");
        }
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Clave privada VAPID inválida", e);
        }
    }

    /** Clave pública P-256 desde su forma sin comprimir (0x04 || X || Y). */
    public static ECPublicKey clavePublica(byte[] sinComprimir) {
        if (sinComprimir.length != 65 || sinComprimir[0] != 4) {
            throw new IllegalArgumentException("La clave pública debe ser un punto P-256 sin comprimir (65 bytes)");
        }
        ECPoint punto = new ECPoint(new BigInteger(1, Arrays.copyOfRange(sinComprimir, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(sinComprimir, 33, 65)));
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(punto, P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Clave pública inválida (no es un punto de P-256)", e);
        }
    }

    public static byte[] puntoSinComprimir(ECPublicKey clave) {
        return concatenar(new byte[] {4}, a32Bytes(clave.getW().getAffineX()), a32Bytes(clave.getW().getAffineY()));
    }

    public static byte[] decodificar(String base64url) {
        return B64_DECODER.decode(base64url.trim().replace('+', '-').replace('/', '_').replace("=", ""));
    }

    public static String codificar(byte[] bytes) {
        return B64.encodeToString(bytes);
    }

    /** HKDF-SHA256 (RFC 5869) para salidas de hasta 32 bytes: un solo bloque de expansión. */
    static byte[] hkdf(byte[] sal, byte[] ikm, byte[] info, int largo) throws GeneralSecurityException {
        byte[] prk = hmac(sal, ikm);
        return Arrays.copyOf(hmac(prk, concatenar(info, new byte[] {1})), largo);
    }

    private static byte[] hmac(byte[] clave, byte[] datos) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(clave, "HmacSHA256"));
        return mac.doFinal(datos);
    }

    private static byte[] a32Bytes(BigInteger valor) {
        byte[] bytes = valor.toByteArray();
        if (bytes.length == 32) {
            return bytes;
        }
        byte[] resultado = new byte[32];
        int largo = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - largo, resultado, 32 - largo, largo);
        return resultado;
    }

    static byte[] concatenar(byte[]... partes) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        for (byte[] parte : partes) {
            salida.writeBytes(parte);
        }
        return salida.toByteArray();
    }

    private static ECParameterSpec parametrosP256() {
        try {
            AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
            parametros.init(new ECGenParameterSpec("secp256r1"));
            return parametros.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("La JDK no soporta la curva P-256", e);
        }
    }
}
