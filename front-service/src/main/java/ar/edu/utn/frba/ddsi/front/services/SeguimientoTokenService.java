package ar.edu.utn.frba.ddsi.front.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Convierte el id interno de una ruta en un token opaco para el mapa autenticado.
 * El id no aparece en la URL ni en la página; el token es AES/GCM y no se puede
 * modificar sin la clave del servidor.
 */
@Service
public class SeguimientoTokenService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec clave;
    private final SecureRandom random = new SecureRandom();

    public SeguimientoTokenService(
            @Value("${seguimiento.token-secret:donatrack-seguimiento-local}") String secreto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(secreto.getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar el token de seguimiento", e);
        }
    }

    public String generar(UUID idRuta) {
        if (idRuta == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cifrado = cipher.doFinal(idRuta.toString().getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[IV_LENGTH + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, IV_LENGTH);
            System.arraycopy(cifrado, 0, salida, IV_LENGTH, cifrado.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el token de seguimiento", e);
        }
    }

    public Optional<UUID> resolver(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            byte[] datos = Base64.getUrlDecoder().decode(token);
            if (datos.length <= IV_LENGTH) return Optional.empty();
            byte[] iv = Arrays.copyOfRange(datos, 0, IV_LENGTH);
            byte[] cifrado = Arrays.copyOfRange(datos, IV_LENGTH, datos.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            String id = new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
            return Optional.of(UUID.fromString(id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
