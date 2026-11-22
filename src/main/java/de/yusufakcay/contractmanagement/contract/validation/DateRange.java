package de.yusufakcay.contractmanagement.contract.validation;

import java.time.LocalDate;

public interface DateRange {

	LocalDate startDate();

	LocalDate endDate();
}
