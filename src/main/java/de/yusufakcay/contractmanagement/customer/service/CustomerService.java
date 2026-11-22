package de.yusufakcay.contractmanagement.customer.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import de.yusufakcay.contractmanagement.customer.domain.Customer;
import de.yusufakcay.contractmanagement.customer.domain.CustomerRepository;
import de.yusufakcay.contractmanagement.customer.mapper.CustomerMapper;
import de.yusufakcay.contractmanagement.customer.web.CreateCustomerRequest;
import de.yusufakcay.contractmanagement.customer.web.CustomerDto;
import de.yusufakcay.contractmanagement.customer.web.UpdateCustomerRequest;
import de.yusufakcay.contractmanagement.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository repository;
	private final CustomerMapper mapper;

	public Page<CustomerDto> list(Pageable pageable) {
		return repository.findAll(pageable).map(mapper::toDto);
	}

	public CustomerDto get(Long id) {
		return mapper.toDto(load(id));
	}

	@Transactional
	public CustomerDto create(CreateCustomerRequest request) {
		return mapper.toDto(repository.saveAndFlush(mapper.toEntity(request)));
	}

	@Transactional
	public CustomerDto update(Long id, UpdateCustomerRequest request) {
		Customer customer = load(id);
		customer.checkVersion(request.version());
		mapper.update(request, customer);
		return mapper.toDto(repository.saveAndFlush(customer));
	}

	@Transactional
	public void delete(Long id) {
		repository.delete(load(id));
		repository.flush();
	}

	public Customer load(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Customer %d not found".formatted(id)));
	}
}
