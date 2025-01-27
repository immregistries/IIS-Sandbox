package org.immregistries.iis.kernal;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.MacAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.Date;

@Component
public class JwtUtils {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Value("${iis.api.app.jwtSecret}")
	private String jwtSecret;
	@Value("${iis.api.app.jwtExpirationMs}")
	private int jwtExpirationMs;

	private final static MacAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.HS512;
	private final static String SIGNATURE_ALGORITHM_NAME = "HmacSha512";

	private SecretKey key;

	public String generateJwtToken(Authentication authentication) {
		init();
		return Jwts.builder().id(authentication.getName())
			.subject(authentication.getName())
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + jwtExpirationMs * 1000))
			.signWith(key, SIGNATURE_ALGORITHM)
			.compact();
	}

	public String getUserNameFromJwtToken(String token) {
		init();
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
	}

	public boolean validateJwtToken(String authToken) {
		init();
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
			return true;
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		} catch (JwtException e) {
			logger.error("Invalid JWT signature: {}", e.getMessage());
		}
		return false;
	}

	private void init() {
		if (key == null) {
			this.key = new SecretKeySpec(DatatypeConverter.parseBase64Binary(jwtSecret), SIGNATURE_ALGORITHM_NAME);
		}
	}
}