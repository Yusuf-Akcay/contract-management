package de.yusufakcay.contractmanagement.contract.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;
import de.yusufakcay.contractmanagement.contract.validation.DateRange;
import de.yusufakcay.contractmanagement.contract.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@ValidDateRange
public record CreateContractRequest(
		@Schema(example = "P-20260042") @NotBlank @Pattern(regexp = "P-\\d{8}", message = "must match the format P-12345678") String contractNumber,
		@Schema(example = "1") @NotNull @Positive Long customerId,
		@Schema(example = "HOUSEHOLD") @NotNull LineOfBusiness lineOfBusiness,
		@Schema(description = "Monthly premium in EUR", example = "12.90") @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal premium,
		@Schema(example = "2026-01-01") @NotNull LocalDate startDate,
		@Schema(example = "2027-01-01") LocalDate endDate) implements DateRange {}
