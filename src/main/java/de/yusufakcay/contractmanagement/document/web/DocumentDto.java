package de.yusufakcay.contractmanagement.document.web;

import java.time.Instant;

public record DocumentDto(
		Long id,
		String filename,
		String contentType,
		long size,
		int chunks,
		Instant createdAt,
		String createdBy) {}
