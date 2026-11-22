package de.yusufakcay.contractmanagement.contract.validation;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;
import de.yusufakcay.contractmanagement.contract.web.CreateContractRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CreateContractRequestValidationTest {

	private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
	private final Validator validator = FACTORY.getValidator();

	@AfterAll
	static void close() {
		FACTORY.close();
	}

	@Test
	void validRequestHasNoViolations() {
		assertThat(validator.validate(request("P-20260001", new BigDecimal("12.90"), LocalDate.of(2027, 1, 1)))).isEmpty();
	}

	@Test
	void openEndDateIsAllowed() {
		assertThat(validator.validate(request("P-20260001", new BigDecimal("12.90"), null))).isEmpty();
	}

	@Test
	void endBeforeStartViolatesClassConstraintOnEndDate() {
		var violations = validator.validate(request("P-20260001", new BigDecimal("12.90"), LocalDate.of(2025, 12, 31)));

		assertThat(violations).singleElement().satisfies(v -> {
			assertThat(v.getPropertyPath()).hasToString("endDate");
			assertThat(v.getConstraintDescriptor().getAnnotation()).isInstanceOf(ValidDateRange.class);
		});
	}

	@Test
	void formatAndAmountAreChecked() {
		var violations = validator.validate(request("12345", new BigDecimal("0.001"), null));

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath).map(Object::toString)
			.containsExactlyInAnyOrder("contractNumber", "premium", "premium");
	}

	private static CreateContractRequest request(String number, BigDecimal premium, LocalDate endDate) {
		return new CreateContractRequest(number, 1L, LineOfBusiness.HOUSEHOLD, premium, LocalDate.of(2026, 1, 1), endDate);
	}
}
