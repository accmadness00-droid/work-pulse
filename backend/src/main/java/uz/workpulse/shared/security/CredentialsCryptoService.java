package uz.workpulse.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uz.workpulse.shared.config.JwtConfig;

@Service
public class CredentialsCryptoService {

    private static final String ENCRYPTED_PREFIX = "enc:v1:";
    private static final String VAULT_PREFIX = "vault:";
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public CredentialsCryptoService(JwtConfig jwtConfig) {
        byte[] keyBytes = Arrays.copyOf(jwtConfig.secret().getBytes(StandardCharsets.UTF_8), 32);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        if (plainText.startsWith(ENCRYPTED_PREFIX) || plainText.startsWith(VAULT_PREFIX)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[12];
            new java.security.SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encrypt credentials secret", ex);
        }
    }

    public String decrypt(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return storedValue;
        }
        if (storedValue.startsWith(VAULT_PREFIX)) {
            return resolveVaultReference(storedValue);
        }
        if (!storedValue.startsWith(ENCRYPTED_PREFIX)) {
            return storedValue;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedValue.substring(ENCRYPTED_PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to decrypt credentials secret", ex);
        }
    }

    private String resolveVaultReference(String vaultReference) {
        String secretName = vaultReference.substring(VAULT_PREFIX.length());
        String envKey = secretName.replace('-', '_').replace('/', '_').toUpperCase(Locale.ROOT);
        String envValue = System.getenv(envKey);
        if (StringUtils.hasText(envValue)) {
            return envValue;
        }
        throw new IllegalStateException("Vault reference not resolved: " + vaultReference);
    }
}
