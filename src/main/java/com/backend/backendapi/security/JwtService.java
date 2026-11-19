package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Firma y valida los JWT HS256. El secreto configurado se expande con SHA-256
 * para garantizar una clave de 256 bits aunque el valor original sea corto.
 */
@Service
public class JwtService {

    private final MACSigner signer;
    private final MACVerifier verifier;
    private final Duration timeToLive;

    public JwtService(AppProperties properties) {
        byte[] key = deriveKey(properties.jwt().secret());
        try {
            this.signer = new MACSigner(key);
            this.verifier = new MACVerifier(key);
        } catch (JOSEException ex) {
            throw new IllegalStateException("No se pudo inicializar la firma JWT", ex);
        }
        this.timeToLive = Duration.ofMinutes(properties.jwt().expirationMinutes());
    }

    public String createToken(String username) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(timeToLive)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException ex) {
            throw new IllegalStateException("No se pudo firmar el token JWT", ex);
        }
        return jwt.serialize();
    }

    /**
     * @return el username del token si la firma y la expiracion son validas.
     */
    public Optional<String> validateAndGetUsername(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                return Optional.empty();
            }
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(subject);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static byte[] deriveKey(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
