package de.yusufakcay.contractmanagement.contract.mcp;

import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import de.yusufakcay.contractmanagement.contract.domain.ContractFilter;
import de.yusufakcay.contractmanagement.contract.domain.ContractStatus;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;
import de.yusufakcay.contractmanagement.contract.service.ContractService;
import de.yusufakcay.contractmanagement.contract.web.ContractDto;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Validated
public class ContractMcpTools {

	private static final PageRequest MAX_RESULTS = PageRequest.of(0, 20, Sort.by("contractNumber"));

	private final ContractService service;

	@McpTool(name = "search_contracts",
		description = "Searches insurance contracts in the portfolio, optionally filtered by line of business and status. Returns at most 20 results.",
		annotations = @McpTool.McpAnnotations(readOnlyHint = true))
	public List<ContractDto> searchContracts(
		@McpToolParam(required = false, description = "Line of business: MOTOR, HOUSEHOLD, LIABILITY, BUILDING, LIFE, HEALTH") LineOfBusiness lineOfBusiness,
		@McpToolParam(required = false, description = "Status: ACTIVE, SUSPENDED, CANCELLED") ContractStatus status) {
		return service.search(new ContractFilter(lineOfBusiness, status), MAX_RESULTS).getContent();
	}

	@McpTool(name = "contracts_of_customer",
		description = "All contracts of a customer by customer number (e.g. C-100001), at most 20.",
		annotations = @McpTool.McpAnnotations(readOnlyHint = true))
	public List<ContractDto> contractsOfCustomer(@McpToolParam(description = "Customer number, e.g. C-100001") @NotBlank String customerNumber) {
		return service.findByCustomer(customerNumber, MAX_RESULTS).getContent();
	}

	@McpTool(name = "contract_details",
		description = "Details of a single contract by contract number (e.g. P-20260001).",
		annotations = @McpTool.McpAnnotations(readOnlyHint = true))
	public ContractDto contractDetails(@McpToolParam(description = "Contract number, e.g. P-20260001") @NotBlank String contractNumber) {
		return service.get(contractNumber);
	}
}
