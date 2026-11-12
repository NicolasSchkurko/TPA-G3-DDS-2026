package ar.edu.utn.frba.ddsi.front.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Genera y valida el token del link de verificación de email.
 * El token lleva cifrados el id de Keycloak y su vencimiento (AES/GCM),
 * así que no se puede falsificar ni estirar sin la clave del servidor.
 */
@Service
public class VerificacionEmailTokenService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final Duration VIGENCIA = Duration.ofHours(24);
    private static final String SEPARADOR = "|";

    private final SecretKeySpec clave;
    private final SecureRandom random = new SecureRandom();

    public VerificacionEmailTokenService(
            @Value("${verificacion.token-secret:donatrack-verificacion-local}") String secreto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(secreto.getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar el token de verificación", e);
        }
    }

    public String generar(String userId) {
        if (userId == null || userId.isBlank()) return null;
        long vencimiento = Instant.now().plus(VIGENCIA).toEpochMilli();
        String contenido = userId + SEPARADOR + vencimiento;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cifrado = cipher.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[IV_LENGTH + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, IV_LENGTH);
            System.arraycopy(cifrado, 0, salida, IV_LENGTH, cifrado.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el token de verificación", e);
        }
    }

    public Optional<String> resolver(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            byte[] datos = Base64.getUrlDecoder().decode(token);
            if (datos.length <= IV_LENGTH) return Optional.empty();
            byte[] iv = Arrays.copyOfRange(datos, 0, IV_LENGTH);
            byte[] cifrado = Arrays.copyOfRange(datos, IV_LENGTH, datos.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            String contenido = new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);

            String[] partes = contenido.split("\\|");
            if (partes.length != 2 || partes[0].isBlank()) return Optional.empty();

            Instant vencimiento = Instant.ofEpochMilli(Long.parseLong(partes[1]));
            if (vencimiento.isBefore(Instant.now())) return Optional.empty();

            return Optional.of(partes[0]);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
