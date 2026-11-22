package de.yusufakcay.contractmanagement.contract.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import de.yusufakcay.contractmanagement.contract.domain.Contract;
import de.yusufakcay.contractmanagement.contract.web.ContractDto;
import de.yusufakcay.contractmanagement.contract.web.CreateContractRequest;
import de.yusufakcay.contractmanagement.contract.web.UpdateContractRequest;

@Mapper(componentModel = "spring")
public interface ContractMapper {

	@Mapping(target = "customerId", source = "customer.id")
	ContractDto toDto(Contract contract);

	@BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
	Contract toEntity(CreateContractRequest request);

	@BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
	void update(UpdateContractRequest request, @MappingTarget Contract contract);
}
