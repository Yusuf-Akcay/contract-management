package de.yusufakcay.contractmanagement.document.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public final class UnsupportedDocumentException extends ErrorResponseException {

	public UnsupportedDocumentException(HttpStatus status, String detail) {
		super(status, ProblemDetail.forStatusAndDetail(status, detail), null);
	}
}
