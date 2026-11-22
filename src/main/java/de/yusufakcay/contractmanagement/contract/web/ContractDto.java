package de.yusufakcay.contractmanagement.contract.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import de.yusufakcay.contractmanagement.contract.domain.ContractStatus;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;

public record ContractDto(
		Long id,
		String contractNumber,
		Long customerId,
		LineOfBusiness lineOfBusiness,
		BigDecimal premium,
		LocalDate startDate,
		LocalDate endDate,
		ContractStatus status,
		Long version,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy) {}
