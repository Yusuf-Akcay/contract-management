package de.yusufakcay.contractmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthProbesIT {

	@Autowired
	private MockMvcTester mvc;
	@Autowired
	private ApplicationContext context;
	@MockitoBean
	private RabbitTemplate rabbitTemplate;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void probesAreUpWithoutToken() {
		for (String path : new String[] { "/actuator/health/liveness", "/actuator/health/readiness", "/livez", "/readyz" }) {
			var result = mvc.get().uri(path).exchange();
			assertThat(result).as(path).hasStatusOk();
			assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("UP");
		}
	}

	@Test
	void readinessReportsOutOfServiceWhenTrafficIsRefused() {
		AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
		try {
			assertThat(mvc.get().uri("/readyz")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
			assertThat(mvc.get().uri("/livez")).hasStatusOk();
		} finally {
			AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
		}
	}
}
