package com.example.veylora.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Encrypts users' mailbox passwords with AES-GCM. The key comes from MAIL_ENCRYPTION_KEY in .env. */
@Component
public class MailCrypto {

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public MailCrypto(@Value("${MAIL_ENCRYPTION_KEY:}") String secret) {
        SecretKeySpec k = null;
        if (secret != null && secret.length() >= 16) {
            try {
                k = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)), "AES");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        this.key = k;
    }

    public boolean isReady() { return key != null; }

    public String encrypt(String plain) {
        requireReady();
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] enc = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt the password", e);
        }
    }

    public String decrypt(String stored) {
        requireReady();
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the saved mail password. Save it again in Settings.", e);
        }
    }

    private void requireReady() {
        if (key == null) {
            throw new IllegalStateException("MAIL_ENCRYPTION_KEY is not set on the server (at least 16 characters). Add it to .env and restart.");
        }
    }
}
