package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.immregistries.iis.kernal.security.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns the user management failures into status codes instead of 500s.
 * <p>
 * Deliberately scoped to the two user management controllers: a global advice would change
 * error mapping for every existing REST controller in the app.
 */
@RestControllerAdvice(assignableTypes = {UserSelfRestController.class, AdminUserRestController.class})
public class UserManagementExceptionAdvice {

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(UserNotFoundException e) {
		return body(HttpStatus.NOT_FOUND, e.getMessage());
	}

	/**
	 * Wrong current password, or an account disabled since the session was opened.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException e) {
		return body(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
		return body(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	private ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(Map.of("error", status.getReasonPhrase(), "message",
			message == null ? "" : message));
	}
}
