package de.yusufakcay.contractmanagement.shared.web;

import java.util.List;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	public record FieldViolation(String field, String message) {}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
		HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getBindingResult().getAllErrors().stream()
			.map(error -> new FieldViolation(
				error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
				error.getDefaultMessage()))
			.toList();
		return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		return validationProblem(ex.getConstraintViolations().stream()
			.map(violation -> new FieldViolation(lastNode(violation.getPropertyPath().toString()), violation.getMessage()))
			.toList());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"The record was modified in the meantime, please reload it (version).");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"Data conflict (e.g. number already taken or customer still has contracts).");
	}

	@ExceptionHandler({ ResourceAccessException.class, TransientAiException.class })
	ProblemDetail handleUnreachableModel(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
			"Language model service (Ollama) is not reachable, please try again later.");
	}

	@ExceptionHandler(NonTransientAiException.class)
	ProblemDetail handleModelError(NonTransientAiException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Language model request failed: " + ex.getMessage());
	}

	private static ProblemDetail validationProblem(List<FieldViolation> errors) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
		detail.setProperty("errors", errors);
		return detail;
	}

	private static String lastNode(String propertyPath) {
		return propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
	}
}
