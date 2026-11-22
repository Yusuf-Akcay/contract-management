package de.yusufakcay.contractmanagement.contract.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import de.yusufakcay.contractmanagement.contract.domain.ContractFilter;
import de.yusufakcay.contractmanagement.contract.domain.ContractStatus;
import de.yusufakcay.contractmanagement.contract.domain.LineOfBusiness;
import de.yusufakcay.contractmanagement.contract.service.ContractService;
import de.yusufakcay.contractmanagement.shared.config.SecurityConfig;
import de.yusufakcay.contractmanagement.shared.security.KeycloakJwtAuthenticationConverter;
import de.yusufakcay.contractmanagement.shared.web.NotFoundException;

@WebMvcTest(ContractController.class)
@Import({ SecurityConfig.class, KeycloakJwtAuthenticationConverter.class })
class ContractControllerTest {

	private static final String VALID = """
		{"contractNumber":"P-20260042","customerId":1,"lineOfBusiness":"HOUSEHOLD","premium":12.90,"startDate":"2026-01-01"}
		""";

	@Autowired
	private MockMvcTester mvc;
	@MockitoBean
	private ContractService service;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void withoutTokenIs401() {
		assertThat(mvc.get().uri("/api/contracts")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void readerSeesPageWithFilter() {
		given(service.search(eq(new ContractFilter(LineOfBusiness.MOTOR, null)), any(Pageable.class)))
			.willAnswer(inv -> new PageImpl<>(List.of(dto()), inv.getArgument(1), 1));

		var result = mvc.get().uri("/api/contracts?lineOfBusiness=MOTOR&size=5").with(reader()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.content[0].contractNumber").isEqualTo("P-20260042");
		assertThat(result).bodyJson().extractingPath("$.page.size").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
	}

	@Test
	void readerCannotCreate() {
		assertThat(mvc.post().uri("/api/contracts").with(reader())
			.contentType(MediaType.APPLICATION_JSON).content(VALID)).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void writerCreatesWithLocation() {
		given(service.create(any())).willReturn(dto());

		var result = mvc.post().uri("/api/contracts").with(writer())
			.contentType(MediaType.APPLICATION_JSON).content(VALID).exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).headers().hasValue("Location", "http://localhost/api/contracts/42");
	}

	@Test
	void invalidRequestReturnsProblemDetailWithFieldViolations() {
		var result = mvc.post().uri("/api/contracts").with(writer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"contractNumber":"X1","customerId":1,"lineOfBusiness":"MOTOR","premium":-5,"startDate":"2026-01-01","endDate":"2025-01-01"}
				""")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(400);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
			.containsExactlyInAnyOrder("contractNumber", "premium", "endDate");
	}

	@Test
	void invalidIdInPathIs400() {
		var result = mvc.get().uri("/api/contracts/0").with(reader()).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("id");
	}

	@Test
	void unknownContractIs404() {
		given(service.get(99L)).willThrow(new NotFoundException("Contract 99 not found"));

		var result = mvc.get().uri("/api/contracts/99").with(reader()).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Contract 99 not found");
	}

	@Test
	void versionConflictIs409() {
		given(service.update(eq(42L), any())).willThrow(new ObjectOptimisticLockingFailureException("Contract", 42L));

		var result = mvc.put().uri("/api/contracts/42").with(writer())
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"premium":13.50,"status":"ACTIVE","version":0}
				""").exchange();

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
	}

	private static JwtRequestPostProcessor reader() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_contract-read"));
	}

	private static JwtRequestPostProcessor writer() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_contract-read"), new SimpleGrantedAuthority("ROLE_contract-write"));
	}

	private static ContractDto dto() {
		return new ContractDto(42L, "P-20260042", 1L, LineOfBusiness.MOTOR, new BigDecimal("12.90"), LocalDate.of(2026, 1, 1), null,
			ContractStatus.ACTIVE, 0L, null, "clerk", null, "clerk");
	}
}
