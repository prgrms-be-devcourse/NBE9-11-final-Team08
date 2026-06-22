package com.team08.backend.global.auth.util;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CloudFrontCookieSigner {

    @Value("${cloud.aws.cloudfront.distribution-domain:cloudfront-domain}")
    private String distributionDomain;

    @Value("${cloud.aws.cloudfront.key-pair-id:dummy-id}")
    private String keyPairId;

    @Value("${cloud.aws.cloudfront.private-key:cloudfront-private-key}")
    private String privateKeyPem;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        boolean isDevOrTest = isDevelopmentOrTestEnvironment();

        try {
            if (isDevOrTest) {
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
            throw new IllegalStateException("CloudFront 키 초기화 실패로 애플리케이션을 시작할 수 없습니다.", e);
        }
        // Todo: 비디오 관련 기능만 별도의 가벼운 독립 서버로 분리하면, 전체 서비스가 터지지 않으면서 다른 기능들은 정상적으로 동작할 수 있음. 현재는 CloudFront 키 초기화 실패 시 전체 서비스가 터짐.
    }

    public ResponseCookie[] createSignedCookies(String resourcePath, String cookiePath) {
        String fullResourceUrl = "https://" + distributionDomain + resourcePath;
        long expires = Instant.now().plus(Duration.ofHours(1)).getEpochSecond();

        String policy = "{\"Statement\":[{\"Resource\":\"" + fullResourceUrl +
                "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + expires + "}}}]}";

        String base64Policy = encodeBase64(policy.getBytes(StandardCharsets.UTF_8));
        String signature = signPolicy(policy);

        return new ResponseCookie[]{
                buildCookie("CloudFront-Policy", base64Policy, cookiePath),
                buildCookie("CloudFront-Signature", signature, cookiePath),
                buildCookie("CloudFront-Key-Pair-Id", keyPairId, cookiePath)
        };
    }

    private String signPolicy(String policy) {
        try {
            if (privateKey == null) {
                if (isDevelopmentOrTestEnvironment()) {
                    return "dummy-signature";
                }
                throw new IllegalStateException("CloudFront 서명 키가 설정되지 않았습니다.");
            }
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(policy.getBytes(StandardCharsets.UTF_8));

            return encodeBase64(sig.sign());
        } catch (CustomException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("CloudFront signature generation failed", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isDevelopmentOrTestEnvironment() {
        return "cloudfront-private-key".equals(privateKeyPem) ||
                "dummy-id".equals(keyPairId) ||
                privateKeyPem.contains("cloudfront-private-key");
    }

    private ResponseCookie buildCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(path)
                .maxAge(Duration.ofHours(1))
                .build();
    }

    private String encodeBase64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                .replace('_', '~');
    }
}