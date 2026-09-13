package com.clipzy.security;

import com.clipzy.config.ClipzyProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  public static final String CLAIM_TYPE = "typ";
  public static final String TYPE_ACCESS = "access";
  public static final String TYPE_REFRESH = "refresh";

  private final ClipzyProperties properties;
  private final SecretKey key;

  public JwtService(ClipzyProperties properties) {
    this.properties = properties;
    byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
    this.key = Keys.hmacShaKeyFor(secretBytes);
  }

  public String createAccessToken(UserPrincipal principal) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(properties.getJwt().getAccessTokenMinutes() * 60);
    return Jwts.builder()
        .subject(principal.getId().toString())
        .claim("email", principal.getUsername())
        .claim(CLAIM_TYPE, TYPE_ACCESS)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public String createRefreshToken(UserPrincipal principal) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(properties.getJwt().getRefreshTokenDays() * 24 * 60 * 60);
    return Jwts.builder()
        .subject(principal.getId().toString())
        .claim(CLAIM_TYPE, TYPE_REFRESH)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public UUID requireUserId(String token, String expectedType) {
    try {
      Claims claims = parse(token);
      String typ = claims.get(CLAIM_TYPE, String.class);
      if (!expectedType.equals(typ)) {
        throw new JwtAuthException("Invalid token type");
      }
      return UUID.fromString(claims.getSubject());
    } catch (ExpiredJwtException e) {
      throw new JwtAuthException("Token expired");
    } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
      throw new JwtAuthException("Invalid token");
    }
  }

  public static class JwtAuthException extends RuntimeException {
    public JwtAuthException(String message) {
      super(message);
    }
  }
}
