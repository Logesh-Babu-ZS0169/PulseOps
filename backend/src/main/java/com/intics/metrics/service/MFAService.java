package com.intics.metrics.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.intics.metrics.entity.MFASecret;
import com.intics.metrics.entity.RecoveryCode;
import com.intics.metrics.entity.User;
import com.intics.metrics.repository.MFASecretRepository;
import com.intics.metrics.repository.RecoveryCodeRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MFAService {

    private final MFASecretRepository mfaSecretRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    private static final String ISSUER = "Inbound Metrics";
    private static final int QR_CODE_SIZE = 400;
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String ENCRYPTION_KEY = "MetricsDashboard"; // TODO: Move to secure config

    @Transactional
    public Map<String, Object> enrollMFA(User user) {
        log.info("Starting MFA enrollment for user: {}", user.getUsername());

        // Check if user already has MFA
        Optional<MFASecret> existing = mfaSecretRepository.findByUser(user);
        if (existing.isPresent() && existing.get().getIsEnabled()) {
            throw new RuntimeException("MFA is already enabled for this user");
        }

        // Generate TOTP secret
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secretKey = key.getKey();

        // Encrypt secret before storing
        String encryptedSecret = encryptSecret(secretKey);

        // Save or update MFA secret
        MFASecret mfaSecret;
        if (existing.isPresent()) {
            mfaSecret = existing.get();
            mfaSecret.setSecretKey(encryptedSecret);
            mfaSecret.setUpdatedAt(LocalDateTime.now());
        } else {
            mfaSecret = new MFASecret();
            mfaSecret.setUser(user);
            mfaSecret.setSecretKey(encryptedSecret);
            mfaSecret.setIsEnabled(false);
        }
        mfaSecretRepository.save(mfaSecret);

        // Generate QR code URL
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                ISSUER,
                user.getUsername(),
                key
        );

        // Generate QR code image as Base64
        String qrCodeImage = generateQRCodeImage(qrCodeUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("secretKey", secretKey);
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("qrCodeImage", qrCodeImage);
        response.put("issuer", ISSUER);
        response.put("username", user.getUsername());
        response.put("message", "Scan the QR code with Google Authenticator or Authy. If scanning fails, manually enter the secret key.");

        log.info("MFA enrollment initiated for user: {} with secret key (first 4 chars): {}...",
                user.getUsername(), secretKey.substring(0, Math.min(4, secretKey.length())));
        return response;
    }

    @Transactional
    public boolean verifyAndEnable(User user, int code) {
        log.info("Verifying MFA code for user: {}", user.getUsername());

        MFASecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("MFA not enrolled for this user"));

        String decryptedSecret = decryptSecret(mfaSecret.getSecretKey());
        boolean isValid = googleAuthenticator.authorize(decryptedSecret, code);

        if (isValid) {
            mfaSecret.setIsEnabled(true);
            mfaSecret.setEnabledAt(LocalDateTime.now());
            mfaSecret.setLastUsedAt(LocalDateTime.now());
            mfaSecretRepository.save(mfaSecret);
            log.info("MFA enabled successfully for user: {}", user.getUsername());
        } else {
            log.warn("Invalid MFA code provided for user: {}", user.getUsername());
        }

        return isValid;
    }

    public boolean verify(User user, int code) {
        log.info("Verifying MFA code for user: {}", user.getUsername());

        MFASecret mfaSecret = mfaSecretRepository.findByUserAndIsEnabled(user, true)
                .orElseThrow(() -> new RuntimeException("MFA is not enabled for this user"));

        String decryptedSecret = decryptSecret(mfaSecret.getSecretKey());
        boolean isValid = googleAuthenticator.authorize(decryptedSecret, code);

        if (isValid) {
            mfaSecret.setLastUsedAt(LocalDateTime.now());
            mfaSecretRepository.save(mfaSecret);
            log.info("MFA verification successful for user: {}", user.getUsername());
        } else {
            log.warn("Invalid MFA code provided for user: {}", user.getUsername());
        }

        return isValid;
    }

    @Transactional
    public boolean verifyRecoveryCode(User user, String code) {
        log.info("Verifying recovery code for user: {}", user.getUsername());

        Optional<RecoveryCode> recoveryCode = recoveryCodeRepository.findByCodeAndIsUsed(code, false);

        if (recoveryCode.isPresent() && recoveryCode.get().getUser().getId().equals(user.getId())) {
            RecoveryCode rc = recoveryCode.get();
            rc.setIsUsed(true);
            rc.setUsedAt(LocalDateTime.now());
            recoveryCodeRepository.save(rc);
            log.info("Recovery code verified and marked as used for user: {}", user.getUsername());
            return true;
        }

        log.warn("Invalid or already used recovery code for user: {}", user.getUsername());
        return false;
    }

    @Transactional
    public List<String> generateRecoveryCodes(User user) {
        log.info("Generating recovery codes for user: {}", user.getUsername());

        // Delete existing recovery codes
        recoveryCodeRepository.deleteByUser(user);

        // Generate new recovery codes
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = generateRandomCode(random);

            RecoveryCode recoveryCode = new RecoveryCode();
            recoveryCode.setUser(user);
            recoveryCode.setCode(code);
            recoveryCode.setIsUsed(false);
            recoveryCodeRepository.save(recoveryCode);

            codes.add(code);
        }

        // Mark in MFA secret that recovery codes were generated
        mfaSecretRepository.findByUser(user).ifPresent(mfaSecret -> {
            mfaSecret.setBackupCodesGenerated(true);
            mfaSecretRepository.save(mfaSecret);
        });

        log.info("Generated {} recovery codes for user: {}", RECOVERY_CODE_COUNT, user.getUsername());
        return codes;
    }

    public boolean isMFAEnabled(User user) {
        return mfaSecretRepository.findByUserAndIsEnabled(user, true).isPresent();
    }

    public Map<String, Object> getMFAStatus(User user) {
        Map<String, Object> status = new HashMap<>();

        Optional<MFASecret> mfaSecret = mfaSecretRepository.findByUser(user);

        if (mfaSecret.isPresent()) {
            MFASecret secret = mfaSecret.get();
            status.put("enabled", secret.getIsEnabled());
            status.put("enabledAt", secret.getEnabledAt());
            status.put("lastUsedAt", secret.getLastUsedAt());
            status.put("backupCodesGenerated", secret.getBackupCodesGenerated());

            long unusedCodes = recoveryCodeRepository.countByUserAndIsUsed(user, false);
            status.put("unusedRecoveryCodes", unusedCodes);
        } else {
            status.put("enabled", false);
            status.put("enrollmentRequired", true);
        }

        return status;
    }

    @Transactional
    public void disableMFA(User user) {
        log.info("Disabling MFA for user: {}", user.getUsername());

        mfaSecretRepository.findByUser(user).ifPresent(mfaSecret -> {
            mfaSecret.setIsEnabled(false);
            mfaSecretRepository.save(mfaSecret);
        });

        // Delete recovery codes
        recoveryCodeRepository.deleteByUser(user);

        log.info("MFA disabled for user: {}", user.getUsername());
    }

    private String generateRandomCode(SecureRandom random) {
        int code = 100000 + random.nextInt(900000);
        return String.format("%06d", code);
    }

    private String generateQRCodeImage(String url) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

            log.debug("Generated QR code for URL: {}", url);
            return base64Image;
        } catch (WriterException | java.io.IOException e) {
            log.error("Failed to generate QR code image", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private String encryptSecret(String secret) {
        try {
            byte[] keyBytes = Arrays.copyOf(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), 16);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encrypt MFA secret", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decryptSecret(String encryptedSecret) {
        try {
            byte[] keyBytes = Arrays.copyOf(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), 16);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedSecret));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt MFA secret", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

