package at.pegelhub.shared.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps uncaught exceptions to RFC 9457 problem detail responses.
 */
@ControllerAdvice
public class ExceptionMapper extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, null, new HttpHeaders(), ApiErrorResponse.from(ex).code().status(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                            Object body,
                                                            HttpHeaders headers,
                                                            HttpStatusCode status,
                                                            WebRequest request) {
        ApiErrorResponse error = ApiErrorResponse.from(ex, status);
        logMappedException(ex, error);
        return super.handleExceptionInternal(ex, error.toProblemDetail(request), headers, error.code().status(), request);
    }

    private void logMappedException(Exception ex, ApiErrorResponse error) {
        if (error.shouldLogStackTrace()) {
            LOGGER.error("{} mapped to {}", ex.getClass().getSimpleName(), error.code().name(), ex);
            return;
        }
        LOGGER.warn("{} mapped to {}: {}", ex.getClass().getSimpleName(), error.code().name(), error.detail());
    }
}
