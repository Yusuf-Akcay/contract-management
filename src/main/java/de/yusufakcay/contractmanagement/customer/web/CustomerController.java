package de.yusufakcay.contractmanagement.customer.web;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import de.yusufakcay.contractmanagement.contract.service.ContractService;
import de.yusufakcay.contractmanagement.contract.web.ContractDto;
import de.yusufakcay.contractmanagement.customer.service.CustomerService;
import de.yusufakcay.contractmanagement.shared.security.ReadAccess;
import de.yusufakcay.contractmanagement.shared.security.WriteAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "Customers", description = "Policyholders in the portfolio")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

	private final CustomerService service;
	private final ContractService contractService;

	@Operation(summary = "List customers (paged)")
	@GetMapping
	@ReadAccess
	public Page<CustomerDto> list(@ParameterObject @PageableDefault(size = 20, sort = "customerNumber") Pageable pageable) {
		return service.list(pageable);
	}

	@Operation(summary = "Get customer by ID")
	@ApiResponse(responseCode = "404", description = "Customer not found")
	@GetMapping("/{id}")
	@ReadAccess
	public CustomerDto get(@PathVariable @Positive Long id) {
		return service.get(id);
	}

	@Operation(summary = "Contracts of a customer")
	@GetMapping("/{id}/contracts")
	@ReadAccess
	public Page<ContractDto> contracts(@PathVariable @Positive Long id,
		@ParameterObject @PageableDefault(size = 20, sort = "contractNumber") Pageable pageable) {
		return contractService.findByCustomer(id, pageable);
	}

	@Operation(summary = "Create customer")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "Validation error (ProblemDetail with errors[])")
	@ApiResponse(responseCode = "409", description = "Customer number already taken")
	@PostMapping
	@WriteAccess
	public ResponseEntity<CustomerDto> create(@Valid @RequestBody CreateCustomerRequest request) {
		CustomerDto customer = service.create(request);
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}").buildAndExpand(customer.id()).toUri()).body(customer);
	}

	@Operation(summary = "Update customer (optimistic locking via version)")
	@ApiResponse(responseCode = "409", description = "Version conflict")
	@PutMapping("/{id}")
	@WriteAccess
	public CustomerDto update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateCustomerRequest request) {
		return service.update(id, request);
	}

	@Operation(summary = "Delete customer (only without contracts)")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@WriteAccess
	public void delete(@PathVariable @Positive Long id) {
		service.delete(id);
	}
}
