package de.yusufakcay.contractmanagement.contract.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import de.yusufakcay.contractmanagement.contract.domain.Contract;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;

/** Public event contract (never serialize the entity), versioned via {@code schemaVersion}. */
public record ContractCreatedEvent(
		UUID eventId,
		int schemaVersion,
		Instant occurredAt,
		Long contractId,
		String contractNumber,
		String customerNumber,
		LineOfBusiness lineOfBusiness,
		BigDecimal premium,
		LocalDate startDate) {

	public static ContractCreatedEvent of(Contract contract) {
		return new ContractCreatedEvent(UUID.randomUUID(), 1, Instant.now(), contract.getId(), contract.getContractNumber(),
			contract.getCustomer().getCustomerNumber(), contract.getLineOfBusiness(), contract.getPremium(), contract.getStartDate());
	}
}
