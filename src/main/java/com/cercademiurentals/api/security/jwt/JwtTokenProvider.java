package com.cercademiurentals.api.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecretString; // La clave secreta como String desde application.properties

    @Value("${jwt.expiration.ms}")
    private int jwtExpirationInMs;

    private Key jwtSecretKey; // La clave secreta como objeto Key

    @PostConstruct
    public void init() {
        // Convierte la clave secreta String a un objeto Key seguro para HS512
        // Es importante que jwtSecretString sea suficientemente largo y complejo para HS512.
        // Una buena práctica es que sea una cadena codificada en Base64 de al menos 64 bytes.
        // Si tu jwtSecretString no está en Base64, puedes usar:
        // this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));
        // Pero si ya es una cadena aleatoria larga, el siguiente método es más directo para Keys.secretKeyFor
        // Sin embargo, para reproducibilidad entre reinicios si no guardas la Key, es mejor generarla a partir del String.
        // Asegúrate de que tu jwt.secret en application.properties sea una cadena segura y suficientemente larga.
        // Para este ejemplo, asumimos que jwtSecretString es una clave suficientemente fuerte.
        // Una forma más robusta si tu string no es base64 es:
        // byte[] keyBytes = Decoders.BASE64.decode(jwtSecretString); // Si tu secret está en Base64
        // this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
        // O si es un string simple (NO RECOMENDADO PARA PRODUCCIÓN POR SEGURIDAD):
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretString.getBytes());
        logger.info("JwtSecretKey inicializada.");
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512) // Usar la Key y el algoritmo
                .compact();
    }
    
    // Sobrecarga para generar token directamente desde el nombre de usuario (útil en algunos casos)
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }


    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecretKey).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}