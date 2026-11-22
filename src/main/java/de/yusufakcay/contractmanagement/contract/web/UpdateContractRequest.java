package de.yusufakcay.contractmanagement.contract.web;

import java.math.BigDecimal;
import de.yusufakcay.contractmanagement.contract.domain.ContractStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateContractRequest(
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal premium,
		@NotNull ContractStatus status,
		@NotNull @PositiveOrZero Long version) {}
