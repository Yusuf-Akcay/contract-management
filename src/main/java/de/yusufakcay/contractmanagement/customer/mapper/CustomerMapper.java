package de.yusufakcay.contractmanagement.customer.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import de.yusufakcay.contractmanagement.customer.domain.Customer;
import de.yusufakcay.contractmanagement.customer.web.CreateCustomerRequest;
import de.yusufakcay.contractmanagement.customer.web.CustomerDto;
import de.yusufakcay.contractmanagement.customer.web.UpdateCustomerRequest;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

	CustomerDto toDto(Customer customer);

	@BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
	Customer toEntity(CreateCustomerRequest request);

	@BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
	void update(UpdateCustomerRequest request, @MappingTarget Customer customer);
}
