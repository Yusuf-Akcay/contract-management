package de.yusufakcay.contractmanagement.contract.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Class-level constraint: {@code endDate} (if set) must be after {@code startDate}. The violation is reported on {@code endDate}. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDateRangeValidator.class)
public @interface ValidDateRange {

	String message() default "end date must be after the start date";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
