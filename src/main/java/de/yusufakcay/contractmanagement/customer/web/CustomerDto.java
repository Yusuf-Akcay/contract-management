package de.yusufakcay.contractmanagement.customer.web;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerDto(
		Long id,
		String customerNumber,
		String firstName,
		String lastName,
		LocalDate dateOfBirth,
		String email,
		Long version,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy) {}
