package de.yusufakcay.contractmanagement.contract.web;

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
import de.yusufakcay.contractmanagement.contract.domain.ContractFilter;
import de.yusufakcay.contractmanagement.contract.service.ContractService;
import de.yusufakcay.contractmanagement.shared.security.ReadAccess;
import de.yusufakcay.contractmanagement.shared.security.WriteAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "Contracts", description = "Insurance contracts in the portfolio")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Validated
public class ContractController {

	private final ContractService service;

	@Operation(summary = "Search contracts", description = "Optionally filtered by lineOfBusiness and status; page, size, sort=field,asc|desc")
	@GetMapping
	@ReadAccess
	public Page<ContractDto> search(@ParameterObject ContractFilter filter,
		@ParameterObject @PageableDefault(size = 20, sort = "contractNumber") Pageable pageable) {
		return service.search(filter, pageable);
	}

	@Operation(summary = "Get contract by ID")
	@ApiResponse(responseCode = "404", description = "Contract not found")
	@GetMapping("/{id}")
	@ReadAccess
	public ContractDto get(@PathVariable @Positive Long id) {
		return service.get(id);
	}

	@Operation(summary = "Create contract", description = "Publishes the ContractCreated event to RabbitMQ after commit")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "400", description = "Validation error (ProblemDetail with errors[])")
	@ApiResponse(responseCode = "403", description = "Role contract-write missing")
	@ApiResponse(responseCode = "404", description = "Customer not found")
	@ApiResponse(responseCode = "409", description = "Contract number already taken")
	@PostMapping
	@WriteAccess
	public ResponseEntity<ContractDto> create(@Valid @RequestBody CreateContractRequest request) {
		ContractDto contract = service.create(request);
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}").buildAndExpand(contract.id()).toUri()).body(contract);
	}

	@Operation(summary = "Update premium/status (optimistic locking via version)")
	@ApiResponse(responseCode = "409", description = "Version conflict")
	@PutMapping("/{id}")
	@WriteAccess
	public ContractDto update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateContractRequest request) {
		return service.update(id, request);
	}

	@Operation(summary = "Delete contract")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@WriteAccess
	public void delete(@PathVariable @Positive Long id) {
		service.delete(id);
	}
}
