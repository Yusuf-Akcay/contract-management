package de.yusufakcay.contractmanagement.customer.web;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
		@Schema(example = "C-100042") @NotBlank @Pattern(regexp = "C-\\d{6}", message = "must match the format C-123456") String customerNumber,
		@Schema(example = "Erika") @NotBlank @Size(max = 100) String firstName,
		@Schema(example = "Mustermann") @NotBlank @Size(max = 100) String lastName,
		@Schema(example = "1985-04-12") @NotNull @Past LocalDate dateOfBirth,
		@Schema(example = "erika@example.com") @Email @Size(max = 255) String email) {}
