// GlobalExceptionHandler.java
// ------------------------------------------------------------
// Spring @RestControllerAdvice that catches any uncaught exception
// from a REST controller, logs the error, and returns a standard
// JSON error payload. The payload contains:
//   - code:      HTTP status code (int)
//   - message:   Human‑readable error message
//   - type:      Exception class name
//   - timestamp: ISO‑8601 instant when the error occurred
//   - traceId:   Unique identifier for correlating logs
// ------------------------------------------------------------

package org.immregistries.iis.kernal.web.exception;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Any exception not explicitly handled by a controller method bubbles up
 * to this advice. The handler logs the exception (including stack trace)
 * and returns a consistent JSON error object that clients can rely on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Catch‑all handler for any {@link Exception} thrown from a REST endpoint.
     *
     * @param ex      the exception that was thrown
     * @param request the current web request (unused but allows future extension)
     * @return a {@link ResponseEntity} containing a structured error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        // Log the full stack trace for diagnostics.
		 System.out.println("OHOHOHO");
        logger.error("Unhandled exception in REST controller", ex);

        // Generate a correlation identifier to tie logs and the HTTP response together.
        String traceId = UUID.randomUUID().toString();

        // Build a minimal yet useful error payload.
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                ex.getClass().getSimpleName(),
                Instant.now().toString(),
                traceId);

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Simple DTO representing the JSON error structure returned to clients.
     */
    public static class ErrorResponse {
        private final int code;
        private final String message;
        private final String type;
        private final String timestamp;
        private final String traceId;

        public ErrorResponse(int code, String message, String type, String timestamp, String traceId) {
            this.code = code;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.traceId = traceId;
        }

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public String getTimestamp() { return timestamp; }
        public String getTraceId() { return traceId; }
    }
}
