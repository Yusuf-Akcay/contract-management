package de.yusufakcay.contractmanagement.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import de.yusufakcay.contractmanagement.contract.domain.Contract;
import de.yusufakcay.contractmanagement.contract.domain.ContractRepository;
import de.yusufakcay.contractmanagement.contract.domain.ContractStatus;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;
import de.yusufakcay.contractmanagement.contract.event.ContractCreatedEvent;
import de.yusufakcay.contractmanagement.contract.mapper.ContractMapperImpl;
import de.yusufakcay.contractmanagement.contract.web.CreateContractRequest;
import de.yusufakcay.contractmanagement.contract.web.UpdateContractRequest;
import de.yusufakcay.contractmanagement.customer.domain.Customer;
import de.yusufakcay.contractmanagement.customer.domain.CustomerRepository;
import de.yusufakcay.contractmanagement.shared.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

	@Mock
	private ContractRepository repository;
	@Mock
	private CustomerRepository customerRepository;
	@Mock
	private ApplicationEventPublisher events;

	private ContractService service;

	@BeforeEach
	void setUp() {
		service = new ContractService(repository, customerRepository, new ContractMapperImpl(), events);
	}

	@Test
	void createSetsStatusActiveAndPublishesEvent() {
		var customer = new Customer();
		customer.setId(7L);
		customer.setCustomerNumber("C-100007");
		given(customerRepository.findById(7L)).willReturn(Optional.of(customer));
		given(repository.saveAndFlush(any(Contract.class))).willAnswer(inv -> {
			Contract c = inv.getArgument(0);
			c.setId(42L);
			return c;
		});

		var dto = service.create(new CreateContractRequest("P-20260042", 7L, LineOfBusiness.MOTOR, new BigDecimal("54.30"),
			LocalDate.of(2026, 1, 1), null));

		assertThat(dto.status()).isEqualTo(ContractStatus.ACTIVE);
		assertThat(dto.customerId()).isEqualTo(7L);
		ArgumentCaptor<ContractCreatedEvent> event = ArgumentCaptor.forClass(ContractCreatedEvent.class);
		then(events).should().publishEvent(event.capture());
		assertThat(event.getValue()).satisfies(e -> {
			assertThat(e.contractId()).isEqualTo(42L);
			assertThat(e.customerNumber()).isEqualTo("C-100007");
			assertThat(e.eventId()).isNotNull();
		});
	}

	@Test
	void createForUnknownCustomerIs404WithoutEvent() {
		given(customerRepository.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(new CreateContractRequest("P-20260042", 99L, LineOfBusiness.MOTOR,
			BigDecimal.TEN, LocalDate.of(2026, 1, 1), null))).isInstanceOf(NotFoundException.class);
		then(events).should(never()).publishEvent(any());
	}

	@Test
	void updateWithStaleVersionThrowsConflict() {
		var contract = new Contract();
		contract.setId(1L);
		ReflectionTestUtils.setField(contract, "version", 3L);
		given(repository.findById(1L)).willReturn(Optional.of(contract));

		assertThatThrownBy(() -> service.update(1L, new UpdateContractRequest(BigDecimal.ONE, ContractStatus.SUSPENDED, 2L)))
			.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		then(repository).should(never()).saveAndFlush(any());
	}
}
