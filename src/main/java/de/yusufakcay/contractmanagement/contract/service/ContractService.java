package de.yusufakcay.contractmanagement.contract.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import de.yusufakcay.contractmanagement.contract.domain.Contract;
import de.yusufakcay.contractmanagement.contract.domain.ContractFilter;
import de.yusufakcay.contractmanagement.contract.domain.ContractRepository;
import de.yusufakcay.contractmanagement.contract.domain.ContractStatus;
import de.yusufakcay.contractmanagement.contract.event.ContractCreatedEvent;
import de.yusufakcay.contractmanagement.contract.mapper.ContractMapper;
import de.yusufakcay.contractmanagement.contract.web.ContractDto;
import de.yusufakcay.contractmanagement.contract.web.CreateContractRequest;
import de.yusufakcay.contractmanagement.contract.web.UpdateContractRequest;
import de.yusufakcay.contractmanagement.customer.domain.Customer;
import de.yusufakcay.contractmanagement.customer.domain.CustomerRepository;
import de.yusufakcay.contractmanagement.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

	private final ContractRepository repository;
	private final CustomerRepository customerRepository;
	private final ContractMapper mapper;
	private final ApplicationEventPublisher events;

	public Page<ContractDto> search(ContractFilter filter, Pageable pageable) {
		return repository.search(filter, pageable).map(mapper::toDto);
	}

	public Page<ContractDto> findByCustomer(Long customerId, Pageable pageable) {
		if (!customerRepository.existsById(customerId)) {
			throw customerNotFound(customerId);
		}
		return repository.findByCustomerId(customerId, pageable).map(mapper::toDto);
	}

	public Page<ContractDto> findByCustomer(String customerNumber, Pageable pageable) {
		return repository.findByCustomerCustomerNumber(customerNumber, pageable).map(mapper::toDto);
	}

	public ContractDto get(Long id) {
		return mapper.toDto(load(id));
	}

	public ContractDto get(String contractNumber) {
		return repository.findByContractNumber(contractNumber)
			.map(mapper::toDto)
			.orElseThrow(() -> new NotFoundException("Contract %s not found".formatted(contractNumber)));
	}

	@Transactional
	public ContractDto create(CreateContractRequest request) {
		Customer customer = customerRepository.findById(request.customerId()).orElseThrow(() -> customerNotFound(request.customerId()));
		Contract contract = mapper.toEntity(request);
		contract.setCustomer(customer);
		contract.setStatus(ContractStatus.ACTIVE);
		Contract saved = repository.saveAndFlush(contract);
		events.publishEvent(ContractCreatedEvent.of(saved));
		return mapper.toDto(saved);
	}

	@Transactional
	public ContractDto update(Long id, UpdateContractRequest request) {
		Contract contract = load(id);
		contract.checkVersion(request.version());
		mapper.update(request, contract);
		return mapper.toDto(repository.saveAndFlush(contract));
	}

	@Transactional
	public void delete(Long id) {
		repository.delete(load(id));
	}

	private Contract load(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Contract %d not found".formatted(id)));
	}

	private static NotFoundException customerNotFound(Long customerId) {
		return new NotFoundException("Customer %d not found".formatted(customerId));
	}
}
