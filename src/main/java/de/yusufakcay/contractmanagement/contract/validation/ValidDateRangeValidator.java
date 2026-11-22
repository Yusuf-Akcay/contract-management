package de.yusufakcay.contractmanagement.contract.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

	@Override
	public boolean isValid(DateRange range, ConstraintValidatorContext context) {
		if (range == null || range.startDate() == null || range.endDate() == null
			|| range.endDate().isAfter(range.startDate())) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
			.addPropertyNode("endDate")
			.addConstraintViolation();
		return false;
	}
}
