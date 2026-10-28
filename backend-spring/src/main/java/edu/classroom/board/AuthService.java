
package edu.classroom.board;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final Users users;

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.issuer}")
  private String issuer;

  @Value("${app.jwt.audience}")
  private String audience;

  @Value("${app.jwt.ttl}")
  private long ttl;

  private SecretKey key;

  public AuthService(Users users) {
    this.users = users;
  }

  @PostConstruct
  public void init() {
    if (secret == null || !secret.matches("[0-9a-fA-F]{64}")) {
      throw new IllegalStateException("JWT_SECRET 설정을 확인하세요.");
    }

    byte[] bytes = new byte[32];
    for (int i = 0; i < 32; i++) {
      bytes[i] = (byte) Integer.parseInt(secret.substring(i * 2, i * 2 + 2), 16);
    }

    key = Keys.hmacShaKeyFor(bytes);

    if (ttl <= 0) {
      throw new IllegalStateException("TOKEN_TTL 설정을 확인하세요.");
    }
  }

  // BE-01: JWT 발급
  public String issueToken(Users.PublicUser user) {
    Date now = new Date();
    Date expires = new Date(now.getTime() + ttl * 1000);

    return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(now)
            .expiration(expires)
            .signWith(key, Jwts.SIG.HS256)
            .compact();
  }

  // BE-02: JWT 검증
  public long currentUserId(HttpServletRequest request) {
    String header = request.getHeader("Authorization");

    if (header == null || !header.matches("Bearer [^\\s]+")) {
      throw new ApiError(401, "TOKEN_REQUIRED", "Bearer 토큰이 필요합니다.");
    }

    String token = header.substring(7);

    try {
      var jwt = Jwts.parser()
              .verifyWith(key)
              .requireIssuer(issuer)
              .requireAudience(audience)
              .build()
              .parseSignedClaims(token);

      if (!"HS256".equals(jwt.getHeader().getAlgorithm())) {
        throw new ApiError(401, "INVALID_TOKEN", "잘못된 토큰입니다.");
      }

      Claims claims = jwt.getPayload();
      String sub = claims.getSubject();

      if (claims.getExpiration() == null
              || sub == null
              || !sub.matches("[1-9][0-9]*")) {
        throw new ApiError(401, "INVALID_TOKEN", "잘못된 토큰입니다.");
      }

      long userId = Long.parseLong(sub);

      if (users.find(userId) == null) {
        throw new ApiError(401, "INVALID_TOKEN", "존재하지 않는 사용자입니다.");
      }

      return userId;

    } catch (ExpiredJwtException e) {
      // 만료된 토큰도 서명과 필수 클레임을 확인
      try {
        Claims claims = e.getClaims();
        if (!"HS256".equals(e.getHeader().getAlgorithm())
                || !issuer.equals(claims.getIssuer())
                || claims.getAudience() == null
                || !claims.getAudience().contains(audience)
                || claims.getExpiration() == null
                || claims.getSubject() == null
                || !claims.getSubject().matches("[1-9][0-9]*")
                || users.find(Long.parseLong(claims.getSubject())) == null) {
          throw new ApiError(401, "INVALID_TOKEN", "잘못된 토큰입니다.");
        }
      } catch (JwtException | IllegalArgumentException ex) {
        throw new ApiError(401, "INVALID_TOKEN", "잘못된 토큰입니다.");
      }
      throw new ApiError(401, "TOKEN_EXPIRED", "토큰이 만료되었습니다.");

    } catch (JwtException | IllegalArgumentException e) {
      throw new ApiError(401, "INVALID_TOKEN", "잘못된 토큰입니다.");
    }
  }

  // BE-03: 작성자 권한 확인
  public void assertOwner(long userId, long authorId) {
    if (userId != authorId) {
      throw new ApiError(403, "FORBIDDEN", "본인이 작성한 글만 수정하거나 삭제할 수 있습니다.");
    }
  }
}
