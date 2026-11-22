package de.yusufakcay.contractmanagement.contract.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import de.yusufakcay.contractmanagement.TestcontainersConfiguration;
import de.yusufakcay.contractmanagement.customer.domain.Customer;
import de.yusufakcay.contractmanagement.customer.domain.CustomerRepository;
import de.yusufakcay.contractmanagement.shared.config.JpaConfig;
import de.yusufakcay.contractmanagement.shared.security.SpringSecurityAuditorAware;

@DataJpaTest
@Import({ JpaConfig.class, SpringSecurityAuditorAware.class, TestcontainersConfiguration.class })
class ContractRepositoryTest {

	@Autowired
	private ContractRepository repository;
	@Autowired
	private CustomerRepository customerRepository;

	private Customer customer;

	@BeforeEach
	void setUp() {
		customer = new Customer();
		customer.setCustomerNumber("C-900001");
		customer.setFirstName("Erika");
		customer.setLastName("Mustermann");
		customer.setDateOfBirth(LocalDate.of(1985, 4, 12));
		customerRepository.save(customer);
		repository.saveAllAndFlush(List.of(
			contract("P-90000001", LineOfBusiness.MOTOR, ContractStatus.ACTIVE),
			contract("P-90000002", LineOfBusiness.MOTOR, ContractStatus.CANCELLED),
			contract("P-90000003", LineOfBusiness.HOUSEHOLD, ContractStatus.ACTIVE)));
	}

	@Test
	void searchFiltersOptionallyAndPaginates() {
		var motor = repository.search(new ContractFilter(LineOfBusiness.MOTOR, null), PageRequest.of(0, 1, Sort.by("contractNumber").descending()));

		assertThat(motor.getTotalElements()).isEqualTo(2);
		assertThat(motor.getContent()).extracting(Contract::getContractNumber).containsExactly("P-90000002");
		assertThat(repository.search(new ContractFilter(null, ContractStatus.ACTIVE), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
		assertThat(repository.search(new ContractFilter(null, null), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
	}

	@Test
	void derivedQueryByCustomerNumber() {
		assertThat(repository.findByCustomerCustomerNumber("C-900001", PageRequest.of(0, 10))).hasSize(3);
	}

	@Test
	void auditingAndVersionAreSet() {
		Contract loaded = repository.findByContractNumber("P-90000001").orElseThrow();

		assertThat(loaded.getVersion()).isZero();
		assertThat(loaded.getCreatedAt()).isNotNull();
		assertThat(loaded.getUpdatedAt()).isNotNull();
	}

	@Test
	void staleVersionIsRejectedOnSave() {
		Contract current = repository.findByContractNumber("P-90000001").orElseThrow();
		current.setPremium(new BigDecimal("99.00"));
		repository.saveAndFlush(current);
		assertThat(current.getVersion()).isEqualTo(1L);

		Contract stale = contract("P-90000001", LineOfBusiness.MOTOR, ContractStatus.ACTIVE);
		stale.setId(current.getId());
		ReflectionTestUtils.setField(stale, "version", 0L);

		assertThatThrownBy(() -> repository.saveAndFlush(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private Contract contract(String number, LineOfBusiness lineOfBusiness, ContractStatus status) {
		var contract = new Contract();
		contract.setContractNumber(number);
		contract.setCustomer(customer);
		contract.setLineOfBusiness(lineOfBusiness);
		contract.setStatus(status);
		contract.setPremium(new BigDecimal("10.00"));
		contract.setStartDate(LocalDate.of(2026, 1, 1));
		return contract;
	}
}
