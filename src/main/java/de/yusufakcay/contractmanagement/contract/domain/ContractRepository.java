package de.yusufakcay.contractmanagement.contract.domain;

import java.util.Optional;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

	Page<Contract> findByCustomerId(Long customerId, Pageable pageable);

	Page<Contract> findByCustomerCustomerNumber(String customerNumber, Pageable pageable);

	Optional<Contract> findByContractNumber(String contractNumber);

	default Page<Contract> search(ContractFilter filter, Pageable pageable) {
		var probe = new Contract();
		probe.setLineOfBusiness(filter.lineOfBusiness());
		probe.setStatus(filter.status());
		return findAll(Example.of(probe), pageable);
	}
}
