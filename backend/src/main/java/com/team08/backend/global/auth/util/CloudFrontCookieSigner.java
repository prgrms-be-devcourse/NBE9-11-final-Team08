package com.team08.backend.global.auth.util;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
public class CloudFrontCookieSigner {

    @Value("${cloud.aws.cloudfront.distribution-domain:localhost}")
    private String distributionDomain;

    @Value("${cloud.aws.cloudfront.key-pair-id:dummy-id}")
    private String keyPairId;

    @Value("${cloud.aws.cloudfront.private-key:-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC3\n-----END PRIVATE KEY-----}")
    private String privateKeyPem;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            if (privateKeyPem.contains("dummy") || privateKeyPem.length() < 100) {
                return;
            }
            String privateKeyRaw = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyRaw);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(spec);
        } catch (Exception e) {
            log.error("CloudFront private key initialization failed", e);
        }
    }

    public ResponseCookie[] createSignedCookies(Long lectureId, String uuid) {
        String resourcePath = "https://" + distributionDomain + "/lectures/" + lectureId + "/" + uuid + "/*";
        long expires = Instant.now().plus(Duration.ofHours(1)).getEpochSecond();

        String policy = "{\"Statement\":[{\"Resource\":\"" + resourcePath +
                "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + expires + "}}}]}";

        String base64Policy = encodeBase64(policy.getBytes(StandardCharsets.UTF_8));
        String signature = signPolicy(policy);

        return new ResponseCookie[]{
                buildCookie("CloudFront-Policy", base64Policy),
                buildCookie("CloudFront-Signature", signature),
                buildCookie("CloudFront-Key-Pair-Id", keyPairId)
        };
    }

    private String signPolicy(String policy) {
        try {
            if (privateKey == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(policy.getBytes(StandardCharsets.UTF_8));

            return encodeBase64(sig.sign());
        } catch (Exception e) {
            log.error("CloudFront signature generation failed", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseCookie buildCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();
    }

    private String encodeBase64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                .replace('_', '~');
    }
}