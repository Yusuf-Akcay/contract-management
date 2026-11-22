package de.yusufakcay.contractmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import com.jayway.jsonpath.JsonPath;
import de.yusufakcay.contractmanagement.contract.event.ContractCreatedEvent;
import de.yusufakcay.contractmanagement.contract.mcp.ContractMcpTools;
import jakarta.validation.ConstraintViolationException;

/** Full context on PostgreSQL (Testcontainers): REST → service → JPA → event after commit → RabbitTemplate (mocked). */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContractCreationIT {

	@Autowired
	private MockMvcTester mvc;
	@Autowired
	private ContractMcpTools mcpTools;
	@MockitoBean
	private RabbitTemplate rabbitTemplate;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void creatingCustomerAndContractPublishesEventAndIsVisibleViaMcp() throws Exception {
		var customer = mvc.post().uri("/api/customers").with(clerk()).contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"customerNumber":"C-555001","firstName":"Erika","lastName":"Mustermann","dateOfBirth":"1985-04-12"}
				""").exchange();
		assertThat(customer).hasStatus(HttpStatus.CREATED);
		assertThat(customer).bodyJson().extractingPath("$.createdBy").isEqualTo("clerk");
		Integer customerId = JsonPath.read(customer.getResponse().getContentAsString(), "$.id");

		var contract = mvc.post().uri("/api/contracts").with(clerk()).contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"contractNumber":"P-55500001","customerId":%d,"lineOfBusiness":"BUILDING","premium":31.20,"startDate":"2026-02-01"}
				""".formatted(customerId)).exchange();
		assertThat(contract).hasStatus(HttpStatus.CREATED);
		assertThat(contract).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");

		ArgumentCaptor<ContractCreatedEvent> event = ArgumentCaptor.forClass(ContractCreatedEvent.class);
		then(rabbitTemplate).should().convertAndSend(eq("contract.events"), eq("contract.created"), event.capture());
		assertThat(event.getValue().contractNumber()).isEqualTo("P-55500001");
		assertThat(event.getValue().customerNumber()).isEqualTo("C-555001");

		assertThat(mvc.post().uri("/api/contracts").with(clerk()).contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"contractNumber":"P-55500001","customerId":%d,"lineOfBusiness":"MOTOR","premium":10,"startDate":"2026-02-01"}
				""".formatted(customerId))).hasStatus(HttpStatus.CONFLICT);

		assertThat(mcpTools.contractsOfCustomer("C-555001")).singleElement()
			.satisfies(c -> assertThat(c.contractNumber()).isEqualTo("P-55500001"));
	}

	@Test
	void mcpEndpointRequiresReadRole() {
		assertThat(mvc.post().uri("/mcp").contentType(MediaType.APPLICATION_JSON).content("{}")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.post().uri("/mcp").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void mcpToolsRejectBlankParameters() {
		assertThatThrownBy(() -> mcpTools.contractDetails(" ")).isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> mcpTools.contractsOfCustomer(null)).isInstanceOf(ConstraintViolationException.class);
	}

	private static JwtRequestPostProcessor clerk() {
		return jwt().jwt(j -> j.claim("preferred_username", "clerk"))
			.authorities(new SimpleGrantedAuthority("ROLE_contract-read"), new SimpleGrantedAuthority("ROLE_contract-write"));
	}
}
