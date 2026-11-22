package de.yusufakcay.contractmanagement.customer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import de.yusufakcay.contractmanagement.contract.service.ContractService;
import de.yusufakcay.contractmanagement.customer.service.CustomerService;
import de.yusufakcay.contractmanagement.shared.config.SecurityConfig;
import de.yusufakcay.contractmanagement.shared.security.KeycloakJwtAuthenticationConverter;

@WebMvcTest(CustomerController.class)
@Import({ SecurityConfig.class, KeycloakJwtAuthenticationConverter.class })
class CustomerControllerTest {

	@Autowired
	private MockMvcTester mvc;
	@MockitoBean
	private CustomerService service;
	@MockitoBean
	private ContractService contractService;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void invalidCustomerReturnsFieldViolations() {
		var result = mvc.post().uri("/api/customers")
			.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_contract-write")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"customerNumber":"123","firstName":"","lastName":"Muster","dateOfBirth":"2999-01-01","email":"no-mail"}
				""")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
			.containsExactlyInAnyOrder("customerNumber", "firstName", "dateOfBirth", "email");
	}

	@Test
	void tokenWithoutRolesCannotRead() {
		assertThat(mvc.get().uri("/api/customers").with(jwt())).hasStatus(HttpStatus.FORBIDDEN);
	}
}
