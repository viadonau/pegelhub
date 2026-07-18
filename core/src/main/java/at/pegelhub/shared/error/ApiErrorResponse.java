package at.pegelhub.shared.error;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

record ApiErrorResponse(
        ApiErrorCode code,
        String detail,
        List<ApiFieldError> errors
) {

    static ApiErrorResponse from(Exception exception) {
        return from(exception, null);
    }

    static ApiErrorResponse from(Exception exception, HttpStatusCode fallbackStatus) {
        ApiErrorCode code = resolveCode(exception, fallbackStatus);
        return new ApiErrorResponse(code, resolveDetail(exception, code), resolveErrors(exception));
    }

    ProblemDetail toProblemDetail(WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(code.type());
        problem.setTitle(code.title());
        problem.setInstance(requestUri(request));
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }

    boolean shouldLogStackTrace() {
        return code.status().is5xxServerError();
    }

    private static ApiErrorCode resolveCode(Exception exception, HttpStatusCode fallbackStatus) {
        if (exception instanceof ApiException apiException) {
            return apiException.errorCode();
        }
        if (exception instanceof AccessDeniedException) {
            return ApiErrorCode.FORBIDDEN;
        }
        if (exception instanceof MethodArgumentNotValidException
                || exception instanceof HandlerMethodValidationException) {
            return ApiErrorCode.VALIDATION_FAILED;
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return ApiErrorCode.MALFORMED_REQUEST;
        }
        if (exception instanceof MissingServletRequestParameterException) {
            return ApiErrorCode.MISSING_REQUEST_PARAMETER;
        }
        if (exception instanceof TypeMismatchException) {
            return ApiErrorCode.INVALID_REQUEST_PARAMETER;
        }
        if (exception instanceof IllegalArgumentException) {
            return ApiErrorCode.BAD_REQUEST;
        }
        if (fallbackStatus != null && fallbackStatus.isError()) {
            return ApiErrorCode.fromStatus(fallbackStatus);
        }
        return ApiErrorCode.INTERNAL_SERVER_ERROR;
    }

    private static String resolveDetail(Exception exception, ApiErrorCode code) {
        if (!exposesExceptionMessage(code)) {
            return code.defaultDetail();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? code.defaultDetail() : message;
    }

    private static boolean exposesExceptionMessage(ApiErrorCode code) {
        return code != ApiErrorCode.INTERNAL_SERVER_ERROR
                && code != ApiErrorCode.FORBIDDEN
                && code != ApiErrorCode.MALFORMED_REQUEST
                && code != ApiErrorCode.VALIDATION_FAILED;
    }

    private static List<ApiFieldError> resolveErrors(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidException.getBindingResult().getAllErrors().stream()
                    .map(ApiErrorResponse::fieldError)
                    .toList();
        }
        if (exception instanceof HandlerMethodValidationException handlerMethodValidationException) {
            return handlerMethodValidationException.getParameterValidationResults().stream()
                    .flatMap(ApiErrorResponse::parameterErrors)
                    .toList();
        }
        if (exception instanceof MissingServletRequestParameterException missingParameterException) {
            return List.of(new ApiFieldError(missingParameterException.getParameterName(), missingParameterException.getMessage()));
        }
        if (exception instanceof TypeMismatchException typeMismatchException) {
            return List.of(new ApiFieldError(typeMismatchField(typeMismatchException), exception.getMessage()));
        }
        return List.of();
    }

    private static ApiFieldError fieldError(ObjectError error) {
        String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        return new ApiFieldError(field, error.getDefaultMessage());
    }

    private static java.util.stream.Stream<ApiFieldError> parameterErrors(ParameterValidationResult result) {
        return result.getResolvableErrors().stream()
                .map(error -> new ApiFieldError(result.getMethodParameter().getParameterName(), message(error)));
    }

    private static String message(MessageSourceResolvable error) {
        return error.getDefaultMessage();
    }

    private static String typeMismatchField(TypeMismatchException exception) {
        if (exception instanceof MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
            return methodArgumentTypeMismatchException.getName();
        }
        return exception.getPropertyName();
    }

    private static URI requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        String description = request.getDescription(false);
        return URI.create(description.startsWith("uri=") ? description.substring(4) : description);
    }

    record ApiFieldError(String field, String message) {
    }
}
