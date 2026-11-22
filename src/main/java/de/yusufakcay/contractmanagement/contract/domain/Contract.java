package de.yusufakcay.contractmanagement.contract.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import de.yusufakcay.contractmanagement.customer.domain.Customer;
import de.yusufakcay.contractmanagement.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
public class Contract extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false, length = 20)
	private String contractNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, updatable = false)
	private Customer customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LineOfBusiness lineOfBusiness;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal premium;

	@Column(nullable = false)
	private LocalDate startDate;

	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContractStatus status;
}
